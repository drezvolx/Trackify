package com.blankpages.trackify.ui.locations

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.blankpages.trackify.domain.PerformanceConfig
import com.blankpages.trackify.domain.PerformancePolicy
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Penyimpanan foto lokasi: kamera masuk galeri, salinan stabil disimpan di storage aplikasi. */
object LocationPhotoStore {
    /**
     * Cache preview lintas scroll. Tanpa ini, setiap kali kartu keluar masuk
     * komposisi bitmap di-decode ulang di IO thread dan memori low-end cepat penuh.
     */
    private val previewCache = object :
        LinkedHashMap<String, ImageBitmap>(CACHE_INITIAL_CAPACITY, CACHE_LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
            size > CACHE_MAX_ENTRIES
    }

    @Synchronized
    fun cached(path: String): ImageBitmap? = previewCache[path]

    @Synchronized
    private fun cache(path: String, bitmap: ImageBitmap) {
        previewCache[path] = bitmap
    }

    /**
     * Baris MediaStore sengaja tidak ditandai pending: aplikasi kamera eksternal harus bisa
     * menulis JPEG ke URI ini lewat izin yang diberikan `TakePicture`. Menandainya pending
     * mengunci tulis hanya untuk aplikasi ini dan membuat kamera vendor crash saat menekan done.
     */
    fun createGalleryImageUri(context: Context): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Trackify_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Trackify")
            }
        }
        return runCatching {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull()
    }

    /**
     * Foto dibaca sekali sebagai byte (JPEG sudah terkompresi), lalu di-decode dua kali:
     * sekadar untuk ukuran, lalu dengan [PerformancePolicy.sampleSize] sesuai profil perangkat.
     * Ini menghindari bitmap penuh di memory saat perangkat low-RAM.
     */
    suspend fun persist(
        context: Context,
        source: Uri,
        config: PerformanceConfig,
        directory: String = "location_photos",
        prefix: String = "loc_",
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val sample = PerformancePolicy.sampleSize(
                bounds.outWidth,
                bounds.outHeight,
                config.maxPhotoDimension,
            )
            val bitmap = BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return@runCatching null
            val dir = File(context.filesDir, directory).apply { mkdirs() }
            val file = File(dir, "${prefix}${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, config.photoQuality, output)
            }
            file.absolutePath
        }.getOrNull()
    }

    fun load(path: String, config: PerformanceConfig): ImageBitmap? {
        if (path.isBlank()) return null
        cached(path)?.let { return it }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val sample = PerformancePolicy.sampleSize(
                bounds.outWidth,
                bounds.outHeight,
                config.maxPreviewDimension,
            )
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )?.asImageBitmap()
        }.getOrNull()?.also { cache(path, it) }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        synchronized(this) { previewCache.remove(path) }
        runCatching { File(path).delete() }
    }

    private const val CACHE_INITIAL_CAPACITY = 16
    private const val CACHE_MAX_ENTRIES = 24
    private const val CACHE_LOAD_FACTOR = 0.75f
}
