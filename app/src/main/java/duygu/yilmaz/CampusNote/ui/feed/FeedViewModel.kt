package duygu.yilmaz.CampusNote.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.NoteRepository
import duygu.yilmaz.CampusNote.data.repository.UserRepository

class FeedViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<FeedUiState>(FeedUiState.Idle)
    val uiState: LiveData<FeedUiState> = _uiState

    private var stopObservingNotes: (() -> Unit)? = null
    private var requestId = 0

    fun startFeed() {
        val currentRequestId = ++requestId
        removeNoteListener()

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = FeedUiState.MissingSession
            return
        }

        _uiState.value = FeedUiState.Loading
        userRepository.getUser(
            userId = user.uid,
            onSuccess = { profile ->
                if (currentRequestId != requestId) return@getUser

                val department = profile?.department.orEmpty()
                if (department.isBlank() || profile?.hasUploadedNote != true) {
                    _uiState.value = FeedUiState.Locked
                    return@getUser
                }

                stopObservingNotes = noteRepository.observeNotesByDepartment(
                    department = department,
                    onNotesChanged = { posts ->
                        if (currentRequestId == requestId) {
                            _uiState.value = FeedUiState.Content(posts)
                        }
                    },
                    onFailure = { exception ->
                        if (currentRequestId == requestId) {
                            _uiState.value = FeedUiState.Error(
                                exception = exception,
                                stage = FeedFailureStage.NOTES
                            )
                        }
                    }
                )
            },
            onFailure = { exception ->
                if (currentRequestId == requestId) {
                    _uiState.value = FeedUiState.Error(
                        exception = exception,
                        stage = FeedFailureStage.USER_PROFILE
                    )
                }
            }
        )
    }

    fun stopFeed() {
        requestId++
        removeNoteListener()
    }

    override fun onCleared() {
        stopFeed()
        super.onCleared()
    }

    private fun removeNoteListener() {
        stopObservingNotes?.invoke()
        stopObservingNotes = null
    }
}
