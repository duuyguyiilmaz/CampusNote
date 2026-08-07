package duygu.yilmaz.CampusNote.ui.upload

sealed interface UploadUiState {
    data object Idle : UploadUiState
    data object Loading : UploadUiState
    data object Success : UploadUiState
    data object MissingSession : UploadUiState
    data class Error(
        val exception: Exception,
        val stage: UploadFailureStage
    ) : UploadUiState
}

enum class UploadFailureStage {
    USER_PROFILE,
    NOTE
}
