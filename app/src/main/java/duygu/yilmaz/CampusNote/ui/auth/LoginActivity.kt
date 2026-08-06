package duygu.yilmaz.CampusNote.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var tvAppName: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var cardForm: View
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("campusnote_prefs", MODE_PRIVATE)

        val rememberMe = prefs.getBoolean("remember_me", false)
        if (rememberMe && viewModel.hasAuthenticatedUser()) {
            goToMainActivity()
            return
        }

        initViews()
        observeAuthState(prefs)
        setupClickListeners(prefs)
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
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners(prefs: android.content.SharedPreferences) {
        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString()?.trim() ?: ""

            if (!validateInput(email, password)) return@setOnClickListener

            viewModel.signIn(
                email = email,
                password = password
            )
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeAuthState(prefs: android.content.SharedPreferences) {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                AuthUiState.Idle -> showLoading(false)
                AuthUiState.Loading -> showLoading(true)
                AuthUiState.Success -> {
                    viewModel.resetState()
                    prefs.edit().putBoolean("remember_me", cbRememberMe.isChecked).apply()
                    showLoading(false)
                    Toast.makeText(this, "Hoş geldin!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                }
                is AuthUiState.Error -> {
                    viewModel.resetState()
                    showLoading(false)
                    showFirebaseError(state.exception)
                }
            }
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

        btnLogin.alpha = 0f
        btnLogin.translationY = 30f
        btnLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(700)
            .setDuration(400)
            .start()

        btnRegister.alpha = 0f
        btnRegister.translationY = 30f
        btnRegister.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(850)
            .setDuration(400)
            .start()
    }

    private fun validateInput(email: String, password: String): Boolean {
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
            else -> true
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnRegister.isEnabled = !show
    }

    private fun showFirebaseError(e: Exception) {
        val errorMessage = when {
            e.message?.contains("no user record") == true ->
                "Bu email ile kayıtlı kullanıcı bulunamadı"
            e.message?.contains("password is invalid") == true ->
                "Şifre hatalı"
            e.message?.contains("badly formatted") == true ->
                "Geçersiz email formatı"
            e.message?.contains("network") == true ->
                "İnternet bağlantınızı kontrol edin"
            else -> "Giriş başarısız. Tekrar deneyin."
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
