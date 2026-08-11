package duygu.yilmaz.CampusNote.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.databinding.ActivityRegisterBinding
import duygu.yilmaz.CampusNote.ui.main.MainActivity

class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var binding: ActivityRegisterBinding

    /** 0. sıra seçim uyarısıdır; [validateInput] bu indeksi geçersiz sayar. */
    private val departments: List<String> by lazy {
        listOf(getString(R.string.department_prompt)) +
            resources.getStringArray(R.array.departments)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        observeAuthState()
        setupClickListeners()
        animateViews()
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            departments
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDepartment.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""
            val departmentIndex = binding.spDepartment.selectedItemPosition

            if (!validateInput(email, password, departmentIndex)) return@setOnClickListener

            viewModel.register(
                email = email,
                password = password,
                department = departments[departmentIndex]
            )
        }

        binding.btnBack.setOnClickListener {
            finish()
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
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                }
                is AuthUiState.Error -> {
                    viewModel.resetState()
                    showLoading(false)
                    if (state.stage == AuthFailureStage.USER_PROFILE) {
                        Toast.makeText(
                            this,
                            getString(
                                R.string.register_error_profile_save,
                                state.exception.message.orEmpty()
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        showFirebaseError(state.exception)
                    }
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

        binding.btnRegister.alpha = 0f
        binding.btnRegister.translationY = 30f
        binding.btnRegister.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(700)
            .setDuration(400)
            .start()


        binding.btnBack.alpha = 0f
        binding.btnBack.translationY = 30f
        binding.btnBack.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(850)
            .setDuration(400)
            .start()
    }

    private fun validateInput(email: String, password: String, departmentIndex: Int): Boolean {
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
            departmentIndex == DEPARTMENT_PROMPT_INDEX -> {
                Toast.makeText(this, R.string.error_department_not_selected, Toast.LENGTH_SHORT)
                    .show()
                false
            }
            else -> true
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnBack.isEnabled = !show
    }

    /** Firebase hata metinleri yerelleştirilmemiş geliyor; kullanıcıya Türkçesini gösteriyoruz. */
    private fun showFirebaseError(e: Exception) {
        val messageId = when {
            e.message?.contains("email address is already") == true ->
                R.string.register_error_email_in_use
            e.message?.contains("badly formatted") == true ->
                R.string.error_invalid_email_format
            e.message?.contains("weak password") == true ->
                R.string.register_error_weak_password
            e.message?.contains("network") == true ->
                R.string.error_network
            else -> R.string.register_error_generic
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        const val DEPARTMENT_PROMPT_INDEX = 0
    }
}
