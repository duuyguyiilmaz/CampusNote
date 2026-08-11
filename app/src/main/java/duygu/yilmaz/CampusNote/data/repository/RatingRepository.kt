package duygu.yilmaz.CampusNote.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import duygu.yilmaz.CampusNote.data.model.RatingResult
import kotlinx.coroutines.tasks.await

class RatingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun submitRating(
        noteId: String,
        raterUid: String,
        newRating: Int
    ): RatingResult {
        require(noteId.isNotBlank() && raterUid.isNotBlank()) {
            "Note and user identifiers are required"
        }
        require(newRating in MIN_RATING..MAX_RATING) {
            "Rating must be between 1 and 5"
        }

        val noteReference = firestore.collection(NOTES_COLLECTION).document(noteId)
        val ratingReference = firestore.collection(RATINGS_COLLECTION)
            .document("${raterUid}_$noteId")

        return firestore.runTransaction { transaction ->
            val noteSnapshot = transaction.get(noteReference)
            if (!noteSnapshot.exists()) throw NoteNotFoundException()

            val ownerUid = noteSnapshot.getString(UPLOADER_UID_FIELD).orEmpty()
            if (ownerUid == raterUid) throw OwnNoteRatingException()

            val ratingSnapshot = transaction.get(ratingReference)
            val ownerReference = ownerUid
                .takeIf { it.isNotBlank() }
                ?.let { firestore.collection(USERS_COLLECTION).document(it) }
            val ownerSnapshot = ownerReference?.let { reference ->
                transaction.get(reference)
            }

            val oldSum = noteSnapshot.getLong(RATING_SUM_FIELD) ?: 0L
            val storedCount = noteSnapshot.getLong(RATING_COUNT_FIELD) ?: 0L
            val updatedExistingRating = ratingSnapshot.exists()
            val oldRating = if (updatedExistingRating) {
                ratingSnapshot.getLong(RATING_FIELD) ?: 0L
            } else {
                0L
            }
            val difference = newRating.toLong() - oldRating
            val newCount = if (updatedExistingRating) {
                storedCount.coerceAtLeast(1L)
            } else {
                storedCount + 1L
            }
            val newSum = (oldSum + difference).coerceAtLeast(0L)
            val newAverage = if (newCount == 0L) 0.0 else newSum.toDouble() / newCount

            transaction.update(
                noteReference,
                mapOf(
                    RATING_SUM_FIELD to newSum,
                    RATING_COUNT_FIELD to newCount,
                    AVERAGE_RATING_FIELD to newAverage
                )
            )
            transaction.set(
                ratingReference,
                mapOf(
                    USER_ID_FIELD to raterUid,
                    NOTE_ID_FIELD to noteId,
                    RATING_FIELD to newRating.toLong()
                )
            )

            if (ownerReference != null && ownerSnapshot?.exists() == true) {
                val currentPoints = ownerSnapshot.getLong(POINTS_FIELD) ?: 0L
                val newPoints = (currentPoints + difference).coerceAtLeast(0L)
                transaction.update(ownerReference, POINTS_FIELD, newPoints)
            }

            RatingResult(
                average = newAverage,
                count = newCount,
                sum = newSum,
                updatedExistingRating = updatedExistingRating
            )
        }.await()
    }

    private companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 5
        const val NOTES_COLLECTION = "notes"
        const val RATINGS_COLLECTION = "ratings"
        const val USERS_COLLECTION = "users"
        const val UPLOADER_UID_FIELD = "uploaderUid"
        const val RATING_SUM_FIELD = "ratingSum"
        const val RATING_COUNT_FIELD = "ratingCount"
        const val AVERAGE_RATING_FIELD = "avgRating"
        const val RATING_FIELD = "rating"
        const val USER_ID_FIELD = "uid"
        const val NOTE_ID_FIELD = "noteId"
        const val POINTS_FIELD = "points"
    }
}

class NoteNotFoundException : IllegalStateException("Note is missing")

class OwnNoteRatingException : IllegalStateException("Users cannot rate their own notes")

class NoteNotOwnedException : IllegalStateException("Only the uploader can change this note")
