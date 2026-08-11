package duygu.yilmaz.campusnote.data.local

import android.content.Context

class OnboardingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun isCompleted(): Boolean = preferences.getBoolean(ONBOARDING_COMPLETED_KEY, false)

    fun markCompleted() {
        preferences.edit()
            .putBoolean(ONBOARDING_COMPLETED_KEY, true)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "CampusNote"
        const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    }
}
