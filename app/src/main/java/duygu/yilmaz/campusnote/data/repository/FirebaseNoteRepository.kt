package duygu.yilmaz.campusnote.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.data.model.NoteDraft
import duygu.yilmaz.campusnote.data.model.NoteUpdate
import duygu.yilmaz.campusnote.data.model.Post
import duygu.yilmaz.campusnote.data.model.uploaderName
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NoteRepository {

    /**
     * Notu iki dokümana yazar: metadata ana dokümana, dosya içeriği ise
     * `notes/{id}/content/file` altına. Böylece feed'i dinleyen sorgular
     * base64 verisini hiç indirmez.
     */
    override suspend fun createNote(
        draft: NoteDraft,
        uploaderUid: String,
        uploaderName: String,
        department: String
    ) {
        val noteReference = firestore.collection(NOTES_COLLECTION).document()
        val noteData = hashMapOf(
            "course" to draft.course,
            "title" to draft.title,
            "description" to draft.description,
            "tag" to draft.tag,
            "department" to department,
            "uploaderUid" to uploaderUid,
            "uploaderName" to uploaderName,
            "createdAt" to FieldValue.serverTimestamp(),
            "ratingSum" to 0L,
            "ratingCount" to 0L,
            "fileName" to draft.fileName,
            "fileType" to draft.fileType,
            "fileSize" to draft.fileSize
        )

        firestore.runBatch { batch ->
            batch.set(noteReference, noteData)

            if (draft.fileData.isNotEmpty()) {
                batch.set(
                    noteReference.fileDocument(),
                    mapOf(FILE_DATA_FIELD to draft.fileData)
                )
            }
        }.await()
    }

    /**
     * Eski notlarda içerik hâlâ ana dokümanın `fileData` alanında olabilir,
     * o yüzden içerik dokümanı bulunamazsa oraya düşülür.
     */
    override suspend fun getNoteFile(noteId: String): String? {
        val noteReference = firestore.collection(NOTES_COLLECTION).document(noteId)

        val content = noteReference.fileDocument().get().await()
        content.getString(FILE_DATA_FIELD)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        val legacy = noteReference.get().await().getString(FILE_DATA_FIELD)
        return legacy?.takeIf { it.isNotEmpty() }
    }

    /**
     * Sıralama artık istemcide değil sorguda: `limit` ancak bir sıra tanımlıysa
     * anlamlı, aksi halde Firestore rastgele bir alt küme döndürür.
     *
     * Bu sorgu `department` + `createdAt` bileşik index'ini gerektiriyor
     * (`firestore.indexes.json`). Index yayına alınmadan sorgu FAILED_PRECONDITION
     * ile düşer — feed hiç açılmaz.
     */
    override fun observeNotesByDepartment(department: String, limit: Long): Flow<List<Post>> =
        observePosts(
            query = firestore.collection(NOTES_COLLECTION)
                .whereEqualTo(DEPARTMENT_FIELD, department)
                .orderBy(CREATED_AT_FIELD, Query.Direction.DESCENDING)
                .limit(limit)
        )

    override fun observeNotesByUploader(
        uploaderUid: String,
        defaultUploaderName: String
    ): Flow<List<Post>> =
        observePosts(
            query = firestore.collection(NOTES_COLLECTION)
                .whereEqualTo(UPLOADER_UID_FIELD, uploaderUid),
            defaultUploaderName = defaultUploaderName
        )

    /**
     * Sıralama ve kesme Firestore'da yapılıyor, istemcide değil: `limit` ancak bir
     * sıra tanımlıyken anlamlı, aksi halde rastgele bir alt küme döner.
     *
     * Feed'in aksine burada sayfalama yok — [LEADERBOARD_SIZE] satır sabit. Liderlik
     * tablosu doğası gereği "ilk N" görünümü; 300. sıraya kadar kaydırmak kimsenin
     * ihtiyacı değil. Bu bir erişim kısıtlaması da değil: notların tamamına feed'den
     * ulaşılıyor, burası içeriğe giden yol değil bir sıralama görünümü.
     *
     * `department` + `ratingSum` bileşik index'ini gerektiriyor
     * (`firestore.indexes.json`); index olmadan sorgu FAILED_PRECONDITION ile düşer.
     */
    override fun observeLeaderboard(department: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val listener = firestore.collection(NOTES_COLLECTION)
            .whereEqualTo(DEPARTMENT_FIELD, department)
            .orderBy(RATING_SUM_FIELD, Query.Direction.DESCENDING)
            .limit(LEADERBOARD_SIZE)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents
                    ?.mapNotNull(::toLeaderboardEntry)
                    .orEmpty()

                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    /** Notu ve içerik dokümanını birlikte siler — Firestore alt koleksiyonları kendiliğinden silmez. */
    override suspend fun deleteNote(noteId: String) {
        val noteReference = firestore.collection(NOTES_COLLECTION).document(noteId)

        firestore.runBatch { batch ->
            batch.delete(noteReference.fileDocument())
            batch.delete(noteReference)
        }.await()
    }

    /**
     * `limit(1)` sayesinde sorgu kullanıcının kaç notu olduğundan bağımsız olarak sabit
     * maliyetli: Firestore yalnızca tek doküman okur.
     */
    override suspend fun hasUploadedNote(uploaderUid: String): Boolean =
        !firestore.collection(NOTES_COLLECTION)
            .whereEqualTo(UPLOADER_UID_FIELD, uploaderUid)
            .limit(1)
            .get()
            .await()
            .isEmpty

    override suspend fun getNote(noteId: String): Post? {
        val document = firestore.collection(NOTES_COLLECTION)
            .document(noteId)
            .get()
            .await()

        return if (document.exists()) toPost(document) else null
    }

    override suspend fun updateNote(
        noteId: String,
        uploaderUid: String,
        update: NoteUpdate
    ) {
        val noteReference = firestore.collection(NOTES_COLLECTION).document(noteId)
        val document = noteReference.get().await()

        if (!document.exists()) throw NoteNotFoundException()
        if (document.getString(UPLOADER_UID_FIELD) != uploaderUid) throw NoteNotOwnedException()

        noteReference.update(
            mapOf(
                "course" to update.course,
                "title" to update.title,
                "description" to update.description,
                "tag" to update.tag,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Firestore listener'ını akışa çevirir. Akış toplanmayı bıraktığında
     * [awaitClose] listener'ı kaldırır; ayrı bir "durdurma" çağrısına gerek kalmaz.
     */
    private fun observePosts(
        query: Query,
        defaultUploaderName: String = ""
    ): Flow<List<Post>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            val posts = snapshot?.documents
                ?.mapNotNull { document -> toPost(document, defaultUploaderName) }
                ?.sortedByDescending { it.timeMills }
                .orEmpty()

            trySend(posts)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Migration'dan önce yazılmış notlar hâlâ tam `uploaderEmail` taşıyor; okunurken
     * adı ondan türetiyoruz. Yeni notlarda alan hiç yok — bkz.
     * `firestore-tests/strip-uploader-email.js`.
     */
    private fun toPost(
        document: DocumentSnapshot,
        defaultUploaderName: String = ""
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
            uploaderName = document.displayName() ?: defaultUploaderName,
            department = document.getString("department") ?: "",
            timeMills = createdAt,
            uploaderUid = document.getString("uploaderUid") ?: "",
            // Eski notlar hâlâ bir `avgRating` alanı taşıyor; okunmuyor. Ortalama
            // artık ne saklanıyor ne de gösteriliyor — bkz. firestore.rules.
            ratingCount = document.getLong("ratingCount")
                ?: document.getDouble("ratingCount")?.toLong()
                ?: 0L,
            ratingSum = document.getLong("ratingSum") ?: 0L,
            course = document.getString("course") ?: "",
            tag = document.getString("tag") ?: "",
            fileName = document.getString("fileName") ?: "",
            fileType = document.getString("fileType") ?: "",
            fileSize = document.getLong("fileSize") ?: 0L
        )
    }

    /** Yeni şekil önce; eski notlarda ad e-postadan türetiliyor. */
    private fun DocumentSnapshot.displayName(): String? =
        getString("uploaderName")
            ?: getString("uploaderEmail")?.uploaderName()

    private fun DocumentReference.fileDocument(): DocumentReference =
        collection(CONTENT_COLLECTION).document(FILE_DOCUMENT)

    private fun toLeaderboardEntry(document: DocumentSnapshot): LeaderboardEntry? {
        val title = document.getString("title") ?: return null

        return LeaderboardEntry(
            docId = document.id,
            title = title,
            uploaderName = document.displayName() ?: "",
            department = document.getString("department") ?: "",
            ratingCount = document.getLong("ratingCount") ?: 0L,
            ratingSum = document.getLong("ratingSum") ?: 0L
        )
    }

    private companion object {
        const val NOTES_COLLECTION = "notes"
        const val CONTENT_COLLECTION = "content"
        const val FILE_DOCUMENT = "file"
        const val FILE_DATA_FIELD = "fileData"
        const val DEPARTMENT_FIELD = "department"
        const val UPLOADER_UID_FIELD = "uploaderUid"
        const val CREATED_AT_FIELD = "createdAt"
        const val RATING_SUM_FIELD = "ratingSum"

        /** Liderlik tablosunda gösterilen sıra sayısı. */
        const val LEADERBOARD_SIZE = 50L
    }
}
