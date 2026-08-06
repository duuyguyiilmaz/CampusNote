package duygu.yilmaz.CampusNote.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.model.UserProfile
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.UserRepository

class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun register(email: String, password: String, department: String) {
        if (_uiState.value == AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading
        authRepository.register(
            email = email,
            password = password,
            onSuccess = { authenticatedUser ->
                val userProfile = UserProfile(
                    id = authenticatedUser.uid,
                    email = email,
                    department = department,
                    createdAt = System.currentTimeMillis()
                )

                userRepository.saveUser(
                    user = userProfile,
                    onSuccess = {
                        _uiState.value = AuthUiState.Success
                    },
                    onFailure = { exception ->
                        _uiState.value = AuthUiState.Error(
                            exception = exception,
                            stage = AuthFailureStage.USER_PROFILE
                        )
                    }
                )
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
