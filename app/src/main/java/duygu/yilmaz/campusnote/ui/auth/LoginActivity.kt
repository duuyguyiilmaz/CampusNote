package duygu.yilmaz.campusnote.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.databinding.ActivityLoginBinding
import duygu.yilmaz.campusnote.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    /**
     * Oturum, kullanıcı Profil'den çıkış yapana kadar açık kalır.
     *
     * Eskiden burada bir "Oturumu açık tut" kutusu vardı ve atlama yalnızca kutu
     * işaretliyse yapılıyordu. Firebase oturumu zaten diske yazdığı için kutu
     * işaretsizken kullanıcı hâlâ giriş yapmış oluyordu — uygulama sadece bunu yok
     * sayıp formu gösteriyordu. Sonuç: uygulamadan yanlışlıkla çıkan biri her
     * açılışta şifresini yeniden yazmak zorunda kalıyordu, üstelik "çıkış yapmış"
     * da değildi. Kutu kaldırıldı; çıkış tek yerden, Profil'deki düğmeden yapılıyor.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.hasAuthenticatedUser()) {
            goToMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeAuthState()
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

    private fun observeAuthState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                AuthUiState.Idle -> showLoading(false)
                AuthUiState.Loading -> showLoading(true)
                AuthUiState.Success -> {
                    viewModel.resetState()
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
        Toast.makeText(this, authErrorMessage(e, AuthAction.SIGN_IN), Toast.LENGTH_LONG).show()
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
