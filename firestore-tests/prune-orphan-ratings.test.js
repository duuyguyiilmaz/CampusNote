import { initializeApp, deleteApp } from "firebase-admin/app";
import { Firestore } from "firebase-admin/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, test } from "vitest";
import { noteIdOf, prune } from "./prune-orphan-ratings.js";

// The emulator is already running (npm test wraps the suite in emulators:exec), and
// the admin SDK talks to it as long as this is set before it connects.
process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";

const PROJECT = "campusnote-rules-test";

let app;
let firestore;

/** Swallows the script's progress output so the test run stays readable. */
const quiet = () => {};

beforeAll(() => {
  app = initializeApp({ projectId: PROJECT }, "prune-ratings-test");
  firestore = new Firestore({ projectId: PROJECT });
});

afterAll(async () => {
  await deleteApp(app);
});

beforeEach(async () => {
  for (const name of ["notes", "ratings"]) {
    const existing = await firestore.collection(name).get();
    await Promise.all(existing.docs.map((doc) => doc.ref.delete()));
  }
});

const seedNote = (id) =>
  firestore.collection("notes").doc(id).set({
    title: "Ders notu",
    department: "Bilgisayar Mühendisliği",
    uploaderUid: "uid-1",
  });

const seedRating = (uid, noteId, extra = {}) =>
  firestore
    .collection("ratings")
    .doc(`${uid}_${noteId}`)
    .set({ uid, noteId, rating: 4, ...extra });

describe("reading a rating's note id", () => {
  test("the field is used, not the document id", () => {
    // The id is `<uid>_<noteId>` and a uid may itself contain an underscore, so
    // splitting it is ambiguous in exactly the case that matters.
    expect(noteIdOf({ uid: "a_b", noteId: "note-1", rating: 4 })).toBe("note-1");
  });

  test("a rating with no usable noteId reports none", () => {
    expect(noteIdOf({ uid: "a", rating: 4 })).toBeNull();
    expect(noteIdOf({ uid: "a", noteId: "", rating: 4 })).toBeNull();
    expect(noteIdOf({ uid: "a", noteId: 7, rating: 4 })).toBeNull();
  });
});

describe("running against Firestore", () => {
  test("a dry run reports without deleting", async () => {
    await seedRating("uid-2", "gone");

    const result = await prune(firestore, { log: quiet });

    expect(result.counts.orphan).toBe(1);
    expect(result.deleted).toBe(0);
    const after = await firestore.collection("ratings").get();
    expect(after.size).toBe(1);
  });

  /**
   * The assertion the script exists for: a vote whose note was deleted before the
   * repository started removing them is unreachable by any client — the rule calls
   * `get()` on a note that is gone — so this is the only thing that can clear it.
   */
  test("applying deletes the orphan and keeps the live vote", async () => {
    await seedNote("note-1");
    await seedRating("uid-2", "note-1");
    await seedRating("uid-3", "gone");

    const result = await prune(firestore, { apply: true, log: quiet });

    expect(result.counts).toMatchObject({ live: 1, orphan: 1 });
    expect(result.deleted).toBe(1);

    const remaining = await firestore.collection("ratings").get();
    expect(remaining.docs.map((doc) => doc.id)).toEqual(["uid-2_note-1"]);
  });

  test("a rating with no noteId is reported and left alone", async () => {
    await firestore.collection("ratings").doc("broken").set({ uid: "uid-2", rating: 4 });

    const result = await prune(firestore, { apply: true, log: quiet });

    expect(result.counts.unreadable).toBe(1);
    expect(result.deleted).toBe(0);
    const after = await firestore.collection("ratings").doc("broken").get();
    expect(after.exists).toBe(true);
  });

  test("re-running finds nothing left to do", async () => {
    await seedNote("note-1");
    await seedRating("uid-2", "note-1");
    await seedRating("uid-3", "gone");

    await prune(firestore, { apply: true, log: quiet });
    const second = await prune(firestore, { apply: true, log: quiet });

    expect(second.counts.orphan).toBe(0);
    expect(second.deleted).toBe(0);
  });
});
