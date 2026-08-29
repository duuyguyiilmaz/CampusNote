package duygu.yilmaz.campusnote.ui.editnote

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.data.model.NoteUpdate
import duygu.yilmaz.campusnote.data.repository.NoteNotOwnedException
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import duygu.yilmaz.campusnote.testing.post
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * [EditNoteViewModel] birim testleri.
 *
 * Düzenleme yetkisi iki katmanda kontrol ediliyor: ViewModel oturumu, repository ise
 * notun gerçek sahibini doğruluyor. Buradaki testler ikisinin de UI'a doğru durumu
 * ilettiğini sabitliyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditNoteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = EditNoteViewModel(
        authRepository = authRepository,
        noteRepository = noteRepository
    )

    private val update = NoteUpdate(
        course = "BIL201",
        title = "Güncellenmiş Başlık",
        description = "Yeni açıklama",
        tag = "özet"
    )

    @Test
    fun `bos kimlikle acilan ekran kayip notu bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())

            val viewModel = viewModel()
            viewModel.loadNote("")
            advanceUntilIdle()

            assertEquals(EditNoteUiState.MissingNote, viewModel.uiState.value)
        }

    @Test
    fun `oturum yoksa not okunmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertEquals(EditNoteUiState.MissingSession, viewModel.uiState.value)
    }

    @Test
    fun `not yuklendiginde formu dolduracak icerik gelir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            noteRepository.note = post(id = "note-1", title = "Veri Yapıları Özeti")

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()

            val state = viewModel.uiState.value as EditNoteUiState.Content
            assertEquals("Veri Yapıları Özeti", state.note.title)
        }

    @Test
    fun `silinmis not kayip olarak bildirilir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(authenticatedUser())
        noteRepository.note = null

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertEquals(EditNoteUiState.MissingNote, viewModel.uiState.value)
    }

    @Test
    fun `not okunamazsa hata durumu gosterilir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(authenticatedUser())
        noteRepository.getNoteError = IOException("okunamadı")

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is EditNoteUiState.Error)
    }

    @Test
    fun `kaydetme oturumdaki kullanicinin kimligiyle yapilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Sahiplik kontrolünü repository yapıyor; ViewModel doğru uid'yi
            // göndermezse kendi notunu düzenleyen kullanıcı reddedilirdi.
            authRepository.setUser(authenticatedUser(uid = "uid-1"))

            val viewModel = viewModel()
            viewModel.saveNote("note-1", update)
            advanceUntilIdle()

            assertEquals(EditNoteActionState.Success, viewModel.actionState.value)
            val applied = noteRepository.appliedUpdates.single()
            assertEquals("note-1", applied.noteId)
            assertEquals("uid-1", applied.uploaderUid)
            assertEquals(update, applied.update)
        }

    @Test
    fun `oturum yoksa kaydetme yapilmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.saveNote("note-1", update)
        advanceUntilIdle()

        assertEquals(EditNoteActionState.MissingSession, viewModel.actionState.value)
        assertTrue(noteRepository.appliedUpdates.isEmpty())
    }

    @Test
    fun `baskasinin notunu kaydetmek sahiplik hatasi verir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            noteRepository.updateNoteError = NoteNotOwnedException()

            val viewModel = viewModel()
            viewModel.saveNote("note-1", update)
            advanceUntilIdle()

            assertEquals(EditNoteActionState.NotOwner, viewModel.actionState.value)
        }

    @Test
    fun `beklenmeyen hata genel hata durumuna dusurulur`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            noteRepository.updateNoteError = IOException("ağ yok")

            val viewModel = viewModel()
            viewModel.saveNote("note-1", update)
            advanceUntilIdle()

            assertTrue(viewModel.actionState.value is EditNoteActionState.Error)
        }

    @Test
    fun `kaydetme surerken ikinci istek yok sayilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())

            val viewModel = viewModel()
            viewModel.saveNote("note-1", update)
            viewModel.saveNote("note-1", update.copy(title = "İkinci"))
            advanceUntilIdle()

            assertEquals(update, noteRepository.appliedUpdates.single().update)
        }
}
