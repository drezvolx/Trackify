package com.blankpages.trackify.domain

/**
 * Tingkat kemampuan perangkat. Nilai murni domain — tidak bergantung pada Android,
 * sehingga bisa diuji tanpa emulator.
 */
enum class PerformanceTier {
    LOW,
    BALANCED,
    HIGH,
}

/**
 * Semua angka yang dipakai komponen performa. Satu objek untuk foto, GPS, dan preview
 * supaya tidak ada konstanta tersebar di UI.
 */
data class PerformanceConfig(
    val tier: PerformanceTier,
    val maxPhotoDimension: Int,
    val photoQuality: Int,
    val maxPreviewDimension: Int,
    val gpsIntervalMillis: Long,
    val gpsMinDistanceMeters: Float,
    val maxPreviewItems: Int,
)

/**
 * Aturan pemetaan [PerformanceTier] → [PerformanceConfig] dan pembulatan sample foto.
 * Tidak menyimpan state; aman dipakai dari thread mana pun.
 */
object PerformancePolicy {
    fun configFor(tier: PerformanceTier): PerformanceConfig = when (tier) {
        PerformanceTier.LOW ->
            PerformanceConfig(
                tier = tier,
                maxPhotoDimension = 1024,
                photoQuality = 70,
                maxPreviewDimension = 640,
                gpsIntervalMillis = 10_000L,
                gpsMinDistanceMeters = 25f,
                maxPreviewItems = 20,
            )

        PerformanceTier.BALANCED ->
            PerformanceConfig(
                tier = tier,
                maxPhotoDimension = 1600,
                photoQuality = 78,
                maxPreviewDimension = 960,
                gpsIntervalMillis = 5_000L,
                gpsMinDistanceMeters = 10f,
                maxPreviewItems = 40,
            )

        PerformanceTier.HIGH ->
            PerformanceConfig(
                tier = tier,
                maxPhotoDimension = 2048,
                photoQuality = 85,
                maxPreviewDimension = 1280,
                gpsIntervalMillis = 5_000L,
                gpsMinDistanceMeters = 10f,
                maxPreviewItems = 60,
            )
    }

    /** Pangkat dua terbesar yang membuat sisi terpanjang ≤ [maxDimension]. */
    fun sampleSize(srcWidth: Int, srcHeight: Int, maxDimension: Int): Int {
        val longest = maxOf(srcWidth, srcHeight)
        if (longest <= 0 || maxDimension <= 0) return 1
        var sample = 1
        while (longest / sample > maxDimension) sample *= 2
        return sample.coerceAtLeast(1)
    }
}
