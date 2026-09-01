package duygu.yilmaz.campusnote.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import duygu.yilmaz.campusnote.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    /**
     * Profili alan alan yazıyor, veri sınıfını olduğu gibi değil: `createdAt` sunucu
     * saatinden geliyor ve [FieldValue.serverTimestamp] bir `Long` alanına sığmıyor.
     * Güvenlik kuralı da `request.time` bekliyor — kayıt tarihi istemcinin
     * söylediği şey olmamalı.
     */
    override suspend fun saveUser(user: UserProfile) {
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(
                mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "department" to user.department,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    override suspend fun getUser(userId: String): UserProfile? {
        val document = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        if (!document.exists()) return null

        return UserProfile(
            id = document.getString("id") ?: document.id,
            email = document.getString("email") ?: "",
            department = document.getString("department") ?: "",
            createdAt = document.createdAtMillis()
        )
    }

    /**
     * Kayıt tarihi artık sunucu saatinden gelen bir `Timestamp`; sunucu saatine
     * geçmeden önce yazılmış profiller hâlâ epoch millis taşıyor.
     *
     * `getTimestamp()` alan sayı olduğunda null dönmüyor, [RuntimeException]
     * fırlatıyor — yani `?:` ile yedeklemek yetmiyor, çağrının kendisi sarılmak
     * zorunda. Aynı tuzak `FirebaseNoteRepository.toPost()` içinde de var ve orada
     * da böyle çözülmüş; ikisi ayrışırsa biri eski veride çöker.
     */
    private fun DocumentSnapshot.createdAtMillis(): Long = try {
        getTimestamp("createdAt")?.toDate()?.time ?: getLong("createdAt") ?: 0L
    } catch (_: Exception) {
        getLong("createdAt") ?: 0L
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
