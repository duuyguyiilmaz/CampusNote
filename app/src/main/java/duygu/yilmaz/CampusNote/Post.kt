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
    val ratingCount: Long = 0L,
    val ratingSum: Long = 0L,
    val course: String = "",
    val tag: String = "",
    val fileName: String = "",
    val fileType: String = "",
    val fileData: String = ""



)