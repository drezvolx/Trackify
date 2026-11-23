package com.blankpages.trackify

import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.WageCalculator
import com.blankpages.trackify.domain.WageMode
import com.blankpages.trackify.domain.WagePeriod
import com.blankpages.trackify.domain.toWageSummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WageSettingsTest {

    private fun log(date: String, delivered: Int = 4, bonus: Double = 0.0, working: Boolean = true) = DailyLog(
        date = date,
        isWorkingDay = working,
        packagesCarried = 10,
        packagesDelivered = delivered,
        ratePerPackage = 2_500.0,
        bonus = bonus,
    )

    @Test
    fun perPackageModeKeepsLegacyIncome() {
        val entry = log("2026-11-01", delivered = 4, bonus = 1_000.0)
        assertEquals(entry.netIncome, WageCalculator.netIncomeOf(entry, WageMode.PER_PACKAGE, 9_000.0), 0.0)
    }

    @Test
    fun flatDailyModeUsesWorkingDaysInsteadOfPackages() {
        val entry = log("2026-11-01", delivered = 0, bonus = 500.0)
        assertEquals(3_000.0, WageCalculator.netIncomeOf(entry, WageMode.FLAT_DAILY, 2_500.0), 0.0)
        val off = log("2026-11-02", working = false)
        assertEquals(0.0, WageCalculator.netIncomeOf(off, WageMode.FLAT_DAILY, 2_500.0), 0.0)
    }

    @Test
    fun wageSummaryHonoursSelectedMode() {
        val logs = listOf(
            log("2026-11-01", delivered = 4),
            log("2026-11-02", delivered = 2),
            log("2026-11-03", working = false),
        )
        val flat = logs.toWageSummary(WageMode.FLAT_DAILY, 2_500.0)
        assertEquals(2, flat.totalWorkingDays)
        assertEquals(6, flat.totalDelivered)
        assertEquals(5_000.0, flat.totalNetIncome, 0.0)
    }

    @Test
    fun periodTargetAndIncomeAreDerivedFromSelection() {
        assertEquals(120_000.0, WageCalculator.targetForPeriod(3_600_000.0, WagePeriod.DAILY), 0.0)
        assertEquals(900_000.0, WageCalculator.targetForPeriod(3_600_000.0, WagePeriod.WEEKLY), 0.0)
        assertEquals(3_600_000.0, WageCalculator.targetForPeriod(3_600_000.0, WagePeriod.MONTHLY), 0.0)

        val logs = listOf(log("2026-11-24", delivered = 0))
        val weekly = WageCalculator.incomeForPeriod(
            logs,
            LocalDate.of(2026, 11, 24),
            WageMode.FLAT_DAILY,
            2_500.0,
            WagePeriod.WEEKLY,
        )
        assertEquals(2_500.0, weekly, 0.0)
    }
}
