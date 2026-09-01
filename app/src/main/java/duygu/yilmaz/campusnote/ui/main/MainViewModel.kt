package duygu.yilmaz.campusnote.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseUserRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {

    private val _route = MutableLiveData<MainRoute>()
    val route: LiveData<MainRoute> = _route

    fun hasAuthenticatedUser(): Boolean = authRepository.currentUser() != null

    /**
     * Oturumun kullanılabilir olup olmadığına karar verir.
     *
     * Auth hesabı olup Firestore profili olmayan bir oturum mümkün: kayıt iki ayrı
     * sisteme yazıyor ve ikincisi düşerse — üstelik geri alma da düşerse — geriye
     * profilsiz bir hesap kalıyor. O hesapla uygulamada dolaşmak bir çıkmaz: feed
     * kilitli, yükleme bölüm istiyor, profil ekranı boş. Böyle bir oturum profil
     * tamamlama ekranına gidiyor.
     *
     * Profilin *okunamaması* ile *olmaması* ayrı tutuluyor. Ağ düşmesi yüzünden
     * okunamayan profil kullanıcıyı tamamlama ekranına atmamalı — orada zaten
     * yazamaz. Bu durumda uygulama normal açılıyor; ekranlar profil hatasını kendi
     * yeniden deneme mesajlarıyla gösteriyor.
     */
    fun resolveRoute() {
        val user = authRepository.currentUser()
        if (user == null) {
            _route.value = MainRoute.SignedOut
            return
        }

        viewModelScope.launch {
            _route.value = try {
                if (userRepository.getUser(user.uid) == null) {
                    MainRoute.NeedsProfile
                } else {
                    MainRoute.Ready
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                MainRoute.Ready
            }
        }
    }
}

sealed interface MainRoute {
    data object SignedOut : MainRoute
    data object NeedsProfile : MainRoute
    data object Ready : MainRoute
}
