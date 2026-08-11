package duygu.yilmaz.campusnote.ui.onboarding

import androidx.lifecycle.ViewModel
import duygu.yilmaz.campusnote.data.local.OnboardingPreferences

class OnboardingViewModel(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {
    fun isCompleted(): Boolean = onboardingPreferences.isCompleted()

    fun completeOnboarding() {
        onboardingPreferences.markCompleted()
    }
}
