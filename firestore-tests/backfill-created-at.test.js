import { initializeApp, deleteApp } from "firebase-admin/app";
import { Firestore, Timestamp } from "firebase-admin/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, test } from "vitest";
import { backfill, normalisedCreatedAt, reasonFor } from "./backfill-created-at.js";

// The emulator is already running (npm test wraps the suite in emulators:exec), and
// the admin SDK talks to it as long as this is set before it connects.
process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";

const PROJECT = "campusnote-rules-test";

let app;
let firestore;

/** Swallows the script's progress output so the test run stays readable. */
const quiet = () => {};

beforeAll(() => {
  app = initializeApp({ projectId: PROJECT }, "backfill-test");
  firestore = new Firestore({ projectId: PROJECT });
});

afterAll(async () => {
  await deleteApp(app);
});

beforeEach(async () => {
  const existing = await firestore.collection("notes").get();
  await Promise.all(existing.docs.map((doc) => doc.ref.delete()));
});

const note = (extra) => ({
  title: "Ders notu",
  department: "Bilgisayar Mühendisliği",
  uploaderUid: "uid-1",
  ...extra,
});

describe("classifying a note", () => {
  test("a proper Timestamp needs no write", () => {
    const data = note({ createdAt: Timestamp.fromMillis(1_700_000_000_000) });

    expect(reasonFor(data)).toBe("ok");
    expect(normalisedCreatedAt(data)).toBeNull();
  });

  test("epoch millis keep their date", () => {
    const millis = 1_700_000_000_000;

    const replacement = normalisedCreatedAt(note({ createdAt: millis }));

    expect(reasonFor(note({ createdAt: millis }))).toBe("number");
    expect(replacement.toMillis()).toBe(millis);
  });

  test("a missing date falls back to updatedAt when there is one", () => {
    const updated = Timestamp.fromMillis(1_600_000_000_000);

    const replacement = normalisedCreatedAt(note({ updatedAt: updated }));

    expect(replacement.toMillis()).toBe(updated.toMillis());
  });

  test("a note with no evidence at all is dated to the epoch, not to now", () => {
    // Dating it "now" would float undated legacy notes to the top of every feed.
    const replacement = normalisedCreatedAt(note({}));

    expect(replacement.toMillis()).toBe(0);
  });
});

describe("running against the emulator", () => {
  test("a dry run writes nothing", async () => {
    await firestore.collection("notes").doc("legacy").set(note({ createdAt: 123 }));

    const result = await backfill(firestore, { apply: false, log: quiet });

    expect(result.written).toBe(0);
    const after = await firestore.collection("notes").doc("legacy").get();
    expect(after.data().createdAt).toBe(123);
  });

  test("applying converts every shape and leaves correct notes alone", async () => {
    const good = Timestamp.fromMillis(1_700_000_000_000);
    await firestore.collection("notes").doc("ok").set(note({ createdAt: good }));
    await firestore.collection("notes").doc("millis").set(note({ createdAt: 1_650_000_000_000 }));
    await firestore.collection("notes").doc("absent").set(note({}));

    const result = await backfill(firestore, { apply: true, log: quiet });

    expect(result.scanned).toBe(3);
    expect(result.written).toBe(2);
    expect(result.counts).toMatchObject({ ok: 1, number: 1, missing: 1 });

    const docs = await firestore.collection("notes").get();
    for (const doc of docs.docs) {
      expect(doc.data().createdAt).toBeInstanceOf(Timestamp);
    }
    const untouched = await firestore.collection("notes").doc("ok").get();
    expect(untouched.data().createdAt.toMillis()).toBe(good.toMillis());
    const converted = await firestore.collection("notes").doc("millis").get();
    expect(converted.data().createdAt.toMillis()).toBe(1_650_000_000_000);
  });

  test("running it twice changes nothing the second time", async () => {
    // The whole point is that this is safe to re-run after a partial failure.
    await firestore.collection("notes").doc("millis").set(note({ createdAt: 1 }));
    await firestore.collection("notes").doc("absent").set(note({}));

    await backfill(firestore, { apply: true, log: quiet });
    const second = await backfill(firestore, { apply: true, log: quiet });

    expect(second.written).toBe(0);
    expect(second.counts.ok).toBe(2);
  });

  test("after the backfill, an ordered query returns every note", async () => {
    // This is the behaviour the whole migration exists for: before it, the note
    // without a createdAt is missing from the ordered query entirely.
    await firestore.collection("notes").doc("dated").set(note({ createdAt: Timestamp.now() }));
    await firestore.collection("notes").doc("undated").set(note({}));

    const before = await firestore.collection("notes").orderBy("createdAt", "desc").get();
    expect(before.size).toBe(1);

    await backfill(firestore, { apply: true, log: quiet });

    const after = await firestore.collection("notes").orderBy("createdAt", "desc").get();
    expect(after.size).toBe(2);
    expect(after.docs.map((doc) => doc.id)).toEqual(["dated", "undated"]);
  });
});
