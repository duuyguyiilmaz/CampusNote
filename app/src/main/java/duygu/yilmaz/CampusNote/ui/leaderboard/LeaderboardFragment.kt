package duygu.yilmaz.CampusNote.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LeaderboardAdapter

    private val viewModel: LeaderboardViewModel by lazy {
        ViewModelProvider(this)[LeaderboardViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LeaderboardAdapter(mutableListOf())
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter

        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)

        animateViews()
    }

    override fun onDestroyView() {
        binding.rvLeaderboard.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startLeaderboard()
    }

    override fun onStop() {
        viewModel.stopLeaderboard()
        super.onStop()
    }

    private fun animateViews() {
        binding.tvLeaderboardTitle.alpha = 0f
        binding.tvLeaderboardTitle.translationY = -20f
        binding.tvLeaderboardTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        binding.tvLeaderboardSubtitle.alpha = 0f
        binding.tvLeaderboardSubtitle.translationY = -15f
        binding.tvLeaderboardSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .start()

        binding.layoutBadges.alpha = 0f
        binding.layoutBadges.translationY = -10f
        binding.layoutBadges.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .start()

        binding.rvLeaderboard.alpha = 0f
        binding.rvLeaderboard.animate()
            .alpha(1f)
            .setStartDelay(600)
            .setDuration(400)
            .start()
    }

    private fun renderState(state: LeaderboardUiState) {
        when (state) {
            LeaderboardUiState.Idle -> Unit

            LeaderboardUiState.Empty -> {
                adapter.refresh(emptyList())
                binding.rvLeaderboard.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            }

            is LeaderboardUiState.Content -> {
                adapter.refresh(state.entries)
                binding.rvLeaderboard.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            }

            is LeaderboardUiState.Error -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_generic, state.exception.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
