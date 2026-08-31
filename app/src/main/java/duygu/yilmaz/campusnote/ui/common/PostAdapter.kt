package duygu.yilmaz.campusnote.ui.common

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.data.model.Post
import duygu.yilmaz.campusnote.databinding.ItemPostBinding

/**
 * Feed ve profildeki not listesi.
 *
 * Liste bir Firestore snapshot dinleyicisinden besleniyor: herhangi bir nota oy
 * verildiğinde listenin tamamı yeniden geliyor. Adapter eskiden bunu
 * `notifyDataSetChanged()` ile karşılıyordu, yani "neyin değiştiğini bilmiyorum"
 * diyordu; RecyclerView de ekrandaki her satırı yeniden bağlıyor ve hiçbir şeyi
 * canlandıramıyordu. [DiffUtil] farkı kendisi bulduğu için artık yalnızca gerçekten
 * değişen satır güncelleniyor.
 */
class PostAdapter(
    private val onItemClick: ((Post) -> Unit)? = null,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onDeleteClick: ((Post) -> Unit)? = null
) : ListAdapter<Post, PostAdapter.PostVH>(DIFF) {

    class PostVH(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostVH(binding)
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = getItem(position)

        with(holder.binding) {
            tvTitle.text = post.title
            tvDesc.text = post.desc
            tvUploader.text = post.uploaderName
            tvDept.text = post.department.take(3).uppercase()
            tvRating.text = String.format("%.1f", post.avgRating)
            tvRatingCount.text = root.context.getString(R.string.vote_count, post.ratingCount)

            root.setOnClickListener {
                onItemClick?.invoke(post)
            }

            if (onEditClick != null) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener { onEditClick.invoke(post) }
            } else {
                btnEdit.visibility = View.GONE
            }

            if (onDeleteClick != null) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    AlertDialog.Builder(root.context)
                        .setTitle(R.string.delete_note_title)
                        .setMessage(
                            root.context.getString(R.string.delete_note_message, post.title)
                        )
                        .setPositiveButton(R.string.delete_note_confirm) { _, _ ->
                            onDeleteClick.invoke(post)
                        }
                        .setNegativeButton(R.string.action_cancel, null)
                        .show()
                }
            } else {
                btnDelete.visibility = View.GONE
            }
        }
    }

    fun refresh(newItems: List<Post>) = submitList(newItems)

    internal companion object {
        val DIFF = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post) =
                oldItem.id == newItem.id

            // Post bir data class; `==` bütün alanları karşılaştırıyor, dolayısıyla
            // nota yeni bir oy geldiğinde avgRating farkı buradan yakalanıyor.
            override fun areContentsTheSame(oldItem: Post, newItem: Post) =
                oldItem == newItem
        }
    }
}
