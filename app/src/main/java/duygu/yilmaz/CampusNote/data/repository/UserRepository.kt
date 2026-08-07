package duygu.yilmaz.CampusNote.data.repository

import duygu.yilmaz.CampusNote.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun saveUser(
        user: UserProfile,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun getUser(
        userId: String,
        onSuccess: (UserProfile?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                onSuccess(
                    UserProfile(
                        id = document.getString("id") ?: document.id,
                        email = document.getString("email") ?: "",
                        department = document.getString("department") ?: "",
                        points = document.getLong("points") ?: 0L,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        hasUploadedNote = document.getBoolean("hasUploadedNote") ?: false
                    )
                )
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
