package duygu.yilmaz.CampusNote.data.model

data class RatingResult(
    val average: Double,
    val count: Long,
    val sum: Long,
    val updatedExistingRating: Boolean
)
