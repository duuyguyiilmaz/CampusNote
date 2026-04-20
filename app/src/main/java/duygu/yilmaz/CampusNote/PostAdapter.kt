package duygu.yilmaz.CampusNote

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(
    private val items: MutableList<Post>,
    private val onItemClick: ((Post) -> Unit)? = null,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onDeleteClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostVH>() {

    class PostVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvDept: TextView = itemView.findViewById(R.id.tvDept)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvRatingCount: TextView = itemView.findViewById(R.id.tvRatingCount)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostVH(view)
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = items[position]

        holder.tvTitle.text = post.title
        holder.tvDesc.text = post.desc
        holder.tvEmail.text = post.authorEmail.substringBefore("@")
        holder.tvDept.text = post.department.take(3).uppercase()
        holder.tvRating.text = String.format("%.1f", post.avgRating)
        holder.tvRatingCount.text = "${post.ratingCount} oy"

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(post)
        }

        if (onEditClick != null) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener {
                onEditClick.invoke(post)
            }
        } else {
            holder.btnEdit.visibility = View.GONE
        }

        if (onDeleteClick != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Notu Sil")
                    .setMessage("\"${post.title}\" notunu silmek istediğine emin misin?")
                    .setPositiveButton("Evet, Sil") { _, _ ->
                        onDeleteClick.invoke(post)
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun refresh(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}