package duygu.yilmaz.campusnote.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [RatingCalculator] birim testleri.
 *
 * Bu hesap uygulamanın en kritik yeri: hem notun ortalama puanını hem de not
 * sahibinin liderlik tablosundaki puanını belirliyor. Yanlış çalışırsa kimse
 * fark etmez, sadece sıralama sessizce bozulur — o yüzden test edilmesi gereken
 * ilk şey burası.
 */
class RatingCalculatorTest {

    @Test
    fun `ilk oy hic puanlanmamis nota verildiginde toplam ve sayi bastan olusur`() {
        val totals = RatingCalculator.recalculate(
            currentSum = 0L,
            currentCount = 0L,
            previousRating = null,
            newRating = 4
        )

        assertEquals(4L, totals.sum)
        assertEquals(1L, totals.count)
        assertFalse(totals.updatedExistingRating)
    }

    @Test
    fun `yeni oy mevcut toplamin uzerine eklenir ve sayi bir artar`() {
        val totals = RatingCalculator.recalculate(
            currentSum = 9L,
            currentCount = 2L,
            previousRating = null,
            newRating = 3
        )

        assertEquals(12L, totals.sum)
        assertEquals(3L, totals.count)
    }

    @Test
    fun `mevcut oy guncellenirken oy sayisi artmaz`() {
        val totals = RatingCalculator.recalculate(
            currentSum = 9L,
            currentCount = 2L,
            previousRating = 4L,
            newRating = 5
        )

        assertEquals(10L, totals.sum)
        assertEquals(2L, totals.count)
        assertTrue(totals.updatedExistingRating)
    }

    @Test
    fun `oy dusurulunce toplam azalir ve puan farki negatif olur`() {
        val totals = RatingCalculator.recalculate(
            currentSum = 10L,
            currentCount = 2L,
            previousRating = 5L,
            newRating = 1
        )

        assertEquals(6L, totals.sum)
        assertEquals(2L, totals.count)
    }

    @Test
    fun `ayni oy tekrar verilirse hicbir sey degismez`() {
        val totals = RatingCalculator.recalculate(
            currentSum = 7L,
            currentCount = 2L,
            previousRating = 3L,
            newRating = 3
        )

        assertEquals(7L, totals.sum)
        assertEquals(2L, totals.count)
    }

    @Test
    fun `bozuk kayitta toplam negatife dusmez`() {
        // Firestore'daki toplam gerçekte olması gerekenden küçük kalmışsa
        // (ör. elle silinmiş bir oy) fark toplamı eksiye sürükleyebilir.
        val totals = RatingCalculator.recalculate(
            currentSum = 1L,
            currentCount = 1L,
            previousRating = 5L,
            newRating = 1
        )

        assertEquals(0L, totals.sum)
    }

    @Test
    fun `oy sayisi sifir gorunen kayitta guncelleme sifira bolmeye dusmez`() {
        val totals = RatingCalculator.recalculate(
            currentSum = 5L,
            currentCount = 0L,
            previousRating = 2L,
            newRating = 5
        )

        assertEquals(1L, totals.count)
        assertEquals(8L, totals.sum)
    }

    @Test
    fun `sinir degerler kabul edilir`() {
        val lowest = RatingCalculator.recalculate(0L, 0L, null, RatingCalculator.MIN_RATING)
        val highest = RatingCalculator.recalculate(0L, 0L, null, RatingCalculator.MAX_RATING)

        assertEquals(1L, lowest.sum)
        assertEquals(5L, highest.sum)
    }

    @Test
    fun `aralik disindaki oy reddedilir`() {
        assertThrows(IllegalArgumentException::class.java) {
            RatingCalculator.recalculate(0L, 0L, null, newRating = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RatingCalculator.recalculate(0L, 0L, null, newRating = 6)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RatingCalculator.recalculate(0L, 0L, null, newRating = -3)
        }
    }
}
