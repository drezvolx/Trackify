package com.blankpages.trackify.ui.theme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blankpages.trackify.R

/** Token solid pendamping gradien Fase 15: semua nilai masih monokrom. */
object TrackifyMockupPalette {
    val DarkBg = Color(0xFF1E1F22)
    val DarkSurface = Color(0xFF25262A)
    val DarkRaised = Color(0xFF2C2D31)
    val DarkLine = Color(0xFF3A3B3F)
    val DarkInk = Color(0xFFF5F5F5)
    val DarkMute = Color(0xFFA0A0A4)
    val DarkSeg = Color(0xFF4C4E51)

    val LightBg = Color(0xFFF1F1F1)
    val LightSurface = Color(0xFFFAFAFA)
    val LightRaised = Color(0xFFE8E8E8)
    val LightLine = Color(0xFFD6D6D6)
    val LightInk = Color(0xFF161617)
    val LightMute = Color(0xFF6A6A6A)
    val LightSeg = Color(0xFFC9C9C9)
}

val MockupDarkColorScheme: ColorScheme = darkColorScheme(
    primary = TrackifyMockupPalette.DarkInk,
    onPrimary = TrackifyMockupPalette.DarkBg,
    primaryContainer = TrackifyMockupPalette.DarkRaised,
    onPrimaryContainer = TrackifyMockupPalette.DarkInk,
    secondary = TrackifyMockupPalette.DarkSeg,
    onSecondary = TrackifyMockupPalette.DarkInk,
    secondaryContainer = TrackifyMockupPalette.DarkRaised,
    onSecondaryContainer = TrackifyMockupPalette.DarkInk,
    background = TrackifyMockupPalette.DarkBg,
    onBackground = TrackifyMockupPalette.DarkInk,
    surface = TrackifyMockupPalette.DarkSurface,
    onSurface = TrackifyMockupPalette.DarkInk,
    surfaceVariant = TrackifyMockupPalette.DarkRaised,
    onSurfaceVariant = TrackifyMockupPalette.DarkMute,
    outline = TrackifyMockupPalette.DarkSeg,
    outlineVariant = TrackifyMockupPalette.DarkLine,
    error = TrackifyMockupPalette.DarkInk,
    errorContainer = TrackifyMockupPalette.DarkRaised,
)

val MockupLightColorScheme: ColorScheme = lightColorScheme(
    primary = TrackifyMockupPalette.LightInk,
    onPrimary = TrackifyMockupPalette.LightSurface,
    primaryContainer = TrackifyMockupPalette.LightRaised,
    onPrimaryContainer = TrackifyMockupPalette.LightInk,
    secondary = TrackifyMockupPalette.LightSeg,
    onSecondary = TrackifyMockupPalette.LightInk,
    secondaryContainer = TrackifyMockupPalette.LightRaised,
    onSecondaryContainer = TrackifyMockupPalette.LightInk,
    background = TrackifyMockupPalette.LightBg,
    onBackground = TrackifyMockupPalette.LightInk,
    surface = TrackifyMockupPalette.LightSurface,
    onSurface = TrackifyMockupPalette.LightInk,
    surfaceVariant = TrackifyMockupPalette.LightRaised,
    onSurfaceVariant = TrackifyMockupPalette.LightMute,
    outline = TrackifyMockupPalette.LightSeg,
    outlineVariant = TrackifyMockupPalette.LightLine,
    error = TrackifyMockupPalette.LightInk,
    errorContainer = TrackifyMockupPalette.LightRaised,
)

/** Geist dari res/font. Medium dipakai juga untuk berat 500-ish. */
val GeistFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
    // Fase 16: aset bold yang sama dipakai untuk 800 agar judul ExtraBold tanpa aset baru.
    Font(R.font.geist_bold, FontWeight.ExtraBold),
)

/** Style angka: `tnum` sudah aktif di semua slot, token ini membuat niatnya eksplisit di UI. */
val TabularFigures: TextStyle
    @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")

private fun TextStyle.geist(): TextStyle = copy(fontFamily = GeistFamily, fontFeatureSettings = "tnum")

private val base = Typography()

/** Skala tipografi mockup v2, dipetakan ke slot Material3 supaya komponen bawaan ikut. */
val MockupTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 60.sp,
        lineHeight = 56.sp,
        letterSpacing = (-2.7).sp,
    ).geist(),
    displayMedium = base.displayMedium.geist(),
    displaySmall = base.displaySmall.geist(),
    headlineLarge = base.headlineLarge.geist(),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.84).sp,
    ).geist(),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.4).sp,
    ).geist(),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.4).sp,
    ).geist(),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.17).sp,
    ).geist(),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp,
    ).geist(),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp).geist(),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp).geist(),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp).geist(),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp,
    ).geist(),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp).geist(),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp).geist(),
)
