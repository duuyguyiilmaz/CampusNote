package duygu.yilmaz.CampusNote.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.NoteRepository
import duygu.yilmaz.CampusNote.data.repository.UserRepository

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<ProfileUiState>(ProfileUiState.Idle)
    val uiState: LiveData<ProfileUiState> = _uiState

    private val _actionState = MutableLiveData<ProfileActionState>(ProfileActionState.Idle)
    val actionState: LiveData<ProfileActionState> = _actionState

    private var stopObservingNotes: (() -> Unit)? = null
    private var requestId = 0

    fun startProfile() {
        val currentRequestId = ++requestId
        removeNotesListener()

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = ProfileUiState.MissingSession
            return
        }

        _uiState.value = ProfileUiState.Loading
        userRepository.getUser(
            userId = user.uid,
            onSuccess = { profile ->
                if (currentRequestId != requestId) return@getUser

                stopObservingNotes = noteRepository.observeNotesByUploader(
                    uploaderUid = user.uid,
                    defaultUploaderEmail = user.email,
                    onNotesChanged = { posts ->
                        if (currentRequestId == requestId) {
                            _uiState.value = ProfileUiState.Content(
                                email = user.email.ifBlank { "—" },
                                department = profile?.department?.ifBlank { "—" } ?: "—",
                                posts = posts,
                                totalPoints = posts.sumOf { it.ratingSum }
                            )
                        }
                    },
                    onFailure = { exception ->
                        if (currentRequestId == requestId) {
                            _uiState.value = ProfileUiState.Error(
                                exception = exception,
                                stage = ProfileFailureStage.NOTES
                            )
                        }
                    }
                )
            },
            onFailure = { exception ->
                if (currentRequestId == requestId) {
                    _uiState.value = ProfileUiState.Error(
                        exception = exception,
                        stage = ProfileFailureStage.USER_PROFILE
                    )
                }
            }
        )
    }

    fun stopProfile() {
        requestId++
        removeNotesListener()
    }

    fun deleteNote(noteId: String) {
        if (_actionState.value == ProfileActionState.DeletingNote) return

        _actionState.value = ProfileActionState.DeletingNote
        noteRepository.deleteNote(
            noteId = noteId,
            onSuccess = {
                _actionState.value = ProfileActionState.NoteDeleted
            },
            onFailure = { exception ->
                _actionState.value = ProfileActionState.DeleteError(exception)
            }
        )
    }

    fun resetActionState() {
        _actionState.value = ProfileActionState.Idle
    }

    fun signOut() {
        authRepository.signOut()
    }

    override fun onCleared() {
        stopProfile()
        super.onCleared()
    }

    private fun removeNotesListener() {
        stopObservingNotes?.invoke()
        stopObservingNotes = null
    }
}
