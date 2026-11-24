package com.blankpages.trackify.performance

import com.blankpages.trackify.domain.PerformanceTier

/**
 * Anggaran kerja yang dipakai komponen TPG. Angka di sini tidak mengubah warna,
 * ikon, teks, atau jumlah foto yang akhirnya tampil — hanya *kapan* kerja berat
 * dilakukan dan seberapa sensitif telemetry mendeteksi query lambat.
 */
data class AdaptiveBudget(
    val tier: PerformanceTier,
    val grade: FrameGrade,
    val previewQuota: Int,
    val queryWarnMs: Long,
    val frameSampleEvery: Int,
    val deferDecodeWhileScrolling: Boolean,
) {
    companion object {
        /** Dipakai sampai [TrackifyGovernor.bind] menerima profil perangkat asli. */
        val INITIAL = AdaptiveBudget(
            tier = PerformanceTier.BALANCED,
            grade = FrameGrade.SMOOTH,
            previewQuota = 3,
            queryWarnMs = 20L,
            frameSampleEvery = 2,
            deferDecodeWhileScrolling = false,
        )
    }
}
