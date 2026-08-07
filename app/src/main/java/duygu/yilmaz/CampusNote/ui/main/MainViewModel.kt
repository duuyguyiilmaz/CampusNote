package duygu.yilmaz.CampusNote.ui.main

import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.repository.AuthRepository

class MainViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    fun hasAuthenticatedUser(): Boolean = authRepository.currentUser() != null
}
