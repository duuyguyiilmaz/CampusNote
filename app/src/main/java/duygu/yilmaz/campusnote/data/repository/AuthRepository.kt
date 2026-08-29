package duygu.yilmaz.campusnote.data.repository

import duygu.yilmaz.campusnote.data.model.AuthenticatedUser

/**
 * Oturum işlemleri.
 *
 * Arayüz olarak duruyor çünkü ViewModel'lerin tamamı oturumu bu tip üzerinden okuyor.
 * Firebase'e bağlı tek gerçekleme [FirebaseAuthRepository]; testler bunun yerine
 * elle yazılmış bir sahte veriyor, böylece ViewModel testleri ağa çıkmadan çalışıyor.
 */
interface AuthRepository {

    /** Yerel oturumu okur; ağa çıkmaz, o yüzden suspend değil. */
    fun currentUser(): AuthenticatedUser?

    suspend fun signIn(email: String, password: String): AuthenticatedUser

    suspend fun register(email: String, password: String): AuthenticatedUser

    fun signOut()
}
