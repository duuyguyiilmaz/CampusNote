package duygu.yilmaz.CampusNote.ui.upload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.model.NoteDraft
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.NoteRepository
import duygu.yilmaz.CampusNote.data.repository.UserRepository

class UploadViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<UploadUiState>(UploadUiState.Idle)
    val uiState: LiveData<UploadUiState> = _uiState

    fun uploadNote(draft: NoteDraft) {
        if (_uiState.value == UploadUiState.Loading) return

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = UploadUiState.MissingSession
            return
        }

        _uiState.value = UploadUiState.Loading
        userRepository.getUser(
            userId = user.uid,
            onSuccess = { profile ->
                if (profile == null) {
                    _uiState.value = UploadUiState.Error(
                        exception = IllegalStateException("User profile is missing"),
                        stage = UploadFailureStage.USER_PROFILE
                    )
                    return@getUser
                }

                noteRepository.createNote(
                    draft = draft,
                    uploaderUid = user.uid,
                    uploaderEmail = user.email.ifBlank { profile.email },
                    department = profile.department.ifBlank { "Bilinmiyor" },
                    onSuccess = {
                        _uiState.value = UploadUiState.Success
                    },
                    onFailure = { exception ->
                        _uiState.value = UploadUiState.Error(
                            exception = exception,
                            stage = UploadFailureStage.NOTE
                        )
                    }
                )
            },
            onFailure = { exception ->
                _uiState.value = UploadUiState.Error(
                    exception = exception,
                    stage = UploadFailureStage.USER_PROFILE
                )
            }
        )
    }

    fun resetState() {
        _uiState.value = UploadUiState.Idle
    }
}
