package duygu.yilmaz.campusnote.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.databinding.ActivityMainBinding
import duygu.yilmaz.campusnote.ui.auth.CompleteProfileActivity
import duygu.yilmaz.campusnote.ui.auth.LoginActivity
import duygu.yilmaz.campusnote.ui.feed.FeedFragment
import duygu.yilmaz.campusnote.ui.leaderboard.LeaderboardFragment
import duygu.yilmaz.campusnote.ui.profile.ProfileFragment
import duygu.yilmaz.campusnote.ui.upload.UploadFragment

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

        // Oturum var ama profili olmayabilir; bkz. MainViewModel.resolveRoute.
        viewModel.route.observe(this) { route ->
            if (route == MainRoute.NeedsProfile) {
                startActivity(Intent(this, CompleteProfileActivity::class.java))
                finish()
            }
        }
        viewModel.resolveRoute()

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
