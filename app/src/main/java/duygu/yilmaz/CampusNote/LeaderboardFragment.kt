package duygu.yilmaz.CampusNote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LeaderboardFragment : Fragment() {

    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: LeaderboardAdapter

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvLeaderboard = view.findViewById(R.id.rvLeaderboard)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        adapter = LeaderboardAdapter(mutableListOf())
        rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        rvLeaderboard.adapter = adapter

        animateViews(view)
    }

    override fun onStart() {
        super.onStart()
        startListening()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun animateViews(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvLeaderboardTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvLeaderboardSubtitle)
        val layoutBadges = view.findViewById<LinearLayout>(R.id.layoutBadges)

        // Başlık animasyonu
        tvTitle.alpha = 0f
        tvTitle.translationY = -20f
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        // Alt başlık
        tvSubtitle.alpha = 0f
        tvSubtitle.translationY = -15f
        tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .start()

        // Badge'ler
        layoutBadges.alpha = 0f
        layoutBadges.translationY = -10f
        layoutBadges.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .start()

        // RecyclerView
        rvLeaderboard.alpha = 0f
        rvLeaderboard.animate()
            .alpha(1f)
            .setStartDelay(600)
            .setDuration(400)
            .start()
    }

    private fun startListening() {
        listener?.remove()
        listener = db.collection("notes")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val entries = snap.documents.mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    val email = d.getString("uploaderEmail") ?: ""
                    val dept = d.getString("department") ?: ""
                    val avg = d.getDouble("avgRating") ?: 0.0
                    val count = d.getLong("ratingCount") ?: 0L

                    LeaderboardEntry(
                        docId = d.id,
                        title = title,
                        uploaderEmail = email,
                        department = dept,
                        avgRating = avg,
                        ratingCount = count
                    )
                }
                    .sortedWith(
                        compareByDescending<LeaderboardEntry> { it.avgRating }
                            .thenByDescending { it.ratingCount }
                    )

                // Empty state kontrolü
                if (entries.isEmpty()) {
                    rvLeaderboard.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    rvLeaderboard.visibility = View.VISIBLE
                    layoutEmpty.visibility = View.GONE
                    adapter.refresh(entries)
                }
            }
    }
}