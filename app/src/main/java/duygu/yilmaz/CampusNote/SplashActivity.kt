package duygu.yilmaz.CampusNote
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
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


        Glide.with(this)
            .load(R.drawable.campusnote__11)
            .fitCenter()
            .into(ivLogo)

        tvWelcome.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(300)
            .setDuration(600)
            .start()

        // 2. Logo - 700ms sonra
        ivLogo.scaleX = 0.5f
        ivLogo.scaleY = 0.5f
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(700)
            .setDuration(800)
            .start()

        // 3. Slogan - 1600ms sonra
        tvSlogan.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setStartDelay(1600)
            .setDuration(600)
            .start()

        // 4. Loading - 2000ms sonra
        progressBar.animate()
            .alpha(1f)
            .setStartDelay(2000)
            .setDuration(400)
            .start()

        // 5. Başla butonu - 2500ms sonra
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
            // OnboardingActivity'nin var olduğunu varsayıyoruz.
            // Eğer ismi farklıysa burayı düzeltmelisiniz.
            try {
                val intent = Intent(this, Class.forName("duygu.yilmaz.simpleexample.OnboardingActivity"))
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            } catch (e: ClassNotFoundException) {
                // Eğer OnboardingActivity yoksa MainActivity'ye git
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
