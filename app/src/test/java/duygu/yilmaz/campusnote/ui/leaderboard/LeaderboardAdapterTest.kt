package duygu.yilmaz.campusnote.ui.leaderboard

import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.testing.leaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sıra numarası artık `onBindViewHolder`'ın `position` parametresinden değil öğenin
 * kendisinden geliyor. Bu testler o kararın taşıdığı riski sabitliyor: DiffUtil bir
 * satırı "değişmedi" sayarsa onu yeniden bağlamaz, dolayısıyla sıra bilgisi öğenin
 * içinde olmasaydı yer değiştiren notların rozetleri eski satırlarında kalırdı.
 */
class LeaderboardAdapterTest {

    private val diff = LeaderboardAdapter.DIFF

    @Test
    fun `ranks the list from one, in the order given`() {
        val ranked = listOf(
            leaderboardEntry(docId = "a"),
            leaderboardEntry(docId = "b"),
            leaderboardEntry(docId = "c")
        ).ranked()

        assertEquals(listOf(1, 2, 3), ranked.map { it.rank })
        assertEquals(listOf("a", "b", "c"), ranked.map { it.entry.docId })
    }

    @Test
    fun `an empty leaderboard ranks to nothing`() {
        assertEquals(emptyList<RankedEntry>(), emptyList<LeaderboardEntry>().ranked())
    }

    @Test
    fun `rows are matched by document id, not by position`() {
        val first = RankedEntry(1, leaderboardEntry(docId = "a"))
        val demoted = RankedEntry(2, leaderboardEntry(docId = "a"))
        val other = RankedEntry(1, leaderboardEntry(docId = "b"))

        assertTrue(diff.areItemsTheSame(first, demoted))
        assertFalse(diff.areItemsTheSame(first, other))
    }

    @Test
    fun `a note that only changed places is still rebound`() {
        // Asıl tuzak bu. İki notun verisi aynı kalıp yalnızca sıraları değişebilir;
        // sıra öğenin bir alanı olmasaydı DiffUtil ikisini de "aynı" sayar ve
        // altın rozeti eski birincinin üzerinde kalırdı.
        val entry = leaderboardEntry(docId = "a")

        assertFalse(
            diff.areContentsTheSame(RankedEntry(1, entry), RankedEntry(2, entry))
        )
    }

    @Test
    fun `a new vote on the same note is rebound`() {
        val before = RankedEntry(1, leaderboardEntry(docId = "a", ratingSum = 10L))
        val after = RankedEntry(1, leaderboardEntry(docId = "a", ratingSum = 14L))

        assertTrue(diff.areItemsTheSame(before, after))
        assertFalse(diff.areContentsTheSame(before, after))
    }

    @Test
    fun `an unchanged row is left alone`() {
        val row = RankedEntry(1, leaderboardEntry(docId = "a"))

        assertTrue(diff.areContentsTheSame(row, row.copy()))
    }
}
