package duygu.yilmaz.campusnote.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.data.model.LeaderboardEntry
import duygu.yilmaz.campusnote.databinding.ItemLeaderboardBinding

class LeaderboardAdapter(
    private val items: MutableList<LeaderboardEntry>
) : RecyclerView.Adapter<LeaderboardAdapter.LBViewHolder>() {

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
        val entry = items[position]
        val rank = position + 1

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
            tvEmail.text = entry.uploaderEmail
            tvDept.text = entry.department
            tvScore.text = entry.ratingSum.toString()
            tvRatingCount.text = context.getString(R.string.vote_count, entry.ratingCount)
        }
    }

    override fun getItemCount(): Int = items.size

    fun refresh(newItems: List<LeaderboardEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private companion object {
        const val FIRST_PLACE = 1
        const val SECOND_PLACE = 2
        const val THIRD_PLACE = 3
    }
}
