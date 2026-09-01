package duygu.yilmaz.campusnote.testing

import duygu.yilmaz.campusnote.data.model.AuthenticatedUser
import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.data.model.NoteDraft
import duygu.yilmaz.campusnote.data.model.NoteUpdate
import duygu.yilmaz.campusnote.data.model.Post
import duygu.yilmaz.campusnote.data.model.RatingResult
import duygu.yilmaz.campusnote.data.model.UserProfile
import duygu.yilmaz.campusnote.data.repository.AuthRepository
import duygu.yilmaz.campusnote.data.repository.NoteRepository
import duygu.yilmaz.campusnote.data.repository.RatingRepository
import duygu.yilmaz.campusnote.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repository arayüzlerinin elle yazılmış sahteleri.
 *
 * Bir mock kütüphanesi yerine bunlar tercih edildi: davranış tek bir dosyada açıkça
 * okunuyor, testler "şu çağrı şu değeri döndürsün" kurulumundan çok "veri şu hâldeyken
 * ViewModel ne yapıyor" sorusuna odaklanıyor.
 *
 * Ortak kalıp: `...Error` alanı doldurulursa çağrı onu fırlatır, aksi hâlde
 * kurulan veriyi döndürür. Yapılan çağrılar ayrıca kaydedilir, böylece testler
 * ViewModel'in repository'ye ne gönderdiğini de doğrulayabiliyor.
 */
class FakeAuthRepository(
    private var user: AuthenticatedUser? = null
) : AuthRepository {

    var signInError: Exception? = null
    var registerError: Exception? = null

    var signInCount = 0
        private set
    var registerCount = 0
        private set
    var signOutCount = 0
        private set
    var lastCredentials: Pair<String, String>? = null
        private set

    fun setUser(value: AuthenticatedUser?) {
        user = value
    }

    override fun currentUser(): AuthenticatedUser? = user

    override suspend fun signIn(email: String, password: String): AuthenticatedUser {
        signInCount++
        lastCredentials = email to password
        signInError?.let { throw it }
        return AuthenticatedUser(uid = user?.uid ?: DEFAULT_UID, email = email)
    }

    override suspend fun register(email: String, password: String): AuthenticatedUser {
        registerCount++
        lastCredentials = email to password
        registerError?.let { throw it }
        return AuthenticatedUser(uid = user?.uid ?: DEFAULT_UID, email = email)
    }

    override fun signOut() {
        signOutCount++
        user = null
    }

    private companion object {
        const val DEFAULT_UID = "uid-1"
    }
}

class FakeUserRepository(
    var profile: UserProfile? = null
) : UserRepository {

    var getUserError: Exception? = null
    var saveUserError: Exception? = null

    val savedUsers = mutableListOf<UserProfile>()
    val requestedUserIds = mutableListOf<String>()

    override suspend fun saveUser(user: UserProfile) {
        saveUserError?.let { throw it }
        savedUsers += user
    }

    override suspend fun getUser(userId: String): UserProfile? {
        requestedUserIds += userId
        getUserError?.let { throw it }
        return profile
    }
}

class FakeNoteRepository : NoteRepository {

    /** [hasUploadedNote] ne döndürsün — feed'in katkı kapısını bu belirliyor. */
    var hasUploadedNote = false
    var note: Post? = null
    var noteFile: String? = null

    var departmentNotes: Flow<List<Post>> = emptyFlow()
    var uploaderNotes: Flow<List<Post>> = emptyFlow()
    var leaderboard: Flow<List<LeaderboardEntry>> = emptyFlow()

    var createNoteError: Exception? = null
    var getNoteError: Exception? = null
    var getNoteFileError: Exception? = null
    var hasUploadedNoteError: Exception? = null
    var deleteNoteError: Exception? = null
    var updateNoteError: Exception? = null

    val createdNotes = mutableListOf<CreatedNote>()
    val deletedNoteIds = mutableListOf<String>()
    val appliedUpdates = mutableListOf<AppliedUpdate>()
    var observedDepartment: String? = null
        private set
    var observedLeaderboardDepartment: String? = null
        private set
    var observedUploader: Pair<String, String>? = null
        private set

    override suspend fun createNote(
        draft: NoteDraft,
        uploaderUid: String,
        uploaderEmail: String,
        department: String
    ) {
        createNoteError?.let { throw it }
        createdNotes += CreatedNote(draft, uploaderUid, uploaderEmail, department)
    }

    override suspend fun getNoteFile(noteId: String): String? {
        getNoteFileError?.let { throw it }
        return noteFile
    }

    /** Son istenen pencere; sayfalama testleri limitin büyüdüğünü buradan görüyor. */
    var requestedLimits = mutableListOf<Long>()
        private set

    override fun observeNotesByDepartment(department: String, limit: Long): Flow<List<Post>> {
        observedDepartment = department
        requestedLimits += limit
        return departmentNotes
    }

    override fun observeNotesByUploader(
        uploaderUid: String,
        defaultUploaderEmail: String
    ): Flow<List<Post>> {
        observedUploader = uploaderUid to defaultUploaderEmail
        return uploaderNotes
    }

    override fun observeLeaderboard(department: String): Flow<List<LeaderboardEntry>> {
        observedLeaderboardDepartment = department
        return leaderboard
    }

    override suspend fun deleteNote(noteId: String) {
        deleteNoteError?.let { throw it }
        deletedNoteIds += noteId
    }

    override suspend fun hasUploadedNote(uploaderUid: String): Boolean {
        hasUploadedNoteError?.let { throw it }
        return hasUploadedNote
    }

    override suspend fun getNote(noteId: String): Post? {
        getNoteError?.let { throw it }
        return note
    }

    override suspend fun updateNote(noteId: String, uploaderUid: String, update: NoteUpdate) {
        updateNoteError?.let { throw it }
        appliedUpdates += AppliedUpdate(noteId, uploaderUid, update)
    }

    data class CreatedNote(
        val draft: NoteDraft,
        val uploaderUid: String,
        val uploaderEmail: String,
        val department: String
    )

    data class AppliedUpdate(
        val noteId: String,
        val uploaderUid: String,
        val update: NoteUpdate
    )
}

class FakeRatingRepository : RatingRepository {

    var result = RatingResult(count = 2L, sum = 8L, updatedExistingRating = false)
    var error: Exception? = null

    val submissions = mutableListOf<Submission>()

    override suspend fun submitRating(
        noteId: String,
        raterUid: String,
        newRating: Int
    ): RatingResult {
        submissions += Submission(noteId, raterUid, newRating)
        error?.let { throw it }
        return result
    }

    data class Submission(
        val noteId: String,
        val raterUid: String,
        val rating: Int
    )
}
