package duygu.yilmaz.campusnote.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class LeaderboardViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val noteRepository: NoteRepository = FirebaseNoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<LeaderboardUiState>(LeaderboardUiState.Idle)
    val uiState: LiveData<LeaderboardUiState> = _uiState

    private var leaderboardJob: Job? = null

    fun startLeaderboard() {
        leaderboardJob?.cancel()

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = LeaderboardUiState.MissingSession
            return
        }

        _uiState.value = LeaderboardUiState.Loading
        leaderboardJob = viewModelScope.launch {
            // Sıralama feed ile aynı bölümü kullanıyor; ikisinin farklı kapsamdan
            // beslenmesi, kullanıcının feed'inde hiç göremeyeceği notların
            // sıralamada çıkması demek olurdu.
            val department = try {
                userRepository.getUser(user.uid)?.department.orEmpty()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = LeaderboardUiState.Error(
                    exception = exception,
                    stage = LeaderboardFailureStage.USER_PROFILE
                )
                return@launch
            }

            // Boş bölümle sorgu kurulmuyor: `whereEqualTo` boş metni gerçek bir değer
            // sayar ve bölümü kaydedilmemiş herkesin notlarını eşleştirirdi.
            if (department.isBlank()) {
                _uiState.value = LeaderboardUiState.Empty
                return@launch
            }

            noteRepository.observeLeaderboard(department)
                .catch { throwable ->
                    _uiState.value = LeaderboardUiState.Error(
                        exception = throwable as? Exception ?: Exception(throwable),
                        stage = LeaderboardFailureStage.NOTES
                    )
                }
                .collect { entries ->
                    _uiState.value = if (entries.isEmpty()) {
                        LeaderboardUiState.Empty
                    } else {
                        LeaderboardUiState.Content(entries)
                    }
                }
        }
    }

    fun stopLeaderboard() {
        leaderboardJob?.cancel()
        leaderboardJob = null
    }

    override fun onCleared() {
        stopLeaderboard()
        super.onCleared()
    }
}
