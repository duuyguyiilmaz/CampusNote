package duygu.yilmaz.CampusNote

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostAdapter(
    private val items: MutableList<Post>,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onDeleteClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostVH>() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    class PostVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle       : TextView = itemView.findViewById(R.id.tvTitle)
        val tvDesc        : TextView = itemView.findViewById(R.id.tvDesc)
        val tvEmail       : TextView = itemView.findViewById(R.id.tvEmail)
        val tvDept        : TextView = itemView.findViewById(R.id.tvDept)
        val tvRating      : TextView = itemView.findViewById(R.id.tvRating)
        val tvRatingCount : TextView = itemView.findViewById(R.id.tvRatingCount)
        val btnEdit       : Button   = itemView.findViewById(R.id.btnEdit)
        val btnDelete     : Button   = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostVH(v)
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = items[position]

        holder.tvTitle.text       = post.title
        holder.tvDesc.text        = post.department
        holder.tvEmail.text       = post.authorEmail.substringBefore("@")
        holder.tvDept.text        = post.department.take(3).uppercase()
        holder.tvRating.text      = String.format("%.1f", post.avgRating)
        holder.tvRatingCount.text = "${post.ratingCount} oy"

        // Karta tıklayınca puan ver
        holder.itemView.setOnClickListener {
            showRatingDialog(post, holder)
        }

        // Düzenle butonu
        if (onEditClick != null) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEditClick.invoke(post) }
        } else {
            holder.btnEdit.visibility = View.GONE
        }

        // Sil butonu
        if (onDeleteClick != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Notu Sil")
                    .setMessage("\"${post.title}\" notunu silmek istediğine emin misin?")
                    .setPositiveButton("Evet, Sil") { _, _ -> onDeleteClick.invoke(post) }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    private fun showRatingDialog(post: Post, holder: PostVH) {
        auth.currentUser?.uid ?: return

        if (post.uploaderUid == auth.currentUser?.uid) {
            Toast.makeText(holder.itemView.context,
                "Kendi notuna puan veremezsin!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val ratings = arrayOf("1", "2", "3", "4", "5")

        AlertDialog.Builder(holder.itemView.context)
            .setTitle("Puan Ver")
            .setItems(ratings) { _, which ->
                val newRating = (which + 1).toFloat()
                submitRating(post, newRating, holder)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun submitRating(post: Post, newRating: Float, holder: PostVH) {
        val uid = auth.currentUser?.uid ?: return

        val docRef    = db.collection("notes").document(post.id)
        val ratingRef = db.collection("ratings").document("${uid}_${post.id}")

        ratingRef.get().addOnSuccessListener { ratingSnap ->
            if (ratingSnap.exists()) {
                // Daha önce oy verilmiş — güncelle
                val oldRating = ratingSnap.getLong("rating")?.toFloat() ?: 0f
                val diff = newRating - oldRating

                docRef.get().addOnSuccessListener { noteSnap ->
                    val oldSum   = noteSnap.getLong("ratingSum")   ?: 0L
                    val oldCount = noteSnap.getLong("ratingCount") ?: 1L
                    val newSum   = oldSum + diff
                    val newAvg   = newSum.toDouble() / oldCount

                    docRef.update(mapOf(
                        "ratingSum" to newSum,
                        "avgRating" to newAvg
                    ))
                    ratingRef.update("rating", newRating.toLong())

                    // Not sahibine puan farkını ekle
                    val diffInt = diff.toInt()
                    if (diffInt != 0) {
                        addPointsToNoteOwner(post.uploaderUid, diffInt)
                    }

                    Toast.makeText(holder.itemView.context, "Puanın güncellendi!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // İlk kez oy veriliyor
                docRef.get().addOnSuccessListener { noteSnap ->
                    val oldSum   = noteSnap.getLong("ratingSum")   ?: 0L
                    val oldCount = noteSnap.getLong("ratingCount") ?: 0L
                    val newSum   = oldSum + newRating
                    val newCount = oldCount + 1
                    val newAvg   = newSum.toDouble() / newCount

                    docRef.update(mapOf(
                        "ratingSum"   to newSum,
                        "ratingCount" to newCount,
                        "avgRating"   to newAvg
                    ))
                    ratingRef.set(mapOf(
                        "uid"    to uid,
                        "noteId" to post.id,
                        "rating" to newRating.toLong()
                    ))

                    // Not sahibine yıldız sayısı kadar puan ekle
                    addPointsToNoteOwner(post.uploaderUid, newRating.toInt())

                    Toast.makeText(holder.itemView.context, "Puan verildi!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Not sahibinin puanına verilen yıldız sayısı kadar ekler.
     * Negatif diff gelirse puan düşer (oy güncellemesinde).
     */
    private fun addPointsToNoteOwner(ownerUid: String, pointsToAdd: Int) {
        if (ownerUid.isEmpty()) return

        val userRef = db.collection("users").document(ownerUid)
        userRef.get().addOnSuccessListener { snap ->
            val currentPoints = snap.getLong("points")?.toInt() ?: 0
            val newPoints = (currentPoints + pointsToAdd).coerceAtLeast(0)
            userRef.update("points", newPoints)
        }
    }

    override fun getItemCount(): Int = items.size

    fun refresh(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}