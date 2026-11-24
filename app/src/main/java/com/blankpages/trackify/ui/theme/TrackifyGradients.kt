package com.blankpages.trackify.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradien monokrom. Sumber kebenaran warna: `res/values/colors.xml`.
 * Tema tetap mengikuti sistem; tidak ada toggle manual.
 */
object TrackifyGradients {
    @Composable
    private fun darkBackground(): List<Color> = listOf(
        TrackifyColors.DarkSurface,
        TrackifyColors.DarkBackground,
        TrackifyColors.DarkBackgroundEnd,
    )

    @Composable
    private fun lightBackground(): List<Color> = listOf(
        TrackifyColors.LightSurface,
        TrackifyColors.LightBackground,
        TrackifyColors.LightRaised,
    )

    @Composable
    private fun darkHero(): List<Color> = listOf(
        TrackifyColors.HeroDarkStart,
        TrackifyColors.HeroDarkMid,
        TrackifyColors.HeroDarkEnd,
    )

    @Composable
    private fun lightHero(): List<Color> = listOf(
        TrackifyColors.HeroLightStart,
        TrackifyColors.HeroLightMid,
        TrackifyColors.LightInk,
    )

    /** Warna teks di atas hero gradien (terang di kedua tema). */
    val HeroInk: Color
        @Composable get() = TrackifyColors.Inverse

    val HeroMute: Color
        @Composable get() = TrackifyColors.InverseMute

    val HeroTrack: Color
        @Composable get() = TrackifyColors.InverseTrack

    val HeroLine: Color
        @Composable get() = TrackifyColors.InverseLine

    val HeroChipInk: Color
        @Composable get() = TrackifyColors.DarkSurface

    val HeroChipBackground: Color
        @Composable get() = TrackifyColors.Inverse

    @Composable
    fun background(): Brush = Brush.verticalGradient(
        colors = if (LocalTrackifyDarkTheme.current) darkBackground() else lightBackground(),
    )

    @Composable
    fun hero(): Brush = Brush.verticalGradient(
        colors = if (LocalTrackifyDarkTheme.current) darkHero() else lightHero(),
    )
}
