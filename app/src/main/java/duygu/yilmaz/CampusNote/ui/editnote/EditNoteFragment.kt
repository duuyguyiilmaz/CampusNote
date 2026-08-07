package duygu.yilmaz.CampusNote.ui.editnote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.data.model.NoteUpdate

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

    private val viewModel: EditNoteViewModel by lazy {
        ViewModelProvider(this)[EditNoteViewModel::class.java]
    }

    private val noteId: String
        get() = arguments?.getString(ARG_DOC_ID).orEmpty()

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_edit_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinner()
        setupClickListeners()

        viewModel.uiState.observe(viewLifecycleOwner, ::renderUiState)
        viewModel.actionState.observe(viewLifecycleOwner, ::renderActionState)
        viewModel.loadNote(noteId)
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
        btnSave.isEnabled = false
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
        btnSave.setOnClickListener { saveChanges() }
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveChanges() {
        val course = etCourse.text?.toString()?.trim().orEmpty()
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val description = etDesc.text?.toString()?.trim().orEmpty()
        val tagIndex = spTag.selectedItemPosition

        if (!validateInput(course, title, description)) return

        viewModel.saveNote(
            noteId = noteId,
            update = NoteUpdate(
                course = course,
                title = title,
                description = description,
                tag = if (tagIndex > 0) tags[tagIndex] else ""
            )
        )
    }

    private fun validateInput(course: String, title: String, description: String): Boolean {
        tilCourse.error = null
        tilTitle.error = null
        tilDesc.error = null

        return when {
            course.isEmpty() -> {
                tilCourse.error = "Ders adı boş olamaz"
                false
            }

            title.isEmpty() -> {
                tilTitle.error = "Başlık boş olamaz"
                false
            }

            description.isEmpty() -> {
                tilDesc.error = "Açıklama boş olamaz"
                false
            }

            else -> true
        }
    }

    private fun renderUiState(state: EditNoteUiState) {
        when (state) {
            EditNoteUiState.Idle,
            EditNoteUiState.Loading -> btnSave.isEnabled = false

            EditNoteUiState.MissingSession -> {
                btnSave.isEnabled = false
                Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
            }

            EditNoteUiState.MissingNote -> {
                btnSave.isEnabled = false
                Toast.makeText(requireContext(), "Not bulunamadı.", Toast.LENGTH_LONG).show()
            }

            is EditNoteUiState.Content -> {
                showNote(state)
                btnSave.isEnabled = true
            }

            is EditNoteUiState.Error -> {
                btnSave.isEnabled = false
                Toast.makeText(
                    requireContext(),
                    "Not yüklenemedi: ${state.exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showNote(state: EditNoteUiState.Content) {
        val note = state.note
        etCourse.setText(note.course)
        etTitle.setText(note.title)
        etDesc.setText(note.desc)

        val tagIndex = tags.indexOf(note.tag)
        spTag.setSelection(if (tagIndex > 0) tagIndex else 0)

        if (note.fileName.isNotEmpty()) {
            layoutCurrentFile.visibility = View.VISIBLE
            tvCurrentFileName.text = note.fileName
            val icon = if (note.fileType == "pdf") R.drawable.ic_pdf else R.drawable.ic_image
            ivCurrentFileIcon.setImageResource(icon)
        } else {
            layoutCurrentFile.visibility = View.GONE
        }
    }

    private fun renderActionState(state: EditNoteActionState) {
        when (state) {
            EditNoteActionState.Idle -> {
                btnSave.isEnabled = viewModel.uiState.value is EditNoteUiState.Content
            }

            EditNoteActionState.Saving -> btnSave.isEnabled = false

            EditNoteActionState.Success -> {
                Toast.makeText(requireContext(), "Not güncellendi!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }

            EditNoteActionState.MissingSession -> {
                Toast.makeText(requireContext(), "Giriş bulunamadı.", Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }

            EditNoteActionState.NotOwner -> {
                Toast.makeText(
                    requireContext(),
                    "Bu notu düzenleme yetkin yok!",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetActionState()
            }

            is EditNoteActionState.Error -> {
                Toast.makeText(
                    requireContext(),
                    "Hata: ${state.exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetActionState()
            }
        }
    }
}
