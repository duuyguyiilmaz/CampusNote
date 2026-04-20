package duygu.yilmaz.CampusNote

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

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
    private var myNotesListener: ListenerRegistration? = null

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
                val editFrag = EditNoteFragment.newInstance(post.id, post.title, post.desc)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, editFrag)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { post ->
                db.collection("notes").document(post.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Not silindi!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Silinemedi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        )

        rvMyNotes.layoutManager = LinearLayoutManager(requireContext())
        rvMyNotes.adapter = myNotesAdapter
    }

    private fun setupLogout() {
        btnLogout.setOnClickListener {
            auth.signOut()
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
        myNotesListener?.remove()
        myNotesListener = null

        loadProfileAndMyNotes()
    }

    override fun onStop() {
        super.onStop()
        myNotesListener?.remove()
        myNotesListener = null

    }

    private fun loadProfileAndMyNotes() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        val emailText = user.email ?: "—"
        tvEmail.text = emailText
        tvAvatar.text = emailText.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                if (isAdded) {
                    tvDepartment.text = snap.getString("department") ?: "—"
                }
            }

        myNotesListener?.remove()
        myNotesListener = db.collection("notes")
            .whereEqualTo("uploaderUid", user.uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Notlarım okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snap == null || !isAdded) return@addSnapshotListener

                var totalPoints = 0L

                val posts = snap.documents.mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    val desc = d.getString("description") ?: ""
                    val email = d.getString("uploaderEmail") ?: (user.email ?: "")
                    val dept = d.getString("department") ?: ""
                    val uploaderUid = d.getString("uploaderUid") ?: ""

                    val time: Long = try {
                        d.getTimestamp("createdAt")?.toDate()?.time
                            ?: d.getLong("createdAt")
                            ?: 0L
                    } catch (_: Exception) {
                        d.getLong("createdAt") ?: 0L
                    }

                    val avgRating = d.getDouble("avgRating") ?: 0.0
                    val ratingCount = d.getLong("ratingCount") ?: 0L
                    val ratingSum = d.getLong("ratingSum") ?: 0L

                    totalPoints += ratingSum

                    Post(
                        id = d.id,
                        title = title,
                        desc = desc,
                        authorEmail = email,
                        department = dept,
                        timeMills = time,
                        uploaderUid = uploaderUid,
                        avgRating = avgRating,
                        ratingCount = ratingCount,
                        ratingSum = ratingSum
                    )
                }.sortedByDescending { it.timeMills }

                myNotesAdapter.refresh(posts)
                tvNoteCount.text = "${posts.size} not"

                updatePointsUI(totalPoints.toInt())

                val empty = posts.isEmpty()
                layoutEmptyNotes.visibility = if (empty) View.VISIBLE else View.GONE
                rvMyNotes.visibility = if (empty) View.GONE else View.VISIBLE
            }
    }

    private fun updatePointsUI(points: Int) {
        tvPoints.text = points.toString()
        progressPoints.progress = points.coerceAtMost(100)

        if (points >= 100) {
            tvPointsRemaining.text = "Tebrikler! İndirimler açıldı!"
            layoutDiscountsLocked.visibility = View.GONE
            layoutDiscountsUnlocked.visibility = View.VISIBLE
        } else {
            val remaining = 100 - points
            tvPointsRemaining.text = "$remaining puan daha kazan, indirimleri aç!"
            layoutDiscountsLocked.visibility = View.VISIBLE
            layoutDiscountsUnlocked.visibility = View.GONE
        }
    }
}