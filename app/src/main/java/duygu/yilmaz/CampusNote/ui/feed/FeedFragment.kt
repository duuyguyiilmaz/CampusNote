package duygu.yilmaz.CampusNote.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.databinding.FragmentFeedBinding
import duygu.yilmaz.CampusNote.ui.common.PostAdapter
import duygu.yilmaz.CampusNote.ui.main.MainActivity
import duygu.yilmaz.CampusNote.ui.notedetail.NoteDetailFragment

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter

    private val viewModel: FeedViewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
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
            mutableListOf(),
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
        when (state) {
            FeedUiState.Idle,
            FeedUiState.Loading -> Unit

            FeedUiState.Locked -> {
                adapter.refresh(emptyList())
                showLocked(false)
            }

            FeedUiState.MissingSession -> {
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
            }

            is FeedUiState.Content -> {
                adapter.refresh(state.posts)
                showLocked(true)
            }

            is FeedUiState.Error -> when (state.stage) {
                FeedFailureStage.USER_PROFILE -> {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_user_profile_read,
                            state.exception.message.orEmpty()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }

                FeedFailureStage.NOTES -> {
                    Log.e(TAG, "Firestore listen error: ${state.exception.message}")
                }
            }
        }
    }

    private fun showLocked(hasUploaded: Boolean) {
        binding.rvFeed.visibility = if (hasUploaded) View.VISIBLE else View.GONE
        binding.layoutLocked.visibility = if (hasUploaded) View.GONE else View.VISIBLE
    }

    private companion object {
        const val TAG = "FeedFragment"
    }
}
