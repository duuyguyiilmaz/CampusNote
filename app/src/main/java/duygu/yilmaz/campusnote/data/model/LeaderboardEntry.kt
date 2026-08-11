package duygu.yilmaz.campusnote.data.model

data class LeaderboardEntry(
    val docId: String,
    val title: String,
    val uploaderEmail: String,
    val department: String,
    val ratingCount: Long,
    val ratingSum: Long = 0L
)
