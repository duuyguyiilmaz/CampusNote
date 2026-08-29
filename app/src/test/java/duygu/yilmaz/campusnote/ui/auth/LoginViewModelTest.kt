package duygu.yilmaz.campusnote.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()

    private fun viewModel() = LoginViewModel(authRepository = authRepository)

    @Test
    fun `acik oturum varken giris ekrani atlanabilir`() {
        authRepository.setUser(authenticatedUser())

        assertTrue(viewModel().hasAuthenticatedUser())
    }

    @Test
    fun `oturum yoksa giris ekrani gosterilir`() {
        authRepository.setUser(null)

        assertFalse(viewModel().hasAuthenticatedUser())
    }

    @Test
    fun `basarili giris girilen adresle yapilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.signIn("duygu@ogr.akdeniz.edu.tr", "gizli123")
        advanceUntilIdle()

        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        assertEquals("duygu@ogr.akdeniz.edu.tr" to "gizli123", authRepository.lastCredentials)
    }

    @Test
    fun `hatali giris auth asamasini bildirir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInError = IOException("şifre yanlış")

        val viewModel = viewModel()
        viewModel.signIn("duygu@ogr.akdeniz.edu.tr", "yanlis")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(AuthFailureStage.AUTHENTICATION, (state as AuthUiState.Error).stage)
    }

    @Test
    fun `giris surerken ikinci istek yok sayilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.signIn("duygu@ogr.akdeniz.edu.tr", "gizli123")
        viewModel.signIn("duygu@ogr.akdeniz.edu.tr", "gizli123")
        advanceUntilIdle()

        assertEquals(1, authRepository.signInCount)
    }

    @Test
    fun `durum sifirlanabilir`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInError = IOException("hata")

        val viewModel = viewModel()
        viewModel.signIn("duygu@ogr.akdeniz.edu.tr", "yanlis")
        advanceUntilIdle()
        viewModel.resetState()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }
}
