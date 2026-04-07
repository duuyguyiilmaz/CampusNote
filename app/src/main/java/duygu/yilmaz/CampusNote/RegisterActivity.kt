package duygu.yilmaz.CampusNote

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val auth = FirebaseAuth.getInstance()
        val db   = FirebaseFirestore.getInstance()

        val etEmail      = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword   = findViewById<TextInputEditText>(R.id.etPassword)
        val spDepartment = findViewById<Spinner>(R.id.spDepartment)
        val btnRegister  = findViewById<MaterialButton>(R.id.btnRegister)
        val btnBack      = findViewById<MaterialButton>(R.id.btnBack)

        val departments = listOf(
            "Bölüm Seç",
            "Bilgisayar Mühendisliği",
            "Elektrik-Elektronik Mühendisliği",
            "Endüstri Mühendisliği",
            "İnşaat Mühendisliği",
            "Makine Muhendisligi"
        )

        spDepartment.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            departments
        )

        btnRegister.setOnClickListener {
            val email      = etEmail.text.toString().trim()
            val password   = etPassword.text.toString().trim()
            val department = spDepartment.selectedItem.toString()

            if (!email.endsWith("@ogr.akdeniz.edu.tr")) {
                Toast.makeText(this, "Sadece Akdeniz öğrenci maili ile kayıt olunabilir", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Şifre boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (department == "Bölüm Seç") {
                Toast.makeText(this, "Lütfen bölüm seç", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    val userMap = hashMapOf(
                        "email"      to email,
                        "department" to department,
                        "createdAt"  to System.currentTimeMillis()
                    )

                    db.collection("users").document(uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Profil kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Kayıt başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}