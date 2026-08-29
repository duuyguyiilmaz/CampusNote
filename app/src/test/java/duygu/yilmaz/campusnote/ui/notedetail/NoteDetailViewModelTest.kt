package duygu.yilmaz.campusnote.ui.notedetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.data.model.NoteFileType
import duygu.yilmaz.campusnote.data.model.RatingResult
import duygu.yilmaz.campusnote.data.repository.NoteNotFoundException
import duygu.yilmaz.campusnote.data.repository.OwnNoteRatingException
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.FakeRatingRepository
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
 * [NoteDetailViewModel] birim testleri.
 *
 * İki ayrı yükleme akışı var — metadata ve base64 dosya içeriği — ve puanlamanın
 * kendi kuralları. Dosya içeriği ayrı dokümanda tutulduğu için, dosyası olmayan
 * notta o okumanın hiç yapılmadığını da doğruluyoruz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val ratingRepository = FakeRatingRepository()
    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = NoteDetailViewModel(
        authRepository = authRepository,
        ratingRepository = ratingRepository,
        noteRepository = noteRepository
    )

    @Test
    fun `bos not kimligiyle acilan ekran kayip notu bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.loadNote("")
            advanceUntilIdle()

            assertEquals(NoteDetailUiState.Missing, viewModel.noteState.value)
        }

    @Test
    fun `silinmis not kayip olarak gosterilir`() = runTest(mainDispatcherRule.testDispatcher) {
        noteRepository.note = null

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertEquals(NoteDetailUiState.Missing, viewModel.noteState.value)
    }

    @Test
    fun `dosyasiz notta icerik okunmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        noteRepository.note = post(fileType = NoteFileType.NONE)
        noteRepository.noteFile = "okunmamali"

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertTrue(viewModel.noteState.value is NoteDetailUiState.Content)
        assertEquals(NoteFileUiState.None, viewModel.fileState.value)
    }

    @Test
    fun `dosyali notun icerigi metadata sonrasi yuklenir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            noteRepository.note = post(fileType = NoteFileType.PDF)
            noteRepository.noteFile = "JVBERi0x"

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()

            assertEquals(NoteFileUiState.Content("JVBERi0x"), viewModel.fileState.value)
        }

    @Test
    fun `dosya dokumani bulunamazsa metadata yine de gosterilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Not okunabiliyorsa başlık ve açıklama ekranda kalmalı; sadece
            // dosya bölümü "eksik" olmalı.
            noteRepository.note = post(fileType = NoteFileType.IMAGE)
            noteRepository.noteFile = null

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()

            assertTrue(viewModel.noteState.value is NoteDetailUiState.Content)
            assertEquals(NoteFileUiState.Missing, viewModel.fileState.value)
        }

    @Test
    fun `dosya okunamazsa hata sadece dosya durumuna yansir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            noteRepository.note = post(fileType = NoteFileType.PDF)
            noteRepository.getNoteFileError = IOException("indirilemedi")

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()

            assertTrue(viewModel.noteState.value is NoteDetailUiState.Content)
            assertTrue(viewModel.fileState.value is NoteFileUiState.Error)
        }

    @Test
    fun `yuklenmis not tekrar sorgulanmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        // Fragment yeniden oluşturulduğunda aynı notu ikinci kez indirmek
        // hem gereksiz okuma hem de görünen bir yeniden yükleme titremesi olurdu.
        noteRepository.note = post(id = "note-1")

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        noteRepository.note = post(id = "note-2", title = "Baska Not")
        viewModel.loadNote("note-2")
        advanceUntilIdle()

        val state = viewModel.noteState.value as NoteDetailUiState.Content
        assertEquals("note-1", state.post.id)
    }

    @Test
    fun `kendi notunu puanlamak engellenir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(authenticatedUser(uid = "uid-1"))
        noteRepository.note = post(uploaderUid = "uid-1")

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertEquals(RatingAvailability.OWN_NOTE, viewModel.ratingAvailability())
    }

    @Test
    fun `baskasinin notu puanlanabilir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(authenticatedUser(uid = "uid-1"))
        noteRepository.note = post(uploaderUid = "uid-2")

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertEquals(RatingAvailability.ALLOWED, viewModel.ratingAvailability())
    }

    @Test
    fun `oturum yoksa puanlama kapali`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)
        noteRepository.note = post()

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()

        assertEquals(RatingAvailability.MISSING_SESSION, viewModel.ratingAvailability())
    }

    @Test
    fun `not yuklenmeden puanlama kapali`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(authenticatedUser())

        assertEquals(RatingAvailability.MISSING_NOTE, viewModel().ratingAvailability())
    }

    @Test
    fun `basarili oy ekrandaki ortalamayi da gunceller`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Ekran, yeniden yüklemeye gerek kalmadan yeni ortalamayı göstermeli.
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            noteRepository.note = post(id = "note-1", uploaderUid = "uid-2")
            ratingRepository.result = RatingResult(
                average = 4.5,
                count = 4L,
                sum = 18L,
                updatedExistingRating = false
            )

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()
            viewModel.submitRating(5)
            advanceUntilIdle()

            assertTrue(viewModel.ratingState.value is RatingUiState.Success)
            assertEquals(
                FakeRatingRepository.Submission("note-1", "uid-1", 5),
                ratingRepository.submissions.single()
            )

            val post = (viewModel.noteState.value as NoteDetailUiState.Content).post
            assertEquals(4.5, post.avgRating, 1e-9)
            assertEquals(4L, post.ratingCount)
            assertEquals(18L, post.ratingSum)
        }

    @Test
    fun `repository kendi notu hatasi verirse ozel durum gosterilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // ViewModel önden engelliyor, ama sunucu tarafı da aynı kuralı uyguluyor;
            // arada not sahibi değişmişse gelen hata anlaşılır bir mesaja dönmeli.
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            noteRepository.note = post(uploaderUid = "uid-2")
            ratingRepository.error = OwnNoteRatingException()

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()
            viewModel.submitRating(4)
            advanceUntilIdle()

            assertEquals(RatingUiState.OwnNote, viewModel.ratingState.value)
        }

    @Test
    fun `puanlama sirasinda not silinmisse kayip not bildirilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            noteRepository.note = post(uploaderUid = "uid-2")
            ratingRepository.error = NoteNotFoundException()

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()
            viewModel.submitRating(4)
            advanceUntilIdle()

            assertEquals(RatingUiState.MissingNote, viewModel.ratingState.value)
        }

    @Test
    fun `beklenmeyen hata genel hata durumuna dusurulur`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            noteRepository.note = post(uploaderUid = "uid-2")
            ratingRepository.error = IOException("ağ yok")

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()
            viewModel.submitRating(4)
            advanceUntilIdle()

            assertTrue(viewModel.ratingState.value is RatingUiState.Error)
        }

    @Test
    fun `oturum yokken oy gonderilmez`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)
        noteRepository.note = post()

        val viewModel = viewModel()
        viewModel.loadNote("note-1")
        advanceUntilIdle()
        viewModel.submitRating(4)
        advanceUntilIdle()

        assertEquals(RatingUiState.MissingSession, viewModel.ratingState.value)
        assertTrue(ratingRepository.submissions.isEmpty())
    }

    @Test
    fun `oy gonderilirken ikinci istek yok sayilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            noteRepository.note = post(uploaderUid = "uid-2")

            val viewModel = viewModel()
            viewModel.loadNote("note-1")
            advanceUntilIdle()

            viewModel.submitRating(5)
            viewModel.submitRating(1)
            advanceUntilIdle()

            assertEquals(5, ratingRepository.submissions.single().rating)
        }
}
