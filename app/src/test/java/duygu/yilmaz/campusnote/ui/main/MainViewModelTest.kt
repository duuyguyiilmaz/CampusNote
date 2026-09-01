package duygu.yilmaz.campusnote.ui.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.MainDispatcherRule
import duygu.yilmaz.campusnote.testing.authenticatedUser
import duygu.yilmaz.campusnote.testing.userProfile
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    private fun viewModel() = MainViewModel(authRepository, userRepository)

    @Test
    fun `oturum acikken ana ekran kullaniciyi disari atmaz`() {
        authRepository.setUser(authenticatedUser())

        assertTrue(viewModel().hasAuthenticatedUser())
    }

    @Test
    fun `oturum kapaliysa ana ekran giris ekranina yonlendirir`() {
        authRepository.setUser(null)

        assertFalse(viewModel().hasAuthenticatedUser())
    }

    @Test
    fun `oturum yoksa rota giris ekranidir`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(null)

            val viewModel = viewModel()
            viewModel.resolveRoute()
            advanceUntilIdle()

            assertEquals(MainRoute.SignedOut, viewModel.route.value)
        }

    @Test
    fun `profili olan oturum uygulamaya girer`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            userRepository.profile = userProfile(department = "Bilgisayar Mühendisliği")

            val viewModel = viewModel()
            viewModel.resolveRoute()
            advanceUntilIdle()

            assertEquals(MainRoute.Ready, viewModel.route.value)
        }

    /**
     * Auth hesabı olup profili olmayan oturum: kayıt yarıda kalmış ve geri alma da
     * düşmüş demektir. O hesapla uygulamada dolaşmak çıkmaz — feed kilitli, yükleme
     * bölüm istiyor — tek yolu profili tamamlamak.
     */
    @Test
    fun `profili olmayan oturum tamamlama ekranina gider`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            userRepository.profile = null

            val viewModel = viewModel()
            viewModel.resolveRoute()
            advanceUntilIdle()

            assertEquals(MainRoute.NeedsProfile, viewModel.route.value)
        }

    /**
     * "Profil okunamadı" ile "profil yok" aynı şey değil. Ağ düştüğü için okunamayan
     * profil kullanıcıyı tamamlama ekranına atmamalı: orada da yazamaz, ve elindeki
     * profil aslında yerinde duruyordur. Uygulama normal açılır, ekranlar hatayı
     * kendi yeniden deneme mesajlarıyla gösterir.
     */
    @Test
    fun `profil okunamadiginda kullanici tamamlama ekranina atilmaz`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.setUser(authenticatedUser(uid = "uid-1"))
            userRepository.getUserError = IllegalStateException("network down")

            val viewModel = viewModel()
            viewModel.resolveRoute()
            advanceUntilIdle()

            assertEquals(MainRoute.Ready, viewModel.route.value)
        }
}
