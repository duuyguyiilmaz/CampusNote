package duygu.yilmaz.campusnote.ui.notedetail

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.data.model.NoteFileType
import duygu.yilmaz.campusnote.data.model.Post
import duygu.yilmaz.campusnote.databinding.DialogRatingBinding
import duygu.yilmaz.campusnote.databinding.FragmentNoteDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteDetailViewModel by lazy {
        ViewModelProvider(this)[NoteDetailViewModel::class.java]
    }

    private var noteId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteId = arguments?.getString(ARG_NOTE_ID).orEmpty()

        setupClickListeners()

        viewModel.noteState.observe(viewLifecycleOwner, ::renderNoteState)
        viewModel.fileState.observe(viewLifecycleOwner, ::renderFileState)
        viewModel.ratingState.observe(viewLifecycleOwner, ::renderRatingState)

        viewModel.loadNote(noteId)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---- 1. aşama: metadata ----

    private fun renderNoteState(state: NoteDetailUiState) {
        when (state) {
            NoteDetailUiState.Loading -> showLoading(true)

            is NoteDetailUiState.Content -> {
                showLoading(false)
                binding.groupNoteContent.visibility = View.VISIBLE
                fillViews(state.post)
                showFileCard(state.post)
            }

            NoteDetailUiState.Missing -> {
                showLoading(false)
                Toast.makeText(requireContext(), R.string.error_note_not_found, Toast.LENGTH_LONG)
                    .show()
                parentFragmentManager.popBackStack()
            }

            is NoteDetailUiState.Error -> {
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_note_load, state.exception.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarDetail.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.groupNoteContent.visibility = View.GONE
            binding.layoutPdfCard.visibility = View.GONE
            binding.layoutImageCard.visibility = View.GONE
        }
    }

    private fun fillViews(post: Post) = with(binding) {
        tvNoteTitle.text = post.title
        tvCourse.text = post.course
        tvUploader.text = post.uploaderName
        tvDescription.text = post.desc

        if (post.tag.isEmpty()) {
            tvTag.visibility = View.GONE
        } else {
            tvTag.visibility = View.VISIBLE
            tvTag.text = post.tag.uppercase()
        }

        tvAvgRating.text = String.format("%.1f", post.avgRating)
        tvRatingCount.text = getString(R.string.rating_count, post.ratingCount)
    }

    /** Dosya kartı metadata ile hemen çizilir; içerik henüz yolda olabilir. */
    private fun showFileCard(post: Post) {
        when (post.fileType) {
            NoteFileType.PDF -> {
                binding.layoutPdfCard.visibility = View.VISIBLE
                binding.layoutImageCard.visibility = View.GONE
                binding.tvPdfFileName.text = buildFileLabel(post)
                binding.btnOpenPdf.isEnabled = false
            }

            NoteFileType.IMAGE -> {
                binding.layoutPdfCard.visibility = View.GONE
                binding.layoutImageCard.visibility = View.VISIBLE
            }

            else -> {
                binding.layoutPdfCard.visibility = View.GONE
                binding.layoutImageCard.visibility = View.GONE
            }
        }
    }

    private fun buildFileLabel(post: Post): String {
        if (post.fileSize <= 0L) return post.fileName
        return getString(R.string.file_size_label, post.fileName, post.fileSize / BYTES_PER_KB)
    }

    // ---- 2. aşama: dosya içeriği ----

    private fun renderFileState(state: NoteFileUiState) {
        when (state) {
            NoteFileUiState.None -> Unit

            NoteFileUiState.Loading -> {
                binding.btnOpenPdf.isEnabled = false
                binding.btnOpenPdf.setText(R.string.file_loading)
                binding.progressBarImage.visibility = View.VISIBLE
                binding.ivNoteImage.visibility = View.GONE
            }

            is NoteFileUiState.Content -> {
                binding.btnOpenPdf.isEnabled = true
                binding.btnOpenPdf.setText(R.string.file_open)
                if (binding.layoutImageCard.visibility == View.VISIBLE) {
                    showImageFromBase64(state.data)
                }
            }

            NoteFileUiState.Missing ->
                showFileUnavailable(getString(R.string.error_file_not_found))

            is NoteFileUiState.Error -> showFileUnavailable(
                getString(R.string.error_file_load, state.exception.message.orEmpty())
            )
        }
    }

    private fun showFileUnavailable(message: String) {
        binding.btnOpenPdf.isEnabled = false
        binding.btnOpenPdf.setText(R.string.file_unavailable)
        binding.progressBarImage.visibility = View.GONE
        binding.layoutImageCard.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /** Base64 çözme ve bitmap üretimi IO'da; ana thread sadece hazır bitmap'i basar. */
    private fun showImageFromBase64(fileData: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val imageBytes = Base64.decode(fileData, Base64.DEFAULT)

                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)

                    val options = BitmapFactory.Options().apply {
                        inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
                    }
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                }.getOrNull()
            }

            binding.progressBarImage.visibility = View.GONE

            if (bitmap == null) {
                binding.layoutImageCard.visibility = View.GONE
                Toast.makeText(requireContext(), R.string.error_image_load, Toast.LENGTH_SHORT)
                    .show()
            } else {
                binding.ivNoteImage.visibility = View.VISIBLE
                binding.ivNoteImage.setImageBitmap(bitmap)
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1

        var sampleSize = 1
        while (width / (sampleSize * 2) >= MAX_IMAGE_SIZE_PX ||
            height / (sampleSize * 2) >= MAX_IMAGE_SIZE_PX
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    // ---- etkileşim ----

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnOpenPdf.setOnClickListener {
            openPdfFile()
        }

        binding.btnRate.setOnClickListener {
            showRatingDialog()
        }
    }

    private fun openPdfFile() {
        val post = currentPost()
        val fileData = (viewModel.fileState.value as? NoteFileUiState.Content)?.data

        if (post == null || fileData.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.error_pdf_data_missing, Toast.LENGTH_SHORT)
                .show()
            return
        }

        binding.btnOpenPdf.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val pdfFile = withContext(Dispatchers.IO) {
                runCatching {
                    val pdfBytes = Base64.decode(fileData, Base64.DEFAULT)
                    val file = File(requireContext().cacheDir, safePdfName(post.fileName))
                    FileOutputStream(file).use { it.write(pdfBytes) }
                    file
                }.getOrNull()
            }

            binding.btnOpenPdf.isEnabled = true

            if (pdfFile == null) {
                Toast.makeText(requireContext(), R.string.error_pdf_open, Toast.LENGTH_LONG).show()
                return@launch
            }

            try {
                val uri: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}$FILE_PROVIDER_SUFFIX",
                    pdfFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, NoteFileType.PDF_MIME_TYPE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(
                    Intent.createChooser(intent, getString(R.string.pdf_chooser_title))
                )
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_pdf_open_reason, e.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Dosya adı Firestore'dan geliyor; cache klasörünün dışına yazmayı engelle. */
    private fun safePdfName(fileName: String): String {
        val name = File(fileName).name.filter { it.isLetterOrDigit() || it in "-_. " }.trim()
        return name.ifEmpty { FALLBACK_PDF_NAME }
    }

    private fun currentPost(): Post? =
        (viewModel.noteState.value as? NoteDetailUiState.Content)?.post

    private fun showRatingDialog() {
        when (viewModel.ratingAvailability()) {
            RatingAvailability.MISSING_SESSION -> {
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
                return
            }

            RatingAvailability.MISSING_NOTE -> {
                Toast.makeText(requireContext(), R.string.error_note_not_found, Toast.LENGTH_SHORT)
                    .show()
                return
            }

            RatingAvailability.OWN_NOTE -> {
                Toast.makeText(
                    requireContext(),
                    R.string.error_rating_own_note,
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            RatingAvailability.ALLOWED -> Unit
        }

        val dialogBinding = DialogRatingBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val stars = listOf(
            dialogBinding.star1,
            dialogBinding.star2,
            dialogBinding.star3,
            dialogBinding.star4,
            dialogBinding.star5
        )

        var selectedRating = 0

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStars(stars, selectedRating)
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSubmit.setOnClickListener {
            if (selectedRating > 0) {
                viewModel.submitRating(selectedRating)
                dialog.dismiss()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.error_rating_not_selected,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun updateStars(stars: List<ImageView>, rating: Int) {
        stars.forEachIndexed { index, star ->
            val filled = index < rating
            star.setImageResource(
                if (filled) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            star.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (filled) R.color.star_filled else R.color.star_empty
                ),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun renderRatingState(state: RatingUiState) {
        when (state) {
            RatingUiState.Idle -> binding.btnRate.isEnabled = true
            RatingUiState.Submitting -> binding.btnRate.isEnabled = false

            is RatingUiState.Success -> {
                binding.btnRate.isEnabled = true
                val messageId = if (state.result.updatedExistingRating) {
                    R.string.rating_updated
                } else {
                    R.string.rating_saved
                }
                Toast.makeText(requireContext(), messageId, Toast.LENGTH_SHORT).show()
                viewModel.resetRatingState()
            }

            RatingUiState.MissingSession -> {
                binding.btnRate.isEnabled = true
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
                viewModel.resetRatingState()
            }

            RatingUiState.OwnNote -> {
                binding.btnRate.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    R.string.error_rating_own_note,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetRatingState()
            }

            RatingUiState.MissingNote -> {
                binding.btnRate.isEnabled = true
                Toast.makeText(requireContext(), R.string.error_note_not_found, Toast.LENGTH_LONG)
                    .show()
                viewModel.resetRatingState()
            }

            is RatingUiState.Error -> {
                binding.btnRate.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_rating_save, state.exception.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetRatingState()
            }
        }
    }

    companion object {
        private const val ARG_NOTE_ID = "noteId"
        private const val MAX_IMAGE_SIZE_PX = 1080
        private const val BYTES_PER_KB = 1024
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        private const val FALLBACK_PDF_NAME = "note.pdf"

        fun newInstance(noteId: String): NoteDetailFragment {
            return NoteDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTE_ID, noteId)
                }
            }
        }
    }
}
