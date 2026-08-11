package duygu.yilmaz.CampusNote.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.data.local.OnboardingPreferences
import duygu.yilmaz.CampusNote.databinding.ActivityOnboardingBinding
import duygu.yilmaz.CampusNote.databinding.ItemOnboardingCardBinding
import duygu.yilmaz.CampusNote.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var viewModel: OnboardingViewModel

    private data class OnboardingStep(
        @StringRes val titleId: Int,
        @StringRes val descriptionId: Int
    )

    /** Kart sırası ekrandaki sırayı belirler; adım numarası indeksten türetilir. */
    private val steps = listOf(
        OnboardingStep(
            R.string.onboarding_step_one_title,
            R.string.onboarding_step_one_body
        ),
        OnboardingStep(
            R.string.onboarding_step_two_title,
            R.string.onboarding_step_two_body
        ),
        OnboardingStep(
            R.string.onboarding_step_three_title,
            R.string.onboarding_step_three_body
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = OnboardingViewModelFactory(
            OnboardingPreferences(applicationContext)
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[OnboardingViewModel::class.java]

        if (viewModel.isCompleted()) {
            navigateToLogin()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // <include> etiketleri id'li olduğu için ViewBinding her kartı tipli bir alt binding
        // olarak üretiyor; kart içindeki view'lara doğrudan erişebiliyoruz.
        val cards = listOf(binding.cardOne, binding.cardTwo, binding.cardThree)

        setupCards(cards)
        animateCards(cards)

        binding.btnStart.setOnClickListener {
            viewModel.completeOnboarding()
            navigateToLogin()
        }
    }

    private fun setupCards(cards: List<ItemOnboardingCardBinding>) {
        cards.forEachIndexed { index, card ->
            val step = steps[index]
            card.tvStepNumber.text = (index + 1).toString()
            card.tvCardTitle.setText(step.titleId)
            card.tvCardBody.setText(step.descriptionId)
        }
    }

    private fun animateCards(cards: List<ItemOnboardingCardBinding>) {
        binding.tvTitle.apply {
            alpha = 0f
            translationY = -30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()
        }

        cards.forEachIndexed { index, card ->
            card.root.alpha = 0f
            card.root.translationX = -100f
            card.root.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay((300 + index * 150).toLong())
                .setDuration(400)
                .start()
        }

        binding.btnStart.apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(800)
                .setDuration(400)
                .start()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
