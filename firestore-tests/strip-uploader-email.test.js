import { initializeApp, deleteApp } from "firebase-admin/app";
import { Firestore } from "firebase-admin/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, test } from "vitest";
import { displayNameFor, reasonFor, strip } from "./strip-uploader-email.js";

// The emulator is already running (npm test wraps the suite in emulators:exec), and
// the admin SDK talks to it as long as this is set before it connects.
process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";

const PROJECT = "campusnote-rules-test";

let app;
let firestore;

/** Swallows the script's progress output so the test run stays readable. */
const quiet = () => {};

beforeAll(() => {
  app = initializeApp({ projectId: PROJECT }, "strip-email-test");
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
  test("a legacy note yields the name in front of the @", () => {
    const data = note({ uploaderEmail: "duygu@ogr.akdeniz.edu.tr" });

    expect(reasonFor(data)).toBe("email");
    expect(displayNameFor(data)).toBe("duygu");
  });

  test("an already migrated note needs no write", () => {
    const data = note({ uploaderName: "duygu" });

    expect(reasonFor(data)).toBe("name");
    expect(displayNameFor(data)).toBeNull();
  });

  /**
   * A half-migrated note — the name arrived but the address is still there — is the
   * shape a partial run leaves behind, and it is exactly the one that must still be
   * rewritten. Treating it as "done" would leave the address in place forever.
   */
  test("a note carrying both is still rewritten", () => {
    const data = note({ uploaderName: "duygu", uploaderEmail: "duygu@ogr.akdeniz.edu.tr" });

    expect(reasonFor(data)).toBe("both");
    expect(displayNameFor(data)).toBe("duygu");
  });

  test("a note with neither field gets an empty name rather than being skipped", () => {
    const data = note({});

    expect(reasonFor(data)).toBe("neither");
    expect(displayNameFor(data)).toBe("");
  });
});

describe("running against Firestore", () => {
  test("a dry run reports without writing", async () => {
    await firestore
      .collection("notes")
      .doc("legacy")
      .set(note({ uploaderEmail: "duygu@ogr.akdeniz.edu.tr" }));

    const result = await strip(firestore, { log: quiet });

    expect(result.written).toBe(0);
    const after = await firestore.collection("notes").doc("legacy").get();
    expect(after.data().uploaderEmail).toBe("duygu@ogr.akdeniz.edu.tr");
    expect(after.data().uploaderName).toBeUndefined();
  });

  /**
   * The assertion that matters: after the migration the address is not merely
   * hidden from the UI, it is absent from the document.
   */
  test("applying replaces the address with the name", async () => {
    await firestore
      .collection("notes")
      .doc("legacy")
      .set(note({ uploaderEmail: "duygu@ogr.akdeniz.edu.tr" }));

    const result = await strip(firestore, { apply: true, log: quiet });

    expect(result.written).toBe(1);
    const after = await firestore.collection("notes").doc("legacy").get();
    expect(after.data().uploaderName).toBe("duygu");
    expect(after.data().uploaderEmail).toBeUndefined();
  });

  test("re-running after a partial failure is safe", async () => {
    await firestore
      .collection("notes")
      .doc("done")
      .set(note({ uploaderName: "duygu" }));
    await firestore
      .collection("notes")
      .doc("pending")
      .set(note({ uploaderEmail: "asya@ogr.akdeniz.edu.tr" }));

    const first = await strip(firestore, { apply: true, log: quiet });
    expect(first.written).toBe(1);

    const second = await strip(firestore, { apply: true, log: quiet });
    expect(second.written).toBe(0);

    const pending = await firestore.collection("notes").doc("pending").get();
    expect(pending.data().uploaderName).toBe("asya");
    expect(pending.data().uploaderEmail).toBeUndefined();
  });

  test("no note is left carrying an address", async () => {
    await firestore.collection("notes").doc("a").set(note({ uploaderEmail: "a@x.tr" }));
    await firestore.collection("notes").doc("b").set(note({ uploaderName: "b" }));
    await firestore
      .collection("notes")
      .doc("c")
      .set(note({ uploaderName: "c", uploaderEmail: "c@x.tr" }));

    await strip(firestore, { apply: true, log: quiet });

    const all = await firestore.collection("notes").get();
    const withEmail = all.docs.filter((doc) => doc.data().uploaderEmail !== undefined);
    expect(withEmail).toHaveLength(0);
  });
});
