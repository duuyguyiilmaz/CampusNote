package duygu.yilmaz.CampusNote

import android.content.Intent
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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tvEmail: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var tvNoteCount: TextView
    private lateinit var rvMyNotes: RecyclerView
    private lateinit var btnLogout: MaterialButton
    private lateinit var layoutEmptyNotes: LinearLayout

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

        tvEmail          = view.findViewById(R.id.tvEmail)
        tvDepartment     = view.findViewById(R.id.tvDepartment)
        tvAvatar         = view.findViewById(R.id.tvAvatar)
        tvNoteCount      = view.findViewById(R.id.tvNoteCount)
        rvMyNotes        = view.findViewById(R.id.rvMyNotes)
        btnLogout        = view.findViewById(R.id.btnLogout)
        layoutEmptyNotes = view.findViewById(R.id.layoutEmptyNotes)

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
                        Toast.makeText(requireContext(), "Not silindi! 🗑️", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Silinemedi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        )

        rvMyNotes.layoutManager = LinearLayoutManager(requireContext())
        rvMyNotes.adapter = myNotesAdapter

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
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

        // Email & Avatar
        val emailText = user.email ?: "—"
        tvEmail.text = emailText
        tvAvatar.text = emailText.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        // Department
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                tvDepartment.text = snap.getString("department") ?: "—"
            }
            .addOnFailureListener {
                tvDepartment.text = "—"
            }

        // My notes
        myNotesListener?.remove()
        myNotesListener = db.collection("notes")
            .whereEqualTo("uploaderUid", user.uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Notlarım okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val posts = snap.documents.mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    val desc  = d.getString("description") ?: ""
                    val email = d.getString("uploaderEmail") ?: (user.email ?: "")
                    val dept  = d.getString("department") ?: ""

                    val time: Long = try {
                        d.getTimestamp("createdAt")?.toDate()?.time
                            ?: d.getLong("createdAt")
                            ?: 0L
                    } catch (_: Exception) {
                        d.getLong("createdAt") ?: 0L
                    }

                    Post(
                        id          = d.id,
                        title       = title,
                        desc        = desc,
                        authorEmail = email,
                        department  = dept,
                        timeMills   = time
                    )
                }.sortedByDescending { it.timeMills }

                myNotesAdapter.refresh(posts)

                tvNoteCount.text = "${posts.size} not"

                val empty = posts.isEmpty()
                layoutEmptyNotes.visibility = if (empty) View.VISIBLE else View.GONE
                rvMyNotes.visibility = if (empty) View.GONE else View.VISIBLE
            }
    }
}