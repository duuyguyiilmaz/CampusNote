package duygu.yilmaz.campusnote.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.repository.FirebaseNoteRepository
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val noteRepository: NoteRepository = FirebaseNoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<LeaderboardUiState>(LeaderboardUiState.Idle)
    val uiState: LiveData<LeaderboardUiState> = _uiState

    private var leaderboardJob: Job? = null

    fun startLeaderboard() {
        leaderboardJob?.cancel()

        leaderboardJob = viewModelScope.launch {
            noteRepository.observeLeaderboard()
                .catch { throwable ->
                    _uiState.value = LeaderboardUiState.Error(
                        throwable as? Exception ?: Exception(throwable)
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
