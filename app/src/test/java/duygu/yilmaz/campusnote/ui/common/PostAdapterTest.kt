package duygu.yilmaz.campusnote.ui.common

import duygu.yilmaz.campusnote.testing.post
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feed bir Firestore snapshot dinleyicisinden besleniyor, yani bir not oy aldığında
 * listenin tamamı yeniden geliyor. Bu testler DiffUtil'in o listeden doğru satırı
 * ayıklayabildiğini gösteriyor — aksi halde tek oy bütün satırları yeniden çizerdi.
 */
class PostAdapterTest {

    private val diff = PostAdapter.DIFF

    @Test
    fun `rows are matched by note id`() {
        assertTrue(diff.areItemsTheSame(post(id = "a"), post(id = "a", title = "Yeni ad")))
        assertFalse(diff.areItemsTheSame(post(id = "a"), post(id = "b")))
    }

    @Test
    fun `a new vote on a note marks only that row as changed`() {
        val before = post(id = "a", ratingSum = 10L)
        val after = post(id = "a", ratingSum = 14L)

        assertTrue(diff.areItemsTheSame(before, after))
        assertFalse(diff.areContentsTheSame(before, after))
    }

    @Test
    fun `an unchanged note is left alone`() {
        assertTrue(diff.areContentsTheSame(post(id = "a"), post(id = "a")))
    }
}
