package com.blankpages.trackify.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.blankpages.trackify.R

/**
 * Facade warna Compose untuk token di `res/values/colors.xml`.
 *
 * Kontrak: `colors.xml` adalah satu-satunya sumber warna. File Kotlin UI dilarang
 * memanggil `Color(...)`, `Color.Black`, `Color.White`, `Color.Transparent`, atau
 * `colorResource(...)` langsung; semua lewat objek ini.
 */
object TrackifyColors {
    val Transparent: Color
        @Composable get() = colorResource(R.color.trackify_transparent)

    val DarkBackground: Color
        @Composable get() = colorResource(R.color.trackify_background_dark)

    val DarkBackgroundEnd: Color
        @Composable get() = colorResource(R.color.trackify_background_dark_end)

    val DarkSurface: Color
        @Composable get() = colorResource(R.color.trackify_surface_dark)

    val DarkRaised: Color
        @Composable get() = colorResource(R.color.trackify_raised_dark)

    val DarkLine: Color
        @Composable get() = colorResource(R.color.trackify_line_dark)

    val DarkInk: Color
        @Composable get() = colorResource(R.color.trackify_ink_dark)

    val DarkMute: Color
        @Composable get() = colorResource(R.color.trackify_mute_dark)

    val DarkSegment: Color
        @Composable get() = colorResource(R.color.trackify_segment_dark)

    val LightBackground: Color
        @Composable get() = colorResource(R.color.trackify_background_light)

    val LightSurface: Color
        @Composable get() = colorResource(R.color.trackify_surface_light)

    val LightRaised: Color
        @Composable get() = colorResource(R.color.trackify_raised_light)

    val LightLine: Color
        @Composable get() = colorResource(R.color.trackify_line_light)

    val LightInk: Color
        @Composable get() = colorResource(R.color.trackify_ink_light)

    val LightMute: Color
        @Composable get() = colorResource(R.color.trackify_mute_light)

    val LightSegment: Color
        @Composable get() = colorResource(R.color.trackify_segment_light)

    val HeroLightStart: Color
        @Composable get() = colorResource(R.color.trackify_hero_light_start)

    val HeroLightMid: Color
        @Composable get() = colorResource(R.color.trackify_hero_light_mid)

    val HeroDarkStart: Color
        @Composable get() = colorResource(R.color.trackify_hero_dark_start)

    val HeroDarkMid: Color
        @Composable get() = colorResource(R.color.trackify_hero_dark_mid)

    val HeroDarkEnd: Color
        @Composable get() = colorResource(R.color.trackify_hero_dark_end)

    val Inverse: Color
        @Composable get() = colorResource(R.color.trackify_inverse)

    val InverseMute: Color
        @Composable get() = colorResource(R.color.trackify_inverse_mute)

    val InverseTrack: Color
        @Composable get() = colorResource(R.color.trackify_inverse_track)

    val InverseLine: Color
        @Composable get() = colorResource(R.color.trackify_inverse_line)

    /**
     * Mask ikon dengan alpha penuh. RGB nilai ini tidak pernah tampil: `Icon` menimpa path
     * lewat `tint`, tetapi path butuh alpha 1 supaya tint punya area untuk diwarnai.
     * Disimpan di facade supaya tidak ada literal warna di `TrackifyIcons.kt`.
     */
    val IconMask: Color = Color.White
}
