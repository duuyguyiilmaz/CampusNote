package duygu.yilmaz.campusnote.ui.feed

import duygu.yilmaz.campusnote.data.model.Post

sealed interface FeedUiState {
    data object Idle : FeedUiState
    data object Loading : FeedUiState
    data object Locked : FeedUiState
    data object MissingSession : FeedUiState
    data class Content(val posts: List<Post>) : FeedUiState
    data class Error(
        val exception: Exception,
        val stage: FeedFailureStage
    ) : FeedUiState
}

enum class FeedFailureStage {
    USER_PROFILE,
    NOTES
}
