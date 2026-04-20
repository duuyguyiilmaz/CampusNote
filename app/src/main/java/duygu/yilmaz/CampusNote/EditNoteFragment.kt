package duygu.yilmaz.CampusNote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditNoteFragment : Fragment() {

    companion object {
        private const val ARG_DOC_ID = "docId"

        fun newInstance(docId: String): EditNoteFragment {
            return EditNoteFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOC_ID, docId)
                }
            }
        }

        fun newInstance(docId: String, title: String, desc: String): EditNoteFragment {
            return newInstance(docId)
        }
    }

    private lateinit var tilCourse: TextInputLayout
    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilDesc: TextInputLayout
    private lateinit var etCourse: TextInputEditText
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var spTag: Spinner
    private lateinit var layoutCurrentFile: LinearLayout
    private lateinit var ivCurrentFileIcon: ImageView
    private lateinit var tvCurrentFileName: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private val db   by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val tags = listOf(
        "Etiket seçiniz",
        "Ders Notu",
        "Vize",
        "Final",
        "Özet",
        "Slayt",
        "Ödev",
        "Diğer"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_edit_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinner()
        loadNoteData()

        btnSave.setOnClickListener { saveChanges() }
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun initViews(view: View) {
        tilCourse = view.findViewById(R.id.tilEditCourse)
        tilTitle = view.findViewById(R.id.tilEditTitle)
        tilDesc = view.findViewById(R.id.tilEditDesc)
        etCourse = view.findViewById(R.id.etEditCourse)
        etTitle = view.findViewById(R.id.etEditTitle)
        etDesc = view.findViewById(R.id.etEditDesc)
        spTag = view.findViewById(R.id.spEditTag)
        layoutCurrentFile = view.findViewById(R.id.layoutCurrentFile)
        ivCurrentFileIcon = view.findViewById(R.id.ivCurrentFileIcon)
        tvCurrentFileName = view.findViewById(R.id.tvCurrentFileName)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tags
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTag.adapter = adapter
    }

    private fun loadNoteData() {
        val docId = arguments?.getString(ARG_DOC_ID) ?: return

        db.collection("notes").document(docId)
            .get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener

                etCourse.setText(snap.getString("course") ?: "")
                etTitle.setText(snap.getString("title") ?: "")
                etDesc.setText(snap.getString("description") ?: "")

                val tag = snap.getString("tag") ?: ""
                val tagIndex = tags.indexOf(tag)
                if (tagIndex > 0) {
                    spTag.setSelection(tagIndex)
                }

                val fileName = snap.getString("fileName") ?: ""
                val fileType = snap.getString("fileType") ?: ""
                if (fileName.isNotEmpty()) {
                    layoutCurrentFile.visibility = View.VISIBLE
                    tvCurrentFileName.text = fileName
                    if (fileType == "pdf") {
                        ivCurrentFileIcon.setImageResource(R.drawable.ic_pdf)
                    } else {
                        ivCurrentFileIcon.setImageResource(R.drawable.ic_image)
                    }
                } else {
                    layoutCurrentFile.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Not yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveChanges() {
        val docId = arguments?.getString(ARG_DOC_ID) ?: return
        val newCourse = etCourse.text.toString().trim()
        val newTitle = etTitle.text.toString().trim()
        val newDesc = etDesc.text.toString().trim()
        val tagIndex = spTag.selectedItemPosition

        // Validation
        tilCourse.error = null
        tilTitle.error = null
        tilDesc.error = null

        if (newCourse.isEmpty()) {
            tilCourse.error = "Ders adı boş olamaz"
            return
        }
        if (newTitle.isEmpty()) {
            tilTitle.error = "Başlık boş olamaz"
            return
        }
        if (newDesc.isEmpty()) {
            tilDesc.error = "Açıklama boş olamaz"
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

                val updates = mapOf(
                    "course"      to newCourse,
                    "title"       to newTitle,
                    "description" to newDesc,
                    "tag"         to if (tagIndex > 0) tags[tagIndex] else "",
                    "updatedAt"   to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                db.collection("notes").document(docId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Not güncellendi!", Toast.LENGTH_SHORT).show()
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