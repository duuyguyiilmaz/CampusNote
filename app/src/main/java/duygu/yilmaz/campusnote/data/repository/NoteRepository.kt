package duygu.yilmaz.campusnote.data.repository

import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.data.model.NoteDraft
import duygu.yilmaz.campusnote.data.model.NoteUpdate
import duygu.yilmaz.campusnote.data.model.Post
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    suspend fun createNote(
        draft: NoteDraft,
        uploaderUid: String,
        uploaderName: String,
        department: String
    )

    /** Notun dosya içeriğini base64 olarak döndürür; dosya yoksa null. */
    suspend fun getNoteFile(noteId: String): String?

    /**
     * @param limit en fazla kaç not dinlensin. Sayfalama tek bir dinleyicinin
     *   limitini büyüterek yapılıyor: ikinci bir sorgu açıp sonuçları birleştirmek
     *   yerine aynı dinleyici daha geniş bir pencereyle yeniden kuruluyor, böylece
     *   feed canlı kalıyor ve sıra tek bir yerden geliyor.
     */
    fun observeNotesByDepartment(department: String, limit: Long): Flow<List<Post>>

    fun observeNotesByUploader(
        uploaderUid: String,
        defaultUploaderName: String
    ): Flow<List<Post>>

    /**
     * @param department sıralama feed ile aynı bölümle sınırlı; kullanıcı başka
     *   bölümlerin notlarıyla yarışmıyor.
     */
    fun observeLeaderboard(department: String): Flow<List<LeaderboardEntry>>

    suspend fun deleteNote(noteId: String)

    /**
     * Kullanıcının en az bir notu var mı — feed'in katkı kapısı bunu kullanır.
     *
     * Bu bilgi eskiden `users/{uid}.hasUploadedNote` alanında tutuluyordu, ama alan
     * yükleme sırasında `true` yapılıp not silinince geri alınmıyordu. Bir kez not
     * yükleyip silen kullanıcı feed'e kalıcı erişim kazanıyordu. Doğrudan notlara
     * bakmak tek bir doğruluk kaynağı bırakıyor, o yüzden tutarsızlık oluşamaz.
     */
    suspend fun hasUploadedNote(uploaderUid: String): Boolean

    suspend fun getNote(noteId: String): Post?

    /**
     * @throws NoteNotFoundException not silinmişse
     * @throws NoteNotOwnedException notu yükleyen kişi değilsen
     */
    suspend fun updateNote(
        noteId: String,
        uploaderUid: String,
        update: NoteUpdate
    )
}
