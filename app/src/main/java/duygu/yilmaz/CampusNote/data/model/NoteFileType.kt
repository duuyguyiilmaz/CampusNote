package duygu.yilmaz.CampusNote.data.model

/**
 * Bir nota eklenen dosyanın türü. Firestore'da `fileType` alanında bu değerler saklanır,
 * o yüzden metinler değiştirilemez — mevcut kayıtlar okunamaz hâle gelir.
 */
object NoteFileType {
    const val PDF = "pdf"
    const val IMAGE = "image"

    /** Dosya seçilmeyen notlarda `fileType` boş kalır. */
    const val NONE = ""

    /** Dosya seçici ve görüntüleyici intent'lerinde kullanılan MIME tipleri. */
    const val PDF_MIME_TYPE = "application/pdf"
    const val IMAGE_MIME_TYPE = "image/*"
}
