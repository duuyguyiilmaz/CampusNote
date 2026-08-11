package duygu.yilmaz.campusnote.ui.leaderboard

import duygu.yilmaz.campusnote.data.model.LeaderboardEntry

sealed interface LeaderboardUiState {
    data object Idle : LeaderboardUiState
    data object Empty : LeaderboardUiState
    data class Content(val entries: List<LeaderboardEntry>) : LeaderboardUiState
    data class Error(val exception: Exception) : LeaderboardUiState
}
