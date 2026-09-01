package duygu.yilmaz.campusnote.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Yükleyen adının e-postadan türetilmesi.
 *
 * Bu eskiden bir *gösterim* kararıydı: adres nota tam hâliyle yazılıyor, ekranda
 * kırpılıyordu. Kırpmak adresi ekrandan saklıyordu, dokümandan değil — aynı
 * bölümdeki herkes ham alanı okuyabiliyordu.
 *
 * Artık türetme yazma anında yapılıyor: nota yalnızca bu fonksiyonun sonucu
 * yazılıyor, adres hiç gitmiyor. Fonksiyon iki yerde yaşıyor — yükleme yolunda ve
 * migration'dan önce yazılmış notları okurken — ve ikisinin aynı cevabı vermesi
 * gerekiyor, çünkü kural adı oturumdaki e-postanın aynı şekilde bölünmüş hâliyle
 * karşılaştırıyor.
 */
class UploaderNameTest {

    @Test
    fun `okul adresinin sadece kullanici adi kismi alinir`() {
        assertEquals("duygu.yilmaz", "duygu.yilmaz@ogr.akdeniz.edu.tr".uploaderName())
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

    /**
     * Kuraldaki `email.split('@')[0]` ile aynı davranış: ilk parça alınır. İkisi
     * ayrışırsa yükleme sunucuda reddedilir, o yüzden burada sabitleniyor.
     */
    @Test
    fun `birden fazla at isaretinde ilk parca alinir`() {
        assertEquals("duygu", "duygu@ogr@akdeniz.edu.tr".uploaderName())
    }
}
