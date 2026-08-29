package duygu.yilmaz.campusnote.data.repository

/**
 * Repository'lerin ViewModel'lere anlattığı hata durumları.
 *
 * Ayrı bir dosyada duruyorlar çünkü hem [NoteRepository] hem [RatingRepository]
 * aynı tipleri fırlatıyor ve testlerdeki sahteler de bunları kullanıyor.
 */
class NoteNotFoundException : IllegalStateException("Note is missing")

class OwnNoteRatingException : IllegalStateException("Users cannot rate their own notes")

class NoteNotOwnedException : IllegalStateException("Only the uploader can change this note")
