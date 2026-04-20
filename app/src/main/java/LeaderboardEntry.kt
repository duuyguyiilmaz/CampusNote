package duygu.yilmaz.CampusNote

data class LeaderboardEntry(
    val docId: String,
    val title: String,
    val uploaderEmail: String,
    val department: String,
    val avgRating: Double,
    val ratingCount: Long,
    val ratingSum: Long = 0L
)