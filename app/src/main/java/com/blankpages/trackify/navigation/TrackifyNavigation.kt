package com.blankpages.trackify.navigation
import androidx.compose.ui.graphics.vector.ImageVector
import com.blankpages.trackify.ui.theme.TrackifyIcons

enum class TrackifyTab(val label: String, val icon: ImageVector) {
    HOME("Home", TrackifyIcons.Home),
    REKAP("Rekap", TrackifyIcons.Table),
    LOKASI("Lokasi", TrackifyIcons.MapPin),
    CATATAN("Catatan", TrackifyIcons.PageText),
    ;

    companion object {
        fun of(ordinal: Int): TrackifyTab = entries.getOrElse(ordinal) { HOME }
    }
}
