/**
 * Deletes rating documents whose note no longer exists.
 *
 * Deliberately has no `#!` line, for the same reason as the other two scripts: the
 * repo normalises to CRLF on Windows checkouts and esbuild does not strip a shebang
 * ending in `\r`, which broke the test that imports the file — on Windows only.
 *
 * Why this exists: `ratings` is a top-level collection keyed `<uid>_<noteId>`, not a
 * subcollection of the note, so deleting a note never touched the votes cast on it.
 * `deleteNote` now removes them in the same commit and the rules only permit a vote
 * to be deleted alongside its note, so no new orphans appear — but the ones already
 * written cannot be reached by any client: the rule calls `get()` on the note, and a
 * note that is gone makes that an evaluation error, which fails closed.
 *
 * The script reports by default and only writes when told to:
 *
 *     node prune-orphan-ratings.js --project <id>            # dry run, changes nothing
 *     node prune-orphan-ratings.js --project <id> --apply    # deletes
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
import { Firestore } from "firebase-admin/firestore";

const NOTES = "notes";
const RATINGS = "ratings";
const NOTE_ID_FIELD = "noteId";
// Firestore accepts at most 500 writes per batch.
const BATCH_LIMIT = 450;

/**
 * @returns the note id a rating claims, or null when the document cannot say.
 *
 * The field is the source of truth rather than the document id: the id is
 * `<uid>_<noteId>` and a uid may itself contain an underscore, so splitting it is
 * ambiguous in exactly the case that matters.
 */
export function noteIdOf(data) {
  const noteId = data?.[NOTE_ID_FIELD];
  return typeof noteId === "string" && noteId.length > 0 ? noteId : null;
}

export async function prune(firestore, { apply = false, log = console.log } = {}) {
  const [ratings, notes] = await Promise.all([
    firestore.collection(RATINGS).get(),
    firestore.collection(NOTES).get(),
  ]);

  // One pass over the notes rather than a get() per rating: the whole point is to
  // run cheaply over a collection that has been accumulating.
  const liveNotes = new Set(notes.docs.map((doc) => doc.id));

  const counts = { live: 0, orphan: 0, unreadable: 0 };
  const pending = [];

  for (const doc of ratings.docs) {
    const noteId = noteIdOf(doc.data());

    if (noteId === null) {
      // No note id to check against. Left alone: deleting on a guess is worse than
      // reporting it and letting someone look.
      counts.unreadable += 1;
      continue;
    }

    if (liveNotes.has(noteId)) {
      counts.live += 1;
    } else {
      counts.orphan += 1;
      pending.push(doc.ref);
    }
  }

  log(`ratings scanned: ${ratings.size}`);
  log(`  note still exists: ${counts.live}`);
  log(`  orphaned:          ${counts.orphan}`);
  log(`  no noteId field:   ${counts.unreadable}`);

  if (counts.unreadable > 0) {
    log(`  ${counts.unreadable} rating(s) carry no noteId and are left untouched.`);
  }

  if (!apply) {
    log("dry run — nothing was deleted. Re-run with --apply to delete.");
    return { scanned: ratings.size, counts, deleted: 0 };
  }

  let deleted = 0;
  for (let i = 0; i < pending.length; i += BATCH_LIMIT) {
    const batch = firestore.batch();
    for (const ref of pending.slice(i, i + BATCH_LIMIT)) {
      batch.delete(ref);
    }
    await batch.commit();
    deleted += Math.min(BATCH_LIMIT, pending.length - i);
    log(`deleted: ${deleted}/${pending.length}`);
  }

  return { scanned: ratings.size, counts, deleted };
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
    console.error("usage: node prune-orphan-ratings.js --project <id> [--apply]");
    process.exit(2);
  }

  initializeApp({ projectId });
  const firestore = new Firestore({ projectId });

  prune(firestore, { apply })
    .then((result) => {
      console.log(apply ? `done — ${result.deleted} rating(s) deleted.` : "done.");
      process.exit(0);
    })
    .catch((error) => {
      console.error(error);
      process.exit(1);
    });
}
