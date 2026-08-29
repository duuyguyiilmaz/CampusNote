package duygu.yilmaz.campusnote.ui.main

import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.authenticatedUser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {

    private val authRepository = FakeAuthRepository()

    @Test
    fun `oturum acikken ana ekran kullaniciyi disari atmaz`() {
        authRepository.setUser(authenticatedUser())

        assertTrue(MainViewModel(authRepository).hasAuthenticatedUser())
    }

    @Test
    fun `oturum kapaliysa ana ekran giris ekranina yonlendirir`() {
        authRepository.setUser(null)

        assertFalse(MainViewModel(authRepository).hasAuthenticatedUser())
    }
}
