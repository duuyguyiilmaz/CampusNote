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

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
