package com.blankpages.trackify.performance

import com.blankpages.trackify.domain.PerformanceConfig
import com.blankpages.trackify.domain.PerformanceTier

/**
 * Pemetaan profil perangkat (Fase 10) + kesehatan frame (TPG) menjadi anggaran.
 *
 * Dua sifat yang dijaga:
 * 1. `previewQuota` selalu sama dengan konfigurasi perangkat, sehingga jumlah
 *    foto yang tampil tidak pernah berubah karena publikasi ini.
 * 2. Makin buruk frame, makin sedikit kerja yang dilakukan TPG sendiri
 *    (`frameSampleEvery` naik) dan makin sensitif ambang query lambat.
 */
object AdaptiveBudgetPolicy {

    fun budgetFor(config: PerformanceConfig, health: FrameHealth): AdaptiveBudget {
        val baseWarnMs = when (config.tier) {
            PerformanceTier.LOW -> 32L
            PerformanceTier.BALANCED -> 20L
            PerformanceTier.HIGH -> 12L
        }
        val warnScale = when (health.grade) {
            FrameGrade.SMOOTH -> 100L
            FrameGrade.STRAINED -> 85L
            FrameGrade.CRITICAL -> 70L
        }
        val sampleEvery = when (health.grade) {
            FrameGrade.SMOOTH -> 2
            FrameGrade.STRAINED -> 4
            FrameGrade.CRITICAL -> 8
        } + if (config.tier == PerformanceTier.LOW) 2 else 0

        return AdaptiveBudget(
            tier = config.tier,
            grade = health.grade,
            previewQuota = config.maxPreviewItems,
            queryWarnMs = (baseWarnMs * warnScale / 100L).coerceAtLeast(4L),
            frameSampleEvery = sampleEvery,
            deferDecodeWhileScrolling = health.grade != FrameGrade.SMOOTH,
        )
    }
}
