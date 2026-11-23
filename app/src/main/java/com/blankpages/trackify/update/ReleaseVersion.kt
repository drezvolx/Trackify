package com.blankpages.trackify.update

/**
 * Versi rilis GitHub, misalnya `v1.0.0`, `1.1`, atau `v2.0.0-beta`.
 *
 * Bagian pre-release dan build metadata sengaja diabaikan supaya perbandingan
 * tetap numerik dan sederhana. Endpoint `/releases/latest` sendiri sudah tidak
 * mengembalikan draft atau pre-release.
 */
data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
    )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** Mengembalikan `null` untuk tag yang bukan versi numerik. */
        fun parse(raw: String?): ReleaseVersion? {
            val core = raw?.trim()
                ?.removePrefix("v")
                ?.removePrefix("V")
                ?.substringBefore("-")
                ?.substringBefore("+")
                ?: return null
            val parts = core.split('.')
            if (parts.isEmpty() || parts.size > 3) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            if (numbers.any { it < 0 }) return null
            return ReleaseVersion(
                major = numbers[0],
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 },
            )
        }
    }
}
