package duygu.yilmaz.campusnote.ui.auth

import duygu.yilmaz.campusnote.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Bu eşlemenin bozulması sessiz: yanlış kod hâlâ *bir* mesaj döndürür, sadece işe
 * yaramaz olanını. Eski hâli tam olarak böyle bozulmuştu — Firebase'in İngilizce
 * cümlesi değişti, `contains` tutmaz oldu, her hata "Tekrar deneyin."e düştü ve
 * hiçbir test kırılmadı.
 */
class AuthErrorMessagesTest {

    @Test
    fun `yanlis sifre giris ekraninda kendi mesajini gosterir`() {
        assertEquals(
            R.string.login_error_wrong_password,
            messageForAuthErrorCode("ERROR_WRONG_PASSWORD", AuthAction.SIGN_IN)
        )
    }

    @Test
    fun `kayitli olmayan kullanici kendi mesajini gosterir`() {
        assertEquals(
            R.string.login_error_user_not_found,
            messageForAuthErrorCode("ERROR_USER_NOT_FOUND", AuthAction.SIGN_IN)
        )
    }

    /**
     * Asıl regresyon: e-posta sayım koruması açık bir projede Firebase artık yanlış
     * şifre ile olmayan hesabı ayırmıyor, ikisine de bu kodu dönüyor. Eşleme bunu
     * tanımazsa giriş hatalarının neredeyse tamamı genel mesaja düşer.
     */
    @Test
    fun `gecersiz kimlik bilgisi birlesik mesaja duser`() {
        assertEquals(
            R.string.login_error_invalid_credentials,
            messageForAuthErrorCode("ERROR_INVALID_CREDENTIAL", AuthAction.SIGN_IN)
        )
    }

    @Test
    fun `kullanilan eposta kayit ekraninda kendi mesajini gosterir`() {
        assertEquals(
            R.string.register_error_email_in_use,
            messageForAuthErrorCode("ERROR_EMAIL_ALREADY_IN_USE", AuthAction.REGISTER)
        )
    }

    @Test
    fun `zayif sifre kayit ekraninda kendi mesajini gosterir`() {
        assertEquals(
            R.string.register_error_weak_password,
            messageForAuthErrorCode("ERROR_WEAK_PASSWORD", AuthAction.REGISTER)
        )
    }

    @Test
    fun `ag hatasi baglanti mesajini gosterir`() {
        assertEquals(
            R.string.error_network,
            messageForAuthErrorCode("ERROR_NETWORK_REQUEST_FAILED", AuthAction.SIGN_IN)
        )
    }

    @Test
    fun `cok fazla deneme kendi mesajini gosterir`() {
        assertEquals(
            R.string.error_too_many_requests,
            messageForAuthErrorCode("ERROR_TOO_MANY_REQUESTS", AuthAction.SIGN_IN)
        )
    }

    @Test
    fun `bilinmeyen kod eylemin genel mesajina duser`() {
        assertEquals(
            R.string.login_error_generic,
            messageForAuthErrorCode("ERROR_SOMETHING_NEW", AuthAction.SIGN_IN)
        )
        assertEquals(
            R.string.register_error_generic,
            messageForAuthErrorCode("ERROR_SOMETHING_NEW", AuthAction.REGISTER)
        )
    }

    @Test
    fun `kodsuz hata eylemin genel mesajina duser`() {
        assertEquals(
            R.string.login_error_generic,
            messageForAuthErrorCode(null, AuthAction.SIGN_IN)
        )
    }

    /**
     * Genel mesaj iki ekran arasında paylaşılamaz: "Giriş başarısız" kayıt ekranında
     * yanlış olur. Ayrı kaldıklarını sabitler.
     */
    @Test
    fun `giris ve kayit ayri genel mesajlar kullanir`() {
        assertNotEquals(
            messageForAuthErrorCode(null, AuthAction.SIGN_IN),
            messageForAuthErrorCode(null, AuthAction.REGISTER)
        )
    }
}
