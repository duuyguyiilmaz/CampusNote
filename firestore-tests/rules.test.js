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
  serverTimestamp,
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
const OWNER_EMAIL = "owner@ogr.akdeniz.edu.tr";
const RATER_EMAIL = "rater@ogr.akdeniz.edu.tr";
const DEPARTMENT = "Bilgisayar Mühendisliği";

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

  // The note-create rule compares uploaderEmail against the session's token, so
  // the contexts have to carry an email claim the way a real sign-in does.
  owner = testEnv.authenticatedContext(OWNER, { email: OWNER_EMAIL }).firestore();
  rater = testEnv.authenticatedContext(RATER, { email: RATER_EMAIL }).firestore();
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
      email: OWNER_EMAIL,
      department: DEPARTMENT,
      createdAt: 1,
    });
    await setDoc(doc(db, "users", RATER), {
      id: RATER,
      email: RATER_EMAIL,
      department: DEPARTMENT,
      createdAt: 1,
    });
    await setDoc(doc(db, "notes", NOTE), {
      ...noteFields({ uploaderUid: OWNER, uploaderEmail: OWNER_EMAIL }),
      createdAt: 1,
    });
    await setDoc(doc(db, "notes", NOTE, "content", "file"), {
      fileData: "ZmFrZQ==",
    });
  });
});

/**
 * A note document in exactly the shape the create rule now demands.
 *
 * The rule pins the field set with hasOnly + hasAll, so tests can no longer write
 * an approximation: a missing field is as fatal as a forged one. Keeping the shape
 * in one place is what lets each test override the single field it is about and
 * stay readable.
 *
 * `createdAt` defaults to a server timestamp because the rule requires
 * request.time — a client-chosen date is one of the things being rejected.
 */
function noteFields(overrides = {}) {
  return {
    course: "Veri Yapıları",
    title: "Ders notu",
    description: "",
    tag: "",
    department: DEPARTMENT,
    uploaderUid: RATER,
    uploaderEmail: RATER_EMAIL,
    createdAt: serverTimestamp(),
    ratingSum: 0,
    ratingCount: 0,
    fileName: "notes.pdf",
    fileType: "pdf",
    fileSize: 1024,
    ...overrides,
  };
}

/**
 * Bir oyu uygulamanın yazdığı gibi yazar: notun toplamları ve oy dokümanı tek
 * atomik commit'te. Kural, notun yeni toplamlarını aynı commit'teki oy
 * dokümanından doğruluyor, o yüzden ikisini ayırmak testin anlamını kaybettirir.
 *
 * @param db oy veren bağlam
 * @param uid oy dokümanının sahibi olarak yazılacak uid — kurala göre çağıranınki olmalı
 * @param totals notun yazılacak yeni değerleri; `extra` ile fazladan alan sızdırılır
 */
function vote(db, uid, noteId, rating, { sum, count, extra = {} }) {
  const batch = writeBatch(db);
  batch.update(doc(db, "notes", noteId), {
    ratingSum: sum,
    ratingCount: count,
    ...extra,
  });
  batch.set(doc(db, "ratings", `${uid}_${noteId}`), {
    uid,
    noteId,
    rating,
  });
  return batch.commit();
}

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
        createdAt: 1,
      }),
    );
  });

  test("a user cannot create a profile under someone else's uid", async () => {
    await testEnv.clearFirestore();
    await assertFails(setDoc(doc(rater, "users", OWNER), { id: OWNER }));
  });

  test("a create whose id field disagrees with the document id is rejected", async () => {
    await testEnv.clearFirestore();
    await assertFails(setDoc(doc(rater, "users", RATER), { id: OWNER }));
  });

  test("nobody may update a profile, not even its owner", async () => {
    // The app only ever creates this document. Leaving update open is what forced
    // the old rule to permit writing `points` — the field that made self-awarding
    // a one-line request.
    await assertFails(
      updateDoc(doc(rater, "users", RATER), { department: "Matematik" }),
    );
    await assertFails(
      updateDoc(doc(owner, "users", OWNER), { department: "Matematik" }),
    );
  });

  test("a user cannot award themselves points", async () => {
    // This was the hole: `allow update: if isOwner(userId)` let a user write any
    // field of their own document, so `points: 999999` needed no rating at all.
    await assertFails(
      updateDoc(doc(rater, "users", RATER), { points: 999999 }),
    );
  });

  test("a user cannot write points onto anyone else either", async () => {
    await assertFails(updateDoc(doc(rater, "users", OWNER), { points: 14 }));
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
      setDoc(doc(rater, "notes", "own-note"), noteFields()),
    );
    await assertFails(
      setDoc(
        doc(rater, "notes", "forged-note"),
        noteFields({ uploaderUid: OWNER, uploaderEmail: OWNER_EMAIL }),
      ),
    );
  });

  /**
   * The create rule used to check only uploaderUid and the two score fields, which
   * left everything else — the department a note lands in, the email shown beside
   * it, and any field at all that nobody expected — writable by a client that skips
   * the Android UI. The form validation in the app is not a security boundary: an
   * attacker was never obliged to use the app.
   */
  describe("note creation is pinned to an exact shape", () => {
    test("an unexpected field is refused", async () => {
      await assertFails(
        setDoc(
          doc(rater, "notes", "extra-field"),
          noteFields({ isFeatured: true }),
        ),
      );
    });

    test("a missing field is refused", async () => {
      const { fileSize, ...withoutFileSize } = noteFields();
      await assertFails(setDoc(doc(rater, "notes", "missing-field"), withoutFileSize));
    });

    /**
     * The average is the field this change removed. It can no longer be verified
     * against anything — rules do integer division, so a stored avgRating could
     * only ever be range-checked, which let a legitimate vote carry a fabricated
     * average to the screen. The value shown is now ratingSum itself, and the old
     * field is refused so it cannot creep back.
     */
    test("avgRating cannot be written back", async () => {
      await assertFails(
        setDoc(doc(rater, "notes", "with-average"), noteFields({ avgRating: 5 })),
      );
    });

    test("a note cannot be filed under another department", async () => {
      await assertFails(
        setDoc(
          doc(rater, "notes", "wrong-dept"),
          noteFields({ department: "Makine Mühendisliği" }),
        ),
      );
    });

    test("uploaderEmail cannot be impersonated", async () => {
      await assertFails(
        setDoc(
          doc(rater, "notes", "forged-email"),
          noteFields({ uploaderEmail: OWNER_EMAIL }),
        ),
      );
    });

    test("createdAt must be the server's clock, not the client's", async () => {
      // A client-chosen date would let a note pin itself to the top of a feed that
      // orders on createdAt, or hide itself at the bottom.
      await assertFails(
        setDoc(doc(rater, "notes", "backdated"), noteFields({ createdAt: 1 })),
      );
    });

    test("a field of the wrong type is refused", async () => {
      await assertFails(
        setDoc(doc(rater, "notes", "bad-sum"), noteFields({ ratingSum: "0" })),
      );
      await assertFails(
        setDoc(doc(rater, "notes", "bad-type"), noteFields({ fileType: "exe" })),
      );
    });

    test("an empty title or an oversized one is refused", async () => {
      await assertFails(
        setDoc(doc(rater, "notes", "no-title"), noteFields({ title: "" })),
      );
      await assertFails(
        setDoc(
          doc(rater, "notes", "huge-title"),
          noteFields({ title: "a".repeat(201) }),
        ),
      );
    });
  });

  test("the uploader cannot move their note to another department while editing", async () => {
    await assertFails(
      updateDoc(doc(owner, "notes", NOTE), { department: "Makine Mühendisliği" }),
    );
  });

  test("a note cannot be born with a score", async () => {
    await assertFails(
      setDoc(
        doc(rater, "notes", "head-start"),
        noteFields({ ratingSum: 500, ratingCount: 100 }),
      ),
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

  test("the uploader cannot raise their own note's score while editing it", async () => {
    // The shortest route to the top of the leaderboard used to be editing your
    // own note, since ownership alone authorised the whole document.
    await assertFails(
      updateDoc(doc(owner, "notes", NOTE), {
        title: "Güncellenmiş başlık",
        ratingSum: 500,
      }),
    );
  });

  test("a vote and the totals it implies are accepted together", async () => {
    await assertSucceeds(vote(rater, RATER, NOTE, 4, { sum: 4, count: 1 }));
  });

  test("totals cannot move without a vote to justify them", async () => {
    // This is the whole point of the getAfter() check: the note update is only
    // authorised by a rating document written in the same commit.
    await assertFails(
      updateDoc(doc(rater, "notes", NOTE), {
        ratingSum: 500,
        ratingCount: 100,
      }),
    );
  });

  test("the totals must match the vote, not exceed it", async () => {
    // Every value here stays inside the range checks, so the only thing that can
    // reject them is the arithmetic: a 1-star vote claiming a sum of 5, and a
    // 5-star vote claiming a second voter that does not exist.
    await assertFails(vote(rater, RATER, NOTE, 1, { sum: 5, count: 1 }));
    await assertFails(vote(rater, RATER, NOTE, 5, { sum: 5, count: 2 }));
    await assertFails(vote(rater, RATER, NOTE, 3, { sum: 3, count: 0 }));
  });

  test("changing a vote moves the sum by the difference and leaves the count", async () => {
    await assertSucceeds(vote(rater, RATER, NOTE, 2, { sum: 2, count: 1 }));
    await assertSucceeds(vote(rater, RATER, NOTE, 5, { sum: 5, count: 1 }));
  });

  test("a changed vote cannot be counted twice", async () => {
    await assertSucceeds(vote(rater, RATER, NOTE, 2, { sum: 2, count: 1 }));
    await assertFails(vote(rater, RATER, NOTE, 5, { sum: 5, count: 2 }));
  });

  test("nobody can rate their own note", async () => {
    await assertFails(vote(owner, OWNER, NOTE, 5, { sum: 5, count: 1 }));
  });

  test("a vote cannot be laundered through someone else's rating document", async () => {
    // Writing the totals while pointing getAfter at a rating the caller does not
    // own: the rating rule rejects the forged document, so the batch fails whole.
    await assertFails(vote(rater, OWNER, NOTE, 5, { sum: 5, count: 1 }));
  });

  test("a rating update cannot carry a metadata change with it", async () => {
    await assertFails(
      vote(rater, RATER, NOTE, 4, { sum: 4, count: 1, extra: { title: "Ele geçirildi" } }),
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
    batch.set(doc(rater, "notes", "batched"), noteFields());
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
    // Adding a collection to the app without adding a rule for it must fail closed.
    await assertFails(getDoc(doc(rater, "unruled", "anything")));
    await assertFails(setDoc(doc(rater, "unruled", "anything"), { note: NOTE }));
  });
});
