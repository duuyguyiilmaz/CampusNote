package duygu.yilmaz.CampusNote.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.ui.auth.LoginActivity
import duygu.yilmaz.CampusNote.ui.feed.FeedFragment
import duygu.yilmaz.CampusNote.ui.leaderboard.LeaderboardFragment
import duygu.yilmaz.CampusNote.ui.profile.ProfileFragment
import duygu.yilmaz.CampusNote.ui.upload.UploadFragment

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.hasAuthenticatedUser()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FeedFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
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
}
