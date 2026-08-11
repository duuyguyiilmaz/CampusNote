package duygu.yilmaz.campusnote.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import duygu.yilmaz.campusnote.data.local.OnboardingPreferences

/**
 * [AndroidViewModel] olarak yazıldı çünkü [OnboardingPreferences] SharedPreferences'a
 * eriştiği için bir Context istiyor. Böylece bu ViewModel de diğerleri gibi
 * ayrı bir `ViewModelProvider.Factory` olmadan oluşturulabiliyor.
 *
 * [JvmOverloads] şart: `AndroidViewModelFactory` ViewModel'i reflection ile kurar ve
 * tam olarak `(Application)` imzalı bir constructor arar. Kotlin varsayılan parametre
 * için böyle bir aşırı yükleme üretmediğinden, bu annotation olmadan uygulama
 * çalışma anında `NoSuchMethodException` ile çöker.
 */
class OnboardingViewModel @JvmOverloads constructor(
    application: Application,
    private val onboardingPreferences: OnboardingPreferences =
        OnboardingPreferences(application)
) : AndroidViewModel(application) {

    fun isCompleted(): Boolean = onboardingPreferences.isCompleted()

    fun completeOnboarding() {
        onboardingPreferences.markCompleted()
    }
}
