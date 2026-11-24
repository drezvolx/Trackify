package com.blankpages.trackify.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blankpages.trackify.R

/**
 * Skema warna monokrom lengkap.
 *
 * Semua slot diisi token `TrackifyColors` supaya tidak ada default Material3
 * (merah error, ungu tertiary, hitam scrim) yang bocor ke UI.
 */
@Composable
fun mockupDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = TrackifyColors.DarkInk,
    onPrimary = TrackifyColors.DarkBackground,
    primaryContainer = TrackifyColors.DarkRaised,
    onPrimaryContainer = TrackifyColors.DarkInk,
    inversePrimary = TrackifyColors.DarkSegment,
    secondary = TrackifyColors.DarkSegment,
    onSecondary = TrackifyColors.DarkInk,
    secondaryContainer = TrackifyColors.DarkRaised,
    onSecondaryContainer = TrackifyColors.DarkInk,
    tertiary = TrackifyColors.DarkMute,
    onTertiary = TrackifyColors.DarkBackground,
    tertiaryContainer = TrackifyColors.DarkRaised,
    onTertiaryContainer = TrackifyColors.DarkInk,
    background = TrackifyColors.DarkBackground,
    onBackground = TrackifyColors.DarkInk,
    surface = TrackifyColors.DarkSurface,
    onSurface = TrackifyColors.DarkInk,
    surfaceVariant = TrackifyColors.DarkRaised,
    onSurfaceVariant = TrackifyColors.DarkMute,
    surfaceTint = TrackifyColors.DarkSurface,
    inverseSurface = TrackifyColors.DarkInk,
    inverseOnSurface = TrackifyColors.DarkBackground,
    error = TrackifyColors.DarkInk,
    onError = TrackifyColors.DarkBackground,
    errorContainer = TrackifyColors.DarkRaised,
    onErrorContainer = TrackifyColors.DarkInk,
    outline = TrackifyColors.DarkSegment,
    outlineVariant = TrackifyColors.DarkLine,
    scrim = TrackifyColors.DarkBackground,
    surfaceDim = TrackifyColors.DarkBackground,
    surfaceBright = TrackifyColors.DarkRaised,
    surfaceContainerLowest = TrackifyColors.DarkBackground,
    surfaceContainerLow = TrackifyColors.DarkSurface,
    surfaceContainer = TrackifyColors.DarkSurface,
    surfaceContainerHigh = TrackifyColors.DarkRaised,
    surfaceContainerHighest = TrackifyColors.DarkRaised,
)

/** Pasangan terang untuk [mockupDarkColorScheme] dengan token yang sama. */
@Composable
fun mockupLightColorScheme(): ColorScheme = lightColorScheme(
    primary = TrackifyColors.LightInk,
    onPrimary = TrackifyColors.LightSurface,
    primaryContainer = TrackifyColors.LightRaised,
    onPrimaryContainer = TrackifyColors.LightInk,
    inversePrimary = TrackifyColors.LightMute,
    secondary = TrackifyColors.LightSegment,
    onSecondary = TrackifyColors.LightInk,
    secondaryContainer = TrackifyColors.LightRaised,
    onSecondaryContainer = TrackifyColors.LightInk,
    tertiary = TrackifyColors.LightMute,
    onTertiary = TrackifyColors.LightSurface,
    tertiaryContainer = TrackifyColors.LightRaised,
    onTertiaryContainer = TrackifyColors.LightInk,
    background = TrackifyColors.LightBackground,
    onBackground = TrackifyColors.LightInk,
    surface = TrackifyColors.LightSurface,
    onSurface = TrackifyColors.LightInk,
    surfaceVariant = TrackifyColors.LightRaised,
    onSurfaceVariant = TrackifyColors.LightMute,
    surfaceTint = TrackifyColors.LightSurface,
    inverseSurface = TrackifyColors.LightInk,
    inverseOnSurface = TrackifyColors.LightSurface,
    error = TrackifyColors.LightInk,
    onError = TrackifyColors.LightSurface,
    errorContainer = TrackifyColors.LightRaised,
    onErrorContainer = TrackifyColors.LightInk,
    outline = TrackifyColors.LightSegment,
    outlineVariant = TrackifyColors.LightLine,
    scrim = TrackifyColors.LightInk,
    surfaceDim = TrackifyColors.LightRaised,
    surfaceBright = TrackifyColors.LightSurface,
    surfaceContainerLowest = TrackifyColors.LightSurface,
    surfaceContainerLow = TrackifyColors.LightSurface,
    surfaceContainer = TrackifyColors.LightSurface,
    surfaceContainerHigh = TrackifyColors.LightRaised,
    surfaceContainerHighest = TrackifyColors.LightRaised,
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
