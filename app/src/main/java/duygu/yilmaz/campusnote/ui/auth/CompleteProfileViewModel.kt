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

/**
 * Auth hesabı olup Firestore profili olmayan oturumu toparlar.
 *
 * Kayıt iki ayrı sisteme yazıyor. [RegisterViewModel] profil yazılamadığında Auth
 * hesabını geri alıyor, ama o telafi de düşebilir — ve düştüğünde geriye kimsenin
 * çıkamadığı bir hesap kalıyor: kişi Firebase'de var, aynı adresle yeniden kayıt
 * olamıyor, profili olmadığı için feed kilitli ve yükleme çalışmıyor.
 *
 * Buradaki ekran o hesabın tek çıkışı. Yeni bir hesap açmıyor; var olan oturumun
 * uid'si ve e-postasıyla eksik profili yazıyor. E-posta oturumdan alınıyor çünkü
 * güvenlik kuralı zaten `request.auth.token.email` ile karşılaştırıyor — kullanıcıya
 * sorulacak tek şey bölüm.
 */
class CompleteProfileViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<CompleteProfileUiState>(CompleteProfileUiState.Idle)
    val uiState: LiveData<CompleteProfileUiState> = _uiState

    /** Oturumun e-postası; ekranda "hangi hesabı tamamlıyorsun" olarak gösteriliyor. */
    fun currentEmail(): String = authRepository.currentUser()?.email.orEmpty()

    fun completeProfile(department: String) {
        if (_uiState.value == CompleteProfileUiState.Loading) return

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = CompleteProfileUiState.MissingSession
            return
        }

        _uiState.value = CompleteProfileUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                userRepository.saveUser(
                    UserProfile(
                        id = user.uid,
                        email = user.email,
                        department = department
                    )
                )
                CompleteProfileUiState.Success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                CompleteProfileUiState.Error(exception)
            }
        }
    }

    /**
     * Tamamlamadan vazgeçen kullanıcıyı oturumdan çıkarır.
     *
     * Profilsiz bir oturumla uygulamada dolaşmanın anlamı yok — feed kilitli, yükleme
     * bölüm istiyor, profil ekranı boş. Çıkış, giriş ekranına dönüp başka bir hesapla
     * devam etmenin yolu.
     */
    fun signOut() {
        authRepository.signOut()
    }
}

sealed interface CompleteProfileUiState {
    data object Idle : CompleteProfileUiState
    data object Loading : CompleteProfileUiState
    data object Success : CompleteProfileUiState

    /** Oturum bu ekran açıkken kaybolduysa: yazacak bir uid yok. */
    data object MissingSession : CompleteProfileUiState

    data class Error(val exception: Exception) : CompleteProfileUiState
}
