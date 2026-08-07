package duygu.yilmaz.CampusNote.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.repository.NoteRepository

class LeaderboardViewModel(
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<LeaderboardUiState>(LeaderboardUiState.Idle)
    val uiState: LiveData<LeaderboardUiState> = _uiState

    private var stopObservingLeaderboard: (() -> Unit)? = null
    private var requestId = 0

    fun startLeaderboard() {
        val currentRequestId = ++requestId
        removeLeaderboardListener()

        stopObservingLeaderboard = noteRepository.observeLeaderboard(
            onEntriesChanged = { entries ->
                if (currentRequestId != requestId) return@observeLeaderboard

                _uiState.value = if (entries.isEmpty()) {
                    LeaderboardUiState.Empty
                } else {
                    LeaderboardUiState.Content(entries)
                }
            },
            onFailure = { exception ->
                if (currentRequestId == requestId) {
                    _uiState.value = LeaderboardUiState.Error(exception)
                }
            }
        )
    }

    fun stopLeaderboard() {
        requestId++
        removeLeaderboardListener()
    }

    override fun onCleared() {
        stopLeaderboard()
        super.onCleared()
    }

    private fun removeLeaderboardListener() {
        stopObservingLeaderboard?.invoke()
        stopObservingLeaderboard = null
    }
}
