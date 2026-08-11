package duygu.yilmaz.campusnote.ui.editnote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.data.model.NoteFileType
import duygu.yilmaz.campusnote.data.model.NoteUpdate
import duygu.yilmaz.campusnote.databinding.FragmentEditNoteBinding

class EditNoteFragment : Fragment() {

    companion object {
        private const val ARG_DOC_ID = "docId"
        private const val TAG_PROMPT_INDEX = 0

        fun newInstance(docId: String): EditNoteFragment {
            return EditNoteFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOC_ID, docId)
                }
            }
        }
    }

    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditNoteViewModel by lazy {
        ViewModelProvider(this)[EditNoteViewModel::class.java]
    }

    private val noteId: String
        get() = arguments?.getString(ARG_DOC_ID).orEmpty()

    /** 0. sıra seçim uyarısıdır; etiket seçilmediyse boş string kaydedilir. */
    private val tags: List<String> by lazy {
        listOf(getString(R.string.tag_prompt)) + resources.getStringArray(R.array.note_tags)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSave.isEnabled = false
        setupSpinner()
        setupClickListeners()

        viewModel.uiState.observe(viewLifecycleOwner, ::renderUiState)
        viewModel.actionState.observe(viewLifecycleOwner, ::renderActionState)
        viewModel.loadNote(noteId)
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
        binding.spEditTag.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener { saveChanges() }
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveChanges() {
        val course = binding.etEditCourse.text?.toString()?.trim().orEmpty()
        val title = binding.etEditTitle.text?.toString()?.trim().orEmpty()
        val description = binding.etEditDesc.text?.toString()?.trim().orEmpty()
        val tagIndex = binding.spEditTag.selectedItemPosition

        if (!validateInput(course, title, description)) return

        viewModel.saveNote(
            noteId = noteId,
            update = NoteUpdate(
                course = course,
                title = title,
                description = description,
                tag = if (tagIndex > TAG_PROMPT_INDEX) tags[tagIndex] else ""
            )
        )
    }

    private fun validateInput(course: String, title: String, description: String): Boolean {
        binding.tilEditCourse.error = null
        binding.tilEditTitle.error = null
        binding.tilEditDesc.error = null

        return when {
            course.isEmpty() -> {
                binding.tilEditCourse.error = getString(R.string.error_course_empty)
                false
            }

            title.isEmpty() -> {
                binding.tilEditTitle.error = getString(R.string.error_title_empty)
                false
            }

            description.isEmpty() -> {
                binding.tilEditDesc.error = getString(R.string.error_description_empty)
                false
            }

            else -> true
        }
    }

    private fun renderUiState(state: EditNoteUiState) {
        when (state) {
            EditNoteUiState.Idle,
            EditNoteUiState.Loading -> binding.btnSave.isEnabled = false

            EditNoteUiState.MissingSession -> {
                binding.btnSave.isEnabled = false
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
            }

            EditNoteUiState.MissingNote -> {
                binding.btnSave.isEnabled = false
                Toast.makeText(requireContext(), R.string.error_note_not_found, Toast.LENGTH_LONG)
                    .show()
            }

            is EditNoteUiState.Content -> {
                showNote(state)
                binding.btnSave.isEnabled = true
            }

            is EditNoteUiState.Error -> {
                binding.btnSave.isEnabled = false
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_note_load, state.exception.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showNote(state: EditNoteUiState.Content) {
        val note = state.note
        binding.etEditCourse.setText(note.course)
        binding.etEditTitle.setText(note.title)
        binding.etEditDesc.setText(note.desc)

        val tagIndex = tags.indexOf(note.tag)
        binding.spEditTag.setSelection(if (tagIndex > TAG_PROMPT_INDEX) tagIndex else TAG_PROMPT_INDEX)

        if (note.fileName.isNotEmpty()) {
            binding.layoutCurrentFile.visibility = View.VISIBLE
            binding.tvCurrentFileName.text = note.fileName
            val icon = if (note.fileType == NoteFileType.PDF) {
                R.drawable.ic_pdf
            } else {
                R.drawable.ic_image
            }
            binding.ivCurrentFileIcon.setImageResource(icon)
        } else {
            binding.layoutCurrentFile.visibility = View.GONE
        }
    }

    private fun renderActionState(state: EditNoteActionState) {
        when (state) {
            EditNoteActionState.Idle -> {
                binding.btnSave.isEnabled = viewModel.uiState.value is EditNoteUiState.Content
            }

            EditNoteActionState.Saving -> binding.btnSave.isEnabled = false

            EditNoteActionState.Success -> {
                Toast.makeText(requireContext(), R.string.note_updated, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }

            EditNoteActionState.MissingSession -> {
                Toast.makeText(requireContext(), R.string.error_missing_session, Toast.LENGTH_SHORT)
                    .show()
                viewModel.resetActionState()
            }

            EditNoteActionState.NotOwner -> {
                Toast.makeText(
                    requireContext(),
                    R.string.error_note_not_owner,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetActionState()
            }

            is EditNoteActionState.Error -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_generic, state.exception.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetActionState()
            }
        }
    }
}
