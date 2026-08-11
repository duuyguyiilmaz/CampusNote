package duygu.yilmaz.campusnote.data.repository

import duygu.yilmaz.campusnote.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun saveUser(user: UserProfile) {
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun getUser(userId: String): UserProfile? {
        val document = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        if (!document.exists()) return null

        return UserProfile(
            id = document.getString("id") ?: document.id,
            email = document.getString("email") ?: "",
            department = document.getString("department") ?: "",
            points = document.getLong("points") ?: 0L,
            createdAt = document.getLong("createdAt") ?: 0L,
            hasUploadedNote = document.getBoolean("hasUploadedNote") ?: false
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
