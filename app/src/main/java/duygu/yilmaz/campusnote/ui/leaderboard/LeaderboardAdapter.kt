package duygu.yilmaz.campusnote.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.databinding.ItemLeaderboardBinding

/**
 * Bir not ve tablodaki sırası.
 *
 * Sıra eskiden [RecyclerView.Adapter.onBindViewHolder]'ın `position` parametresinden
 * okunuyordu. [DiffUtil] ile bu bozulurdu: verisi değişmeden yalnızca yer değiştiren
 * bir satır "aynı içerik" sayılıp yeniden bağlanmaz, altın/gümüş/bronz rozeti eski
 * satırında kalırdı. Sırayı öğenin bir alanı yapmak farkın kendisini görünür kılıyor —
 * bir not diğerini geçtiğinde ikisinin de `rank`'i değişir, ikisi de yeniden bağlanır.
 */
data class RankedEntry(
    val rank: Int,
    val entry: LeaderboardEntry
)

/** @receiver zaten puana göre sıralı liste; sıra numarası konumdan gelir. */
internal fun List<LeaderboardEntry>.ranked(): List<RankedEntry> =
    mapIndexed { index, entry -> RankedEntry(index + 1, entry) }

class LeaderboardAdapter :
    ListAdapter<RankedEntry, LeaderboardAdapter.LBViewHolder>(DIFF) {

    class LBViewHolder(val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LBViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LBViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LBViewHolder, position: Int) {
        val (rank, entry) = getItem(position)

        with(holder.binding) {
            val context = root.context

            tvRank.text = when (rank) {
                FIRST_PLACE -> context.getString(R.string.rank_first)
                SECOND_PLACE -> context.getString(R.string.rank_second)
                THIRD_PLACE -> context.getString(R.string.rank_third)
                else -> rank.toString()
            }

            val rankColor = when (rank) {
                FIRST_PLACE -> R.color.rank_gold
                SECOND_PLACE -> R.color.rank_silver
                THIRD_PLACE -> R.color.rank_bronze
                else -> R.color.white
            }
            tvRank.setTextColor(ContextCompat.getColor(context, rankColor))

            tvTitle.text = entry.title
            tvUploader.text = entry.uploaderName
            tvDept.text = entry.department
            tvScore.text = entry.ratingSum.toString()
            tvRatingCount.text = context.getString(R.string.vote_count, entry.ratingCount)
        }
    }

    fun refresh(newItems: List<LeaderboardEntry>) = submitList(newItems.ranked())

    internal companion object {
        const val FIRST_PLACE = 1
        const val SECOND_PLACE = 2
        const val THIRD_PLACE = 3

        val DIFF = object : DiffUtil.ItemCallback<RankedEntry>() {
            override fun areItemsTheSame(oldItem: RankedEntry, newItem: RankedEntry) =
                oldItem.entry.docId == newItem.entry.docId

            // RankedEntry ve LeaderboardEntry data class; `==` hem puanı hem sırayı
            // kapsıyor, o yüzden sırası kayan satır da yeniden bağlanıyor.
            override fun areContentsTheSame(oldItem: RankedEntry, newItem: RankedEntry) =
                oldItem == newItem
        }
    }
}
