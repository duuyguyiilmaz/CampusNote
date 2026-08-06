package duygu.yilmaz.CampusNote.data.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val department: String = "",
    val points: Long = 0L,
    val createdAt: Long = 0L,
    val hasUploadedNote: Boolean = false
)
