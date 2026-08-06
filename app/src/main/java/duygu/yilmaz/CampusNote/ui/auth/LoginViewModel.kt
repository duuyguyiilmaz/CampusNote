package duygu.yilmaz.CampusNote.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.repository.AuthRepository

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun hasAuthenticatedUser(): Boolean = authRepository.currentUser() != null

    fun signIn(email: String, password: String) {
        if (_uiState.value == AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading
        authRepository.signIn(
            email = email,
            password = password,
            onSuccess = {
                _uiState.value = AuthUiState.Success
            },
            onFailure = { exception ->
                _uiState.value = AuthUiState.Error(
                    exception = exception,
                    stage = AuthFailureStage.AUTHENTICATION
                )
            }
        )
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
