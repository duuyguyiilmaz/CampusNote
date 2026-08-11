package duygu.yilmaz.campusnote.ui.upload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.data.local.EncodedFile
import duygu.yilmaz.campusnote.data.local.MAX_NOTE_FILE_KB
import duygu.yilmaz.campusnote.data.local.NoteFileEncoder
import duygu.yilmaz.campusnote.data.model.NoteDraft
import duygu.yilmaz.campusnote.data.model.NoteFileType
import duygu.yilmaz.campusnote.databinding.FragmentUploadBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by lazy {
        ViewModelProvider(this)[UploadViewModel::class.java]
    }

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var selectedFileType: String = ""

    /** 0. sıra seçim uyarısıdır; etiket seçilmediyse boş string kaydedilir. */
    private val tags: List<String> by lazy {
        listOf(getString(R.string.tag_prompt)) + resources.getStringArray(R.array.note_tags)
    }

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                selectedFileName = getFileName(uri)
                selectedFileType = NoteFileType.PDF
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
                selectedFileType = NoteFileType.IMAGE
                showFilePreview()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupClickListeners()
        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)
        animateViews()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tags
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTag.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSelectPdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = NoteFileType.PDF_MIME_TYPE
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pdfPickerLauncher.launch(intent)
        }

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = NoteFileType.IMAGE_MIME_TYPE
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            imagePickerLauncher.launch(intent)
        }

        binding.btnRemoveFile.setOnClickListener {
            removeSelectedFile()
        }

        binding.btnUpload.setOnClickListener {
            uploadNote()
        }
    }

    private fun animateViews() {
        binding.tvUploadTitle.alpha = 0f
        binding.tvUploadTitle.translationY = -20f
        binding.tvUploadTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        binding.tvUploadSubtitle.alpha = 0f
        binding.tvUploadSubtitle.translationY = -15f
        binding.tvUploadSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .start()

        binding.cardUploadForm.alpha = 0f
        binding.cardUploadForm.scaleX = 0.95f
        binding.cardUploadForm.scaleY = 0.95f
        binding.cardUploadForm.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(400)
            .setDuration(500)
            .start()

        binding.btnUpload.alpha = 0f
        binding.btnUpload.translationY = 20f
        binding.btnUpload.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(600)
            .setDuration(400)
            .start()
    }

    private fun showFilePreview() {
        binding.layoutFilePreview.visibility = View.VISIBLE
        binding.tvFileName.text = selectedFileName

        binding.ivFileIcon.setImageResource(
            if (selectedFileType == NoteFileType.PDF) R.drawable.ic_pdf else R.drawable.ic_image
        )
    }

    private fun removeSelectedFile() {
        selectedFileUri = null
        selectedFileName = ""
        selectedFileType = NoteFileType.NONE
        binding.layoutFilePreview.visibility = View.GONE
    }

    private fun getFileName(uri: Uri): String {
        var name = getString(R.string.default_file_name)
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun uploadNote() {
        val course = binding.etCourse.text?.toString()?.trim().orEmpty()
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val desc = binding.etDesc.text?.toString()?.trim().orEmpty()
        val tagIndex = binding.spTag.selectedItemPosition

        if (!validateInput(course, title, desc)) return

        // Dosya okuma + görsel sıkıştırma ana thread'i kilitler; IO'ya alıyoruz.
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)

            val encoder = NoteFileEncoder(requireContext().contentResolver)
            val encoded = withContext(Dispatchers.IO) {
                encoder.encode(selectedFileUri, selectedFileType)
            }

            when (encoded) {
                is EncodedFile.TooLarge -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_file_too_large,
                            encoded.byteSize / BYTES_PER_KB,
                            MAX_NOTE_FILE_KB
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }

                is EncodedFile.Failure -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_file_read, encoded.exception.message.orEmpty()),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                EncodedFile.None -> submitDraft(course, title, desc, tagIndex, "", 0L)

                is EncodedFile.Success ->
                    submitDraft(course, title, desc, tagIndex, encoded.data, encoded.byteSize)
            }
        }
    }

    private fun submitDraft(
        course: String,
        title: String,
        desc: String,
        tagIndex: Int,
        fileData: String,
        fileSize: Long
    ) {
        viewModel.uploadNote(
            NoteDraft(
                course = course,
                title = title,
                description = desc,
                tag = if (tagIndex > TAG_PROMPT_INDEX) tags[tagIndex] else "",
                fileName = selectedFileName,
                fileType = selectedFileType,
                fileData = fileData,
                fileSize = fileSize
            )
        )
    }

    private fun renderState(state: UploadUiState) {
        when (state) {
            UploadUiState.Idle -> showLoading(false)
            UploadUiState.Loading -> showLoading(true)

            UploadUiState.Success -> {
                showLoading(false)
                clearForm()
                Toast.makeText(
                    requireContext(),
                    R.string.upload_success,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetState()
            }

            UploadUiState.MissingSession -> {
                showLoading(false)
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_LONG)
                    .show()
                viewModel.resetState()
            }

            is UploadUiState.Error -> {
                showLoading(false)
                val reason = state.exception.message.orEmpty()
                val message = when (state.stage) {
                    UploadFailureStage.USER_PROFILE ->
                        getString(R.string.error_user_profile_read, reason)

                    UploadFailureStage.NOTE -> getString(R.string.error_generic, reason)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
        }
    }

    private fun validateInput(course: String, title: String, desc: String): Boolean {
        binding.tilCourse.error = null
        binding.tilTitle.error = null
        binding.tilDesc.error = null

        return when {
            course.isEmpty() -> {
                binding.tilCourse.error = getString(R.string.error_course_empty)
                false
            }
            title.isEmpty() -> {
                binding.tilTitle.error = getString(R.string.error_note_title_empty)
                false
            }
            desc.isEmpty() -> {
                binding.tilDesc.error = getString(R.string.error_description_empty)
                false
            }
            else -> true
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnUpload.isEnabled = !show
        binding.btnUpload.setText(
            if (show) R.string.upload_action_in_progress else R.string.upload_action
        )
        binding.btnUpload.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun clearForm() {
        binding.etCourse.text?.clear()
        binding.etTitle.text?.clear()
        binding.etDesc.text?.clear()
        binding.spTag.setSelection(TAG_PROMPT_INDEX)
        removeSelectedFile()
    }

    private companion object {
        const val BYTES_PER_KB = 1024
        const val TAG_PROMPT_INDEX = 0
    }
}
