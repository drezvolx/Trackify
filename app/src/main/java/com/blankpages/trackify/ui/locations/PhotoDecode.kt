package com.blankpages.trackify.ui.locations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.blankpages.trackify.domain.PerformanceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decode foto di IO thread. [defer] dipakai TPG untuk menunda decode saat
 * daftar sedang di-scroll; hasil cache tetap dipakai supaya tidak berkedip.
 */
@Composable
fun rememberDecodedPhoto(path: String, config: PerformanceConfig, defer: Boolean = false): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(LocationPhotoStore.cached(path)) }
    LaunchedEffect(path, config, defer) {
        if (path.isBlank() || defer) return@LaunchedEffect
        LocationPhotoStore.cached(path)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) { LocationPhotoStore.load(path, config) }
    }
    return bitmap
}
