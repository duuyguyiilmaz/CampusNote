/**
 * Replaces every note's `uploaderEmail` with a `uploaderName` holding only the part
 * the app ever displayed.
 *
 * Deliberately has no `#!` line, for the same reason as backfill-created-at.js: the
 * repo normalises to CRLF on Windows checkouts and esbuild does not strip a shebang
 * ending in `\r`, which broke the test that imports the file — on Windows only.
 *
 * Why this exists: the three screens that show an uploader all rendered
 * `email.substringBefore("@")`, so the address itself was never on screen. It was
 * still in the document, and any signed-in student could read it — masking in the UI
 * is not a privacy boundary, it is a rendering choice. Scoping note reads to a
 * department narrows who can see it; deleting the field is what actually removes it.
 *
 * The script reports by default and only writes when told to:
 *
 *     node strip-uploader-email.js --project <id>            # dry run, changes nothing
 *     node strip-uploader-email.js --project <id> --apply    # writes
 *
 * Against production it needs application-default credentials:
 *
 *     gcloud auth application-default login
 *
 * Against the emulator, set FIRESTORE_EMULATOR_HOST instead; no credentials are used.
 */

import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { initializeApp } from "firebase-admin/app";
import { FieldValue, Firestore } from "firebase-admin/firestore";

const NOTES = "notes";
const EMAIL_FIELD = "uploaderEmail";
const NAME_FIELD = "uploaderName";
// Firestore accepts at most 500 writes per batch.
const BATCH_LIMIT = 450;

/**
 * The same derivation the app used at render time: everything before the first `@`.
 *
 * @returns the name to store, or null when the document needs no write.
 */
export function displayNameFor(data) {
  const existing = data?.[NAME_FIELD];
  const email = data?.[EMAIL_FIELD];

  // Already migrated, and no address left behind.
  if (typeof existing === "string" && existing.length > 0 && email === undefined) {
    return null;
  }

  if (typeof email === "string" && email.length > 0) {
    return email.split("@")[0];
  }

  // No address to derive from. An existing name is kept; otherwise the note keeps
  // whatever it has rather than gaining an empty field it never had.
  if (typeof existing === "string") return null;

  return "";
}

export function reasonFor(data) {
  const hasEmail = typeof data?.[EMAIL_FIELD] === "string";
  const hasName = typeof data?.[NAME_FIELD] === "string";

  if (hasEmail && hasName) return "both";
  if (hasEmail) return "email";
  if (hasName) return "name";
  return "neither";
}

export async function strip(firestore, { apply = false, log = console.log } = {}) {
  const snapshot = await firestore.collection(NOTES).get();

  const counts = { both: 0, email: 0, name: 0, neither: 0 };
  const pending = [];

  for (const doc of snapshot.docs) {
    const data = doc.data();
    counts[reasonFor(data)] += 1;

    const replacement = displayNameFor(data);
    if (replacement !== null) {
      pending.push({ ref: doc.ref, value: replacement, id: doc.id });
    }
  }

  log(`notes scanned: ${snapshot.size}`);
  log(`  carrying an email:        ${counts.email}`);
  log(`  carrying email and name:  ${counts.both}`);
  log(`  already migrated:         ${counts.name}`);
  log(`  neither field:            ${counts.neither}`);
  log(`needs a write: ${pending.length}`);

  if (!apply) {
    log("dry run — nothing was written. Re-run with --apply to write.");
    return { scanned: snapshot.size, counts, written: 0 };
  }

  let written = 0;
  for (let i = 0; i < pending.length; i += BATCH_LIMIT) {
    const batch = firestore.batch();
    for (const item of pending.slice(i, i + BATCH_LIMIT)) {
      // One update per document: the name arrives and the address leaves together,
      // so no note is ever readable with both, and a partial run leaves the rest
      // untouched rather than half-converted.
      batch.update(item.ref, {
        [NAME_FIELD]: item.value,
        [EMAIL_FIELD]: FieldValue.delete(),
      });
    }
    await batch.commit();
    written += Math.min(BATCH_LIMIT, pending.length - i);
    log(`written: ${written}/${pending.length}`);
  }

  return { scanned: snapshot.size, counts, written };
}

/** Only runs when the file is executed directly, so the tests can import it. */
const runDirectly =
  process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (runDirectly) {
  const args = process.argv.slice(2);
  const apply = args.includes("--apply");
  const projectIndex = args.indexOf("--project");
  const projectId =
    projectIndex >= 0 ? args[projectIndex + 1] : process.env.GOOGLE_CLOUD_PROJECT;

  if (!projectId) {
    console.error("usage: node strip-uploader-email.js --project <id> [--apply]");
    process.exit(2);
  }

  initializeApp({ projectId });
  const firestore = new Firestore({ projectId });

  strip(firestore, { apply })
    .then((result) => {
      console.log(apply ? `done — ${result.written} note(s) updated.` : "done.");
      process.exit(0);
    })
    .catch((error) => {
      console.error(error);
      process.exit(1);
    });
}
