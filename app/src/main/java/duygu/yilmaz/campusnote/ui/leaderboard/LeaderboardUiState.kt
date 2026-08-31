package duygu.yilmaz.campusnote.ui.leaderboard

import duygu.yilmaz.campusnote.data.model.LeaderboardEntry

sealed interface LeaderboardUiState {
    data object Idle : LeaderboardUiState
    data object Loading : LeaderboardUiState
    data object MissingSession : LeaderboardUiState
    data object Empty : LeaderboardUiState
    data class Content(val entries: List<LeaderboardEntry>) : LeaderboardUiState
    data class Error(
        val exception: Exception,
        val stage: LeaderboardFailureStage
    ) : LeaderboardUiState
}

/**
 * Sıralama iki okumadan geçiyor — önce kullanıcının bölümü, sonra o bölümün notları —
 * ve hangisinin düştüğü kullanıcıya farklı şey anlatıyor, o yüzden ayrı tutuluyor.
 */
enum class LeaderboardFailureStage {
    USER_PROFILE,
    NOTES
}
