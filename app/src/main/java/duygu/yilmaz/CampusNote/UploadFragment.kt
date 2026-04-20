package duygu.yilmaz.CampusNote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Base64

class UploadFragment : Fragment() {

    private lateinit var tilCourse: TextInputLayout
    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilDesc: TextInputLayout
    private lateinit var etCourse: TextInputEditText
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var spTag: Spinner
    private lateinit var btnSelectPdf: LinearLayout
    private lateinit var btnSelectImage: LinearLayout
    private lateinit var layoutFilePreview: LinearLayout
    private lateinit var ivFileIcon: ImageView
    private lateinit var tvFileName: TextView
    private lateinit var btnRemoveFile: ImageView
    private lateinit var btnUpload: MaterialButton
    private lateinit var progressBar: ProgressBar
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }


    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var selectedFileType: String = ""

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


    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                selectedFileName = getFileName(uri)
                selectedFileType = "pdf"
                showFilePreview()
            }
        }
    }


    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                selectedFileName = getFileName(uri)
                selectedFileType = "image"
                showFilePreview()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinner()
        setupClickListeners()
        animateViews(view)
    }

    private fun initViews(view: View) {
        tilCourse = view.findViewById(R.id.tilCourse)
        tilTitle = view.findViewById(R.id.tilTitle)
        tilDesc = view.findViewById(R.id.tilDesc)
        etCourse = view.findViewById(R.id.etCourse)
        etTitle = view.findViewById(R.id.etTitle)
        etDesc = view.findViewById(R.id.etDesc)
        spTag = view.findViewById(R.id.spTag)
        btnSelectPdf = view.findViewById(R.id.btnSelectPdf)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        layoutFilePreview = view.findViewById(R.id.layoutFilePreview)
        ivFileIcon = view.findViewById(R.id.ivFileIcon)
        tvFileName = view.findViewById(R.id.tvFileName)
        btnRemoveFile = view.findViewById(R.id.btnRemoveFile)
        btnUpload = view.findViewById(R.id.btnUpload)
        progressBar = view.findViewById(R.id.progressBar)
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

    private fun setupClickListeners() {
        btnSelectPdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pdfPickerLauncher.launch(intent)
        }

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            imagePickerLauncher.launch(intent)
        }

        btnRemoveFile.setOnClickListener {
            removeSelectedFile()
        }

        btnUpload.setOnClickListener {
            uploadNote()
        }
    }

    private fun animateViews(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvUploadTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvUploadSubtitle)
        val cardForm = view.findViewById<View>(R.id.cardUploadForm)

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

        cardForm.alpha = 0f
        cardForm.scaleX = 0.95f
        cardForm.scaleY = 0.95f
        cardForm.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(400)
            .setDuration(500)
            .start()

        btnUpload.alpha = 0f
        btnUpload.translationY = 20f
        btnUpload.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(600)
            .setDuration(400)
            .start()
    }

    private fun showFilePreview() {
        layoutFilePreview.visibility = View.VISIBLE
        tvFileName.text = selectedFileName

        if (selectedFileType == "pdf") {
            ivFileIcon.setImageResource(R.drawable.ic_pdf)
        } else {
            ivFileIcon.setImageResource(R.drawable.ic_image)
        }
    }

    private fun removeSelectedFile() {
        selectedFileUri = null
        selectedFileName = ""
        selectedFileType = ""
        layoutFilePreview.visibility = View.GONE
    }

    private fun getFileName(uri: Uri): String {
        var name = "dosya"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun uploadNote() {9
        val course = etCourse.text?.toString()?.trim().orEmpty()
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val desc = etDesc.text?.toString()?.trim().orEmpty()
        val tagIndex = spTag.selectedItemPosition

        if (!validateInput(course, title, desc)) return

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Giriş bulunamadı!", Toast.LENGTH_LONG).show()
            return
        }

        showLoading(true)

        var fileBase64 = ""
        if (selectedFileUri != null) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(selectedFileUri!!)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    fileBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Dosya okunamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val finalFileBase64 = fileBase64

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val department = snap.getString("department") ?: "Bilinmiyor"
                val email = user.email ?: snap.getString("email") ?: ""

                val noteMap = hashMapOf(
                    "course" to course,
                    "title" to title,
                    "description" to desc,
                    "tag" to if (tagIndex > 0) tags[tagIndex] else "",
                    "department" to department,
                    "uploaderUid" to user.uid,
                    "uploaderEmail" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "ratingSum" to 0L,
                    "ratingCount" to 0L,
                    "avgRating" to 0.0,
                    "fileName" to selectedFileName,
                    "fileType" to selectedFileType,
                    "fileData" to finalFileBase64
                )

                db.collection("notes")
                    .add(noteMap)
                    .addOnSuccessListener {
                        db.collection("users").document(user.uid)
                            .update("hasUploadedNote", true)

                        clearForm()
                        showLoading(false)
                        Toast.makeText(requireContext(), "✅ Not başarıyla paylaşıldı!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Kullanıcı bilgisi okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun validateInput(course: String, title: String, desc: String): Boolean {
        tilCourse.error = null
        tilTitle.error = null
        tilDesc.error = null

        return when {
            course.isEmpty() -> {
                tilCourse.error = "Ders adı boş olamaz"
                false
            }
            title.isEmpty() -> {
                tilTitle.error = "Not başlığı boş olamaz"
                false
            }
            desc.isEmpty() -> {
                tilDesc.error = "Açıklama boş olamaz"
                false
            }
            else -> true
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnUpload.isEnabled = !show

        if (show) {
            btnUpload.text = "Yükleniyor..."
            btnUpload.setTextColor(resources.getColor(R.color.white, null))
        } else {
            btnUpload.text = "Notu Paylaş"
            btnUpload.setTextColor(resources.getColor(R.color.white, null))
        }
    }

    private fun clearForm() {
        etCourse.text?.clear()
        etTitle.text?.clear()
        etDesc.text?.clear()
        spTag.setSelection(0)
        removeSelectedFile()
    }
}