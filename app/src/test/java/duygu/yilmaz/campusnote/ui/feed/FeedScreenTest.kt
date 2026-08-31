package duygu.yilmaz.campusnote.ui.feed

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.testing.FakeAuthRepository
import duygu.yilmaz.campusnote.testing.FakeNoteRepository
import duygu.yilmaz.campusnote.testing.FakeUserRepository
import duygu.yilmaz.campusnote.testing.authenticatedUser
import duygu.yilmaz.campusnote.testing.post
import duygu.yilmaz.campusnote.testing.userProfile
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Katkı kapısının ekrana ulaştığını doğrular.
 *
 * [FeedViewModelTest] kapının kararını zaten test ediyor: not yüklemeyen kullanıcı
 * için durum `Locked` oluyor. Ama o karardan ekrandaki görünürlüğe giden yol test
 * edilmemişti — `showFeed(true)` ile `showFeed(false)` yer değiştirse bütün birim
 * testler yeşil kalır ve uygulama herkese açılırdı. Bu testler ekranı gerçekten
 * şişirip ne göründüğüne bakıyor.
 *
 * Robolectric sayesinde emülatör gerekmiyor; testler diğer birim testlerle aynı
 * `testDebugUnitTest` görevinde koşuyor.
 */
@RunWith(RobolectricTestRunner::class)
// Robolectric'in varsayılan ekranı 320x470dp; kilit ekranının içeriği oraya
// sığmadığı için gerçek bir telefon boyutu veriliyor.
@Config(qualifiers = "w411dp-h891dp")
class FeedScreenTest {

    private val auth = FakeAuthRepository(authenticatedUser(uid = "uid-1"))
    private val users = FakeUserRepository(userProfile(department = "Bilgisayar Mühendisliği"))
    private val notes = FakeNoteRepository()

    @Test
    fun `a user who has not uploaded sees the lock, not the feed`() {
        notes.hasUploadedNote = false

        launchFeed()

        onView(withId(R.id.layoutLocked)).check(matches(isDisplayed()))
        onView(withId(R.id.tvLockedTitle)).check(matches(withText(R.string.feed_locked_title)))
        onView(withId(R.id.swipeRefresh))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun `the lock offers the way out of it`() {
        // Kilit ekranının tek işi kapıyı açmanın yolunu göstermek; düğme kaybolursa
        // kullanıcı kilitli kalır ve bunu hiçbir ViewModel testi yakalamaz.
        notes.hasUploadedNote = false

        launchFeed()

        // Kilit ekranı bir ScrollView; küçük ekranda düğmeye kaydırmak gerekebilir,
        // scrollTo bunu ekran boyutundan bağımsız hâle getiriyor.
        onView(withId(R.id.btnUpload)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.btnUpload)).check(matches(withText(R.string.feed_locked_action)))
    }

    @Test
    fun `a user who has uploaded sees the feed, not the lock`() {
        notes.hasUploadedNote = true
        notes.departmentNotes = flowOf(listOf(post(id = "a"), post(id = "b")))

        launchFeed()

        onView(withId(R.id.swipeRefresh)).check(matches(isDisplayed()))
        onView(withId(R.id.layoutLocked))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun `the unlocked feed lists the department's notes`() {
        notes.hasUploadedNote = true
        notes.departmentNotes = flowOf(
            listOf(
                post(id = "a", title = "Veri Yapıları Özeti"),
                post(id = "b", title = "Algoritmalar Vize Notu")
            )
        )

        val scenario = launchFeed()

        onView(withText("Veri Yapıları Özeti")).check(matches(isDisplayed()))
        onView(withText("Algoritmalar Vize Notu")).check(matches(isDisplayed()))
        scenario.onFragment { fragment ->
            val list = fragment.requireView().findViewById<RecyclerView>(R.id.rvFeed)
            assertEquals(2, list.adapter?.itemCount)
        }
    }

    @Test
    fun `a locked feed never queries the department`() {
        // Kilitliyken sorgu kurulmamalı: hem gereksiz okuma hem de kapının
        // gerçekten kapalı olduğunun kanıtı.
        notes.hasUploadedNote = false

        launchFeed()

        assertEquals(null, notes.observedDepartment)
    }

    @Test
    fun `a missing profile keeps the feed locked`() {
        // Kullanıcı oturum açmış ama profil dokümanı yoksa kapı kapalı kalmalı.
        notes.hasUploadedNote = true
        users.profile = null

        launchFeed()

        onView(withId(R.id.layoutLocked)).check(matches(isDisplayed()))
    }

    /**
     * Fabrika [FragmentFactory] üzerinden veriliyor çünkü `viewModel` alanı
     * `onViewCreated` içinde ilk kez okunuyor — fragment oluşturulduktan sonra
     * atamak geç kalırdı.
     */
    private fun launchFeed() = launchFragmentInContainer<FeedFragment>(
        themeResId = R.style.Theme_CampusNote,
        factory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
                FeedFragment().apply { viewModelFactory = FakeViewModelFactory() }
        }
    )

    private inner class FakeViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FeedViewModel(auth, users, notes) as T
    }
}
