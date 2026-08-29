package duygu.yilmaz.campusnote.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun hasAuthenticatedUser(): Boolean = authRepository.currentUser() != null

    fun signIn(email: String, password: String) {
        if (_uiState.value == AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                authRepository.signIn(email, password)
                AuthUiState.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                AuthUiState.Error(
                    exception = exception,
                    stage = AuthFailureStage.AUTHENTICATION
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
