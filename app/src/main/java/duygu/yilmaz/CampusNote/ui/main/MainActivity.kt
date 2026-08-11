package duygu.yilmaz.CampusNote.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.databinding.ActivityMainBinding
import duygu.yilmaz.CampusNote.ui.auth.LoginActivity
import duygu.yilmaz.CampusNote.ui.feed.FeedFragment
import duygu.yilmaz.CampusNote.ui.leaderboard.LeaderboardFragment
import duygu.yilmaz.CampusNote.ui.profile.ProfileFragment
import duygu.yilmaz.CampusNote.ui.upload.UploadFragment

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.hasAuthenticatedUser()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FeedFragment())
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_feed -> FeedFragment()
                R.id.nav_upload -> UploadFragment()
                R.id.nav_leaderboard -> LeaderboardFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> FeedFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, selectedFragment)
                .commit()

            true
        }
    }

    /** Feed'deki "not yükle" kısayolunun alt menüyü değiştirebilmesi için. */
    fun selectUploadTab() {
        binding.bottomNav.selectedItemId = R.id.nav_upload
    }
}
