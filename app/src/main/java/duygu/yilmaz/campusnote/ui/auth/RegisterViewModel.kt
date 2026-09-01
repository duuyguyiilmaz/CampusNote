package duygu.yilmaz.campusnote.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.model.UserProfile
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseUserRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun register(email: String, password: String, department: String) {
        if (_uiState.value == AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val authenticatedUser = try {
                authRepository.register(email, password)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.Error(
                    exception = exception,
                    stage = AuthFailureStage.AUTHENTICATION
                )
                return@launch
            }

            _uiState.value = try {
                userRepository.saveUser(
                    UserProfile(
                        id = authenticatedUser.uid,
                        email = email,
                        department = department
                        // createdAt sunucuda yazılıyor; bkz. FirebaseUserRepository.
                    )
                )
                AuthUiState.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                rollBackAccount()
                AuthUiState.Error(
                    exception = exception,
                    stage = AuthFailureStage.USER_PROFILE
                )
            }
        }
    }

    /**
     * Profil yazılamadığında az önce açılan Auth hesabını geri alır.
     *
     * Kayıt iki sisteme yazıyor ve arada yarıda kalabiliyor. Geri almazsak kişi
     * Firebase'de var olur, aynı adresle yeniden kayıt olamaz ve profili olmadığı
     * için uygulamayı da kullanamaz — kimsenin çıkamadığı bir durum.
     *
     * Silmenin kendisi de düşebilir; o zaman kullanıcıya gösterilecek mesaj
     * değişmiyor, çünkü kullanıcı açısından sonuç aynı: kayıt olmadı. Fark, hesabın
     * ortada kalmasında ve onu [duygu.yilmaz.campusnote.ui.profile.CompleteProfileActivity]
     * topluyor. Bu yüzden burada hata yutuluyor — telafi başarısız olduğu için
     * asıl hatanın üstünü örtmenin anlamı yok.
     */
    private suspend fun rollBackAccount() {
        try {
            authRepository.deleteCurrentUser()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Bilinçli olarak yutuluyor; yukarıdaki açıklamaya bakın.
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
