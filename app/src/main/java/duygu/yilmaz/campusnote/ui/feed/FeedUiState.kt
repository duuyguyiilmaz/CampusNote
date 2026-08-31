package duygu.yilmaz.campusnote.ui.feed

import duygu.yilmaz.campusnote.data.model.Post

sealed interface FeedUiState {
    data object Idle : FeedUiState
    data object Loading : FeedUiState
    data object Locked : FeedUiState
    data object MissingSession : FeedUiState
    /**
     * @param canLoadMore sorgu istenen kadar not döndürdüyse arkada daha olabilir.
     *   Toplam sayı bilinmiyor — öğrenmek fazladan bir okuma demek olurdu — o yüzden
     *   "dolu geldi" kabaca "devamı var" sayılıyor. Yanılma payı son sayfada bir kez
     *   fazladan istek atmak; kullanıcıya boş bir sayfa göstermez, çünkü aynı sonuç
     *   yeniden gelir ve bayrak kapanır.
     */
    data class Content(
        val posts: List<Post>,
        val canLoadMore: Boolean
    ) : FeedUiState
    data class Error(
        val exception: Exception,
        val stage: FeedFailureStage
    ) : FeedUiState
}

enum class FeedFailureStage {
    USER_PROFILE,
    NOTES
}
