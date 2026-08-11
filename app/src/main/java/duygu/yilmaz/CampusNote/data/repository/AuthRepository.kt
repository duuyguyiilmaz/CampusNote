package duygu.yilmaz.CampusNote.data.repository

import duygu.yilmaz.CampusNote.data.model.AuthenticatedUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /** Yerel oturumu okur; ağa çıkmaz, o yüzden suspend değil. */
    fun currentUser(): AuthenticatedUser? = auth.currentUser?.toAuthenticatedUser()

    suspend fun signIn(email: String, password: String): AuthenticatedUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toAuthenticatedUser()
            ?: throw IllegalStateException("Authenticated user is missing")
    }

    suspend fun register(email: String, password: String): AuthenticatedUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.toAuthenticatedUser()
            ?: throw IllegalStateException("Registered user is missing")
    }

    fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        uid = uid,
        email = email.orEmpty()
    )
}
