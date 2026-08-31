package duygu.yilmaz.campusnote.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.databinding.FragmentProfileBinding
import duygu.yilmaz.campusnote.ui.auth.LoginActivity
import duygu.yilmaz.campusnote.ui.common.PostAdapter
import duygu.yilmaz.campusnote.ui.editnote.EditNoteFragment

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private lateinit var myNotesAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupLogout()
        viewModel.uiState.observe(viewLifecycleOwner, ::renderUiState)
        viewModel.actionState.observe(viewLifecycleOwner, ::renderActionState)
        animateViews()
    }

    override fun onDestroyView() {
        binding.rvMyNotes.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupAdapter() {
        myNotesAdapter = PostAdapter(
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

        binding.rvMyNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyNotes.adapter = myNotesAdapter
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            viewModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun animateViews() {
        binding.tvProfileTitle.alpha = 0f
        binding.tvProfileTitle.translationY = -20f
        binding.tvProfileTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        binding.cardAvatar.alpha = 0f
        binding.cardAvatar.scaleX = 0.8f
        binding.cardAvatar.scaleY = 0.8f
        binding.cardAvatar.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(200)
            .setDuration(500)
            .start()

        binding.layoutUserInfo.alpha = 0f
        binding.layoutUserInfo.translationY = 20f
        binding.layoutUserInfo.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .start()

        binding.cardPoints.alpha = 0f
        binding.cardPoints.translationX = -50f
        binding.cardPoints.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(600)
            .setDuration(400)
            .start()

        binding.cardDiscounts.alpha = 0f
        binding.cardDiscounts.translationX = 50f
        binding.cardDiscounts.animate()
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
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
            }

            is ProfileUiState.Content -> {
                binding.tvEmail.text = state.email
                binding.tvDepartment.text = state.department
                binding.tvAvatar.text = state.email.firstOrNull()?.uppercaseChar()?.toString()
                    ?: getString(R.string.avatar_placeholder)

                myNotesAdapter.refresh(state.posts)
                binding.tvNoteCount.text = getString(R.string.note_count, state.posts.size)
                updatePointsUI(state.totalPoints)

                val isEmpty = state.posts.isEmpty()
                binding.layoutEmptyNotes.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvMyNotes.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }

            // Her iki hata da yeniden denenebilir: ikisi de startProfile()'ın adımları.
            is ProfileUiState.Error -> {
                val reason = state.exception.message.orEmpty()
                showRetryableError(
                    when (state.stage) {
                        ProfileFailureStage.USER_PROFILE ->
                            getString(R.string.error_user_profile_read, reason)

                        ProfileFailureStage.NOTES ->
                            getString(R.string.error_my_notes_read, reason)
                    }
                )
            }
        }
    }

    /**
     * Hata mesajını yeniden deneme eylemiyle birlikte gösterir.
     *
     * Eskiden Toast'tı: kaybolduktan sonra profil yarı dolu kalıyordu — bölüm, puan
     * ve not listesi hiç yazılmamış oluyordu ama bunun bir hatadan mı yoksa henüz
     * not paylaşılmamasından mı kaynaklandığını gösteren hiçbir şey yoktu. Feed'in
     * aksine burada aşağı çekme jesti yok, yani bu snackbar tek kurtarma yolu;
     * [Snackbar.LENGTH_INDEFINITE] kaçırılmasını engelliyor.
     *
     * `bottomNav`'a bağlanmasının sebebi de bu: fragment'ın kökü sekme çubuğunun
     * arkasına kadar uzanıyor, dolayısıyla bağlanmayan bir snackbar çubuğun üstüne
     * çizilir ve kapatılana kadar sekme değiştirmeyi engellerdi.
     */
    private fun showRetryableError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAnchorView(R.id.bottomNav)
            .setAction(R.string.action_retry) { viewModel.startProfile() }
            .show()
    }

    private fun renderActionState(state: ProfileActionState) {
        when (state) {
            ProfileActionState.Idle,
            ProfileActionState.DeletingNote -> Unit

            ProfileActionState.NoteDeleted -> {
                Toast.makeText(requireContext(), R.string.note_deleted, Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }

            is ProfileActionState.DeleteError -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_note_delete, state.exception.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetActionState()
            }
        }
    }

    private fun updatePointsUI(points: Long) {
        binding.tvPoints.text = points.toString()
        binding.progressPoints.progress = points.coerceIn(0L, DISCOUNT_THRESHOLD).toInt()

        val unlocked = points >= DISCOUNT_THRESHOLD

        binding.tvPointsRemaining.text = if (unlocked) {
            getString(R.string.points_unlocked)
        } else {
            getString(R.string.points_remaining, DISCOUNT_THRESHOLD - points)
        }

        binding.layoutDiscountsLocked.visibility = if (unlocked) View.GONE else View.VISIBLE
        binding.layoutDiscountsUnlocked.visibility = if (unlocked) View.VISIBLE else View.GONE
    }

    private companion object {
        /** İndirimlerin açıldığı puan eşiği; ilerleme çubuğunun üst sınırı da budur. */
        const val DISCOUNT_THRESHOLD = 100L
    }
}
