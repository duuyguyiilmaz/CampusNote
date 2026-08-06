package duygu.yilmaz.CampusNote.ui.onboarding

import androidx.lifecycle.ViewModel
import duygu.yilmaz.CampusNote.data.local.OnboardingPreferences

class OnboardingViewModel(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {
    fun isCompleted(): Boolean = onboardingPreferences.isCompleted()

    fun completeOnboarding() {
        onboardingPreferences.markCompleted()
    }
}
