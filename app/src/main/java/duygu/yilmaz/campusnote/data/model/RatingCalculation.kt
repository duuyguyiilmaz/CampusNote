package duygu.yilmaz.campusnote.data.model

/** Bir oy sonrası notun güncellenmiş puan toplamları. */
data class RatingTotals(
    val sum: Long,
    val count: Long,
    val average: Double,
    /** Kullanıcı ilk kez mi oy verdi, yoksa eski oyunu mu değiştirdi. */
    val updatedExistingRating: Boolean
)

/**
 * Puanlama aritmetiği — Firestore'dan bağımsız, saf fonksiyon.
 *
 * Bu hesap eskiden [duygu.yilmaz.campusnote.data.repository.RatingRepository] içindeki
 * `runTransaction` bloğunun ortasında duruyordu ve o hâliyle ancak Firestore taklit
 * edilerek test edilebilirdi. Ayrılınca hem transaction'ın işi sadeleşti hem de
 * mantık doğrudan birim testle doğrulanabilir hâle geldi.
 */
object RatingCalculator {

    const val MIN_RATING = 1
    const val MAX_RATING = 5

    /**
     * @param currentSum notun şu anki oy toplamı
     * @param currentCount notun şu anki oy sayısı
     * @param previousRating kullanıcının daha önce verdiği oy; ilk oyda null
     * @param newRating verilen yeni oy, [MIN_RATING]..[MAX_RATING] aralığında
     *
     * @throws IllegalArgumentException oy aralık dışındaysa
     */
    fun recalculate(
        currentSum: Long,
        currentCount: Long,
        previousRating: Long?,
        newRating: Int
    ): RatingTotals {
        require(newRating in MIN_RATING..MAX_RATING) {
            "Rating must be between $MIN_RATING and $MAX_RATING"
        }

        val updatedExistingRating = previousRating != null
        val delta = newRating.toLong() - (previousRating ?: 0L)

        // Oy değiştiriliyorsa sayı artmaz. Sayının 0 göründüğü tutarsız kayıtlarda
        // en az 1'e çekiyoruz, aksi halde ortalama sıfıra bölmeye düşerdi.
        val count = if (updatedExistingRating) {
            currentCount.coerceAtLeast(1L)
        } else {
            currentCount + 1L
        }

        // Toplam hiçbir koşulda negatife düşmemeli; eski veriler bozuksa taban 0.
        val sum = (currentSum + delta).coerceAtLeast(0L)
        val average = if (count == 0L) 0.0 else sum.toDouble() / count

        return RatingTotals(
            sum = sum,
            count = count,
            average = average,
            updatedExistingRating = updatedExistingRating
        )
    }
}
