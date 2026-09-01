package duygu.yilmaz.campusnote.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.databinding.ActivityCompleteProfileBinding
import duygu.yilmaz.campusnote.ui.main.MainActivity

/**
 * Auth hesabı olup Firestore profili olmayan oturumun çıkış yolu.
 *
 * Buraya kayıt akışından gelinmiyor; [duygu.yilmaz.campusnote.ui.main.MainActivity]
 * oturumu çözerken profilin olmadığını görürse yönlendiriyor. Sebebi için bkz.
 * [CompleteProfileViewModel].
 *
 * Ekran yalnızca bölüm soruyor: uid ve e-posta zaten oturumda ve güvenlik kuralı
 * e-postayı token'la karşılaştırdığı için kullanıcıya sorulacak bir şey değil.
 */
class CompleteProfileActivity : AppCompatActivity() {

    private val viewModel: CompleteProfileViewModel by viewModels()
    private lateinit var binding: ActivityCompleteProfileBinding

    /** 0. sıra seçim uyarısıdır; [selectedDepartment] bu indeksi geçersiz sayar. */
    private val departments: List<String> by lazy {
        listOf(getString(R.string.department_prompt)) +
            resources.getStringArray(R.array.departments)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvEmail.text = viewModel.currentEmail()
        setupSpinner()
        observeState()

        binding.btnSave.setOnClickListener {
            val department = selectedDepartment()
            if (department == null) {
                Toast.makeText(this, R.string.error_department_not_selected, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            viewModel.completeProfile(department)
        }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            goToLogin()
        }
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

    private fun selectedDepartment(): String? =
        binding.spDepartment.selectedItemPosition
            .takeIf { it != DEPARTMENT_PROMPT_INDEX }
            ?.let { departments[it] }

    private fun observeState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                CompleteProfileUiState.Idle -> showLoading(false)
                CompleteProfileUiState.Loading -> showLoading(true)

                CompleteProfileUiState.Success -> {
                    showLoading(false)
                    Toast.makeText(this, R.string.complete_profile_saved, Toast.LENGTH_SHORT)
                        .show()
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }

                // Oturum bu ekran açıkken kaybolduysa yazacak bir uid yok; girişe dön.
                CompleteProfileUiState.MissingSession -> {
                    showLoading(false)
                    Toast.makeText(this, R.string.error_missing_session, Toast.LENGTH_SHORT).show()
                    goToLogin()
                }

                is CompleteProfileUiState.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.register_error_profile_save,
                            state.exception.message.orEmpty()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
        binding.btnSignOut.isEnabled = !show
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    private companion object {
        const val DEPARTMENT_PROMPT_INDEX = 0
    }
}
