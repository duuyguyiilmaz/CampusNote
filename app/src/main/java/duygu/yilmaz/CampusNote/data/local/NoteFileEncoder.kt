package duygu.yilmaz.CampusNote.data.local

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import duygu.yilmaz.CampusNote.data.model.NoteFileType
import java.io.ByteArrayOutputStream

/**
 * Bir nota eklenebilecek yaklaşık üst sınır (KB). Firestore doküman limitinden türetilir:
 * base64 ham veriyi ~4/3 büyüttüğü için 900.000 karakter ≈ 660 KB ham dosyaya denk gelir.
 */
const val MAX_NOTE_FILE_KB = 650

/**
 * Seçilen dosyayı Firestore'a yazılabilir base64 metnine çevirir.
 *
 * Firestore'da bir doküman en fazla 1 MiB olabildiği için görseller önce
 * küçültülüp yeniden sıkıştırılır; sonuç yine de sığmıyorsa [EncodedFile.TooLarge]
 * döner ve kullanıcıya anlaşılır bir mesaj gösterilir.
 *
 * Bu sınıf disk okuması ve bitmap işi yapar — her zaman IO dispatcher'ında çağrılmalı.
 */
class NoteFileEncoder(private val contentResolver: ContentResolver) {

    fun encode(uri: Uri?, fileType: String): EncodedFile {
        if (uri == null) return EncodedFile.None

        return try {
            val bytes = when (fileType) {
                NoteFileType.IMAGE -> compressImage(uri)
                else -> readBytes(uri)
            }

            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.length > MAX_ENCODED_LENGTH) {
                EncodedFile.TooLarge(byteSize = bytes.size.toLong())
            } else {
                EncodedFile.Success(data = encoded, byteSize = bytes.size.toLong())
            }
        } catch (exception: Exception) {
            EncodedFile.Failure(exception)
        }
    }

    private fun readBytes(uri: Uri): ByteArray {
        val stream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Dosya açılamadı")
        return stream.use { it.readBytes() }
    }

    /**
     * Görseli en fazla [MAX_DIMENSION] piksele indirir, sonra sığana kadar
     * JPEG kalitesini kademeli düşürür.
     */
    private fun compressImage(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalStateException("Görsel okunamadı")

        try {
            for (quality in JPEG_QUALITY_STEPS) {
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                val bytes = output.toByteArray()

                // base64 boyutu ham boyutun ~4/3'ü; sığıyorsa daha fazla bozmaya gerek yok.
                if (bytes.size * 4 / 3 <= MAX_ENCODED_LENGTH) return bytes
                if (quality == JPEG_QUALITY_STEPS.last()) return bytes
            }
            throw IllegalStateException("Görsel sıkıştırılamadı")
        } finally {
            bitmap.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1

        var sampleSize = 1
        while (width / (sampleSize * 2) >= MAX_DIMENSION ||
            height / (sampleSize * 2) >= MAX_DIMENSION
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private companion object {
        /**
         * Firestore doküman sınırı 1.048.576 byte. İçerik dokümanında sadece
         * base64 metni duruyor; alan adı ve doküman ek yükü için pay bırakıyoruz.
         */
        const val MAX_ENCODED_LENGTH = 900_000

        const val MAX_DIMENSION = 1600
        val JPEG_QUALITY_STEPS = listOf(80, 65, 50)
    }
}

sealed interface EncodedFile {
    /** Kullanıcı dosya seçmedi. */
    data object None : EncodedFile

    data class Success(val data: String, val byteSize: Long) : EncodedFile

    /** Sıkıştırmaya rağmen Firestore dokümanına sığmıyor. */
    data class TooLarge(val byteSize: Long) : EncodedFile

    data class Failure(val exception: Exception) : EncodedFile
}
