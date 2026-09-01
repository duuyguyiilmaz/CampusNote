package duygu.yilmaz.campusnote.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Profilsiz kalmış bir oturumun toparlanması.
 *
 * Bu ekran yalnızca kayıt yarıda kaldığında ve geri almanın da düştüğünde ortaya
 * çıkan hesap için var. Yazdığı şeyin kaydınkiyle aynı olması gerekiyor: aynı uid,
 * oturumdaki e-posta, seçilen bölüm — güvenlik kuralı e-postayı token'la
 * karşılaştırdığı için istemcinin uydurduğu bir adres reddedilirdi.
 */
class CompleteProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(
        authenticatedUser(uid = "uid-1", email = "duygu@ogr.akdeniz.edu.tr")
    )
    private val userRepository = FakeUserRepository()

    private fun viewModel() = CompleteProfileViewModel(authRepository, userRepository)

    @Test
    fun `profil oturumdaki kimlik ve adresle yazilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.completeProfile("Bilgisayar Mühendisliği")
            advanceUntilIdle()

            assertEquals(CompleteProfileUiState.Success, viewModel.uiState.value)
            val saved = userRepository.savedUsers.single()
            assertEquals("uid-1", saved.id)
            assertEquals("duygu@ogr.akdeniz.edu.tr", saved.email)
            assertEquals("Bilgisayar Mühendisliği", saved.department)
        }

    @Test
    fun `oturum yoksa yazma denenmez`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(null)

            val viewModel = viewModel()
            viewModel.completeProfile("Bilgisayar Mühendisliği")
            advanceUntilIdle()

            assertEquals(CompleteProfileUiState.MissingSession, viewModel.uiState.value)
            assertTrue(userRepository.savedUsers.isEmpty())
        }

    @Test
    fun `yazma duserse hata gosterilir ve ekran kilitlenmez`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userRepository.saveUserError = IllegalStateException("firestore down")

            val viewModel = viewModel()
            viewModel.completeProfile("Bilgisayar Mühendisliği")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is CompleteProfileUiState.Error)
        }

    /** Çift dokunuş ikinci bir profil yazmamalı. */
    @Test
    fun `yukleme surerken ikinci istek yok sayilir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.completeProfile("Bilgisayar Mühendisliği")
            viewModel.completeProfile("Matematik")
            advanceUntilIdle()

            assertEquals(1, userRepository.savedUsers.size)
            assertEquals("Bilgisayar Mühendisliği", userRepository.savedUsers.single().department)
        }

    @Test
    fun `ekran hangi hesabin tamamlandigini gosterir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            assertEquals("duygu@ogr.akdeniz.edu.tr", viewModel().currentEmail())
        }

    @Test
    fun `cikis oturumu kapatir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel().signOut()

            assertEquals(1, authRepository.signOutCount)
        }
}
