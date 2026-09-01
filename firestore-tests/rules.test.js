import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
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
// The rule derives the only permitted name from the session's own address.
const OWNER_NAME = "owner";
const RATER_NAME = "rater";
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
      ...noteFields({ uploaderUid: OWNER, uploaderName: OWNER_NAME }),
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
    uploaderName: RATER_NAME,
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
  test("a user may read their own profile", async () => {
    await assertSucceeds(getDoc(doc(rater, "users", RATER)));
  });

  /**
   * Every getUser call in the app passes the caller's own uid — the leaderboard and
   * note detail take the uploader's name from the note, not from a profile. The
   * open read rule was an permission nobody used, and it handed any signed-in
   * student every address and department in the university in one query.
   */
  test("a user cannot read someone else's profile", async () => {
    await assertFails(getDoc(doc(rater, "users", OWNER)));
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
  test("a note in the reader's own department can be read", async () => {
    await assertSucceeds(getDoc(doc(rater, "notes", NOTE)));
  });

  /**
   * The app filters every feed and leaderboard query by department, but that was a
   * rendering decision, not a boundary: a client talking to Firestore directly could
   * drop the filter and pull every note in the university. Rules are evaluated
   * against each document a query returns and the whole query fails if any is
   * denied, so the filter is now the condition for the query to run at all.
   */
  test("a note in another department cannot be read", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "notes", "other-dept"), {
        // Başkasının notu olmalı: kendi notun bölüm değişse de okunabilir kalıyor,
        // o yüzden aynı fixture bu testi ölçmezdi.
        ...noteFields({
          department: "Makine Mühendisliği",
          uploaderUid: OWNER,
          uploaderName: OWNER_NAME,
        }),
        createdAt: 1,
      });
    });

    await assertFails(getDoc(doc(rater, "notes", "other-dept")));
  });

  test("a query without the department filter is refused", async () => {
    await assertFails(getDocs(collection(rater, "notes")));
  });

  test("a query filtered to the reader's own department is allowed", async () => {
    await assertSucceeds(
      getDocs(query(collection(rater, "notes"), where("department", "==", DEPARTMENT))),
    );
  });

  /**
   * The query the contribution gate and the profile screen actually build. It
   * carries no department filter, and a rule with only the department branch
   * rejected it outright — Firestore evaluates a list against the query, not the
   * documents it would return, so "these happen to be my own notes in my own
   * department" is not something it can work out. The feed opened empty in
   * production before this branch existed.
   */
  test("a query for the reader's own notes is allowed without a department filter", async () => {
    await assertSucceeds(
      getDocs(query(collection(rater, "notes"), where("uploaderUid", "==", RATER))),
    );
  });

  test("a query for someone else's notes is refused", async () => {
    await assertFails(
      getDocs(query(collection(rater, "notes"), where("uploaderUid", "==", OWNER))),
    );
  });

  test("a query filtered to another department is refused", async () => {
    await assertFails(
      getDocs(
        query(collection(rater, "notes"), where("department", "==", "Makine Mühendisliği")),
      ),
    );
  });

  test("uploaderUid must be the caller's own uid", async () => {
    await assertSucceeds(
      setDoc(doc(rater, "notes", "own-note"), noteFields()),
    );
    await assertFails(
      setDoc(
        doc(rater, "notes", "forged-note"),
        noteFields({ uploaderUid: OWNER, uploaderName: OWNER_NAME }),
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

    test("uploaderName cannot be impersonated", async () => {
      await assertFails(
        setDoc(
          doc(rater, "notes", "forged-name"),
          noteFields({ uploaderName: OWNER_NAME }),
        ),
      );
    });

    /**
     * The address itself is no longer stored anywhere on a note. Masking it at
     * render time hid it from the screen, not from the document — every student in
     * the department could read the raw field.
     */
    test("an email address cannot be written onto a note", async () => {
      await assertFails(
        setDoc(
          doc(rater, "notes", "with-email"),
          noteFields({ uploaderEmail: RATER_EMAIL }),
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
  test("a file on a note in the reader's own department can be read", async () => {
    await assertSucceeds(getDoc(doc(rater, "notes", NOTE, "content", "file")));
  });

  // Without this the department boundary could be walked around through the file
  // path: the note stays unreadable while its contents do not.
  test("a file on another department's note cannot be read", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, "notes", "other-dept"), {
        // Başkasının notu olmalı: kendi notun bölüm değişse de okunabilir kalıyor,
        // o yüzden aynı fixture bu testi ölçmezdi.
        ...noteFields({
          department: "Makine Mühendisliği",
          uploaderUid: OWNER,
          uploaderName: OWNER_NAME,
        }),
        createdAt: 1,
      });
      await setDoc(doc(db, "notes", "other-dept", "content", "file"), {
        fileData: "ZmFrZQ==",
      });
    });

    await assertFails(getDoc(doc(rater, "notes", "other-dept", "content", "file")));
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

  /**
   * This used to be asserted the other way round, as a gap the rules documented and
   * accepted: a batch supposedly could not verify the parent note's owner because
   * `get()` does not see an uncommitted document. The `get()` half was right and the
   * conclusion wrong — `getAfter()` reads the post-commit state and the rating rule
   * was already relying on it. Any signed-in user could hang a content document off
   * somebody else's note; overwriting an existing file was blocked, but a note that
   * had no file yet would happily take one.
   */
  test("someone else cannot attach content to a note they do not own", async () => {
    await assertFails(
      setDoc(doc(rater, "notes", NOTE, "content", "extra"), {
        fileData: "ZmFrZQ==",
      }),
    );
  });

  test("the uploader may attach a file to their own existing note", async () => {
    // getAfter() returns the current note when the write does not touch it, so the
    // rule covers adding a file later as well as the batched upload.
    await assertSucceeds(
      setDoc(doc(owner, "notes", NOTE, "content", "extra"), {
        fileData: "ZmFrZQ==",
      }),
    );
  });

  test("a content document carrying anything but fileData is refused", async () => {
    await assertFails(
      setDoc(doc(owner, "notes", NOTE, "content", "extra"), {
        fileData: "ZmFrZQ==",
        uploaderUid: RATER,
      }),
    );
    await assertFails(
      setDoc(doc(owner, "notes", NOTE, "content", "extra"), { fileData: 42 }),
    );
  });

  test("a batch cannot smuggle content onto someone else's note", async () => {
    // The batch writes a legitimate note of the caller's own *and* a file on the
    // owner's note. Each write is evaluated separately, so the second must fail and
    // take the whole commit with it.
    const batch = writeBatch(rater);
    batch.set(doc(rater, "notes", "smuggle"), noteFields());
    batch.set(doc(rater, "notes", NOTE, "content", "file"), {
      fileData: "a290dQ==",
    });
    await assertFails(batch.commit());
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

  /** Seeds a vote without going through the rules, so a broken rule shows up as a
   * failed assertion rather than a failed setup. */
  const seedVote = async (vote = validVote, id = `${RATER}_${NOTE}`) => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "ratings", id), vote);
    });
  };

  /**
   * A vote on its own must stay put. Deleting one does not lower the note's total,
   * but it does hand the voter a second first-time vote — the rule would count it
   * afresh and the tally would climb. Free deletion was the short path to a padded
   * score, which is why the rule used to be a flat `if false`.
   */
  test("a vote cannot be deleted on its own", async () => {
    await seedVote();
    await assertFails(deleteDoc(doc(rater, "ratings", `${RATER}_${NOTE}`)));
  });

  test("the note's owner cannot delete a vote while the note stays", async () => {
    await seedVote();
    await assertFails(deleteDoc(doc(owner, "ratings", `${RATER}_${NOTE}`)));
  });

  /**
   * The pairing the repository now performs. `ratings` is a top-level collection
   * rather than a subcollection of the note, so deleting a note never touched its
   * votes and the documents accumulated with nothing able to reach them.
   */
  test("the owner may delete a vote in the same commit as its note", async () => {
    await seedVote();

    const batch = writeBatch(owner);
    batch.delete(doc(owner, "ratings", `${RATER}_${NOTE}`));
    batch.delete(doc(owner, "notes", NOTE));
    await assertSucceeds(batch.commit());
  });

  test("a stranger cannot delete votes by deleting a note they do not own", async () => {
    await seedVote();

    const batch = writeBatch(rater);
    batch.delete(doc(rater, "ratings", `${RATER}_${NOTE}`));
    batch.delete(doc(rater, "notes", NOTE));
    await assertFails(batch.commit());
  });

  /**
   * The gap the rule deliberately leaves: once a note is gone, `get()` on it fails
   * and the delete is refused. Votes orphaned before this change are cleaned up by
   * `prune-orphan-ratings.js`, not by a client.
   */
  test("a vote whose note is already gone cannot be deleted by a client", async () => {
    await seedVote();
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await deleteDoc(doc(context.firestore(), "notes", NOTE));
    });

    await assertFails(deleteDoc(doc(owner, "ratings", `${RATER}_${NOTE}`)));
  });
});

describe("unmatched paths", () => {
  test("a collection with no rule is closed even to signed-in users", async () => {
    // Adding a collection to the app without adding a rule for it must fail closed.
    await assertFails(getDoc(doc(rater, "unruled", "anything")));
    await assertFails(setDoc(doc(rater, "unruled", "anything"), { note: NOTE }));
  });
});
