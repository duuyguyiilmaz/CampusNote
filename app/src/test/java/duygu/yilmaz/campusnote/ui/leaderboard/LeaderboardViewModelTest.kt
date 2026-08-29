package duygu.yilmaz.campusnote.ui.leaderboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = LeaderboardViewModel(noteRepository = noteRepository)

    private fun entry(id: String, ratingSum: Long) = LeaderboardEntry(
        docId = id,
        title = "Not $id",
        uploaderEmail = "ogrenci@ogr.akdeniz.edu.tr",
        department = "Bilgisayar Mühendisliği",
        ratingCount = 3L,
        ratingSum = ratingSum
    )

    @Test
    fun `hic not yoksa bos durum gosterilir`() = runTest(mainDispatcherRule.testDispatcher) {
        // Boş liste ile "henüz yüklenmedi" ayrı durumlar: ikisi farklı ekran gösteriyor.
        noteRepository.leaderboard = flowOf(emptyList())

        val viewModel = viewModel()
        viewModel.startLeaderboard()
        advanceUntilIdle()

        assertEquals(LeaderboardUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `gelen siralama oldugu gibi aktarilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val entries = listOf(entry("note-1", 18L), entry("note-2", 5L))
        noteRepository.leaderboard = flowOf(entries)

        val viewModel = viewModel()
        viewModel.startLeaderboard()
        advanceUntilIdle()

        assertEquals(LeaderboardUiState.Content(entries), viewModel.uiState.value)
    }

    @Test
    fun `dinleyici duserse hata gosterilir`() = runTest(mainDispatcherRule.testDispatcher) {
        noteRepository.leaderboard = flow { throw IOException("dinleyici düştü") }

        val viewModel = viewModel()
        viewModel.startLeaderboard()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LeaderboardUiState.Error)
    }

    @Test
    fun `durdurulduktan sonra gelen sonuc durumu degistirmez`() =
        runTest(mainDispatcherRule.testDispatcher) {
            noteRepository.leaderboard = flowOf(listOf(entry("note-1", 18L)))

            val viewModel = viewModel()
            viewModel.startLeaderboard()
            viewModel.stopLeaderboard()
            advanceUntilIdle()

            assertEquals(LeaderboardUiState.Idle, viewModel.uiState.value)
        }
}
