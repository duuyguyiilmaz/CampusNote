package duygu.yilmaz.campusnote.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` her zaman [Dispatchers.Main] üzerinde çalışır; birim testinde
 * böyle bir looper olmadığı için bu kural onu bir test dispatcher'ıyla değiştirir.
 *
 * Bilinçli olarak [StandardTestDispatcher] seçildi: başlatılan coroutine'ler
 * `advanceUntilIdle()` çağrılana kadar kuyrukta bekler. Böylece testler hem ara
 * `Loading` durumunu hem de iş bitince oluşan son durumu ayrı ayrı doğrulayabiliyor —
 * `UnconfinedTestDispatcher` ile ikisi tek adımda olup biterdi ve `Loading` görülemezdi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
