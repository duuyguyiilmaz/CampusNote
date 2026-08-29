package duygu.yilmaz.campusnote.ui.main

import androidx.lifecycle.ViewModel
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository

class MainViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {
    fun hasAuthenticatedUser(): Boolean = authRepository.currentUser() != null
}
