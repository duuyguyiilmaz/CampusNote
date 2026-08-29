package duygu.yilmaz.campusnote.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import duygu.yilmaz.campusnote.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun saveUser(user: UserProfile) {
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(user)
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
            points = document.getLong("points") ?: 0L,
            createdAt = document.getLong("createdAt") ?: 0L
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
