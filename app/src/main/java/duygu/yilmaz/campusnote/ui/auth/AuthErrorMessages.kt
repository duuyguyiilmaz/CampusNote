package duygu.yilmaz.campusnote.ui.auth

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import duygu.yilmaz.campusnote.R

/**
 * Giriş ve kayıt hatalarını kullanıcıya gösterilecek Türkçe metne çevirir.
 *
 * Eskiden bu eşleme Firebase'in İngilizce hata *cümlesinde* `contains` araması
 * yapıyordu ("password is invalid", "no user record"). O cümleler API'nin bir parçası
 * değil; SDK sürümleriyle değiştiler ve eşleşme sessizce bozuldu — her hata "Tekrar
 * deneyin."e düştü, yani kullanıcı şifresini mi yanlış yazdığını yoksa internetinin mi
 * gittiğini anlayamadı. [FirebaseAuthException.errorCode] ise sabit bir sözleşme.
 */
enum class AuthAction { SIGN_IN, REGISTER }

fun authErrorMessage(exception: Throwable, action: AuthAction): Int = when (exception) {
    is FirebaseNetworkException -> R.string.error_network
    is FirebaseAuthException -> messageForAuthErrorCode(exception.errorCode, action)
    else -> messageForAuthErrorCode(null, action)
}

/**
 * Kodu metne çeviren saf fonksiyon — Firebase tipine ihtiyaç duymadığı için JVM
 * testinden doğrudan çağrılabiliyor.
 *
 * Bilinmeyen kod, eylemin kendi genel mesajına düşer: kayıt ile giriş aynı cümleyi
 * paylaşamaz, "Giriş başarısız" kayıt ekranında yanlış olur.
 */
@StringRes
fun messageForAuthErrorCode(errorCode: String?, action: AuthAction): Int = when (errorCode) {
    "ERROR_INVALID_EMAIL" -> R.string.error_invalid_email_format
    "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.register_error_email_in_use
    "ERROR_WEAK_PASSWORD" -> R.string.register_error_weak_password
    "ERROR_USER_NOT_FOUND" -> R.string.login_error_user_not_found
    "ERROR_WRONG_PASSWORD" -> R.string.login_error_wrong_password
    // Güncel Firebase, yanlış şifre ile olmayan hesabı bu tek kodda birleştiriyor.
    "ERROR_INVALID_CREDENTIAL" -> R.string.login_error_invalid_credentials
    "ERROR_USER_DISABLED" -> R.string.error_user_disabled
    "ERROR_TOO_MANY_REQUESTS" -> R.string.error_too_many_requests
    "ERROR_OPERATION_NOT_ALLOWED" -> R.string.error_operation_not_allowed
    "ERROR_NETWORK_REQUEST_FAILED" -> R.string.error_network
    else -> when (action) {
        AuthAction.SIGN_IN -> R.string.login_error_generic
        AuthAction.REGISTER -> R.string.register_error_generic
    }
}
