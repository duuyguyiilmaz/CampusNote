package duygu.yilmaz.campusnote.ui.notedetail

import duygu.yilmaz.campusnote.data.model.RatingResult

enum class RatingAvailability {
    ALLOWED,
    MISSING_SESSION,
    MISSING_NOTE,
    OWN_NOTE
}

sealed interface RatingUiState {
    data object Idle : RatingUiState
    data object Submitting : RatingUiState
    data object MissingSession : RatingUiState
    data object OwnNote : RatingUiState
    data object MissingNote : RatingUiState
    data class Success(val result: RatingResult) : RatingUiState
    data class Error(val exception: Exception) : RatingUiState
}
