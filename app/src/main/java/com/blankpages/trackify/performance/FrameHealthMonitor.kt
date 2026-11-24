package com.blankpages.trackify.performance

import android.view.Choreographer

/**
 * Sumber sinyal frame TPG. Memakai Choreographer bawaan platform, tanpa library
 * baru. Jeda dihitung rata-rata per frame supaya perubahan frekuensi sampling
 * pada [AdaptiveBudget] tidak terbaca sebagai jank.
 */
object FrameHealthMonitor {
    private var callback: Choreographer.FrameCallback? = null
    private var anchorNanos = 0L
    private var elapsedFrames = 0L

    fun start(governor: TrackifyGovernor) {
        if (callback != null) return
        val ticker = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (anchorNanos == 0L) {
                    anchorNanos = frameTimeNanos
                } else {
                    elapsedFrames++
                    val every = governor.budget.frameSampleEvery.toLong()
                    if (elapsedFrames >= every) {
                        val deltaPerFrame = (frameTimeNanos - anchorNanos) / elapsedFrames
                        governor.recordFrameDeltaNanos(deltaPerFrame)
                        anchorNanos = frameTimeNanos
                        elapsedFrames = 0
                    }
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        callback = ticker
        anchorNanos = 0L
        elapsedFrames = 0L
        Choreographer.getInstance().postFrameCallback(ticker)
    }

    fun stop() {
        val active = callback ?: return
        Choreographer.getInstance().removeFrameCallback(active)
        callback = null
        anchorNanos = 0L
        elapsedFrames = 0L
    }
}
