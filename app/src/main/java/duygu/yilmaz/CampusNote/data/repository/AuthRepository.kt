package duygu.yilmaz.CampusNote.data.repository

import duygu.yilmaz.CampusNote.data.model.AuthenticatedUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun currentUser(): AuthenticatedUser? = auth.currentUser?.toAuthenticatedUser()

    fun signIn(
        email: String,
        password: String,
        onSuccess: (AuthenticatedUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onFailure(IllegalStateException("Authenticated user is missing"))
                } else {
                    onSuccess(user.toAuthenticatedUser())
                }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun register(
        email: String,
        password: String,
        onSuccess: (AuthenticatedUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onFailure(IllegalStateException("Registered user is missing"))
                } else {
                    onSuccess(user.toAuthenticatedUser())
                }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        uid = uid,
        email = email.orEmpty()
    )
}
