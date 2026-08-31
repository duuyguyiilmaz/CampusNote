package duygu.yilmaz.campusnote.data.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val department: String = "",
    val createdAt: Long = 0L
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
