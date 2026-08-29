package duygu.yilmaz.campusnote.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import duygu.yilmaz.campusnote.data.model.RatingCalculator
import duygu.yilmaz.campusnote.data.model.RatingResult
import kotlinx.coroutines.tasks.await

class FirebaseRatingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RatingRepository {

    override suspend fun submitRating(
        noteId: String,
        raterUid: String,
        newRating: Int
    ): RatingResult {
        require(noteId.isNotBlank() && raterUid.isNotBlank()) {
            "Note and user identifiers are required"
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

            val totals = RatingCalculator.recalculate(
                currentSum = noteSnapshot.getLong(RATING_SUM_FIELD) ?: 0L,
                currentCount = noteSnapshot.getLong(RATING_COUNT_FIELD) ?: 0L,
                previousRating = if (ratingSnapshot.exists()) {
                    ratingSnapshot.getLong(RATING_FIELD) ?: 0L
                } else {
                    null
                },
                newRating = newRating
            )

            transaction.update(
                noteReference,
                mapOf(
                    RATING_SUM_FIELD to totals.sum,
                    RATING_COUNT_FIELD to totals.count,
                    AVERAGE_RATING_FIELD to totals.average
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
                val newPoints = (currentPoints + totals.pointsDelta).coerceAtLeast(0L)
                transaction.update(ownerReference, POINTS_FIELD, newPoints)
            }

            RatingResult(
                average = totals.average,
                count = totals.count,
                sum = totals.sum,
                updatedExistingRating = totals.updatedExistingRating
            )
        }.await()
    }

    private companion object {
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
