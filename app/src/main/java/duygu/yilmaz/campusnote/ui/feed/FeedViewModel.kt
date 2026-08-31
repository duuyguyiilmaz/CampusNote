package duygu.yilmaz.campusnote.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseAuthRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseNoteRepository
import duygu.yilmaz.campusnote.data.repository.FirebaseUserRepository
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FeedViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val noteRepository: NoteRepository = FirebaseNoteRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<FeedUiState>(FeedUiState.Idle)
    val uiState: LiveData<FeedUiState> = _uiState

    private var feedJob: Job? = null

    /**
     * Şu an dinlenen pencerenin büyüklüğü. Sayfalama ikinci bir sorgu açıp sonuçları
     * birleştirerek değil, aynı dinleyiciyi daha geniş bir limitle yeniden kurarak
     * yapılıyor: feed canlı bir Firestore dinleyicisi, ikiye bölünürse hangi yarının
     * güncel olduğu belirsizleşirdi.
     *
     * Bedeli açık: pencere büyüdüğünde baştaki notlar yeniden okunuyor, yani 20 + 40
     * = 60 doküman okuması olur, 40 değil. Bugünkü davranış — her açılışta bölümün
     * *bütün* notları — buna göre çok daha pahalı, ve kullanıcıların çoğu ilk sayfanın
     * ötesine hiç gitmiyor.
     */
    private var pageSize = PAGE_SIZE

    fun startFeed() {
        feedJob?.cancel()
        pageSize = PAGE_SIZE
        observeFeed()
    }

    /**
     * Bir sayfa daha ister. Yükleme sürerken ya da elde son sayfa varken çağrılması
     * etkisiz: liste sonuna her gelindiğinde tetiklendiği için korumasız bırakmak
     * aynı pencereyi üst üste yeniden kurardı.
     */
    fun loadMore() {
        val state = _uiState.value
        if (state !is FeedUiState.Content || !state.canLoadMore) return

        pageSize += PAGE_SIZE
        observeFeed()
    }

    private fun observeFeed() {
        feedJob?.cancel()

        val user = authRepository.currentUser()
        if (user == null) {
            _uiState.value = FeedUiState.MissingSession
            return
        }

        // Sonraki sayfalarda Loading'e düşmek listeyi ekrandan silerdi; yalnızca
        // elde gösterilecek bir şey yokken yükleme durumuna geçiliyor.
        if (_uiState.value !is FeedUiState.Content) {
            _uiState.value = FeedUiState.Loading
        }
        feedJob = viewModelScope.launch {
            // Kapı, kullanıcının gerçekten notu olup olmadığına bakılarak belirlenir;
            // profildeki bir bayrağa değil. Not silindiğinde erişim de geri alınır.
            val department = try {
                val profile = userRepository.getUser(user.uid)
                if (profile != null && noteRepository.hasUploadedNote(user.uid)) {
                    profile.department
                } else {
                    ""
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = FeedUiState.Error(
                    exception = exception,
                    stage = FeedFailureStage.USER_PROFILE
                )
                return@launch
            }

            if (department.isBlank()) {
                _uiState.value = FeedUiState.Locked
                return@launch
            }

            val requested = pageSize
            noteRepository.observeNotesByDepartment(department, requested)
                .catch { throwable ->
                    _uiState.value = FeedUiState.Error(
                        exception = throwable as? Exception ?: Exception(throwable),
                        stage = FeedFailureStage.NOTES
                    )
                }
                .collect { posts ->
                    _uiState.value = FeedUiState.Content(
                        posts = posts,
                        canLoadMore = posts.size.toLong() >= requested
                    )
                }
        }
    }

    private companion object {
        /** Bir ekrana sığandan biraz fazlası; kaydırma sonuna gelmeden yenisi gelir. */
        const val PAGE_SIZE = 20L
    }

    fun stopFeed() {
        feedJob?.cancel()
        feedJob = null
    }

    override fun onCleared() {
        stopFeed()
        super.onCleared()
    }
}
