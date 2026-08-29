package duygu.yilmaz.campusnote.ui.upload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.data.model.UserProfile
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import duygu.yilmaz.campusnote.testing.noteDraft
import duygu.yilmaz.campusnote.testing.userProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * [UploadViewModel] birim testleri.
 *
 * Yükleme, katkı kapısını açan tek eylem. Bu yüzden nota yazılan bölüm ve e-posta
 * bilgisinin doğruluğu kritik: yanlış bölüm yazılan not, sahibinin feed'ini açmadığı
 * gibi başka bir bölümün akışına da düşer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()
    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = UploadViewModel(
        authRepository = authRepository,
        userRepository = userRepository,
        noteRepository = noteRepository
    )

    @Test
    fun `oturum yoksa not olusturulmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.uploadNote(noteDraft())
        advanceUntilIdle()

        assertEquals(UploadUiState.MissingSession, viewModel.uiState.value)
        assertTrue(noteRepository.createdNotes.isEmpty())
    }

    @Test
    fun `not kullanicinin bolumu ve kimligiyle olusturulur`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(
                authenticatedUser(uid = "uid-1", email = "duygu@ogr.akdeniz.edu.tr")
            )
            userRepository.profile = userProfile(department = "Bilgisayar Mühendisliği")
            val draft = noteDraft(course = "BIL201")

            val viewModel = viewModel()
            viewModel.uploadNote(draft)
            advanceUntilIdle()

            assertEquals(UploadUiState.Success, viewModel.uiState.value)
            val created = noteRepository.createdNotes.single()
            assertEquals(draft, created.draft)
            assertEquals("uid-1", created.uploaderUid)
            assertEquals("duygu@ogr.akdeniz.edu.tr", created.uploaderEmail)
            assertEquals("Bilgisayar Mühendisliği", created.department)
        }

    @Test
    fun `oturumda e-posta yoksa profildeki adres kullanilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1", email = ""))
            userRepository.profile = userProfile(email = "yedek@ogr.akdeniz.edu.tr")

            val viewModel = viewModel()
            viewModel.uploadNote(noteDraft())
            advanceUntilIdle()

            assertEquals(
                "yedek@ogr.akdeniz.edu.tr",
                noteRepository.createdNotes.single().uploaderEmail
            )
        }

    @Test
    fun `bolumu bos profilde nota bilinmiyor yazilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Boş bölüm Firestore'a yazılırsa `whereEqualTo` sorguları bu notu
            // rastgele başka bir kullanıcının feed'ine düşürebilir.
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile(department = "")

            val viewModel = viewModel()
            viewModel.uploadNote(noteDraft())
            advanceUntilIdle()

            assertEquals(
                UserProfile.UNKNOWN_DEPARTMENT,
                noteRepository.createdNotes.single().department
            )
        }

    @Test
    fun `profil dokumani yoksa yukleme profil asamasinda durur`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = null

            val viewModel = viewModel()
            viewModel.uploadNote(noteDraft())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is UploadUiState.Error)
            assertEquals(UploadFailureStage.USER_PROFILE, (state as UploadUiState.Error).stage)
            assertTrue(noteRepository.createdNotes.isEmpty())
        }

    @Test
    fun `not yazilamazsa hata not asamasini bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()
            noteRepository.createNoteError = IOException("yazılamadı")

            val viewModel = viewModel()
            viewModel.uploadNote(noteDraft())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is UploadUiState.Error)
            assertEquals(UploadFailureStage.NOTE, (state as UploadUiState.Error).stage)
        }

    @Test
    fun `yukleme surerken ikinci istek yok sayilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Butona iki kez basmak aynı notu iki kez yüklememeli.
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()

            val viewModel = viewModel()
            viewModel.uploadNote(noteDraft(title = "İlk"))
            assertEquals(UploadUiState.Loading, viewModel.uiState.value)
            viewModel.uploadNote(noteDraft(title = "İkinci"))
            advanceUntilIdle()

            assertEquals(UploadUiState.Success, viewModel.uiState.value)
            assertEquals("İlk", noteRepository.createdNotes.single().draft.title)
        }

    @Test
    fun `durum sifirlanabilir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.uploadNote(noteDraft())
        viewModel.resetState()

        assertEquals(UploadUiState.Idle, viewModel.uiState.value)
    }
}
