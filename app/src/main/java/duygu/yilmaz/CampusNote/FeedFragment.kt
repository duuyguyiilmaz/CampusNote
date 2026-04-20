package duygu.yilmaz.CampusNote

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


class FeedFragment : Fragment() {

    private lateinit var rvFeed: RecyclerView
    private lateinit var layoutLocked: ScrollView
    private lateinit var btnUpload: MaterialButton
    private lateinit var adapter: PostAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var feedListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFeed       = view.findViewById(R.id.rvFeed)
        layoutLocked = view.findViewById(R.id.layoutLocked)
        btnUpload    = view.findViewById(R.id.btnUpload)

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
        startCloudFeed()
    }

    override fun onStop() {
        super.onStop()
        feedListener?.remove()
        feedListener = null
    }

    private fun startCloudFeed() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { userSnap ->
                val myDept = userSnap.getString("department")

                if (myDept.isNullOrEmpty()) {
                    Log.d("FeedFragment", "Department boş!")
                    adapter.refresh(emptyList())
                    showLocked(false)
                    return@addOnSuccessListener
                }

                val hasUploaded = userSnap.getBoolean("hasUploadedNote") ?: false
                showLocked(hasUploaded)

                if (!hasUploaded) {
                    adapter.refresh(emptyList())
                    return@addOnSuccessListener
                }

                feedListener?.remove()
                feedListener = db.collection("notes")
                    .whereEqualTo("department", myDept)
                    .addSnapshotListener { snap, e ->
                        if (e != null) {
                            Log.e("FeedFragment", "Firestore listen error: ${e.message}")
                            return@addSnapshotListener
                        }
                        if (snap == null) return@addSnapshotListener

                        val posts = snap.documents
                            .mapNotNull { d ->
                                val title = d.getString("title") ?: return@mapNotNull null
                                val desc = d.getString("description") ?: ""
                                val email = d.getString("uploaderEmail") ?: ""
                                val dept = d.getString("department") ?: ""
                                val uploaderUid = d.getString("uploaderUid") ?: ""
                                val time: Long = try {
                                    d.getTimestamp("createdAt")?.toDate()?.time
                                        ?: d.getLong("createdAt")
                                        ?: 0L
                                } catch (_: Exception) {
                                    d.getLong("createdAt") ?: 0L
                                }

                                val avgRating = d.getDouble("avgRating")
                                    ?: (d.getLong("avgRating")?.toDouble() ?: 0.0)

                                val ratingCount = d.getLong("ratingCount")
                                    ?: (d.getDouble("ratingCount")?.toLong() ?: 0L)
                                val ratingSum = d.getLong("ratingSum") ?: 0L
                                val course = d.getString("course") ?: ""
                                val tag = d.getString("tag") ?: ""
                                val fileName = d.getString("fileName") ?: ""
                                val fileType = d.getString("fileType") ?: ""
                                val fileData = d.getString("fileData") ?: ""
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
                                    ratingSum = ratingSum,
                                    course = course,
                                    tag = tag,
                                    fileName = fileName,
                                    fileType = fileType,
                                    fileData = fileData

                                )
                            }
                            .sortedByDescending { it.timeMills }

                        adapter.refresh(posts)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Kullanıcı bilgisi okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLocked(hasUploaded: Boolean) {
        rvFeed.visibility       = if (hasUploaded) View.VISIBLE else View.GONE
        layoutLocked.visibility = if (hasUploaded) View.GONE else View.VISIBLE
    }
}