package duygu.yilmaz.campusnote.ui.leaderboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import duygu.yilmaz.campusnote.testing.userProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private val authRepository = FakeAuthRepository(authenticatedUser(uid = "uid-1"))
    private val userRepository = FakeUserRepository(
        userProfile(department = "Bilgisayar Mühendisliği")
    )
    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = LeaderboardViewModel(
        authRepository = authRepository,
        userRepository = userRepository,
        noteRepository = noteRepository
    )

    private fun entry(id: String, ratingSum: Long) = LeaderboardEntry(
        docId = id,
        title = "Not $id",
        uploaderName = "ogrenci",
        department = "Bilgisayar Mühendisliği",
        ratingCount = 3L,
        ratingSum = ratingSum
    )

    @Test
    fun `siralama kullanicinin bolumuyle sinirlanir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Sıralama eskiden bütün koleksiyonu okuyordu: kullanıcı kendi feed'inde
            // hiç göremeyeceği bölümlerin notlarını sıralamada görüyordu.
            noteRepository.leaderboard = flowOf(listOf(entry("note-1", 18L)))

            val viewModel = viewModel()
            viewModel.startLeaderboard()
            advanceUntilIdle()

            assertEquals("Bilgisayar Mühendisliği", noteRepository.observedLeaderboardDepartment)
        }

    @Test
    fun `oturum yoksa siralama istenmez`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.startLeaderboard()
        advanceUntilIdle()

        assertEquals(LeaderboardUiState.MissingSession, viewModel.uiState.value)
        assertNull(noteRepository.observedLeaderboardDepartment)
    }

    @Test
    fun `profil yoksa sorgu kurulmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        userRepository.profile = null

        val viewModel = viewModel()
        viewModel.startLeaderboard()
        advanceUntilIdle()

        assertEquals(LeaderboardUiState.Empty, viewModel.uiState.value)
        assertNull(noteRepository.observedLeaderboardDepartment)
    }

    @Test
    fun `bos bolumle sorgu kurulmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        // Boş metin `whereEqualTo` için geçerli bir değer; sorgu kurulsaydı bölümü
        // kaydedilmemiş herkesin notları tek listede toplanırdı.
        userRepository.profile = userProfile(department = "")

        val viewModel = viewModel()
        viewModel.startLeaderboard()
        advanceUntilIdle()

        assertEquals(LeaderboardUiState.Empty, viewModel.uiState.value)
        assertNull(noteRepository.observedLeaderboardDepartment)
    }

    @Test
    fun `profil okunamazsa hangi adimin dustugu bildirilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userRepository.getUserError = IOException("profil okunamadı")

            val viewModel = viewModel()
            viewModel.startLeaderboard()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is LeaderboardUiState.Error)
            assertEquals(
                LeaderboardFailureStage.USER_PROFILE,
                (state as LeaderboardUiState.Error).stage
            )
        }

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

        val state = viewModel.uiState.value
        assertTrue(state is LeaderboardUiState.Error)
        assertEquals(LeaderboardFailureStage.NOTES, (state as LeaderboardUiState.Error).stage)
    }

    @Test
    fun `durdurulduktan sonra gelen sonuc durumu degistirmez`() =
        runTest(mainDispatcherRule.testDispatcher) {
            noteRepository.leaderboard = flowOf(listOf(entry("note-1", 18L)))

            val viewModel = viewModel()
            viewModel.startLeaderboard()
            viewModel.stopLeaderboard()
            advanceUntilIdle()

            assertEquals(LeaderboardUiState.Loading, viewModel.uiState.value)
        }
}
