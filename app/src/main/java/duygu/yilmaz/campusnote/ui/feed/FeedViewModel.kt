package duygu.yilmaz.campusnote.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FeedViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<FeedUiState>(FeedUiState.Idle)
    val uiState: LiveData<FeedUiState> = _uiState

    private var feedJob: Job? = null

    fun startFeed() {
        feedJob?.cancel()

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = FeedUiState.MissingSession
            return
        }

        _uiState.value = FeedUiState.Loading
        feedJob = viewModelScope.launch {
            val department = try {
                userRepository.getUser(user.uid)?.let { profile ->
                    if (profile.hasUploadedNote) profile.department else ""
                }.orEmpty()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = FeedUiState.Error(
                    exception = exception,
                    stage = FeedFailureStage.USER_PROFILE
                )
                return@launch
            }

            if (department.isBlank()) {
                _uiState.value = FeedUiState.Locked
                return@launch
            }

            noteRepository.observeNotesByDepartment(department)
                .catch { throwable ->
                    _uiState.value = FeedUiState.Error(
                        exception = throwable as? Exception ?: Exception(throwable),
                        stage = FeedFailureStage.NOTES
                    )
                }
                .collect { posts ->
                    _uiState.value = FeedUiState.Content(posts)
                }
        }
    }

    fun stopFeed() {
        feedJob?.cancel()
        feedJob = null
    }

    override fun onCleared() {
        stopFeed()
        super.onCleared()
    }
}
