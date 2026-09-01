package duygu.yilmaz.campusnote.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.model.UserProfile
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseUserRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun register(email: String, password: String, department: String) {
        if (_uiState.value == AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val authenticatedUser = try {
                authRepository.register(email, password)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.Error(
                    exception = exception,
                    stage = AuthFailureStage.AUTHENTICATION
                )
                return@launch
            }

            _uiState.value = try {
                userRepository.saveUser(
                    UserProfile(
                        id = authenticatedUser.uid,
                        email = email,
                        department = department
                        // createdAt sunucuda yazılıyor; bkz. FirebaseUserRepository.
                    )
                )
                AuthUiState.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                AuthUiState.Error(
                    exception = exception,
                    stage = AuthFailureStage.USER_PROFILE
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
