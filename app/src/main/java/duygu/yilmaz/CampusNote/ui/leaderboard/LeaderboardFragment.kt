package duygu.yilmaz.CampusNote.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import duygu.yilmaz.CampusNote.R

class LeaderboardFragment : Fragment() {

    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: LeaderboardAdapter

    private val viewModel: LeaderboardViewModel by lazy {
        ViewModelProvider(this)[LeaderboardViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvLeaderboard = view.findViewById(R.id.rvLeaderboard)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        adapter = LeaderboardAdapter(mutableListOf())
        rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        rvLeaderboard.adapter = adapter

        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)

        animateViews(view)
    }

    override fun onStart() {
        super.onStart()
        viewModel.startLeaderboard()
    }

    override fun onStop() {
        viewModel.stopLeaderboard()
        super.onStop()
    }

    private fun animateViews(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvLeaderboardTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvLeaderboardSubtitle)
        val layoutBadges = view.findViewById<LinearLayout>(R.id.layoutBadges)

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

        layoutBadges.alpha = 0f
        layoutBadges.translationY = -10f
        layoutBadges.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .start()

        rvLeaderboard.alpha = 0f
        rvLeaderboard.animate()
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
                rvLeaderboard.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            }

            is LeaderboardUiState.Content -> {
                adapter.refresh(state.entries)
                rvLeaderboard.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
            }

            is LeaderboardUiState.Error -> {
                Toast.makeText(
                    requireContext(),
                    "Hata: ${state.exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
