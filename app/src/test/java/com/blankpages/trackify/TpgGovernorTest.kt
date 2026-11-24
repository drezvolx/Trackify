package com.blankpages.trackify

import com.blankpages.trackify.domain.PerformancePolicy
import com.blankpages.trackify.domain.PerformanceTier
import com.blankpages.trackify.performance.AdaptiveBudget
import com.blankpages.trackify.performance.AdaptiveBudgetPolicy
import com.blankpages.trackify.performance.FrameGrade
import com.blankpages.trackify.performance.FrameHealth
import com.blankpages.trackify.performance.FrameHealthTracker
import com.blankpages.trackify.performance.QueryGuardPolicy
import com.blankpages.trackify.performance.TrackifyGovernor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TpgGovernorTest {

    @Test
    fun tracker_smoothFramesStaySmooth() {
        val tracker = FrameHealthTracker()
        repeat(60) { tracker.recordFrameDeltaNanos(16_000_000L) }
        assertEquals(FrameGrade.SMOOTH, tracker.snapshot().grade)
        assertEquals(100, tracker.snapshot().score)
    }

    @Test
    fun tracker_uniformlySlowFramesTurnCritical() {
        val tracker = FrameHealthTracker()
        repeat(60) { tracker.recordFrameDeltaNanos(50_000_000L) }
        assertEquals(FrameGrade.CRITICAL, tracker.snapshot().grade)
    }

    @Test
    fun tracker_confirmationsDelayGradeFlip() {
        val eager = FrameHealthTracker(windowSize = 12, confirmations = 1)
        val cautious = FrameHealthTracker(windowSize = 12, confirmations = 20)
        repeat(12) {
            eager.recordFrameDeltaNanos(50_000_000L)
            cautious.recordFrameDeltaNanos(50_000_000L)
        }
        assertEquals(FrameGrade.CRITICAL, eager.snapshot().grade)
        assertEquals(FrameGrade.SMOOTH, cautious.snapshot().grade)
    }

    @Test
    fun tracker_ignoresResumeStall() {
        val tracker = FrameHealthTracker()
        repeat(40) { tracker.recordFrameDeltaNanos(16_000_000L) }
        tracker.recordFrameDeltaNanos(300_000_000L)
        assertEquals(FrameGrade.SMOOTH, tracker.snapshot().grade)
        assertEquals(40, tracker.snapshot().samples)
    }

    @Test
    fun budget_keepsVisualQuotaAndRaisesItsOwnThresholdsUnderStrain() {
        val config = PerformancePolicy.configFor(PerformanceTier.LOW)
        val smooth = AdaptiveBudgetPolicy.budgetFor(config, FrameHealth(FrameGrade.SMOOTH))
        val critical = AdaptiveBudgetPolicy.budgetFor(config, FrameHealth(FrameGrade.CRITICAL))

        assertEquals(config.maxPreviewItems, smooth.previewQuota)
        assertEquals(config.maxPreviewItems, critical.previewQuota)
        assertTrue(critical.queryWarnMs < smooth.queryWarnMs)
        assertTrue(critical.frameSampleEvery > smooth.frameSampleEvery)
        assertTrue(critical.deferDecodeWhileScrolling)
        assertFalse(smooth.deferDecodeWhileScrolling)
    }

    @Test
    fun queryGuard_reportsSlowQueryOncePerWindow() {
        var now = 1_000L
        val governor = TrackifyGovernor(clockMs = { now })
        governor.bind(PerformancePolicy.configFor(PerformanceTier.HIGH))
        val sql = "SELECT * FROM daily_logs WHERE date LIKE '2026-11%' ORDER BY date DESC"

        assertNull(governor.recordQuery(sql, 5L))
        val verdict = governor.recordQuery(sql, 40L)
        assertNotNull(verdict)
        assertTrue(verdict!!.slow)
        assertTrue(verdict.suggestion.orEmpty().contains("rentang"))
        assertNull(governor.recordQuery(sql, 40L))

        now += 60_001L
        assertNotNull(governor.recordQuery(sql, 40L))
    }

    @Test
    fun queryGuard_shapeModeFlagsRiskySqlWithoutTimingAndDedups() {
        var now = 5_000L
        val governor = TrackifyGovernor(clockMs = { now })
        val risky = "SELECT * FROM daily_logs WHERE date LIKE '2026-11%' ORDER BY date DESC"
        val safe = "SELECT * FROM daily_logs WHERE date >= ? AND date < ? ORDER BY date DESC"

        assertNull(governor.recordQueryShape(safe))
        val verdict = governor.recordQueryShape(risky)
        assertNotNull(verdict)
        assertTrue(verdict!!.slow)
        assertTrue(verdict.suggestion.orEmpty().contains("rentang"))
        assertNull(governor.recordQueryShape(risky))

        now += 60_001L
        assertNotNull(governor.recordQueryShape(risky))
    }

    @Test
    fun queryGuard_normalizesWhitespaceInSignature() {
        assertEquals("SELECT * FROM x", QueryGuardPolicy.signatureOf("SELECT  *\n FROM   x"))
    }

    @Test
    fun governor_bindIsIdempotentAndFrameGradePublishesNewBudget() {
        val governor = TrackifyGovernor()
        val seen = mutableListOf<AdaptiveBudget>()
        val listener: (AdaptiveBudget) -> Unit = { seen += it }
        governor.addListener(listener)

        val config = PerformancePolicy.configFor(PerformanceTier.BALANCED)
        governor.bind(config)
        governor.bind(config)
        assertEquals(1, seen.size)

        repeat(60) { governor.recordFrameDeltaNanos(50_000_000L) }
        assertEquals(2, seen.size)
        assertEquals(FrameGrade.CRITICAL, seen.last().grade)
        governor.removeListener(listener)
    }
}
