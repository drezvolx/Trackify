package com.blankpages.trackify.domain

import java.time.LocalDate

/** Cara penghasilan dihitung. Default lama [PER_PACKAGE] dipertahankan. */
enum class WageMode {
    PER_PACKAGE,
    FLAT_DAILY,
}

/** Periode pada Home/Rekap. Hanya mengubah label dan agregat, bukan schema DB. */
enum class WagePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
}

/** Kalkulasi penghasilan murni — tidak membaca prefs maupun database. */
object WageCalculator {
    fun netIncomeOf(log: DailyLog, mode: WageMode, flatDailyAmount: Double): Double {
        val clean = log.sanitized()
        if (!clean.isWorkingDay) return 0.0
        return when (mode) {
            WageMode.PER_PACKAGE -> clean.netIncome

            WageMode.FLAT_DAILY ->
                flatDailyAmount.coerceAtLeast(0.0) +
                    clean.bonus.coerceAtLeast(0.0) -
                    clean.expense.coerceAtLeast(0.0)
        }
    }

    fun grossIncomeOf(log: DailyLog, mode: WageMode, flatDailyAmount: Double): Double {
        val clean = log.sanitized()
        if (!clean.isWorkingDay) return 0.0
        return when (mode) {
            WageMode.PER_PACKAGE -> TrackifyEngine.grossIncomeOf(clean)
            WageMode.FLAT_DAILY -> flatDailyAmount.coerceAtLeast(0.0) + clean.bonus.coerceAtLeast(0.0)
        }
    }

    /** Agregat sesuai periode yang dipilih user. */
    fun incomeForPeriod(
        logs: List<DailyLog>,
        today: LocalDate,
        mode: WageMode,
        flatDailyAmount: Double,
        period: WagePeriod,
    ): Double {
        val byDate = logs.associateBy { it.date }
        return when (period) {
            WagePeriod.DAILY -> netIncomeOf(
                byDate[today.toString()] ?: DailyLog(today.toString(), isWorkingDay = false),
                mode,
                flatDailyAmount,
            )

            WagePeriod.WEEKLY -> (0L..6L).sumOf { offset ->
                val date = today.minusDays(offset)
                netIncomeOf(
                    byDate[date.toString()] ?: DailyLog(date.toString(), isWorkingDay = false),
                    mode,
                    flatDailyAmount,
                )
            }

            WagePeriod.MONTHLY -> logs.sumOf { netIncomeOf(it, mode, flatDailyAmount) }
        }
    }

    /** Target bulanan dibagi ke periode Home/Rekap supaya progres tetap bermakna. */
    fun targetForPeriod(monthlyTarget: Double, period: WagePeriod): Double = when (period) {
        WagePeriod.DAILY -> monthlyTarget.coerceAtLeast(0.0) / 30.0
        WagePeriod.WEEKLY -> monthlyTarget.coerceAtLeast(0.0) / 4.0
        WagePeriod.MONTHLY -> monthlyTarget.coerceAtLeast(0.0)
    }

    fun labelOf(period: WagePeriod): String = when (period) {
        WagePeriod.DAILY -> "Harian"
        WagePeriod.WEEKLY -> "Mingguan"
        WagePeriod.MONTHLY -> "Bulanan"
    }
}

/** Ringkasan dengan mode gaji yang dipilih user. Top-level agar pemanggil cukup meng-import satu nama. */
fun List<DailyLog>.toWageSummary(mode: WageMode, flatDailyAmount: Double): MonthlySummary {
    var workDays = 0
    var delivered = 0
    var income = 0.0
    for (raw in this) {
        val clean = raw.sanitized()
        if (!clean.isWorkingDay) continue
        workDays++
        delivered += clean.safeDelivered
        income += WageCalculator.netIncomeOf(clean, mode, flatDailyAmount)
    }
    return MonthlySummary(workDays, delivered, income)
}
