package duygu.yilmaz.campusnote.ui.main

import androidx.lifecycle.ViewModel
import duygu.yilmaz.campusnote.data.repository.AuthRepository

class MainViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    fun hasAuthenticatedUser(): Boolean = authRepository.currentUser() != null
}
