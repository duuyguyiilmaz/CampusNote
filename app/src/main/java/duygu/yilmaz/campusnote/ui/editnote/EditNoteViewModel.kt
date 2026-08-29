package duygu.yilmaz.campusnote.ui.editnote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.model.NoteUpdate
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseNoteRepository
import duygu.yilmaz.campusnote.data.repository.NoteNotOwnedException
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class EditNoteViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val noteRepository: NoteRepository = FirebaseNoteRepository()
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
        viewModelScope.launch {
            _uiState.value = try {
                noteRepository.getNote(noteId)
                    ?.let { EditNoteUiState.Content(it) }
                    ?: EditNoteUiState.MissingNote
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                EditNoteUiState.Error(exception)
            }
        }
    }

    fun saveNote(noteId: String, update: NoteUpdate) {
        if (_actionState.value == EditNoteActionState.Saving) return

        val user = authRepository.currentUser()
        if (user == null) {
            _actionState.value = EditNoteActionState.MissingSession
            return
        }

        _actionState.value = EditNoteActionState.Saving
        viewModelScope.launch {
            _actionState.value = try {
                noteRepository.updateNote(
                    noteId = noteId,
                    uploaderUid = user.uid,
                    update = update
                )
                EditNoteActionState.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: NoteNotOwnedException) {
                EditNoteActionState.NotOwner
            } catch (exception: Exception) {
                EditNoteActionState.Error(exception)
            }
        }
    }

    fun resetActionState() {
        _actionState.value = EditNoteActionState.Idle
    }
}
