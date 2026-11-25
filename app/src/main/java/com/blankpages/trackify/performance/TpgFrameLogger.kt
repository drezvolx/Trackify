package com.blankpages.trackify.performance

import android.util.Log
import java.util.Locale

/**
 * Observabilitas ringan TPG: satu baris log hanya saat grade frame berubah.
 *
 * Listener governor dipanggil hanya ketika anggaran dipublikasikan, jadi tidak
 * ada log per frame, tidak ada timer, dan tidak ada kerja tambahan di jalur
 * `Choreographer`. Kelas ini sengaja berada di layer app supaya
 * `:core:performance` tetap murni JVM tanpa dependensi Android.
 */
class TpgFrameLogger(private val governor: TrackifyGovernor, private val log: (String) -> Unit = { Log.d(TAG, it) }) {
    private var lastGrade: FrameGrade = FrameGrade.SMOOTH
    private val listener: (AdaptiveBudget) -> Unit = { budget -> onBudget(budget) }

    /** Pasang setelah governor di-bind. Aman dipanggil ulang. */
    fun attach() {
        lastGrade = governor.snapshot().frameHealth.grade
        governor.addListener(listener)
    }

    private fun onBudget(budget: AdaptiveBudget) {
        val health = governor.snapshot().frameHealth
        val previous = lastGrade
        lastGrade = health.grade
        if (previous == health.grade) return
        log(
            buildString {
                append("grade=").append(previous).append("->").append(health.grade)
                append(" score=").append(health.score)
                append(" jank=").append(String.format(Locale.US, "%.1f%%", health.jankRatio * 100f))
                append(" median=").append(String.format(Locale.US, "%.1fms", health.medianFrameMs))
                append(" samples=").append(health.samples)
                append(" budget(tier=").append(budget.tier)
                append(" queryWarnMs=").append(budget.queryWarnMs)
                append(" frameSampleEvery=").append(budget.frameSampleEvery)
                append(" deferDecode=").append(budget.deferDecodeWhileScrolling)
                append(" previewQuota=").append(budget.previewQuota)
                append(")")
            },
        )
    }

    private companion object {
        const val TAG = "TPG/Frame"
    }
}
