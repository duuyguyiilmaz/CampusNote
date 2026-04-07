package duygu.yilmaz.CampusNote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val auth = FirebaseAuth.getInstance()

        // Oturum acik mi kontrol et
        val prefs = getSharedPreferences("campusnote_prefs", MODE_PRIVATE)
        val rememberMe = prefs.getBoolean("remember_me", false)
        if (rememberMe && auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etEmail    = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val cbRememberMe = findViewById<android.widget.CheckBox>(R.id.cbRememberMe)
        val btnLogin   = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!email.endsWith("@ogr.akdeniz.edu.tr")) {
                Toast.makeText(this, "Sadece Akdeniz ogrenci maili ile giris yapilabilir", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Sifre bos olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // Oturumu acik tut secenegini kaydet
                    prefs.edit().putBoolean("remember_me", cbRememberMe.isChecked).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Giris basarisiz: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}