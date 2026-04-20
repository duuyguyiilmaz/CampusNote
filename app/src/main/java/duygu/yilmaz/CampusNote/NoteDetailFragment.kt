package duygu.yilmaz.CampusNote

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class NoteDetailFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var tvNoteTitle: TextView
    private lateinit var tvTag: TextView
    private lateinit var tvCourse: TextView
    private lateinit var tvUploaderEmail: TextView
    private lateinit var tvDescription: TextView
    private lateinit var layoutPdfCard: LinearLayout
    private lateinit var tvPdfFileName: TextView
    private lateinit var btnOpenPdf: MaterialButton
    private lateinit var layoutImageCard: LinearLayout
    private lateinit var ivNoteImage: ImageView
    private lateinit var tvAvgRating: TextView
    private lateinit var tvRatingCount: TextView
    private lateinit var btnRate: MaterialButton


    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var postId: String = ""
    private var postTitle: String = ""
    private var postDesc: String = ""
    private var postCourse: String = ""
    private var postTag: String = ""
    private var postUploaderEmail: String = ""
    private var postUploaderUid: String = ""
    private var postAvgRating: Double = 0.0
    private var postRatingCount: Long = 0L
    private var postRatingSum: Long = 0L
    private var postFileName: String = ""
    private var postFileType: String = ""
    private var postFileData: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_note_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        readArguments()

        initViews(view)

        fillViews()

        setupFileSection()

        setupClickListeners()
    }

    private fun readArguments() {
        arguments?.let {
            postId = it.getString("id", "")
            postTitle = it.getString("title", "")
            postDesc = it.getString("desc", "")
            postCourse = it.getString("course", "")
            postTag = it.getString("tag", "")
            postUploaderEmail = it.getString("uploaderEmail", "")
            postUploaderUid = it.getString("uploaderUid", "")
            postAvgRating = it.getDouble("avgRating", 0.0)
            postRatingCount = it.getLong("ratingCount", 0L)
            postRatingSum = it.getLong("ratingSum", 0L)
            postFileName = it.getString("fileName", "")
            postFileType = it.getString("fileType", "")
            postFileData = it.getString("fileData", "")
        }
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvNoteTitle = view.findViewById(R.id.tvNoteTitle)
        tvTag = view.findViewById(R.id.tvTag)
        tvCourse = view.findViewById(R.id.tvCourse)
        tvUploaderEmail = view.findViewById(R.id.tvUploaderEmail)
        tvDescription = view.findViewById(R.id.tvDescription)
        layoutPdfCard = view.findViewById(R.id.layoutPdfCard)
        tvPdfFileName = view.findViewById(R.id.tvPdfFileName)
        btnOpenPdf = view.findViewById(R.id.btnOpenPdf)
        layoutImageCard = view.findViewById(R.id.layoutImageCard)
        ivNoteImage = view.findViewById(R.id.ivNoteImage)
        tvAvgRating = view.findViewById(R.id.tvAvgRating)
        tvRatingCount = view.findViewById(R.id.tvRatingCount)
        btnRate = view.findViewById(R.id.btnRate)
    }

    private fun fillViews() {
        tvNoteTitle.text = postTitle
        tvCourse.text = postCourse
        tvUploaderEmail.text = postUploaderEmail
        tvDescription.text = postDesc

        if (postTag.isEmpty()) {
            tvTag.visibility = View.GONE
        } else {
            tvTag.text = postTag.uppercase()
        }

        tvAvgRating.text = String.format("%.1f", postAvgRating)
        tvRatingCount.text = "($postRatingCount oy)"
    }

    private fun setupFileSection() {
        when (postFileType) {
            "pdf" -> {
                layoutPdfCard.visibility = View.VISIBLE
                layoutImageCard.visibility = View.GONE
                tvPdfFileName.text = postFileName
            }
            "image" -> {
                layoutPdfCard.visibility = View.GONE
                layoutImageCard.visibility = View.VISIBLE
                showImageFromBase64()
            }
            else -> {
                layoutPdfCard.visibility = View.GONE
                layoutImageCard.visibility = View.GONE
            }
        }
    }

    private fun showImageFromBase64() {
        if (postFileData.isEmpty()) return
        try {
            val imageBytes = Base64.decode(postFileData, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ivNoteImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnOpenPdf.setOnClickListener {
            openPdfFile()
        }

        btnRate.setOnClickListener {
            showRatingDialog()
        }
    }

    private fun openPdfFile() {
        if (postFileData.isEmpty()) {
            Toast.makeText(requireContext(), "PDF verisi bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfBytes = Base64.decode(postFileData, Base64.DEFAULT)
            val cacheDir = requireContext().cacheDir
            val pdfFile = File(cacheDir, postFileName.ifEmpty { "note.pdf" })
            FileOutputStream(pdfFile).use { it.write(pdfBytes) }

            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "PDF'i aç"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "PDF açılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRatingDialog() {
        val uid = auth.currentUser?.uid ?: return

        if (postUploaderUid == uid) {
            Toast.makeText(requireContext(), "Kendi notuna puan veremezsin!", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_rating, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val stars = listOf(
            dialogView.findViewById<ImageView>(R.id.star1),
            dialogView.findViewById<ImageView>(R.id.star2),
            dialogView.findViewById<ImageView>(R.id.star3),
            dialogView.findViewById<ImageView>(R.id.star4),
            dialogView.findViewById<ImageView>(R.id.star5)
        )

        var selectedRating = 0

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStars(stars, selectedRating)
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener {
            if (selectedRating > 0) {
                submitRating(selectedRating.toFloat())
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Lütfen bir puan seç", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateStars(stars: List<ImageView>, rating: Int) {
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(R.drawable.ic_star_filled)
                star.setColorFilter(
                    android.graphics.Color.parseColor("#F28A55"),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                star.setImageResource(R.drawable.ic_star_outline)
                star.setColorFilter(
                    android.graphics.Color.parseColor("#AAB6D6"),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    private fun submitRating(newRating: Float) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("notes").document(postId)
        val ratingRef = db.collection("ratings").document("${uid}_$postId")

        ratingRef.get().addOnSuccessListener { ratingSnap ->
            if (ratingSnap.exists()) {
                val oldRating = ratingSnap.getLong("rating")?.toFloat() ?: 0f
                val diff = newRating - oldRating

                docRef.get().addOnSuccessListener { noteSnap ->
                    val oldSum = noteSnap.getLong("ratingSum") ?: 0L
                    val oldCount = noteSnap.getLong("ratingCount") ?: 1L
                    val newSum = (oldSum + diff).toLong()
                    val newAvg = newSum.toDouble() / oldCount

                    docRef.update(mapOf(
                        "ratingSum" to newSum,
                        "avgRating" to newAvg
                    )).addOnSuccessListener {
                        postAvgRating = newAvg
                        postRatingSum = newSum
                        tvAvgRating.text = String.format("%.1f", newAvg)
                    }

                    ratingRef.update("rating", newRating.toLong())

                    val diffInt = diff.toInt()
                    if (diffInt != 0) {
                        addPointsToNoteOwner(postUploaderUid, diffInt)
                    }

                    Toast.makeText(requireContext(), "Puanın güncellendi!", Toast.LENGTH_SHORT).show()
                }
            } else {
                docRef.get().addOnSuccessListener { noteSnap ->
                    val oldSum = noteSnap.getLong("ratingSum") ?: 0L
                    val oldCount = noteSnap.getLong("ratingCount") ?: 0L
                    val newSum = (oldSum + newRating).toLong()
                    val newCount = oldCount + 1
                    val newAvg = newSum.toDouble() / newCount

                    docRef.update(mapOf(
                        "ratingSum" to newSum,
                        "ratingCount" to newCount,
                        "avgRating" to newAvg
                    )).addOnSuccessListener {
                        postAvgRating = newAvg
                        postRatingCount = newCount
                        postRatingSum = newSum
                        tvAvgRating.text = String.format("%.1f", newAvg)
                        tvRatingCount.text = "($newCount oy)"
                    }

                    ratingRef.set(mapOf(
                        "uid" to uid,
                        "noteId" to postId,
                        "rating" to newRating.toLong()
                    ))

                    addPointsToNoteOwner(postUploaderUid, newRating.toInt())

                    Toast.makeText(requireContext(), "Puan verildi!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addPointsToNoteOwner(ownerUid: String, pointsToAdd: Int) {
        if (ownerUid.isEmpty()) return
        val userRef = db.collection("users").document(ownerUid)
        userRef.get().addOnSuccessListener { snap ->
            val currentPoints = snap.getLong("points")?.toInt() ?: 0
            val newPoints = (currentPoints + pointsToAdd).coerceAtLeast(0)
            userRef.update("points", newPoints)
        }
    }

    companion object {
        fun newInstance(post: Post): NoteDetailFragment {
            val fragment = NoteDetailFragment()
            val bundle = Bundle()
            bundle.putString("id", post.id)
            bundle.putString("title", post.title)
            bundle.putString("desc", post.desc)
            bundle.putString("course", post.course)
            bundle.putString("tag", post.tag)
            bundle.putString("uploaderEmail", post.authorEmail)
            bundle.putString("uploaderUid", post.uploaderUid)
            bundle.putDouble("avgRating", post.avgRating)
            bundle.putLong("ratingCount", post.ratingCount)
            bundle.putLong("ratingSum", post.ratingSum)
            bundle.putString("fileName", post.fileName)
            bundle.putString("fileType", post.fileType)
            bundle.putString("fileData", post.fileData)
            fragment.arguments = bundle
            return fragment
        }
    }
}