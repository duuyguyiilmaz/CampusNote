package duygu.yilmaz.CampusNote.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import duygu.yilmaz.CampusNote.R
import duygu.yilmaz.CampusNote.data.local.OnboardingPreferences
import duygu.yilmaz.CampusNote.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewModel: OnboardingViewModel

    private data class OnboardingStep(
        val number: String,
        val title: String,
        val description: String
    )

    private val steps = listOf(
        OnboardingStep(
            "1",
            "Not paylaşarak puan kazan",
            "Ders notlarını paylaştıkça puanın artar, sıralamalarda yükselirsin."
        ),
        OnboardingStep(
            "2",
            "100 puana ulaşınca ödül kazan",
            "100 puana ulaştığında seçili restoranlarda %15 indirim kazanırsın."
        ),
        OnboardingStep(
            "3",
            "Kampüs avantajlarına eriş",
            "Anlaşmalı kafe ve restoranlarda özel indirimlerden yararlan."
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

        setContentView(R.layout.activity_onboarding)

        val cards = listOf(
            findViewById<View>(R.id.cardOne),
            findViewById<View>(R.id.cardTwo),
            findViewById<View>(R.id.cardThree)
        )

        setupCards(cards)
        animateCards(cards)

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            viewModel.completeOnboarding()
            navigateToLogin()
        }
    }

    private fun setupCards(cards: List<View>) {
        cards.forEachIndexed { index, card ->
            card.findViewById<TextView>(R.id.tvStepNumber).text = steps[index].number
            card.findViewById<TextView>(R.id.tvCardTitle).text = steps[index].title
            card.findViewById<TextView>(R.id.tvCardBody).text = steps[index].description
        }
    }

    private fun animateCards(cards: List<View>) {

        findViewById<TextView>(R.id.tvTitle).apply {
            alpha = 0f
            translationY = -30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()
        }

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationX = -100f
            card.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay((300 + index * 150).toLong())
                .setDuration(400)
                .start()
        }

        findViewById<MaterialButton>(R.id.btnStart).apply {
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
