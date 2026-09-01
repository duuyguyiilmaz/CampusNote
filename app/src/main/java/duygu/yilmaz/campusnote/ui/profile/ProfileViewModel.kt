package duygu.yilmaz.campusnote.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.model.uploaderName
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseNoteRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseUserRepository
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val noteRepository: NoteRepository = FirebaseNoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<ProfileUiState>(ProfileUiState.Idle)
    val uiState: LiveData<ProfileUiState> = _uiState

    private val _actionState = MutableLiveData<ProfileActionState>(ProfileActionState.Idle)
    val actionState: LiveData<ProfileActionState> = _actionState

    private var profileJob: Job? = null

    fun startProfile() {
        profileJob?.cancel()

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = ProfileUiState.MissingSession
            return
        }

        _uiState.value = ProfileUiState.Loading
        profileJob = viewModelScope.launch {
            val department = try {
                userRepository.getUser(user.uid)?.department?.ifBlank { "—" } ?: "—"
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = ProfileUiState.Error(
                    exception = exception,
                    stage = ProfileFailureStage.USER_PROFILE
                )
                return@launch
            }

            noteRepository.observeNotesByUploader(
                uploaderUid = user.uid,
                defaultUploaderName = user.email.uploaderName()
            )
                .catch { throwable ->
                    _uiState.value = ProfileUiState.Error(
                        exception = throwable as? Exception ?: Exception(throwable),
                        stage = ProfileFailureStage.NOTES
                    )
                }
                .collect { posts ->
                    _uiState.value = ProfileUiState.Content(
                        email = user.email.ifBlank { "—" },
                        department = department,
                        posts = posts,
                        totalPoints = posts.sumOf { it.ratingSum }
                    )
                }
        }
    }

    fun stopProfile() {
        profileJob?.cancel()
        profileJob = null
    }

    fun deleteNote(noteId: String) {
        if (_actionState.value == ProfileActionState.DeletingNote) return

        _actionState.value = ProfileActionState.DeletingNote
        viewModelScope.launch {
            _actionState.value = try {
                noteRepository.deleteNote(noteId)
                ProfileActionState.NoteDeleted
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                ProfileActionState.DeleteError(exception)
            }
        }
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
}
