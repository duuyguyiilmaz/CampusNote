package duygu.yilmaz.campusnote.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Yükleyen adının maskelenmesi.
 *
 * Bu maskeleme bir gizlilik kararı: kimse başka bir öğrencinin tam okul adresini
 * görmemeli. Kural üç ekranda birden kullanıldığı için ([Post], [LeaderboardEntry]
 * ve not detayı) burada tek yerden sabitleniyor — biri sessizce tam adresi
 * göstermeye dönerse test düşer.
 */
class UploaderNameTest {

    @Test
    fun `okul adresinin sadece kullanici adi kismi gosterilir`() {
        assertEquals("duygu.yilmaz", "duygu.yilmaz@ogr.akdeniz.edu.tr".uploaderName())
    }

    @Test
    fun `notun yukleyen adi tam adresi sizdirmaz`() {
        val post = Post(
            id = "note-1",
            title = "Veri Yapıları Özeti",
            desc = "",
            authorEmail = "duygu.yilmaz@ogr.akdeniz.edu.tr",
            department = "Bilgisayar Mühendisliği",
            timeMills = 0L
        )

        assertEquals("duygu.yilmaz", post.uploaderName)
    }

    @Test
    fun `liderlik tablosu da ayni adi gosterir`() {
        val entry = LeaderboardEntry(
            docId = "note-1",
            title = "Veri Yapıları Özeti",
            uploaderEmail = "duygu.yilmaz@ogr.akdeniz.edu.tr",
            department = "Bilgisayar Mühendisliği",
            ratingCount = 3L
        )

        assertEquals("duygu.yilmaz", entry.uploaderName)
    }

    @Test
    fun `adres bos kalmis eski kayitlarda bos ad doner`() {
        // Firestore'da `uploaderEmail` boş kalmış notlar var; çökmemeli.
        assertEquals("", "".uploaderName())
    }

    @Test
    fun `at isareti olmayan deger oldugu gibi kalir`() {
        assertEquals("duygu", "duygu".uploaderName())
    }

    @Test
    fun `birden fazla at isaretinde ilk parca alinir`() {
        assertEquals("duygu", "duygu@ogr@akdeniz.edu.tr".uploaderName())
    }
}
