package duygu.yilmaz.CampusNote
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditNoteFragment : Fragment() {

    companion object {
        private const val ARG_DOC_ID = "docId"
        private const val ARG_TITLE  = "title"
        private const val ARG_DESC   = "desc"

        fun newInstance(docId: String, title: String, desc: String): EditNoteFragment {
            return EditNoteFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOC_ID, docId)
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, desc)
                }
            }
        }
    }

    private lateinit var etTitle  : TextInputEditText
    private lateinit var etDesc   : TextInputEditText
    private lateinit var btnSave  : MaterialButton
    private lateinit var btnCancel: MaterialButton

    private val db   by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_edit_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ ARTIK BUNA GEREK YOK - SİL
        // activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.visibility = View.GONE

        etTitle   = view.findViewById(R.id.etEditTitle)
        etDesc    = view.findViewById(R.id.etEditDesc)
        btnSave   = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        etTitle.setText(arguments?.getString(ARG_TITLE) ?: "")
        etDesc.setText(arguments?.getString(ARG_DESC)   ?: "")

        btnSave.setOnClickListener { saveChanges() }
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveChanges() {
        val docId = arguments?.getString(ARG_DOC_ID) ?: return
        val newTitle = etTitle.text.toString().trim()
        val newDesc  = etDesc.text.toString().trim()

        if (newTitle.isEmpty() || newDesc.isEmpty()) {
            Toast.makeText(requireContext(), "Başlık ve açıklama boş olamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        btnSave.isEnabled = false

        db.collection("notes").document(docId)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.getString("uploaderUid") != uid) {
                    Toast.makeText(requireContext(), "Bu notu düzenleme yetkin yok!", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    return@addOnSuccessListener
                }

                db.collection("notes").document(docId)
                    .update(
                        mapOf(
                            "title"       to newTitle,
                            "description" to newDesc,
                            "updatedAt"   to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Not güncellendi ✅", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                        btnSave.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Doküman okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
            }
    }
}