package com.blankpages.trackify.performance

import java.util.Arrays
import kotlin.math.roundToInt

/**
 * Jendela geser dari jeda antar-frame, dengan histeresis supaya anggaran tidak
 * berbolak-balik setiap kali ada satu frame lambat.
 *
 * Kelas ini sengaja murni JVM: tidak membaca Choreographer, tidak ber allocasi
 * per frame, dan bisa diuji dengan menyuntik jeda nanosecond buatan.
 */
class FrameHealthTracker(
    private val windowSize: Int = 90,
    private val jankFactor: Float = 1.5f,
    private val confirmations: Int = 3,
) {
    private val window = LongArray(windowSize)
    private val scratch = LongArray(windowSize)
    private var cursor = 0
    private var count = 0
    private var pendingGrade = FrameGrade.SMOOTH
    private var pendingHits = 0
    private var current = FrameHealth.INITIAL

    fun snapshot(): FrameHealth = current

    fun recordFrameDeltaNanos(deltaNanos: Long): FrameHealth {
        // Jeda setelah aplikasi kembali dari background bukan jank scroll.
        if (deltaNanos <= 0L || deltaNanos > MAX_DELTA_NANOS) return current

        window[cursor] = deltaNanos
        cursor = (cursor + 1) % windowSize
        if (count < windowSize) count++
        if (count < MIN_SAMPLES) return current

        System.arraycopy(window, 0, scratch, 0, count)
        Arrays.sort(scratch, 0, count)
        val medianMs = scratch[count / 2] / NANOS_PER_MILLI
        val jankThresholdMs = maxOf(MIN_JANK_MS, minOf(medianMs * jankFactor, MAX_JANK_MS))

        var jank = 0
        for (index in 0 until count) {
            if (window[index] / NANOS_PER_MILLI > jankThresholdMs) jank++
        }
        val jankRatio = jank.toFloat() / count
        val slowPenalty = ((medianMs - REFERENCE_FRAME_MS) / REFERENCE_FRAME_MS).coerceIn(0f, 1f) * 20f
        val score = (100f - jankRatio * 70f - slowPenalty).roundToInt().coerceIn(0, 100)

        val rawGrade = when {
            score >= 80 -> FrameGrade.SMOOTH
            score >= 55 -> FrameGrade.STRAINED
            else -> FrameGrade.CRITICAL
        }
        current = current.copy(
            grade = settle(rawGrade),
            score = score,
            jankRatio = jankRatio,
            medianFrameMs = medianMs,
            samples = count,
        )
        return current
    }

    /** Perubahan grade hanya terjadi setelah [confirmations] pembacaan berturut-turut. */
    private fun settle(raw: FrameGrade): FrameGrade {
        if (raw == current.grade) {
            pendingGrade = raw
            pendingHits = 0
            return raw
        }
        if (raw == pendingGrade) {
            pendingHits++
        } else {
            pendingGrade = raw
            pendingHits = 1
        }
        return if (pendingHits >= confirmations) {
            pendingHits = 0
            raw
        } else {
            current.grade
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000f
        const val MIN_SAMPLES = 12
        const val MIN_JANK_MS = 20f
        const val MAX_JANK_MS = 33.3f
        const val REFERENCE_FRAME_MS = 16.7f
        const val MAX_DELTA_NANOS = 250_000_000L
    }
}
