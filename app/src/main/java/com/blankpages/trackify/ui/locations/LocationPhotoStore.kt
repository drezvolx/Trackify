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
    fun createGalleryImageUri(context: Context): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Trackify_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Trackify")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        return runCatching {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull()
    }

    fun finalizeGalleryImage(context: Context, uri: Uri?) {
        if (uri == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val values = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, values, null, null)
        }
    }

    /**
     * Foto dibaca sekali sebagai byte (JPEG sudah terkompresi), lalu di-decode dua kali:
     * sekadar untuk ukuran, lalu dengan [PerformancePolicy.sampleSize] sesuai profil perangkat.
     * Ini menghindari bitmap penuh di memory saat perangkat low-RAM.
     */
    suspend fun persist(context: Context, source: Uri, config: PerformanceConfig): String? =
        withContext(Dispatchers.IO) {
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
                val dir = File(context.filesDir, "location_photos").apply { mkdirs() }
                val file = File(dir, "loc_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, config.photoQuality, output)
                }
                file.absolutePath
            }.getOrNull()
        }

    fun load(path: String, config: PerformanceConfig): ImageBitmap? {
        if (path.isBlank()) return null
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
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
