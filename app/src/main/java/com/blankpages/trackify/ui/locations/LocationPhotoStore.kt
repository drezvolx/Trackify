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

    suspend fun persist(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(source)
            if (stream == null) {
                null
            } else {
                val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                if (bitmap == null) {
                    null
                } else {
                    val dir = File(context.filesDir, "location_photos").apply { mkdirs() }
                    val file = File(dir, "loc_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    }
                    file.absolutePath
                }
            }
        }.getOrNull()
    }

    fun load(path: String): ImageBitmap? = if (path.isBlank()) {
        null
    } else {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
