package duygu.yilmaz.campusnote.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.databinding.FragmentFeedBinding
import duygu.yilmaz.campusnote.ui.common.PostAdapter
import duygu.yilmaz.campusnote.ui.main.MainActivity
import duygu.yilmaz.campusnote.ui.notedetail.NoteDetailFragment

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter

    /**
     * Yalnızca ekran testlerinin sahte repository'lerle kurulmuş bir ViewModel
     * verebilmesi için var; üretimde her zaman null kalır ve varsayılan fabrika
     * kullanılır. [FeedViewModel]'in parametreleri varsayılan değerli olduğu için
     * varsayılan fabrika onu argümansız kurabiliyor.
     */
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory? = null

    private val viewModel: FeedViewModel by lazy {
        viewModelFactory
            ?.let { ViewModelProvider(this, it)[FeedViewModel::class.java] }
            ?: ViewModelProvider(this)[FeedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostAdapter(
            onItemClick = { post ->
                val fragment = NoteDetailFragment.newInstance(post.id)

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.rvFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeed.adapter = adapter

        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)

        binding.btnUpload.setOnClickListener {
            (activity as? MainActivity)?.selectUploadTab()
        }

        // Feed canlı bir Firestore dinleyicisiyle beslendiği için veri normalde hep
        // güncel. Yine de aşağı çekme bir işe yarıyor: dinleyici hata aldığında
        // (ör. bağlantı koptuğunda) yeniden denemenin tek yolu bu.
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.startFeed()
        }

        animateViews()
    }

    override fun onDestroyView() {
        binding.rvFeed.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun animateViews() {
        binding.tvFeedTitle.alpha = 0f
        binding.tvFeedTitle.translationY = -20f
        binding.tvFeedTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        binding.tvFeedSubtitle.alpha = 0f
        binding.tvFeedSubtitle.translationY = -15f
        binding.tvFeedSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .start()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startFeed()
    }

    override fun onStop() {
        viewModel.stopFeed()
        super.onStop()
    }

    private fun renderState(state: FeedUiState) {
        // Yükleme sürerken çark dönmeye devam etmeli; kalan durumların hepsi bitiş.
        if (state != FeedUiState.Idle && state != FeedUiState.Loading) {
            binding.swipeRefresh.isRefreshing = false
        }

        when (state) {
            FeedUiState.Idle,
            FeedUiState.Loading -> Unit

            FeedUiState.Locked -> {
                adapter.refresh(emptyList())
                showFeed(false)
            }

            FeedUiState.MissingSession -> {
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
            }

            is FeedUiState.Content -> {
                adapter.refresh(state.posts)
                showFeed(true)
            }

            // Her iki hata da yeniden denenebilir: ikisi de startFeed()'in adımları.
            is FeedUiState.Error -> showRetryableError(
                message = when (state.stage) {
                    FeedFailureStage.USER_PROFILE -> getString(
                        R.string.error_user_profile_read,
                        state.exception.message.orEmpty()
                    )

                    FeedFailureStage.NOTES -> getString(R.string.error_feed_notes)
                }
            )
        }
    }

    /**
     * Hata mesajını yeniden deneme eylemiyle birlikte gösterir.
     *
     * Dinleyici hatası eskiden yalnızca logcat'e yazılıyordu: ekran olduğu gibi
     * kalıyor, kullanıcı boş bir feed görüp bunun kendi notu olmadığından mı yoksa
     * bağlantıdan mı kaynaklandığını anlayamıyordu. Toast yerine Snackbar seçildi,
     * çünkü asıl eksik olan mesaj değil, tekrar deneme yolu — aşağı çekme jesti
     * bunu zaten yapıyor ama ekranda onu ima eden hiçbir şey yok.
     *
     * Fragment'ın kökü sekme çubuğunun arkasına kadar uzandığı için snackbar
     * `bottomNav`'a bağlanıyor; bağlanmasaydı çubuğun üstüne çizilirdi.
     */
    private fun showRetryableError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.bottomNav)
            .setAction(R.string.action_retry) { viewModel.startFeed() }
            .show()
    }

    /**
     * @param unlocked kullanıcı not paylaştıysa feed, paylaşmadıysa kilit ekranı görünür.
     *
     * Görünürlük [RecyclerView] değil `swipeRefresh` üzerinden değiştiriliyor: kilit
     * ekranındayken yenilenecek bir liste olmadığı için aşağı çekme jesti de kapanmalı.
     */
    private fun showFeed(unlocked: Boolean) {
        binding.swipeRefresh.visibility = if (unlocked) View.VISIBLE else View.GONE
        binding.layoutLocked.visibility = if (unlocked) View.GONE else View.VISIBLE
    }
}
