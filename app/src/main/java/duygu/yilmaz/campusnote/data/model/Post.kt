package duygu.yilmaz.campusnote.data.model

/**
 * Notun metadata'sı. Dosya içeriği bilinçli olarak burada yok:
 * feed ve leaderboard bu modeli listelerken dosya verisini indirmemeli.
 * İçerik ayrı bir dokümanda tutulur, [NoteRepository.getNoteFile] ile okunur.
 */
data class Post(
    val id: String,
    val title: String,
    val desc: String,
    val authorEmail: String,
    val department: String,
    val timeMills: Long,
    val uploaderUid: String = "",
    val ratingCount: Long = 0L,
    val ratingSum: Long = 0L,
    val course: String = "",
    val tag: String = "",
    val fileName: String = "",
    val fileType: String = "",
    val fileSize: Long = 0L
) {
    /** Notu yükleyen kişinin gösterilecek adı; tam e-posta arayüze çıkmaz. */
    val uploaderName: String get() = authorEmail.uploaderName()
}
