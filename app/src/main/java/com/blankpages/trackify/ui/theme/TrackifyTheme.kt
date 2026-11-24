package com.blankpages.trackify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Meneruskan pilihan sistem ke token gradien tanpa menyimpan preferensi tema. */
val LocalTrackifyDarkTheme = compositionLocalOf { false }

/** Tema tunggal app: Royal Charcoal / Pure Charcoal (monokrom gradien) + Geist. */
@Composable
fun TrackifyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTrackifyDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) mockupDarkColorScheme() else mockupLightColorScheme(),
            typography = MockupTypography,
            content = content,
        )
    }
}

/**
 * Judul layar tunggal untuk semua tab.
 *
 * Kontrak: hanya judul, tanpa subtext. Semua header wajib memakai komponen ini
 * supaya ukuran, weight, letter spacing, dan warna tidak bisa beda-beda.
 */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.6).sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
