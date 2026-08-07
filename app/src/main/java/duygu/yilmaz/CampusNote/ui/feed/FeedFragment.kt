package duygu.yilmaz.CampusNote.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.ui.common.PostAdapter
import duygu.yilmaz.CampusNote.ui.notedetail.NoteDetailFragment

class FeedFragment : Fragment() {

    private lateinit var rvFeed: RecyclerView
    private lateinit var layoutLocked: ScrollView
    private lateinit var btnUpload: MaterialButton
    private lateinit var adapter: PostAdapter

    private val viewModel: FeedViewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFeed = view.findViewById(R.id.rvFeed)
        layoutLocked = view.findViewById(R.id.layoutLocked)
        btnUpload = view.findViewById(R.id.btnUpload)

        adapter = PostAdapter(
            mutableListOf(),
            onItemClick = { post ->
                val fragment = NoteDetailFragment.newInstance(post)

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        rvFeed.layoutManager = LinearLayoutManager(requireContext())
        rvFeed.adapter = adapter

        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)

        btnUpload.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
                ?.selectedItemId = R.id.nav_upload
        }
        animateViews(view)
    }

    private fun animateViews(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvFeedTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvFeedSubtitle)

        tvTitle.alpha = 0f
        tvTitle.translationY = -20f
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        tvSubtitle.alpha = 0f
        tvSubtitle.translationY = -15f
        tvSubtitle.animate()
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
        when (state) {
            FeedUiState.Idle,
            FeedUiState.Loading -> Unit

            FeedUiState.Locked -> {
                adapter.refresh(emptyList())
                showLocked(false)
            }

            FeedUiState.MissingSession -> {
                Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
            }

            is FeedUiState.Content -> {
                adapter.refresh(state.posts)
                showLocked(true)
            }

            is FeedUiState.Error -> when (state.stage) {
                FeedFailureStage.USER_PROFILE -> {
                    Toast.makeText(
                        requireContext(),
                        "Kullanıcı bilgisi okunamadı: ${state.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                FeedFailureStage.NOTES -> {
                    Log.e("FeedFragment", "Firestore listen error: ${state.exception.message}")
                }
            }
        }
    }

    private fun showLocked(hasUploaded: Boolean) {
        rvFeed.visibility = if (hasUploaded) View.VISIBLE else View.GONE
        layoutLocked.visibility = if (hasUploaded) View.GONE else View.VISIBLE
    }
}
