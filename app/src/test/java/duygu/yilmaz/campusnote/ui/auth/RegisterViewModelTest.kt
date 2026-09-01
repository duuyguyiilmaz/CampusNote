package duygu.yilmaz.campusnote.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * [RegisterViewModel] birim testleri.
 *
 * Kayıt iki adımlı: önce Firebase Auth hesabı, sonra Firestore profil dokümanı.
 * İkinci adım sessizce başarısız olursa kullanıcının hesabı var ama bölümü yok —
 * feed'i hiçbir zaman açılmaz. Bu yüzden hangi adımda düştüğü ayrı ayrı raporlanıyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    private fun viewModel() = RegisterViewModel(
        authRepository = authRepository,
        userRepository = userRepository
    )

    @Test
    fun `basarili kayit profili de yazar`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.register(
            email = "duygu@ogr.akdeniz.edu.tr",
            password = "gizli123",
            department = "Bilgisayar Mühendisliği"
        )
        advanceUntilIdle()

        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        val saved = userRepository.savedUsers.single()
        assertEquals("duygu@ogr.akdeniz.edu.tr", saved.email)
        assertEquals("Bilgisayar Mühendisliği", saved.department)
    }

    @Test
    fun `auth adimi duserse profil yazilmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.registerError = IOException("hesap açılamadı")

        val viewModel = viewModel()
        viewModel.register("duygu@ogr.akdeniz.edu.tr", "gizli123", "Bilgisayar Mühendisliği")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(AuthFailureStage.AUTHENTICATION, (state as AuthUiState.Error).stage)
        assertTrue(userRepository.savedUsers.isEmpty())
    }

    @Test
    fun `profil yazilamazsa hata profil asamasini bildirir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userRepository.saveUserError = IOException("yazılamadı")

            val viewModel = viewModel()
            viewModel.register("duygu@ogr.akdeniz.edu.tr", "gizli123", "Bilgisayar Mühendisliği")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AuthUiState.Error)
            assertEquals(AuthFailureStage.USER_PROFILE, (state as AuthUiState.Error).stage)
        }

    @Test
    fun `kayit surerken ikinci istek yok sayilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.register("duygu@ogr.akdeniz.edu.tr", "gizli123", "Bilgisayar Mühendisliği")
        assertEquals(AuthUiState.Loading, viewModel.uiState.value)
        viewModel.register("baska@ogr.akdeniz.edu.tr", "gizli123", "Elektrik")
        advanceUntilIdle()

        assertEquals(1, authRepository.registerCount)
    }

    @Test
    fun `durum sifirlanabilir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.registerError = IOException("hata")

        val viewModel = viewModel()
        viewModel.register("duygu@ogr.akdeniz.edu.tr", "gizli123", "Bilgisayar Mühendisliği")
        advanceUntilIdle()
        viewModel.resetState()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    /**
     * Kayıt iki ayrı sisteme yazıyor. Profil düşerse Auth hesabı geride kalır ve
     * kimse ona ulaşamaz: kişi Firebase'de vardır, aynı adresle yeniden kayıt
     * olamaz, profili olmadığı için uygulamayı da kullanamaz.
     */
    @Test
    fun `profil yazilamazsa auth hesabi geri alinir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userRepository.saveUserError = IllegalStateException("firestore down")

            val viewModel = viewModel()
            viewModel.register("duygu@ogr.akdeniz.edu.tr", "sifre123", "Bilgisayar Mühendisliği")
            advanceUntilIdle()

            assertEquals(1, authRepository.deleteCount)
            assertNull(authRepository.currentUser())
        }

    @Test
    fun `auth adimi duserse silinecek hesap olmadigi icin geri alma calismaz`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.registerError = IllegalStateException("auth down")

            val viewModel = viewModel()
            viewModel.register("duygu@ogr.akdeniz.edu.tr", "sifre123", "Bilgisayar Mühendisliği")
            advanceUntilIdle()

            assertEquals(0, authRepository.deleteCount)
        }

    /**
     * Telafi de düşebilir. O zaman kullanıcıya gösterilen mesaj değişmemeli — sonuç
     * onun açısından aynı, kayıt olmadı — ve hata yutulup ekran kilitlenmemeli.
     * Ortada kalan hesabı CompleteProfileViewModel topluyor.
     */
    @Test
    fun `geri alma da duserse kullaniciya yine profil hatasi gosterilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userRepository.saveUserError = IllegalStateException("firestore down")
            authRepository.deleteError = IllegalStateException("delete failed")

            val viewModel = viewModel()
            viewModel.register("duygu@ogr.akdeniz.edu.tr", "sifre123", "Bilgisayar Mühendisliği")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AuthUiState.Error)
            assertEquals(AuthFailureStage.USER_PROFILE, (state as AuthUiState.Error).stage)
        }
}
