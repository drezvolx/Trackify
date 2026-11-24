package com.blankpages.trackify.performance

/**
 * Penilaian kesehatan frame, bukan pengganti JankStats. Di sini sinyal frame dipakai
 * hanya untuk memilih anggaran kerja, jadi tiga tingkat sudah cukup dan stabil.
 */
enum class FrameGrade {
    SMOOTH,
    STRAINED,
    CRITICAL,
}

/**
 * Ringkasan hasil [FrameHealthTracker] tanpa objek yang berubah-ubah di dalamnya,
 * supaya aman dibagikan lintas thread dan dipakai sebagai kunci state Compose.
 */
data class FrameHealth(
    val grade: FrameGrade = FrameGrade.SMOOTH,
    val score: Int = 100,
    val jankRatio: Float = 0f,
    val medianFrameMs: Float = 16.7f,
    val samples: Int = 0,
) {
    companion object {
        val INITIAL = FrameHealth()
    }
}
