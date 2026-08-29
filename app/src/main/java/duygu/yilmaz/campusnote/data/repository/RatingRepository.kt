package duygu.yilmaz.campusnote.data.repository

import duygu.yilmaz.campusnote.data.model.RatingResult

interface RatingRepository {

    /**
     * @throws NoteNotFoundException not silinmişse
     * @throws OwnNoteRatingException kullanıcı kendi notunu puanlamaya çalışırsa
     */
    suspend fun submitRating(
        noteId: String,
        raterUid: String,
        newRating: Int
    ): RatingResult
}
