package duygu.yilmaz.CampusNote.ui.notedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duygu.yilmaz.CampusNote.data.model.Post
import duygu.yilmaz.CampusNote.data.model.RatingResult
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.NoteNotFoundException
import duygu.yilmaz.CampusNote.data.repository.NoteRepository
import duygu.yilmaz.CampusNote.data.repository.OwnNoteRatingException
import duygu.yilmaz.CampusNote.data.repository.RatingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val ratingRepository: RatingRepository = RatingRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {
    private val _noteState = MutableLiveData<NoteDetailUiState>(NoteDetailUiState.Loading)
    val noteState: LiveData<NoteDetailUiState> = _noteState

    private val _fileState = MutableLiveData<NoteFileUiState>(NoteFileUiState.None)
    val fileState: LiveData<NoteFileUiState> = _fileState

    private val _ratingState = MutableLiveData<RatingUiState>(RatingUiState.Idle)
    val ratingState: LiveData<RatingUiState> = _ratingState

    fun loadNote(noteId: String) {
        if (_noteState.value is NoteDetailUiState.Content) return

        if (noteId.isBlank()) {
            _noteState.value = NoteDetailUiState.Missing
            return
        }

        _noteState.value = NoteDetailUiState.Loading
        viewModelScope.launch {
            val post = try {
                noteRepository.getNote(noteId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _noteState.value = NoteDetailUiState.Error(exception)
                return@launch
            }

            if (post == null) {
                _noteState.value = NoteDetailUiState.Missing
                return@launch
            }

            _noteState.value = NoteDetailUiState.Content(post)
            loadFile(post)
        }
    }

    /** Metadata geldikten sonra, sadece nota gerçekten dosya eklenmişse çalışır. */
    private suspend fun loadFile(post: Post) {
        if (post.fileType.isBlank()) {
            _fileState.value = NoteFileUiState.None
            return
        }

        _fileState.value = NoteFileUiState.Loading
        _fileState.value = try {
            noteRepository.getNoteFile(post.id)
                ?.let { NoteFileUiState.Content(it) }
                ?: NoteFileUiState.Missing
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            NoteFileUiState.Error(exception)
        }
    }

    fun ratingAvailability(): RatingAvailability {
        val post = (_noteState.value as? NoteDetailUiState.Content)?.post
            ?: return RatingAvailability.MISSING_NOTE
        val user = authRepository.currentUser() ?: return RatingAvailability.MISSING_SESSION

        return if (post.uploaderUid == user.uid) {
            RatingAvailability.OWN_NOTE
        } else {
            RatingAvailability.ALLOWED
        }
    }

    fun submitRating(rating: Int) {
        if (_ratingState.value == RatingUiState.Submitting) return

        val post = (_noteState.value as? NoteDetailUiState.Content)?.post
        if (post == null) {
            _ratingState.value = RatingUiState.MissingNote
            return
        }

        val user = authRepository.currentUser()
        if (user == null) {
            _ratingState.value = RatingUiState.MissingSession
            return
        }

        _ratingState.value = RatingUiState.Submitting
        viewModelScope.launch {
            _ratingState.value = try {
                val result = ratingRepository.submitRating(
                    noteId = post.id,
                    raterUid = user.uid,
                    newRating = rating
                )
                applyRatingToNote(result)
                RatingUiState.Success(result)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                when (exception) {
                    is NoteNotFoundException -> RatingUiState.MissingNote
                    is OwnNoteRatingException -> RatingUiState.OwnNote
                    else -> RatingUiState.Error(exception)
                }
            }
        }
    }

    fun resetRatingState() {
        _ratingState.value = RatingUiState.Idle
    }

    private fun applyRatingToNote(result: RatingResult) {
        val current = (_noteState.value as? NoteDetailUiState.Content)?.post ?: return

        _noteState.value = NoteDetailUiState.Content(
            current.copy(
                avgRating = result.average,
                ratingCount = result.count,
                ratingSum = result.sum
            )
        )
    }
}
