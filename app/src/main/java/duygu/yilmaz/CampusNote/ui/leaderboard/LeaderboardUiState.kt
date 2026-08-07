package duygu.yilmaz.CampusNote.ui.leaderboard

import duygu.yilmaz.CampusNote.data.model.LeaderboardEntry

sealed interface LeaderboardUiState {
    data object Idle : LeaderboardUiState
    data object Empty : LeaderboardUiState
    data class Content(val entries: List<LeaderboardEntry>) : LeaderboardUiState
    data class Error(val exception: Exception) : LeaderboardUiState
}
