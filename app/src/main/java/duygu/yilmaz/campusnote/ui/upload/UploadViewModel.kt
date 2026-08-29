package duygu.yilmaz.campusnote.ui.upload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.model.NoteDraft
import duygu.yilmaz.campusnote.data.model.UserProfile
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseNoteRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseUserRepository
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class UploadViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val noteRepository: NoteRepository = FirebaseNoteRepository()
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
        viewModelScope.launch {
            val profile = try {
                userRepository.getUser(user.uid)
                    ?: throw IllegalStateException("User profile is missing")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = UploadUiState.Error(
                    exception = exception,
                    stage = UploadFailureStage.USER_PROFILE
                )
                return@launch
            }

            _uiState.value = try {
                noteRepository.createNote(
                    draft = draft,
                    uploaderUid = user.uid,
                    uploaderEmail = user.email.ifBlank { profile.email },
                    department = profile.department
                        .ifBlank { UserProfile.UNKNOWN_DEPARTMENT }
                )
                UploadUiState.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                UploadUiState.Error(
                    exception = exception,
                    stage = UploadFailureStage.NOTE
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = UploadUiState.Idle
    }
}
