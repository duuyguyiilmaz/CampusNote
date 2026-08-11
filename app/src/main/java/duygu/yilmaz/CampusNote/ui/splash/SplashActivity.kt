package duygu.yilmaz.CampusNote.ui.splash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.databinding.ActivitySplashBinding
import duygu.yilmaz.CampusNote.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLogo.setImageResource(R.drawable.campusnote__11)

        binding.tvWelcome.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(300)
            .setDuration(600)
            .start()

        binding.ivLogo.scaleX = 0.5f
        binding.ivLogo.scaleY = 0.5f
        binding.ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(700)
            .setDuration(800)
            .start()

        binding.tvSlogan.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(1600)
            .setDuration(600)
            .start()

        binding.progressBar.animate()
            .alpha(1f)
            .setStartDelay(2000)
            .setDuration(400)
            .start()

        binding.btnNext.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(2500)
            .setDuration(600)
            .withEndAction {
                binding.progressBar.animate().alpha(0f).setDuration(300).start()
            }
            .start()

        binding.btnNext.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}
