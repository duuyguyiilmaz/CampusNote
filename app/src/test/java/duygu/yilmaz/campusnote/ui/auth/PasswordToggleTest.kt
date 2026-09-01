package duygu.yilmaz.campusnote.ui.auth

import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.CheckBox
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.textfield.TextInputLayout
import duygu.yilmaz.campusnote.R
import duygu.yilmaz.campusnote.databinding.ActivityLoginBinding
import duygu.yilmaz.campusnote.databinding.ActivityRegisterBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Şifre görünürlük düğmesinin ikonu *durumu* göstermeli: gizliyken üstü çizili göz,
 * görünürken açık göz.
 *
 * Bu, Material'in varsayılanının tersi — varsayılan eylemi gösterir — ve düğme
 * `endIconDrawable` ile özel bir selector'a bağlı olduğu için sessizce geri
 * dönebilecek bir tercih: birisi `endIconDrawable` satırını silse düğme çalışmaya
 * devam eder, sadece ikon yine ters görünür. Hiçbir ViewModel testi bunu görmez.
 *
 * `state_checked`, TextInputLayout'un password_toggle end icon'unda "şifre görünür"
 * anlamına geliyor; test seçilen drawable yerine bu durumu doğruluyor, çünkü asıl
 * sözleşme selector ile düğme arasında.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PasswordToggleTest {

    private fun themedInflater(): LayoutInflater {
        val themed = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_CampusNote
        )
        return LayoutInflater.from(themed)
    }

    /**
     * End icon bir [CheckBox] değil, Material'in kendi `CheckableImageButton`'ı; tipi
     * kütüphanenin iç paketinde olduğu için [Checkable] arayüzü üzerinden okunuyor.
     */
    private fun TextInputLayout.toggleButton(): View =
        findViewById(com.google.android.material.R.id.text_input_end_icon)

    private val View.checked: Boolean get() = (this as Checkable).isChecked

    @Test
    fun `giris ekraninda sifre once gizli ve ikon isaretsiz`() {
        val binding = ActivityLoginBinding.inflate(themedInflater())

        assertTrue(
            "Şifre alanı gizli başlamalı",
            binding.etPassword.transformationMethod is PasswordTransformationMethod
        )
        assertFalse(
            "Şifre gizliyken ikon işaretsiz olmalı — üstü çizili göz",
            binding.tilPassword.toggleButton().checked
        )
    }

    @Test
    fun `giris ekraninda dugmeye basinca sifre gorunur ve ikon isaretlenir`() {
        val binding = ActivityLoginBinding.inflate(themedInflater())

        binding.tilPassword.toggleButton().performClick()

        assertFalse(
            "Şifre alanı görünür olmalı",
            binding.etPassword.transformationMethod is PasswordTransformationMethod
        )
        assertTrue(
            "Şifre görünürken ikon işaretli olmalı — açık göz",
            binding.tilPassword.toggleButton().checked
        )
    }

    @Test
    fun `kayit ekrani ayni davranisi paylasir`() {
        val binding = ActivityRegisterBinding.inflate(themedInflater())

        assertFalse(binding.tilPassword.toggleButton().checked)
        binding.tilPassword.toggleButton().performClick()
        assertTrue(binding.tilPassword.toggleButton().checked)
    }

    /**
     * Selector'ın bağlı kaldığını sabitler: `endIconDrawable` düşerse Material kendi
     * varsayılan ikonunu koyar ve görünüm sessizce eski ters hâline döner.
     */
    @Test
    fun `iki ekran da ozel selector ikonunu kullanir`() {
        assertNotNull(ActivityLoginBinding.inflate(themedInflater()).tilPassword.endIconDrawable)
        assertNotNull(ActivityRegisterBinding.inflate(themedInflater()).tilPassword.endIconDrawable)
    }

    /**
     * "Oturumu açık tut" kutusu kaldırıldı — oturum, kullanıcı Profil'den çıkana
     * kadar zaten açık kalıyor. Kutunun geri gelmesi eski davranışın da geri
     * gelmesi demek olurdu.
     */
    @Test
    fun `giris ekraninda oturumu acik tut kutusu yok`() {
        val root = ActivityLoginBinding.inflate(themedInflater()).root
        assertNull(
            "Beni hatırla kutusu kaldırıldı",
            root.findViewById<CheckBox>(
                root.resources.getIdentifier("cbRememberMe", "id", "duygu.yilmaz.campusnote")
            )
        )
        assertEquals(0, root.resources.getIdentifier("login_keep_signed_in", "string", "duygu.yilmaz.campusnote"))
    }
}
