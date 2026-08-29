package duygu.yilmaz.campusnote.ui.feed

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * [FeedViewModel] birim testleri.
 *
 * Uygulamanın ana kuralı burada uygulanıyor: kendi notunu yüklemeyen kullanıcı
 * bölüm feed'ini göremez. Kapı bir profil bayrağına değil, kullanıcının gerçekten
 * notu olup olmadığına bakıyor; bu testler kapının hem kapalı hem açık hâlini,
 * hem de kilidin yanlışlıkla açılabileceği kenar durumları sabitliyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()
    private val noteRepository = FakeNoteRepository()

    private fun viewModel() = FeedViewModel(
        authRepository = authRepository,
        userRepository = userRepository,
        noteRepository = noteRepository
    )

    @Test
    fun `oturum yoksa feed hic sorgulanmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(null)

        val viewModel = viewModel()
        viewModel.startFeed()
        advanceUntilIdle()

        assertEquals(FeedUiState.MissingSession, viewModel.uiState.value)
        assertTrue(userRepository.requestedUserIds.isEmpty())
        assertNull(noteRepository.observedDepartment)
    }

    @Test
    fun `notu olan kullanici kendi bolumunun notlarini gorur`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            userRepository.profile = userProfile(department = "Bilgisayar Mühendisliği")
            noteRepository.hasUploadedNote = true
            val posts = listOf(post(id = "note-1"), post(id = "note-2"))
            noteRepository.departmentNotes = flowOf(posts)

            val viewModel = viewModel()
            viewModel.startFeed()
            advanceUntilIdle()

            assertEquals(FeedUiState.Content(posts), viewModel.uiState.value)
            assertEquals("Bilgisayar Mühendisliği", noteRepository.observedDepartment)
            assertEquals(listOf("uid-1"), userRepository.requestedUserIds)
        }

    @Test
    fun `hic not yuklememis kullaniciya feed kilitli kalir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()
            noteRepository.hasUploadedNote = false
            noteRepository.departmentNotes = flowOf(listOf(post()))

            val viewModel = viewModel()
            viewModel.startFeed()
            advanceUntilIdle()

            assertEquals(FeedUiState.Locked, viewModel.uiState.value)
            // Kilitliyken bölüm sorgusu hiç kurulmamalı: aksi hâlde notlar
            // ekrana çizilmese de Firestore'dan okunmuş olurdu.
            assertNull(noteRepository.observedDepartment)
        }

    @Test
    fun `profil dokumani yoksa feed kilitli kalir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = null
            noteRepository.hasUploadedNote = true

            val viewModel = viewModel()
            viewModel.startFeed()
            advanceUntilIdle()

            assertEquals(FeedUiState.Locked, viewModel.uiState.value)
        }

    @Test
    fun `bolumu bos kalmis profil feed kilidini acmaz`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Boş bölüm herkesin notunu birbirine açacak bir joker olurdu.
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile(department = "   ")
            noteRepository.hasUploadedNote = true

            val viewModel = viewModel()
            viewModel.startFeed()
            advanceUntilIdle()

            assertEquals(FeedUiState.Locked, viewModel.uiState.value)
            assertNull(noteRepository.observedDepartment)
        }

    @Test
    fun `profil okunamazsa hata profil asamasini bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.getUserError = IOException("profil okunamadı")

            val viewModel = viewModel()
            viewModel.startFeed()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is FeedUiState.Error)
            assertEquals(FeedFailureStage.USER_PROFILE, (state as FeedUiState.Error).stage)
        }

    @Test
    fun `not akisi patlarsa hata notlar asamasini bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()
            noteRepository.hasUploadedNote = true
            noteRepository.departmentNotes = flow { throw IOException("dinleyici düştü") }

            val viewModel = viewModel()
            viewModel.startFeed()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is FeedUiState.Error)
            assertEquals(FeedFailureStage.NOTES, (state as FeedUiState.Error).stage)
        }

    @Test
    fun `feed once yukleniyor durumuna gecer`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.setUser(authenticatedUser())
        userRepository.profile = userProfile()
        noteRepository.hasUploadedNote = true
        noteRepository.departmentNotes = flowOf(listOf(post()))

        val viewModel = viewModel()
        viewModel.startFeed()

        // advanceUntilIdle() henüz çağrılmadı: coroutine kuyrukta bekliyor.
        assertEquals(FeedUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `feed durdurulduktan sonra gelen sonuc durumu degistirmez`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser())
            userRepository.profile = userProfile()
            noteRepository.hasUploadedNote = true
            noteRepository.departmentNotes = flowOf(listOf(post()))

            val viewModel = viewModel()
            viewModel.startFeed()
            viewModel.stopFeed()
            advanceUntilIdle()

            // Kullanıcı ekrandan ayrıldıysa geç gelen snapshot yazılmamalı.
            assertEquals(FeedUiState.Loading, viewModel.uiState.value)
        }
}
