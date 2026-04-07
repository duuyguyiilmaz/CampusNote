package duygu.yilmaz.CampusNote

data class Post(
    val id: String,
    val title: String,
    val desc: String,
    val authorEmail: String,
    val department: String,
    val timeMills: Long,
    val uploaderUid: String = "",
    val avgRating: Double = 0.0,
    val ratingCount: Long = 0L
)