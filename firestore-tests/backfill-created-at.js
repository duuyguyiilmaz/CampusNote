/**
 * Normalises every note's `createdAt` to a Firestore Timestamp.
 *
 * Deliberately has no `#!` line. The repo normalises to CRLF on Windows checkouts, and
 * a shebang ending in `\r` is one esbuild does not strip, so the test that imports this
 * file failed to parse — on Windows only, which is why CI stayed green. The script is
 * always invoked through `node` or the npm script, so the shebang bought nothing.
 *
 * Why this exists: paginating the feed means ordering on `createdAt` in the query
 * instead of sorting in the client, and Firestore's ordering has two behaviours that
 * turn stale data into user-visible damage.
 *
 *   - A document that lacks the ordered field is **excluded from the query entirely**.
 *     Not sorted last — absent. Any note written before `createdAt` was set would
 *     silently disappear from the feed.
 *   - Values are ordered by type before value, and numbers sort before timestamps. A
 *     note whose `createdAt` is epoch millis (a number) would land below every
 *     timestamped note regardless of its actual date.
 *
 * Both are invisible until someone notices their note is gone, so they are fixed
 * before the ordering changes rather than after.
 *
 * The script reports by default and only writes when told to:
 *
 *     node backfill-created-at.js --project <id>            # dry run, changes nothing
 *     node backfill-created-at.js --project <id> --apply    # writes
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
import { Firestore, Timestamp } from "firebase-admin/firestore";

const NOTES = "notes";
const FIELD = "createdAt";
// Firestore accepts at most 500 writes per batch.
const BATCH_LIMIT = 450;

/**
 * A note with no date at all gets the epoch rather than "now": inventing a recent
 * date would push undated legacy notes to the top of every feed, which is a louder
 * lie than showing them last.
 */
const UNKNOWN_DATE = Timestamp.fromMillis(0);

/**
 * @returns the Timestamp this document should carry, or null if it is already correct.
 */
export function normalisedCreatedAt(data) {
  const value = data?.[FIELD];

  if (value instanceof Timestamp) return null;

  // Epoch millis written by an older version of the app.
  if (typeof value === "number" && Number.isFinite(value)) {
    return Timestamp.fromMillis(value);
  }

  // A string date is not a shape this app ever wrote, but parsing one is cheap and
  // the alternative — treating it as undated — loses real information.
  if (typeof value === "string") {
    const parsed = Date.parse(value);
    if (!Number.isNaN(parsed)) return Timestamp.fromMillis(parsed);
  }

  // Missing, null, or a shape we cannot read. `updatedAt` is the best remaining
  // evidence of when the note existed.
  const updated = data?.updatedAt;
  if (updated instanceof Timestamp) return updated;
  if (typeof updated === "number" && Number.isFinite(updated)) {
    return Timestamp.fromMillis(updated);
  }

  return UNKNOWN_DATE;
}

/** @returns a short label for the report, so the counts say *why* a note was fixed. */
export function reasonFor(data) {
  const value = data?.[FIELD];
  if (value instanceof Timestamp) return "ok";
  if (typeof value === "number") return "number";
  if (typeof value === "string") return "string";
  if (value === undefined || value === null) return "missing";
  return "unreadable";
}

export async function backfill(firestore, { apply = false, log = console.log } = {}) {
  const snapshot = await firestore.collection(NOTES).get();

  const counts = { ok: 0, number: 0, string: 0, missing: 0, unreadable: 0 };
  const pending = [];

  for (const doc of snapshot.docs) {
    const data = doc.data();
    const reason = reasonFor(data);
    counts[reason] += 1;

    const replacement = normalisedCreatedAt(data);
    if (replacement !== null) {
      pending.push({ ref: doc.ref, value: replacement, id: doc.id, reason });
    }
  }

  log(`notes scanned: ${snapshot.size}`);
  log(`  already a Timestamp: ${counts.ok}`);
  log(`  epoch millis:        ${counts.number}`);
  log(`  date string:         ${counts.string}`);
  log(`  missing:             ${counts.missing}`);
  log(`  unreadable:          ${counts.unreadable}`);
  log(`needs a write: ${pending.length}`);

  if (pending.length > 0 && counts.missing > 0) {
    log(
      `  ${counts.missing} note(s) have no date at all and will be dated ` +
        `${UNKNOWN_DATE.toDate().toISOString()}, placing them last in the feed.`
    );
  }

  if (!apply) {
    log("dry run — nothing was written. Re-run with --apply to write.");
    return { scanned: snapshot.size, counts, written: 0 };
  }

  let written = 0;
  for (let i = 0; i < pending.length; i += BATCH_LIMIT) {
    const batch = firestore.batch();
    for (const item of pending.slice(i, i + BATCH_LIMIT)) {
      batch.update(item.ref, { [FIELD]: item.value });
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
    console.error("usage: node backfill-created-at.js --project <id> [--apply]");
    process.exit(2);
  }

  initializeApp({ projectId });
  const firestore = new Firestore({ projectId });

  backfill(firestore, { apply })
    .then((result) => {
      console.log(apply ? `done — ${result.written} note(s) updated.` : "done.");
      process.exit(0);
    })
    .catch((error) => {
      console.error(error);
      process.exit(1);
    });
}
