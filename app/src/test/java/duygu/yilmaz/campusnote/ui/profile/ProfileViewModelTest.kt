package duygu.yilmaz.campusnote.ui.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import duygu.yilmaz.campusnote.testing.post
import duygu.yilmaz.campusnote.testing.userProfile
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

/**
 * [ProfileViewModel] birim testleri.
 *
 * Profildeki toplam puan, kullanıcının notlarının aldığı oyların toplamından
 * hesaplanıyor — ayrı bir sayaç alanı tutulmuyor. Bu testler o toplamanın
 * doğruluğunu ve boş alanların arayüze ham hâliyle sızmadığını sabitliyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()
    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = ProfileViewModel(
        authRepository = authRepository,
        userRepository = userRepository,
        noteRepository = noteRepository
    )

    @Test
    fun `oturum yoksa profil yuklenmez`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.startProfile()
        advanceUntilIdle()

        assertEquals(ProfileUiState.MissingSession, viewModel.uiState.value)
    }

    @Test
    fun `toplam puan kullanicinin notlarindan hesaplanir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(
                authenticatedUser(uid = "uid-1", email = "duygu@ogr.akdeniz.edu.tr")
            )
            userRepository.profile = userProfile(department = "Bilgisayar Mühendisliği")
            noteRepository.uploaderNotes = flowOf(
                listOf(
                    post(id = "note-1", ratingSum = 12L),
                    post(id = "note-2", ratingSum = 7L)
                )
            )

            val viewModel = viewModel()
            viewModel.startProfile()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(19L, state.totalPoints)
            assertEquals(2, state.posts.size)
            assertEquals("duygu@ogr.akdeniz.edu.tr", state.email)
            assertEquals("Bilgisayar Mühendisliği", state.department)
            assertEquals("uid-1" to "duygu@ogr.akdeniz.edu.tr", noteRepository.observedUploader)
        }

    @Test
    fun `notu olmayan kullanicinin puani sifir gorunur`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()
            noteRepository.uploaderNotes = flowOf(emptyList())

            val viewModel = viewModel()
            viewModel.startProfile()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(0L, state.totalPoints)
            assertTrue(state.posts.isEmpty())
        }

    @Test
    fun `bos bolum ve e-posta yerine tire gosterilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(email = ""))
            userRepository.profile = userProfile(department = "")
            noteRepository.uploaderNotes = flowOf(emptyList())

            val viewModel = viewModel()
            viewModel.startProfile()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals("—", state.email)
            assertEquals("—", state.department)
        }

    @Test
    fun `profil okunamazsa hata profil asamasini bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.getUserError = IOException("okunamadı")

            val viewModel = viewModel()
            viewModel.startProfile()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Error)
            assertEquals(ProfileFailureStage.USER_PROFILE, (state as ProfileUiState.Error).stage)
        }

    @Test
    fun `not akisi patlarsa hata notlar asamasini bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()
            noteRepository.uploaderNotes = flow { throw IOException("dinleyici düştü") }

            val viewModel = viewModel()
            viewModel.startProfile()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Error)
            assertEquals(ProfileFailureStage.NOTES, (state as ProfileUiState.Error).stage)
        }

    @Test
    fun `not silinince eylem durumu basariya doner`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.deleteNote("note-1")
            advanceUntilIdle()

            assertEquals(ProfileActionState.NoteDeleted, viewModel.actionState.value)
            assertEquals(listOf("note-1"), noteRepository.deletedNoteIds)
        }

    @Test
    fun `silme basarisiz olursa hata bildirilir`() = runTest(mainDispatcherRule.testDispatcher) {
        noteRepository.deleteNoteError = IOException("silinemedi")

        val viewModel = viewModel()
        viewModel.deleteNote("note-1")
        advanceUntilIdle()

        assertTrue(viewModel.actionState.value is ProfileActionState.DeleteError)
    }

    @Test
    fun `silme surerken ikinci istek yok sayilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.deleteNote("note-1")
        viewModel.deleteNote("note-2")
        advanceUntilIdle()

        assertEquals(listOf("note-1"), noteRepository.deletedNoteIds)
    }

    @Test
    fun `cikis oturumu kapatir`() {
        authRepository.setUser(authenticatedUser())

        viewModel().signOut()

        assertEquals(1, authRepository.signOutCount)
    }
}
