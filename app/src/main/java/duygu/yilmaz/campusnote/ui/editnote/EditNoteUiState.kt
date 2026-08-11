package duygu.yilmaz.campusnote.ui.editnote

import duygu.yilmaz.campusnote.data.model.Post

sealed interface EditNoteUiState {
    data object Idle : EditNoteUiState
    data object Loading : EditNoteUiState
    data object MissingSession : EditNoteUiState
    data object MissingNote : EditNoteUiState
    data class Content(val note: Post) : EditNoteUiState
    data class Error(val exception: Exception) : EditNoteUiState
}

sealed interface EditNoteActionState {
    data object Idle : EditNoteActionState
    data object Saving : EditNoteActionState
    data object Success : EditNoteActionState
    data object MissingSession : EditNoteActionState
    data object NotOwner : EditNoteActionState
    data class Error(val exception: Exception) : EditNoteActionState
}
