package duygu.yilmaz.CampusNote
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvWelcome   = findViewById<TextView>(R.id.tvWelcome)
        val ivLogo      = findViewById<ImageView>(R.id.ivLogo)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnNext     = findViewById<MaterialButton>(R.id.btnNext)
        val tvSlogan    = findViewById<TextView>(R.id.tvSlogan)



        ivLogo.setImageResource(R.drawable.campusnote__11)

        tvWelcome.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(300)
            .setDuration(600)
            .start()

        ivLogo.scaleX = 0.5f
        ivLogo.scaleY = 0.5f
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(700)
            .setDuration(800)
            .start()

        tvSlogan.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(1600)
            .setDuration(600)
            .start()

        progressBar.animate()
            .alpha(1f)
            .setStartDelay(2000)
            .setDuration(400)
            .start()

        btnNext.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(2500)
            .setDuration(600)
            .withEndAction {
                progressBar.animate().alpha(0f).setDuration(300).start()
            }
            .start()

        btnNext.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
