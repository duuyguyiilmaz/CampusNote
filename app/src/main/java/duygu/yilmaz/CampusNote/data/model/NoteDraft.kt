package duygu.yilmaz.CampusNote.data.model

data class NoteDraft(
    val course: String,
    val title: String,
    val description: String,
    val tag: String,
    val fileName: String,
    val fileType: String,
    val fileData: String
)
