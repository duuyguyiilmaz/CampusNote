package duygu.yilmaz.CampusNote.ui.profile

import duygu.yilmaz.CampusNote.data.model.Post

sealed interface ProfileUiState {
    data object Idle : ProfileUiState
    data object Loading : ProfileUiState
    data object MissingSession : ProfileUiState
    data class Content(
        val email: String,
        val department: String,
        val posts: List<Post>,
        val totalPoints: Long
    ) : ProfileUiState
    data class Error(
        val exception: Exception,
        val stage: ProfileFailureStage
    ) : ProfileUiState
}

enum class ProfileFailureStage {
    USER_PROFILE,
    NOTES
}

sealed interface ProfileActionState {
    data object Idle : ProfileActionState
    data object DeletingNote : ProfileActionState
    data object NoteDeleted : ProfileActionState
    data class DeleteError(val exception: Exception) : ProfileActionState
}
