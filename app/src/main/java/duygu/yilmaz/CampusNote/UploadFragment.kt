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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UploadFragment : Fragment() {

    private lateinit var etCourse: TextInputEditText
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var btnUpload: MaterialButton

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etCourse  = view.findViewById(R.id.etCourse)
        etTitle   = view.findViewById(R.id.etTitle)
        etDesc    = view.findViewById(R.id.etDesc)
        btnUpload = view.findViewById(R.id.btnUpload)

        btnUpload.setOnClickListener { uploadNote() }
    }

    private fun uploadNote() {
        val course = etCourse.text?.toString()?.trim().orEmpty()
        val title  = etTitle.text?.toString()?.trim().orEmpty()
        val desc   = etDesc.text?.toString()?.trim().orEmpty()

        if (course.isEmpty()) {
            Toast.makeText(requireContext(), "Ders adı boş olamaz!", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Not başlığı boş olamaz!", Toast.LENGTH_SHORT).show()
            return
        }
        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), "Açıklama boş olamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Giriş bulunamadı!", Toast.LENGTH_LONG).show()
            return
        }

        btnUpload.isEnabled = false
        btnUpload.text = "⏳ Yükleniyor..."

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val department = snap.getString("department") ?: "Bilinmiyor"
                val email      = user.email ?: snap.getString("email") ?: ""

                val noteMap = hashMapOf(
                    "course"        to course,
                    "title"         to title,
                    "description"   to desc,
                    "department"    to department,
                    "uploaderUid"   to user.uid,
                    "uploaderEmail" to email,
                    "createdAt"     to FieldValue.serverTimestamp(),

                    // ✅ tipleri netleştirelim
                    "ratingSum"     to 0L,
                    "ratingCount"   to 0L,
                    "avgRating"     to 0.0,

                    "imageUrls"     to emptyList<String>(),
                    "pdfUrl"        to ""
                )

                db.collection("notes")
                    .add(noteMap)
                    .addOnSuccessListener {
                        // ✅ FEED kilidini açan flag
                        db.collection("users").document(user.uid)
                            .update("hasUploadedNote", true)

                        etCourse.text?.clear()
                        etTitle.text?.clear()
                        etDesc.text?.clear()

                        Toast.makeText(requireContext(), "✅ Not başarıyla paylaşıldı!", Toast.LENGTH_SHORT).show()
                        btnUpload.isEnabled = true
                        btnUpload.text = "Notu Paylaş"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                        btnUpload.isEnabled = true
                        btnUpload.text = "Notu Paylaş"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Kullanıcı bilgisi okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
                btnUpload.isEnabled = true
                btnUpload.text = "Notu Paylaş"
            }
    }
}