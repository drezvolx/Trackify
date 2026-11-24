package com.blankpages.trackify.domain

import java.util.Locale

/** Teks bagikan lokasi. Murni JVM supaya formatnya bisa diuji tanpa Android. */
object LocationShareText {
    /**
     * Susunan final yang disetujui:
     * ```
     * Nama Penerima : …
     * Detail/Patokan : …
     * Maps : …
     * by Trackify.
     * ```
     */
    fun build(customerName: String, landmarkNote: String, latitude: Double, longitude: Double): String {
        val mapsUrl = String.format(
            Locale.US,
            "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
            latitude,
            longitude,
        )
        return listOf(
            "Nama Penerima : ${customerName.trim()}",
            "Detail/Patokan : ${landmarkNote.trim()}",
            "Maps : $mapsUrl",
            "by Trackify.",
        ).joinToString("\n")
    }
}
