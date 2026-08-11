package duygu.yilmaz.campusnote.data.model

data class NoteDraft(
    val course: String,
    val title: String,
    val description: String,
    val tag: String,
    val fileName: String,
    val fileType: String,
    /** Base64'e çevrilmiş dosya içeriği. Dosya seçilmediyse boş. */
    val fileData: String,
    /** Sıkıştırma sonrası ham dosya boyutu (byte). Sadece kullanıcıya göstermek için. */
    val fileSize: Long = 0L
)
