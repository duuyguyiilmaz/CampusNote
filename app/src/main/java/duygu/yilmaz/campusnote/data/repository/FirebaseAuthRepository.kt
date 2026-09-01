package duygu.yilmaz.campusnote.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import duygu.yilmaz.campusnote.data.model.AuthenticatedUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override fun currentUser(): AuthenticatedUser? = auth.currentUser?.toAuthenticatedUser()

    override suspend fun signIn(email: String, password: String): AuthenticatedUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toAuthenticatedUser()
            ?: throw IllegalStateException("Authenticated user is missing")
    }

    override suspend fun register(email: String, password: String): AuthenticatedUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.toAuthenticatedUser()
            ?: throw IllegalStateException("Registered user is missing")
    }

    override suspend fun deleteCurrentUser() {
        auth.currentUser?.delete()?.await()
    }

    override fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        uid = uid,
        email = email.orEmpty()
    )
}
