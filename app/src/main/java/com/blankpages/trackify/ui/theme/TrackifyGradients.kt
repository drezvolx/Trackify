package com.blankpages.trackify.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradien monokrom Fase 15. Sumber kebenaran: `docs/LOCKED_DECISIONS.md`.
 * Tema tetap mengikuti sistem; tidak ada toggle manual.
 */
object TrackifyGradients {
    private val DarkBackground = listOf(
        Color(0xFF25262A),
        Color(0xFF1E1F22),
        Color(0xFF17181A),
    )
    private val LightBackground = listOf(
        Color(0xFFFAFAFA),
        Color(0xFFF1F1F1),
        Color(0xFFE8E8E8),
    )
    private val DarkHero = listOf(
        Color(0xFF4C4E51),
        Color(0xFF2C2D31),
        Color(0xFF1E2023),
    )
    private val LightHero = listOf(
        Color(0xFF3A3A3C),
        Color(0xFF232324),
        Color(0xFF161617),
    )

    /** Warna teks di atas hero gradien (gelap di kedua tema). */
    val HeroInk: Color = Color(0xFFFFFFFF)
    val HeroMute: Color = Color(0xB3FFFFFF)
    val HeroTrack: Color = Color(0x33FFFFFF)
    val HeroLine: Color = Color(0x40FFFFFF)
    val HeroChipInk: Color = Color(0xFF25262A)
    val HeroChipBackground: Color = Color(0xFFFFFFFF)

    @Composable
    fun background(): Brush = Brush.verticalGradient(
        colors = if (LocalTrackifyDarkTheme.current) DarkBackground else LightBackground,
    )

    @Composable
    fun hero(): Brush = Brush.verticalGradient(
        colors = if (LocalTrackifyDarkTheme.current) DarkHero else LightHero,
    )
}
