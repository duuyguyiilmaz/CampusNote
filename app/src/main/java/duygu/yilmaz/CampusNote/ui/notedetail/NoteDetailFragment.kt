package duygu.yilmaz.CampusNote.ui.notedetail

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
import androidx.lifecycle.ViewModelProvider
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.data.model.Post
import com.google.android.material.button.MaterialButton
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

    private val viewModel: NoteDetailViewModel by lazy {
        ViewModelProvider(this)[NoteDetailViewModel::class.java]
    }

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

        viewModel.ratingState.observe(viewLifecycleOwner, ::renderRatingState)
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
        when (viewModel.ratingAvailability(postUploaderUid)) {
            RatingAvailability.MISSING_SESSION -> {
                Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
                return
            }

            RatingAvailability.OWN_NOTE -> {
                Toast.makeText(
                    requireContext(),
                    "Kendi notuna puan veremezsin!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            RatingAvailability.ALLOWED -> Unit
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
                viewModel.submitRating(postId, selectedRating)
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

    private fun renderRatingState(state: RatingUiState) {
        when (state) {
            RatingUiState.Idle -> btnRate.isEnabled = true
            RatingUiState.Submitting -> btnRate.isEnabled = false

            is RatingUiState.Success -> {
                btnRate.isEnabled = true
                postAvgRating = state.result.average
                postRatingCount = state.result.count
                postRatingSum = state.result.sum
                tvAvgRating.text = String.format("%.1f", state.result.average)
                tvRatingCount.text = "(${state.result.count} oy)"

                val message = if (state.result.updatedExistingRating) {
                    "Puanın güncellendi!"
                } else {
                    "Puan verildi!"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.resetRatingState()
            }

            RatingUiState.MissingSession -> {
                btnRate.isEnabled = true
                Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
                viewModel.resetRatingState()
            }

            RatingUiState.OwnNote -> {
                btnRate.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Kendi notuna puan veremezsin!",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetRatingState()
            }

            RatingUiState.MissingNote -> {
                btnRate.isEnabled = true
                Toast.makeText(requireContext(), "Not bulunamadı.", Toast.LENGTH_LONG).show()
                viewModel.resetRatingState()
            }

            is RatingUiState.Error -> {
                btnRate.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Puan kaydedilemedi: ${state.exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetRatingState()
            }
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
