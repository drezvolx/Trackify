package com.blankpages.trackify

import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.toMonthlySummary
import com.blankpages.trackify.domain.TrackifyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class TrackifyEngineTest {

    private fun log(
        date: String,
        carried: Int,
        delivered: Int,
        rate: Double = 2500.0,
        bonus: Double = 0.0,
        expense: Double = 0.0,
        working: Boolean = true
    ) = DailyLog(
        date = date,
        isWorkingDay = working,
        packagesCarried = carried,
        packagesDelivered = delivered,
        ratePerPackage = rate,
        bonus = bonus,
        expense = expense
    )

    @Test
    fun sanitized_clampsDeliveredToCarried() {
        val clean = log("2026-11-01", carried = 10, delivered = 25).sanitized()
        assertEquals(10, clean.packagesDelivered)
        assertEquals(0, clean.remainingPackages)
    }

    @Test
    fun sanitized_workingOffDayIgnoresPackagesAndIncome() {
        val clean = log("2026-11-02", carried = 99, delivered = 99, working = false).sanitized()
        assertEquals(0, clean.packagesCarried)
        assertEquals(0, clean.packagesDelivered)
        assertEquals(0.0, clean.netIncome, 0.0)
        assertFalse(clean.isWorkingDay)
    }

    @Test
    fun monthlySummary_countsOnlyWorkingDays() {
        val summary = listOf(
            log("2026-11-01", 10, 8),
            log("2026-11-02", 10, 5),
            log("2026-11-03", 10, 10, working = false)
        ).toMonthlySummary()

        assertEquals(2, summary.totalWorkingDays)
        assertEquals(13, summary.totalDelivered)
        assertEquals(32_500.0, summary.totalNetIncome, 0.0)
    }

    @Test
    fun grossIncome_usesDeliveredPackagesPlusBonus() {
        val income = TrackifyEngine.grossIncomeOf(log("2026-11-01", 10, 6, rate = 2500.0, bonus = 10_000.0))
        assertEquals(25_000.0, income, 0.0)
    }

    @Test
    fun distanceMeters_samePointIsZero() {
        assertEquals(0.0, TrackifyEngine.distanceMeters(-6.2, 106.8, -6.2, 106.8), 0.001)
    }

    @Test
    fun filterLocations_sortsByDistanceWhenCoordinatesAvailable() {
        val near = SavedLocation(1, "Dekat", "", -6.2000, 106.8000)
        val far = SavedLocation(2, "Jauh", "", -6.3000, 106.9000)
        val result = TrackifyEngine.filterLocations(listOf(far, near), "", -6.2, 106.8)

        assertEquals("Dekat", result.first().first.customerName)
        assertTrue(result.first().second!! < result.last().second!!)
    }

    @Test
    fun filterNotes_respectsStatusAndQuery() {
        val notes = listOf(
            CourierNote(1, "Titipan", "Rumah A", isResolved = false),
            CourierNote(2, "Kendala", "Rumah B", isResolved = true),
            CourierNote(3, "Titipan", "Rumah C", isResolved = false)
        )

        assertEquals(2, TrackifyEngine.filterNotes(notes, "", 1).size)
        assertEquals(1, TrackifyEngine.filterNotes(notes, "", 2).size)
        assertEquals(1, TrackifyEngine.filterNotes(notes, "Rumah C", 0).size)
    }

    @Test
    fun buildMonthlyChart_fillsEveryDayOfMonth() {
        val chart = TrackifyEngine.buildMonthlyChart(YearMonth.of(2026, 2), emptyList())
        assertEquals(28, chart.bars.size)
        assertEquals(0, chart.maxDelivered)
    }

    @Test
    fun computePeriodBreakdown_splitsAtDayFifteen() {
        val breakdown = TrackifyEngine.computePeriodBreakdown(
            listOf(
                log("2026-11-05", 10, 10, rate = 2000.0),
                log("2026-11-20", 10, 5, rate = 2000.0)
            ),
            workDays = 2
        )

        assertEquals(20_000.0, breakdown.p1NetIncome, 0.0)
        assertEquals(10_000.0, breakdown.p2NetIncome, 0.0)
        assertEquals(10_000.0, breakdown.p1HalfPayout, 0.0)
        assertEquals(20_000.0, breakdown.p2TotalPayout, 0.0)
    }

    @Test
    fun csvContainsHeaderAndTotalRow() {
        val csv = TrackifyEngine.encodeMonthlyCsv(listOf(log("2026-11-01", 10, 8)))
        val lines = csv.lines().filter { it.isNotBlank() }

        assertTrue(lines.first().startsWith("Tanggal,Status,Paket_Dibawa"))
        assertTrue(lines.last().startsWith("TOTAL_BULAN,-,"))
        assertTrue(lines.last().contains("20000"))
    }
}
