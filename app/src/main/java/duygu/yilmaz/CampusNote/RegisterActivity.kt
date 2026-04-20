package duygu.yilmaz.CampusNote

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var tvAppName: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var cardForm: View
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var spDepartment: Spinner
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val departments = listOf(
        "Bölüm seçiniz",
        "Alman Dili ve Edebiyatı",
        "Arkeoloji",
        "Aşçılık",
        "Bahçe Bitkileri",
        "Bahçe Tarımı",
        "Bankacılık ve Sigortacılık",
        "Beslenme ve Diyetetik",
        "Bilgisayar Mühendisliği",
        "Bilgisayar Programcılığı",
        "Bitki Koruma",
        "Biyoloji",
        "Biyomedikal Cihaz Teknolojisi",
        "Büro Yönetimi ve Yönetici Asistanlığı",
        "Coğrafi Bilgi Sistemleri",
        "Coğrafya",
        "Çağrı Merkezi Hizmetleri",
        "Çalışma Ekonomisi ve Endüstri İlişkileri",
        "Çevre Koruma ve Kontrol",
        "Çevre Mühendisliği",
        "Çim Alan Tesisi ve Yönetimi",
        "Çocuk Gelişimi",
        "Deniz ve Liman İşletmeciliği",
        "Denizcilik İşletmeleri Yönetimi",
        "Dış Ticaret",
        "Diş Hekimliği",
        "Diyaliz",
        "Doğalgaz ve Tesisatı Teknolojisi",
        "Ekonometri",
        "Ekonomi ve Finans",
        "Elektrik",
        "Elektrik-Elektronik Mühendisliği",
        "Elektronik Haberleşme Teknolojisi",
        "Elektronik Teknolojisi",
        "Eski Yunan Dili ve Edebiyatı",
        "Felsefe",
        "Fen Bilgisi Öğretmenliği",
        "Finans ve Bankacılık",
        "Fizik",
        "Fizyoterapi",
        "Fizyoterapi ve Rehabilitasyon",
        "Gastronomi ve Mutfak Sanatları",
        "Gazetecilik",
        "Geleneksel El Sanatları",
        "Gerontoloji",
        "Gıda Mühendisliği",
        "Gıda Teknolojisi",
        "Grafik Tasarımı",
        "Halkla İlişkiler ve Tanıtım",
        "Harita ve Kadastro",
        "Hemşirelik",
        "Hukuk",
        "İç Mimarlık",
        "İklimlendirme ve Soğutma Teknolojisi",
        "İkram Hizmetleri",
        "İktisat",
        "İlahiyat",
        "İlk ve Acil Yardım",
        "İlköğretim Matematik Öğretmenliği",
        "İngiliz Dili ve Edebiyatı",
        "İngilizce Öğretmenliği",
        "İnşaat Mühendisliği",
        "İnşaat Teknolojisi",
        "İşletme",
        "İşletme Yönetimi",
        "Jeoloji Mühendisliği",
        "Kimya",
        "Kontrol ve Otomasyon Teknolojisi",
        "Latin Dili ve Edebiyatı",
        "Makine",
        "Makine Mühendisliği",
        "Maliye",
        "Mantarcılık",
        "Matematik",
        "Medya ve İletişim",
        "Mekatronik",
        "Meyve ve Sebze İşleme Teknolojisi",
        "Mimari Restorasyon",
        "Mimarlık",
        "Mobilya ve Dekorasyon",
        "Moda Tasarımı",
        "Muhasebe ve Vergi Uygulamaları",
        "Nükleer Teknoloji ve Radyasyon Güvenliği",
        "Okul Öncesi Öğretmenliği",
        "Optisyenlik",
        "Organik Tarım",
        "Otomotiv Teknolojisi",
        "Özel Eğitim Öğretmenliği",
        "Özel Güvenlik ve Koruma",
        "Pastacılık ve Ekmekçilik",
        "Pazarlama",
        "Peyzaj Mimarlığı",
        "Peyzaj ve Süs Bitkileri Yetiştiriciliği",
        "Psikoloji",
        "Radyo, Televizyon ve Sinema",
        "Radyoterapi",
        "Rehberlik ve Psikolojik Danışmanlık",
        "Reklamcılık",
        "Rekreasyon Yönetimi",
        "Rus Dili ve Edebiyatı",
        "Sağlık Kurumları İşletmeciliği",
        "Sahne ve Dekor Tasarımı",
        "Sanat Tarihi",
        "Seracılık",
        "Sigortacılık",
        "Sinema ve Televizyon",
        "Sınıf Öğretmenliği",
        "Sivil Hava Ulaştırma İşletmeciliği",
        "Sivil Savunma ve İtfaiyecilik",
        "Siyaset Bilimi ve Kamu Yönetimi",
        "Sosyal Bilgiler Öğretmenliği",
        "Sosyal Hizmet",
        "Sosyal Hizmetler",
        "Sosyoloji",
        "Su Ürünleri Mühendisliği",
        "Şehir ve Bölge Planlama",
        "Tarım Ekonomisi",
        "Tarım Makineleri",
        "Tarım Makineleri ve Teknolojileri Mühendisliği",
        "Tarımsal Biyoteknoloji",
        "Tarımsal Yapılar ve Sulama",
        "Tarih",
        "Tarla Bitkileri",
        "Tekstil Teknolojisi",
        "Tıp",
        "Tıbbi Dokümantasyon ve Sekreterlik",
        "Tıbbi Görüntüleme Teknikleri",
        "Tıbbi Laboratuvar Teknikleri",
        "Tıbbi ve Aromatik Bitkiler",
        "Toprak Bilimi ve Bitki Besleme",
        "Turist Rehberliği",
        "Turizm İşletmeciliği",
        "Turizm Rehberliği",
        "Turizm ve Otel İşletmeciliği",
        "Turizm ve Seyahat Hizmetleri",
        "Türk Dili ve Edebiyatı",
        "Türkçe Öğretmenliği",
        "Uluslararası İlişkiler",
        "Uluslararası Ticaret ve Lojistik",
        "Uzay Bilimleri ve Teknolojileri",
        "Yapay Zeka ve Veri Mühendisliği",
        "Yapı Denetimi",
        "Yaşlı Bakımı",
        "Yönetim Bilişim Sistemleri",
        "Zootekni"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initViews()
        setupSpinner()
        setupClickListeners()
        animateViews()
    }

    private fun initViews() {
        tvAppName = findViewById(R.id.tvAppName)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        cardForm = findViewById(R.id.cardForm)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spDepartment = findViewById(R.id.spDepartment)
        btnRegister = findViewById(R.id.btnRegister)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            departments
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spDepartment.adapter = adapter
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString()?.trim() ?: ""
            val departmentIndex = spDepartment.selectedItemPosition

            if (!validateInput(email, password, departmentIndex)) return@setOnClickListener

            showLoading(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: ""
                    saveUserToFirestore(userId, email, departments[departmentIndex])
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showFirebaseError(e)
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveUserToFirestore(userId: String, email: String, department: String) {
        val user = hashMapOf(
            "id" to userId,
            "email" to email,
            "department" to department,
            "points" to 0,
            "createdAt" to System.currentTimeMillis(),
            "hasUploadedNote" to false

        )

        firestore.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Kayıt başarılı! Hoş geldin!", Toast.LENGTH_SHORT).show()
                goToMainActivity()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Kullanıcı kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun animateViews() {

        tvAppName.alpha = 0f
        tvAppName.translationY = -30f
        tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()


        tvSubtitle.alpha = 0f
        tvSubtitle.translationY = -20f
        tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .start()

        cardForm.alpha = 0f
        cardForm.scaleX = 0.9f
        cardForm.scaleY = 0.9f
        cardForm.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(400)
            .setDuration(500)
            .start()

        btnRegister.alpha = 0f
        btnRegister.translationY = 30f
        btnRegister.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(700)
            .setDuration(400)
            .start()


        btnBack.alpha = 0f
        btnBack.translationY = 30f
        btnBack.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(850)
            .setDuration(400)
            .start()
    }

    private fun validateInput(email: String, password: String, departmentIndex: Int): Boolean {
        tilEmail.error = null
        tilPassword.error = null

        return when {
            email.isEmpty() -> {
                tilEmail.error = "Email boş olamaz"
                false
            }
            !email.endsWith("@ogr.akdeniz.edu.tr") -> {
                tilEmail.error = "Sadece @ogr.akdeniz.edu.tr uzantılı mailler"
                false
            }
            password.isEmpty() -> {
                tilPassword.error = "Şifre boş olamaz"
                false
            }
            password.length < 6 -> {
                tilPassword.error = "En az 6 karakter olmalı"
                false
            }
            departmentIndex == 0 -> {
                Toast.makeText(this, "Lütfen bölüm seçiniz", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        btnBack.isEnabled = !show
    }

    private fun showFirebaseError(e: Exception) {
        val errorMessage = when {
            e.message?.contains("email address is already") == true ->
                "Bu email zaten kayıtlı"
            e.message?.contains("badly formatted") == true ->
                "Geçersiz email formatı"
            e.message?.contains("weak password") == true ->
                "Şifre çok zayıf"
            e.message?.contains("network") == true ->
                "İnternet bağlantınızı kontrol edin"
            else -> "Kayıt başarısız. Tekrar deneyin."
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}