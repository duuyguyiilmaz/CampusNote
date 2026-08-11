package duygu.yilmaz.CampusNote.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.databinding.ActivityLoginBinding
import duygu.yilmaz.CampusNote.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        if (rememberMe && viewModel.hasAuthenticatedUser()) {
            goToMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeAuthState(prefs)
        setupClickListeners()
        animateViews()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""

            if (!validateInput(email, password)) return@setOnClickListener

            viewModel.signIn(email = email, password = password)
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeAuthState(prefs: SharedPreferences) {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                AuthUiState.Idle -> showLoading(false)
                AuthUiState.Loading -> showLoading(true)
                AuthUiState.Success -> {
                    viewModel.resetState()
                    prefs.edit().putBoolean(KEY_REMEMBER_ME, binding.cbRememberMe.isChecked).apply()
                    showLoading(false)
                    Toast.makeText(this, R.string.login_welcome, Toast.LENGTH_SHORT).show()
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
        binding.tvAppName.alpha = 0f
        binding.tvAppName.translationY = -30f
        binding.tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        binding.tvSubtitle.alpha = 0f
        binding.tvSubtitle.translationY = -20f
        binding.tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .start()

        binding.cardForm.alpha = 0f
        binding.cardForm.scaleX = 0.9f
        binding.cardForm.scaleY = 0.9f
        binding.cardForm.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(400)
            .setDuration(500)
            .start()

        binding.btnLogin.alpha = 0f
        binding.btnLogin.translationY = 30f
        binding.btnLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(700)
            .setDuration(400)
            .start()

        binding.btnRegister.alpha = 0f
        binding.btnRegister.translationY = 30f
        binding.btnRegister.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(850)
            .setDuration(400)
            .start()
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        val emailDomain = getString(R.string.university_email_domain)

        return when {
            email.isEmpty() -> {
                binding.tilEmail.error = getString(R.string.error_email_empty)
                false
            }
            !email.endsWith(emailDomain) -> {
                binding.tilEmail.error = getString(R.string.error_email_domain, emailDomain)
                false
            }
            password.isEmpty() -> {
                binding.tilPassword.error = getString(R.string.error_password_empty)
                false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                binding.tilPassword.error =
                    getString(R.string.error_password_too_short, MIN_PASSWORD_LENGTH)
                false
            }
            else -> true
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnRegister.isEnabled = !show
    }

    /** Firebase hata metinleri yerelleştirilmemiş geliyor; kullanıcıya Türkçesini gösteriyoruz. */
    private fun showFirebaseError(e: Exception) {
        val messageId = when {
            e.message?.contains("no user record") == true ->
                R.string.login_error_user_not_found
            e.message?.contains("password is invalid") == true ->
                R.string.login_error_wrong_password
            e.message?.contains("badly formatted") == true ->
                R.string.error_invalid_email_format
            e.message?.contains("network") == true ->
                R.string.error_network
            else -> R.string.login_error_generic
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val PREFS_NAME = "campusnote_prefs"
        const val KEY_REMEMBER_ME = "remember_me"
        const val MIN_PASSWORD_LENGTH = 6
    }
}
