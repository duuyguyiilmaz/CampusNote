package duygu.yilmaz.CampusNote.ui.notedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.repository.AuthRepository
import duygu.yilmaz.CampusNote.data.repository.NoteNotFoundException
import duygu.yilmaz.CampusNote.data.repository.OwnNoteRatingException
import duygu.yilmaz.CampusNote.data.repository.RatingRepository

class NoteDetailViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val ratingRepository: RatingRepository = RatingRepository()
) : ViewModel() {
    private val _ratingState = MutableLiveData<RatingUiState>(RatingUiState.Idle)
    val ratingState: LiveData<RatingUiState> = _ratingState

    fun ratingAvailability(noteOwnerUid: String): RatingAvailability {
        val user = authRepository.currentUser() ?: return RatingAvailability.MISSING_SESSION
        return if (noteOwnerUid == user.uid) {
            RatingAvailability.OWN_NOTE
        } else {
            RatingAvailability.ALLOWED
        }
    }

    fun submitRating(noteId: String, rating: Int) {
        if (_ratingState.value == RatingUiState.Submitting) return

        val user = authRepository.currentUser()
        if (user == null) {
            _ratingState.value = RatingUiState.MissingSession
            return
        }

        _ratingState.value = RatingUiState.Submitting
        ratingRepository.submitRating(
            noteId = noteId,
            raterUid = user.uid,
            newRating = rating,
            onSuccess = { result ->
                _ratingState.value = RatingUiState.Success(result)
            },
            onFailure = { exception ->
                _ratingState.value = when (exception) {
                    is NoteNotFoundException -> RatingUiState.MissingNote
                    is OwnNoteRatingException -> RatingUiState.OwnNote
                    else -> RatingUiState.Error(exception)
                }
            }
        )
    }

    fun resetRatingState() {
        _ratingState.value = RatingUiState.Idle
    }
}
