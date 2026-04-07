package duygu.yilmaz.CampusNote
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    fun addPost(post: Post) {
        _posts.value = _posts.value.orEmpty() + post
    }
}