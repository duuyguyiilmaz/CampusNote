package duygu.yilmaz.campusnote.ui.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState

    data class Error(
        val exception: Exception,
        val stage: AuthFailureStage
    ) : AuthUiState
}

enum class AuthFailureStage {
    AUTHENTICATION,
    USER_PROFILE
}
