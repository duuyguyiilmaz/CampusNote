package duygu.yilmaz.CampusNote.ui.editnote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.model.NoteUpdate
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.NoteRepository

class EditNoteViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<EditNoteUiState>(EditNoteUiState.Idle)
    val uiState: LiveData<EditNoteUiState> = _uiState

    private val _actionState = MutableLiveData<EditNoteActionState>(EditNoteActionState.Idle)
    val actionState: LiveData<EditNoteActionState> = _actionState

    fun loadNote(noteId: String) {
        if (noteId.isBlank()) {
            _uiState.value = EditNoteUiState.MissingNote
            return
        }

        if (authRepository.currentUser() == null) {
            _uiState.value = EditNoteUiState.MissingSession
            return
        }

        _uiState.value = EditNoteUiState.Loading
        noteRepository.getNote(
            noteId = noteId,
            onSuccess = { note ->
                _uiState.value = if (note == null) {
                    EditNoteUiState.MissingNote
                } else {
                    EditNoteUiState.Content(note)
                }
            },
            onFailure = { exception ->
                _uiState.value = EditNoteUiState.Error(exception)
            }
        )
    }

    fun saveNote(noteId: String, update: NoteUpdate) {
        if (_actionState.value == EditNoteActionState.Saving) return

        val user = authRepository.currentUser()
        if (user == null) {
            _actionState.value = EditNoteActionState.MissingSession
            return
        }

        _actionState.value = EditNoteActionState.Saving
        noteRepository.updateNote(
            noteId = noteId,
            uploaderUid = user.uid,
            update = update,
            onSuccess = {
                _actionState.value = EditNoteActionState.Success
            },
            onNotOwner = {
                _actionState.value = EditNoteActionState.NotOwner
            },
            onFailure = { exception ->
                _actionState.value = EditNoteActionState.Error(exception)
            }
        )
    }

    fun resetActionState() {
        _actionState.value = EditNoteActionState.Idle
    }
}
