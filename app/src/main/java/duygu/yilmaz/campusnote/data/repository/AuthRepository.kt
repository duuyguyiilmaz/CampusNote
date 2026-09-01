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

    /**
     * Az önce oluşturulmuş hesabı geri alır.
     *
     * Kayıt iki ayrı sisteme yazıyor — önce Auth hesabı, sonra Firestore profili —
     * ve ikincisi düşerse geriye kimsenin kullanamadığı bir hesap kalıyor: kişi
     * Firebase'de kayıtlı görünüyor, aynı adresle yeniden kayıt olamıyor, ama
     * profili olmadığı için uygulamanın çoğu kapalı. Bu çağrı o durumu telafi
     * ediyor.
     *
     * Telafi garantili değil — silme çağrısının kendisi de düşebilir — o yüzden
     * ikinci bir savunma var: profili olmayan bir oturum açılışta profil tamamlama
     * ekranına gidiyor.
     */
    suspend fun deleteCurrentUser()

    fun signOut()
}
