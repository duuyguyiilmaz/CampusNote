package duygu.yilmaz.CampusNote.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.ui.auth.LoginActivity
import duygu.yilmaz.CampusNote.ui.common.PostAdapter
import duygu.yilmaz.CampusNote.ui.editnote.EditNoteFragment
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    // Views
    private lateinit var tvProfileTitle: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var tvNoteCount: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvPointsRemaining: TextView
    private lateinit var progressPoints: ProgressBar
    private lateinit var layoutDiscountsLocked: LinearLayout
    private lateinit var layoutDiscountsUnlocked: LinearLayout
    private lateinit var rvMyNotes: RecyclerView
    private lateinit var btnLogout: MaterialButton
    private lateinit var layoutEmptyNotes: LinearLayout
    private lateinit var cardAvatar: CardView
    private lateinit var layoutUserInfo: LinearLayout
    private lateinit var cardPoints: LinearLayout
    private lateinit var cardDiscounts: LinearLayout

    private lateinit var myNotesAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupAdapter()
        setupLogout()
        viewModel.uiState.observe(viewLifecycleOwner, ::renderUiState)
        viewModel.actionState.observe(viewLifecycleOwner, ::renderActionState)
        animateViews(view)
    }

    private fun initViews(view: View) {
        tvProfileTitle = view.findViewById(R.id.tvProfileTitle)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvDepartment = view.findViewById(R.id.tvDepartment)
        tvAvatar = view.findViewById(R.id.tvAvatar)
        tvNoteCount = view.findViewById(R.id.tvNoteCount)
        tvPoints = view.findViewById(R.id.tvPoints)
        tvPointsRemaining = view.findViewById(R.id.tvPointsRemaining)
        progressPoints = view.findViewById(R.id.progressPoints)
        layoutDiscountsLocked = view.findViewById(R.id.layoutDiscountsLocked)
        layoutDiscountsUnlocked = view.findViewById(R.id.layoutDiscountsUnlocked)
        rvMyNotes = view.findViewById(R.id.rvMyNotes)
        btnLogout = view.findViewById(R.id.btnLogout)
        layoutEmptyNotes = view.findViewById(R.id.layoutEmptyNotes)
        cardAvatar = view.findViewById(R.id.cardAvatar)
        layoutUserInfo = view.findViewById(R.id.layoutUserInfo)
        cardPoints = view.findViewById(R.id.cardPoints)
        cardDiscounts = view.findViewById(R.id.cardDiscounts)
    }

    private fun setupAdapter() {
        myNotesAdapter = PostAdapter(
            mutableListOf(),
            onEditClick = { post ->
                val editFrag = EditNoteFragment.newInstance(post.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, editFrag)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { post ->
                viewModel.deleteNote(post.id)
            }
        )

        rvMyNotes.layoutManager = LinearLayoutManager(requireContext())
        rvMyNotes.adapter = myNotesAdapter
    }

    private fun setupLogout() {
        btnLogout.setOnClickListener {
            viewModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun animateViews(view: View) {
        tvProfileTitle.alpha = 0f
        tvProfileTitle.translationY = -20f
        tvProfileTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        cardAvatar.alpha = 0f
        cardAvatar.scaleX = 0.8f
        cardAvatar.scaleY = 0.8f
        cardAvatar.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(200)
            .setDuration(500)
            .start()

        layoutUserInfo.alpha = 0f
        layoutUserInfo.translationY = 20f
        layoutUserInfo.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .start()

        cardPoints.alpha = 0f
        cardPoints.translationX = -50f
        cardPoints.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(600)
            .setDuration(400)
            .start()

        cardDiscounts.alpha = 0f
        cardDiscounts.translationX = 50f
        cardDiscounts.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(800)
            .setDuration(400)
            .start()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startProfile()
    }

    override fun onStop() {
        viewModel.stopProfile()
        super.onStop()
    }

    private fun renderUiState(state: ProfileUiState) {
        when (state) {
            ProfileUiState.Idle,
            ProfileUiState.Loading -> Unit

            ProfileUiState.MissingSession -> {
                Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
            }

            is ProfileUiState.Content -> {
                tvEmail.text = state.email
                tvDepartment.text = state.department
                tvAvatar.text = state.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

                myNotesAdapter.refresh(state.posts)
                tvNoteCount.text = "${state.posts.size} not"
                updatePointsUI(state.totalPoints)

                val isEmpty = state.posts.isEmpty()
                layoutEmptyNotes.visibility = if (isEmpty) View.VISIBLE else View.GONE
                rvMyNotes.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }

            is ProfileUiState.Error -> {
                val message = when (state.stage) {
                    ProfileFailureStage.USER_PROFILE -> {
                        "Kullanıcı bilgisi okunamadı: ${state.exception.message}"
                    }

                    ProfileFailureStage.NOTES -> {
                        "Notlarım okunamadı: ${state.exception.message}"
                    }
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderActionState(state: ProfileActionState) {
        when (state) {
            ProfileActionState.Idle,
            ProfileActionState.DeletingNote -> Unit

            ProfileActionState.NoteDeleted -> {
                Toast.makeText(requireContext(), "Not silindi!", Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }

            is ProfileActionState.DeleteError -> {
                Toast.makeText(
                    requireContext(),
                    "Silinemedi: ${state.exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetActionState()
            }
        }
    }

    private fun updatePointsUI(points: Long) {
        tvPoints.text = points.toString()
        progressPoints.progress = points.coerceIn(0L, 100L).toInt()

        if (points >= 100L) {
            tvPointsRemaining.text = "Tebrikler! İndirimler açıldı!"
            layoutDiscountsLocked.visibility = View.GONE
            layoutDiscountsUnlocked.visibility = View.VISIBLE
        } else {
            val remaining = 100L - points
            tvPointsRemaining.text = "$remaining puan daha kazan, indirimleri aç!"
            layoutDiscountsLocked.visibility = View.VISIBLE
            layoutDiscountsUnlocked.visibility = View.GONE
        }
    }
}
