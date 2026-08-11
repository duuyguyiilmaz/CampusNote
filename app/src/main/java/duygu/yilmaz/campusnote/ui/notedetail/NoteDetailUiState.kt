package duygu.yilmaz.campusnote.ui.notedetail

import duygu.yilmaz.campusnote.data.model.Post

/** Notun metadata'sı — hızlı gelir, ekranın iskeletini bu doldurur. */
sealed interface NoteDetailUiState {
    data object Loading : NoteDetailUiState
    data object Missing : NoteDetailUiState
    data class Content(val post: Post) : NoteDetailUiState
    data class Error(val exception: Exception) : NoteDetailUiState
}

/** Dosya içeriği — ayrı dokümanda olduğu için ayrı yüklenir. */
sealed interface NoteFileUiState {
    /** Nota dosya eklenmemiş. */
    data object None : NoteFileUiState
    data object Loading : NoteFileUiState
    data class Content(val data: String) : NoteFileUiState
    data object Missing : NoteFileUiState
    data class Error(val exception: Exception) : NoteFileUiState
}
