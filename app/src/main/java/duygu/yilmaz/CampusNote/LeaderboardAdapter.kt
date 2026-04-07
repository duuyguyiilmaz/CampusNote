package duygu.yilmaz.CampusNote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private val items: MutableList<LeaderboardEntry>
) : RecyclerView.Adapter<LeaderboardAdapter.LBViewHolder>() {

    class LBViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank        : TextView = itemView.findViewById(R.id.tvRank)
        val tvTitle       : TextView = itemView.findViewById(R.id.tvTitle)
        val tvEmail       : TextView = itemView.findViewById(R.id.tvEmail)
        val tvDept        : TextView = itemView.findViewById(R.id.tvDept)
        val tvAvgRating   : TextView = itemView.findViewById(R.id.tvAvgRating)
        val tvRatingCount : TextView = itemView.findViewById(R.id.tvRatingCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LBViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return LBViewHolder(v)
    }

    override fun onBindViewHolder(holder: LBViewHolder, position: Int) {
        val entry = items[position]
        val rank = position + 1

        holder.tvRank.text = when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> rank.toString()
        }

        // Emoji kullanınca renk şart değil ama istersen:
        holder.tvRank.setTextColor(
            when (rank) {
                1 -> android.graphics.Color.parseColor("#FFD700")
                2 -> android.graphics.Color.parseColor("#C0C0C0")
                3 -> android.graphics.Color.parseColor("#CD7F32")
                else -> android.graphics.Color.WHITE
            }
        )

        holder.tvTitle.text = entry.title
        holder.tvEmail.text = entry.uploaderEmail
        holder.tvDept.text = entry.department
        holder.tvAvgRating.text = String.format(java.util.Locale.US, "%.1f", entry.avgRating)
        holder.tvRatingCount.text = "${entry.ratingCount} oy"
    }

    override fun getItemCount(): Int = items.size

    fun refresh(newItems: List<LeaderboardEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}