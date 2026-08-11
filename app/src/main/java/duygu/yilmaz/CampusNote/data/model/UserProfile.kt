package duygu.yilmaz.CampusNote.data.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val department: String = "",
    val points: Long = 0L,
    val createdAt: Long = 0L,
    val hasUploadedNote: Boolean = false
) {
    companion object {
        /**
         * Bölümü boş kalmış kullanıcıların notlarına yazılan değer.
         *
         * Bilinçli olarak `strings.xml`'e taşınmadı: bu metin Firestore'a *veri* olarak
         * yazılıyor ve feed sorguları `department` alanına göre eşleşme yapıyor.
         * Yerelleştirilseydi cihaz diline göre farklı değerler kaydedilir ve
         * aynı bölümdeki notlar birbirini görmez hâle gelirdi.
         */
        const val UNKNOWN_DEPARTMENT = "Bilinmiyor"
    }
}
