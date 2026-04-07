package duygu.yilmaz.CampusNote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LeaderboardFragment : Fragment() {

    private lateinit var rvLeaderboard: RecyclerView
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
        adapter = LeaderboardAdapter(mutableListOf())
        rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        rvLeaderboard.adapter = adapter
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
                    val dept  = d.getString("department") ?: ""
                    val avg   = d.getDouble("avgRating") ?: 0.0
                    val count = d.getLong("ratingCount") ?: 0L
                    // En az 1 oy almış notları göster (isteğe bağlı)
                    LeaderboardEntry(
                        docId       = d.id,
                        title       = title,
                        uploaderEmail = email,
                        department  = dept,
                        avgRating   = avg,
                        ratingCount = count
                    )
                }
                    // avgRating'e göre büyükten küçüğe, eşitse ratingCount'a göre sırala
                    .sortedWith(compareByDescending<LeaderboardEntry> { it.avgRating }
                        .thenByDescending { it.ratingCount })

                adapter.refresh(entries)
            }
    }
}