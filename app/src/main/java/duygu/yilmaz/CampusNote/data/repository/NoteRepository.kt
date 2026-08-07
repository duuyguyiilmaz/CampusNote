package duygu.yilmaz.CampusNote.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import duygu.yilmaz.CampusNote.data.model.LeaderboardEntry
import duygu.yilmaz.CampusNote.data.model.NoteDraft
import duygu.yilmaz.CampusNote.data.model.NoteUpdate
import duygu.yilmaz.CampusNote.data.model.Post

class NoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun createNote(
        draft: NoteDraft,
        uploaderUid: String,
        uploaderEmail: String,
        department: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteReference = firestore.collection(NOTES_COLLECTION).document()
        val userReference = firestore.collection(USERS_COLLECTION).document(uploaderUid)
        val noteData = hashMapOf(
            "course" to draft.course,
            "title" to draft.title,
            "description" to draft.description,
            "tag" to draft.tag,
            "department" to department,
            "uploaderUid" to uploaderUid,
            "uploaderEmail" to uploaderEmail,
            "createdAt" to FieldValue.serverTimestamp(),
            "ratingSum" to 0L,
            "ratingCount" to 0L,
            "avgRating" to 0.0,
            "fileName" to draft.fileName,
            "fileType" to draft.fileType,
            "fileData" to draft.fileData
        )

        firestore.runBatch { batch ->
            batch.set(noteReference, noteData)
            batch.update(userReference, HAS_UPLOADED_NOTE_FIELD, true)
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun observeNotesByDepartment(
        department: String,
        onNotesChanged: (List<Post>) -> Unit,
        onFailure: (Exception) -> Unit
    ): () -> Unit {
        val listener = firestore.collection(NOTES_COLLECTION)
            .whereEqualTo(DEPARTMENT_FIELD, department)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    ?.mapNotNull { document -> toPost(document) }
                    ?.sortedByDescending { it.timeMills }
                    .orEmpty()

                onNotesChanged(posts)
            }

        return { listener.remove() }
    }

    fun observeNotesByUploader(
        uploaderUid: String,
        defaultUploaderEmail: String,
        onNotesChanged: (List<Post>) -> Unit,
        onFailure: (Exception) -> Unit
    ): () -> Unit {
        val listener = firestore.collection(NOTES_COLLECTION)
            .whereEqualTo(UPLOADER_UID_FIELD, uploaderUid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    ?.mapNotNull { document ->
                        toPost(document, defaultUploaderEmail)
                    }
                    ?.sortedByDescending { it.timeMills }
                    .orEmpty()

                onNotesChanged(posts)
            }

        return { listener.remove() }
    }

    fun deleteNote(
        noteId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection(NOTES_COLLECTION)
            .document(noteId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun getNote(
        noteId: String,
        onSuccess: (Post?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection(NOTES_COLLECTION)
            .document(noteId)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(if (document.exists()) toPost(document) else null)
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun updateNote(
        noteId: String,
        uploaderUid: String,
        update: NoteUpdate,
        onSuccess: () -> Unit,
        onNotOwner: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteReference = firestore.collection(NOTES_COLLECTION).document(noteId)
        noteReference.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onFailure(IllegalStateException("Note is missing"))
                    return@addOnSuccessListener
                }

                if (document.getString(UPLOADER_UID_FIELD) != uploaderUid) {
                    onNotOwner()
                    return@addOnSuccessListener
                }

                val updates = mapOf(
                    "course" to update.course,
                    "title" to update.title,
                    "description" to update.description,
                    "tag" to update.tag,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                noteReference.update(updates)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { exception -> onFailure(exception) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun observeLeaderboard(
        onEntriesChanged: (List<LeaderboardEntry>) -> Unit,
        onFailure: (Exception) -> Unit
    ): () -> Unit {
        val listener = firestore.collection(NOTES_COLLECTION)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents
                    ?.mapNotNull(::toLeaderboardEntry)
                    ?.sortedByDescending { it.ratingSum }
                    .orEmpty()

                onEntriesChanged(entries)
            }

        return { listener.remove() }
    }

    private fun toPost(
        document: DocumentSnapshot,
        defaultUploaderEmail: String = ""
    ): Post? {
        val title = document.getString("title") ?: return null
        val createdAt = try {
            document.getTimestamp("createdAt")?.toDate()?.time
                ?: document.getLong("createdAt")
                ?: 0L
        } catch (_: Exception) {
            document.getLong("createdAt") ?: 0L
        }

        return Post(
            id = document.id,
            title = title,
            desc = document.getString("description") ?: "",
            authorEmail = document.getString("uploaderEmail") ?: defaultUploaderEmail,
            department = document.getString("department") ?: "",
            timeMills = createdAt,
            uploaderUid = document.getString("uploaderUid") ?: "",
            avgRating = document.getDouble("avgRating")
                ?: document.getLong("avgRating")?.toDouble()
                ?: 0.0,
            ratingCount = document.getLong("ratingCount")
                ?: document.getDouble("ratingCount")?.toLong()
                ?: 0L,
            ratingSum = document.getLong("ratingSum") ?: 0L,
            course = document.getString("course") ?: "",
            tag = document.getString("tag") ?: "",
            fileName = document.getString("fileName") ?: "",
            fileType = document.getString("fileType") ?: "",
            fileData = document.getString("fileData") ?: ""
        )
    }

    private fun toLeaderboardEntry(document: DocumentSnapshot): LeaderboardEntry? {
        val title = document.getString("title") ?: return null

        return LeaderboardEntry(
            docId = document.id,
            title = title,
            uploaderEmail = document.getString("uploaderEmail") ?: "",
            department = document.getString("department") ?: "",
            ratingCount = document.getLong("ratingCount") ?: 0L,
            ratingSum = document.getLong("ratingSum") ?: 0L
        )
    }

    private companion object {
        const val NOTES_COLLECTION = "notes"
        const val USERS_COLLECTION = "users"
        const val DEPARTMENT_FIELD = "department"
        const val UPLOADER_UID_FIELD = "uploaderUid"
        const val HAS_UPLOADED_NOTE_FIELD = "hasUploadedNote"
    }
}
