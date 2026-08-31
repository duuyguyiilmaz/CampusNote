import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, test } from "vitest";

// Two signed-in users and one signed-out visitor cover every branch in the rules:
// most of them turn on "is this the owner", and the rest on "is anyone signed in".
const OWNER = "owner-uid";
const RATER = "rater-uid";
const NOTE = "note-1";

let testEnv;
let owner;
let rater;
let anonymous;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "campusnote-rules-test",
    firestore: {
      rules: readFileSync("../firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });

  owner = testEnv.authenticatedContext(OWNER).firestore();
  rater = testEnv.authenticatedContext(RATER).firestore();
  anonymous = testEnv.unauthenticatedContext().firestore();
});

afterAll(async () => {
  await testEnv.cleanup();
});

// Seeded through withSecurityRulesDisabled so the fixtures themselves never
// depend on the rules being tested — otherwise a broken rule would show up as a
// setup failure instead of a failed assertion.
beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "users", OWNER), {
      id: OWNER,
      email: "owner@ogr.akdeniz.edu.tr",
      department: "Bilgisayar Mühendisliği",
      points: 10,
      createdAt: 1,
    });
    await setDoc(doc(db, "users", RATER), {
      id: RATER,
      email: "rater@ogr.akdeniz.edu.tr",
      department: "Bilgisayar Mühendisliği",
      points: 0,
      createdAt: 1,
    });
    await setDoc(doc(db, "notes", NOTE), {
      title: "Ders notu",
      desc: "",
      authorEmail: "owner@ogr.akdeniz.edu.tr",
      department: "Bilgisayar Mühendisliği",
      timeMills: 1,
      uploaderUid: OWNER,
      avgRating: 0,
      ratingCount: 0,
      ratingSum: 0,
      fileName: "notes.pdf",
      fileType: "pdf",
    });
    await setDoc(doc(db, "notes", NOTE, "content", "file"), {
      fileData: "ZmFrZQ==",
    });
  });
});

describe("signed-out access", () => {
  // Every collection matches on isSignedIn() first, so this is the one guard
  // that has to hold everywhere: an unauthenticated client reads nothing.
  test("cannot read users, notes, note content or ratings", async () => {
    await assertFails(getDoc(doc(anonymous, "users", OWNER)));
    await assertFails(getDoc(doc(anonymous, "notes", NOTE)));
    await assertFails(getDoc(doc(anonymous, "notes", NOTE, "content", "file")));
    await assertFails(getDoc(doc(anonymous, "ratings", `${RATER}_${NOTE}`)));
  });

  test("cannot create a note", async () => {
    await assertFails(
      setDoc(doc(anonymous, "notes", "forged"), { uploaderUid: OWNER }),
    );
  });
});

describe("users", () => {
  test("a signed-in user may read any profile", async () => {
    // The leaderboard and note detail screens show other people's profiles.
    await assertSucceeds(getDoc(doc(rater, "users", OWNER)));
  });

  test("registration writes a profile whose id matches the caller", async () => {
    await testEnv.clearFirestore();
    await assertSucceeds(
      setDoc(doc(rater, "users", RATER), {
        id: RATER,
        email: "rater@ogr.akdeniz.edu.tr",
        department: "Bilgisayar Mühendisliği",
        points: 0,
        createdAt: 1,
      }),
    );
  });

  test("a user cannot create a profile under someone else's uid", async () => {
    await testEnv.clearFirestore();
    await assertFails(
      setDoc(doc(rater, "users", OWNER), { id: OWNER, points: 0 }),
    );
  });

  test("a create whose id field disagrees with the document id is rejected", async () => {
    await testEnv.clearFirestore();
    await assertFails(
      setDoc(doc(rater, "users", RATER), { id: OWNER, points: 0 }),
    );
  });

  test("a user may edit their own profile freely", async () => {
    await assertSucceeds(
      updateDoc(doc(rater, "users", RATER), { department: "Matematik" }),
    );
  });

  test("rating another user's note may raise only their points", async () => {
    // The client-side rating flow needs this: it writes the owner's new total.
    await assertSucceeds(
      updateDoc(doc(rater, "users", OWNER), { points: 14 }),
    );
  });

  test("a points write cannot smuggle another field along with it", async () => {
    await assertFails(
      updateDoc(doc(rater, "users", OWNER), {
        points: 14,
        email: "attacker@example.com",
      }),
    );
  });

  test("points must be a non-negative integer", async () => {
    await assertFails(updateDoc(doc(rater, "users", OWNER), { points: -1 }));
    await assertFails(updateDoc(doc(rater, "users", OWNER), { points: 4.5 }));
    await assertFails(updateDoc(doc(rater, "users", OWNER), { points: "9" }));
  });

  test("a stranger cannot rewrite another profile's department", async () => {
    await assertFails(
      updateDoc(doc(rater, "users", OWNER), { department: "Matematik" }),
    );
  });

  test("nobody may delete a profile, not even its owner", async () => {
    await assertFails(deleteDoc(doc(owner, "users", OWNER)));
  });
});

describe("notes", () => {
  test("any signed-in user may read notes", async () => {
    await assertSucceeds(getDoc(doc(rater, "notes", NOTE)));
  });

  test("uploaderUid must be the caller's own uid", async () => {
    await assertSucceeds(
      setDoc(doc(rater, "notes", "own-note"), {
        title: "Note",
        uploaderUid: RATER,
        department: "Bilgisayar Mühendisliği",
      }),
    );
    await assertFails(
      setDoc(doc(rater, "notes", "forged-note"), {
        title: "Note",
        uploaderUid: OWNER,
        department: "Bilgisayar Mühendisliği",
      }),
    );
  });

  test("the uploader may edit their note's metadata", async () => {
    await assertSucceeds(
      updateDoc(doc(owner, "notes", NOTE), { title: "Güncellenmiş başlık" }),
    );
  });

  test("someone else cannot edit a note's metadata", async () => {
    await assertFails(
      updateDoc(doc(rater, "notes", NOTE), { title: "Ele geçirildi" }),
    );
  });

  test("a rater may update only the three rating fields", async () => {
    await assertSucceeds(
      updateDoc(doc(rater, "notes", NOTE), {
        ratingSum: 4,
        ratingCount: 1,
        avgRating: 4,
      }),
    );
  });

  test("a rating update cannot carry a metadata change with it", async () => {
    await assertFails(
      updateDoc(doc(rater, "notes", NOTE), {
        ratingSum: 4,
        ratingCount: 1,
        avgRating: 4,
        title: "Ele geçirildi",
      }),
    );
  });

  test("rating totals cannot go negative", async () => {
    await assertFails(
      updateDoc(doc(rater, "notes", NOTE), {
        ratingSum: -1,
        ratingCount: 1,
        avgRating: 0,
      }),
    );
    await assertFails(
      updateDoc(doc(rater, "notes", NOTE), {
        ratingSum: 4,
        ratingCount: -1,
        avgRating: 4,
      }),
    );
  });

  test("only the uploader may delete a note", async () => {
    // NoteRepository.deleteNote does not check ownership itself; this rule is
    // the only thing standing between a crafted request and someone else's note.
    await assertFails(deleteDoc(doc(rater, "notes", NOTE)));
    await assertSucceeds(deleteDoc(doc(owner, "notes", NOTE)));
  });
});

describe("note content", () => {
  test("any signed-in user may read the attached file", async () => {
    await assertSucceeds(getDoc(doc(rater, "notes", NOTE, "content", "file")));
  });

  test("upload writes the note and its content in one batch", async () => {
    const batch = writeBatch(rater);
    batch.set(doc(rater, "notes", "batched"), {
      title: "Note",
      uploaderUid: RATER,
      department: "Bilgisayar Mühendisliği",
    });
    batch.set(doc(rater, "notes", "batched", "content", "file"), {
      fileData: "ZmFrZQ==",
    });
    await assertSucceeds(batch.commit());
  });

  test("the uploader may replace and remove their own file", async () => {
    await assertSucceeds(
      setDoc(doc(owner, "notes", NOTE, "content", "file"), {
        fileData: "eWVuaQ==",
      }),
    );
    await assertSucceeds(deleteDoc(doc(owner, "notes", NOTE, "content", "file")));
  });

  test("someone else cannot overwrite or delete the file", async () => {
    await assertFails(
      setDoc(doc(rater, "notes", NOTE, "content", "file"), {
        fileData: "a290dQ==",
      }),
    );
    await assertFails(deleteDoc(doc(rater, "notes", NOTE, "content", "file")));
  });

  test("content creation is open to any signed-in user — a known gap", async () => {
    // Documented in firestore.rules: a batch cannot get() its own uncommitted
    // parent note, so create cannot verify ownership. This test pins the gap in
    // place so that closing it later is a deliberate, visible change.
    await assertSucceeds(
      setDoc(doc(rater, "notes", NOTE, "content", "extra"), {
        fileData: "ZmFrZQ==",
      }),
    );
  });
});

describe("ratings", () => {
  const validVote = { uid: RATER, noteId: NOTE, rating: 4 };

  test("a vote is written under <uid>_<noteId>", async () => {
    await assertSucceeds(
      setDoc(doc(rater, "ratings", `${RATER}_${NOTE}`), validVote),
    );
  });

  test("re-voting replaces the existing document", async () => {
    await assertSucceeds(
      setDoc(doc(rater, "ratings", `${RATER}_${NOTE}`), validVote),
    );
    await assertSucceeds(
      setDoc(doc(rater, "ratings", `${RATER}_${NOTE}`), {
        ...validVote,
        rating: 2,
      }),
    );
  });

  test("a user cannot vote in someone else's name", async () => {
    // The uid field and the document id are checked separately, so both
    // halves of the forgery have to be rejected on their own.
    await assertFails(
      setDoc(doc(rater, "ratings", `${OWNER}_${NOTE}`), {
        ...validVote,
        uid: OWNER,
      }),
    );
    await assertFails(
      setDoc(doc(rater, "ratings", `${OWNER}_${NOTE}`), validVote),
    );
  });

  test("the document id must match the uid and note it claims", async () => {
    await assertFails(
      setDoc(doc(rater, "ratings", `${RATER}_other-note`), validVote),
    );
    await assertFails(setDoc(doc(rater, "ratings", "arbitrary-id"), validVote));
  });

  test("the rating must be an integer between 1 and 5", async () => {
    for (const rating of [0, 6, -1, 3.5, "4"]) {
      await assertFails(
        setDoc(doc(rater, "ratings", `${RATER}_${NOTE}`), {
          ...validVote,
          rating,
        }),
      );
    }
  });

  test("votes cannot be deleted", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), "ratings", `${RATER}_${NOTE}`),
        validVote,
      );
    });
    await assertFails(deleteDoc(doc(rater, "ratings", `${RATER}_${NOTE}`)));
  });
});

describe("unmatched paths", () => {
  test("a collection with no rule is closed even to signed-in users", async () => {
    // README mentions a `reports` collection that was never implemented.
    await assertFails(getDoc(doc(rater, "reports", "anything")));
    await assertFails(setDoc(doc(rater, "reports", "anything"), { note: NOTE }));
  });
});
