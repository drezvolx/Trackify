package com.blankpages.trackify.ui.locations

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.blankpages.trackify.domain.LocationShareText
import com.blankpages.trackify.domain.SavedLocation
import java.io.File

/** Satu pintu masuk berbagi lokasi: foto dari storage aplikasi + teks terformat. */
internal object LocationSharing {
    fun share(context: Context, location: SavedLocation) {
        val text = LocationShareText.build(
            customerName = location.customerName.ifBlank { "Pelanggan #${location.id.coerceAtLeast(1)}" },
            landmarkNote = location.landmarkNote,
            latitude = location.latitude,
            longitude = location.longitude,
        )
        val photo = location.photoPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf(File::exists)
        val intent = if (photo != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Lokasi"))
    }
}
