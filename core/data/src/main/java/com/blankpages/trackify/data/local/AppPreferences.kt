package com.blankpages.trackify.data.local

import android.content.Context
import com.blankpages.trackify.domain.WageMode
import com.blankpages.trackify.domain.WagePeriod

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("trackify_prefs", Context.MODE_PRIVATE)

    var defaultRatePerPackage: Double
        get() = Double.fromBits(prefs.getLong(KEY_DEFAULT_RATE, 2500.0.toBits())).coerceAtLeast(0.0)
        set(value) {
            prefs.edit().putLong(KEY_DEFAULT_RATE, value.coerceAtLeast(0.0).toBits()).apply()
        }

    /** Bonus masuk per hari kerja (Rp). */
    var defaultBonusAmount: Double
        get() = Double.fromBits(prefs.getLong(KEY_DEFAULT_BONUS, 0.0.toBits())).coerceAtLeast(0.0)
        set(value) {
            prefs.edit().putLong(KEY_DEFAULT_BONUS, value.coerceAtLeast(0.0).toBits()).apply()
        }

    /** Target pendapatan bersih bulanan (Rp). */
    var monthlyTargetAmount: Double
        get() = Double.fromBits(prefs.getLong(KEY_TARGET_AMOUNT, 3_600_000.0.toBits())).coerceAtLeast(0.0)
        set(value) {
            prefs.edit().putLong(KEY_TARGET_AMOUNT, value.coerceAtLeast(0.0).toBits()).apply()
        }

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DAILY_REMINDER, value).apply()
        }

    /** Cara hitung gaji. Default mengikuti perilaku lama: tarif × paket terkirim. */
    var wageMode: WageMode
        get() = runCatching {
            WageMode.valueOf(prefs.getString(KEY_WAGE_MODE, WageMode.PER_PACKAGE.name)!!)
        }.getOrDefault(WageMode.PER_PACKAGE)
        set(value) {
            prefs.edit().putString(KEY_WAGE_MODE, value.name).apply()
        }

    /** Nominal tetap per hari kerja, dipakai saat [wageMode] = FLAT_DAILY. */
    var flatDailyAmount: Double
        get() = Double.fromBits(prefs.getLong(KEY_FLAT_DAILY_AMOUNT, 2_500.0.toBits())).coerceAtLeast(0.0)
        set(value) {
            prefs.edit().putLong(KEY_FLAT_DAILY_AMOUNT, value.coerceAtLeast(0.0).toBits()).apply()
        }

    /** Periode tampilan Home/Rekap. Default Bulanan seperti perilaku lama. */
    var wagePeriod: WagePeriod
        get() = runCatching {
            WagePeriod.valueOf(prefs.getString(KEY_WAGE_PERIOD, WagePeriod.MONTHLY.name)!!)
        }.getOrDefault(WagePeriod.MONTHLY)
        set(value) {
            prefs.edit().putString(KEY_WAGE_PERIOD, value.name).apply()
        }

    /** Tanggal gajian pada periode Bulanan (1–31). */
    var paydayDate: Int
        get() = prefs.getInt(KEY_PAYDAY_DATE, 25).coerceIn(1, 31)
        set(value) {
            prefs.edit().putInt(KEY_PAYDAY_DATE, value.coerceIn(1, 31)).apply()
        }

    /** Legacy: dipertahankan hanya untuk kompatibilitas backup. Tema aplikasi selalu mengikuti sistem. */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0).coerceIn(0, 2)
        set(value) {
            prefs.edit().putInt(KEY_THEME_MODE, value.coerceIn(0, 2)).apply()
        }

    /** Kembalikan semua preferensi ke nilai awal (dipakai saat reset data). */
    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_DEFAULT_RATE = "default_rate_per_package"
        private const val KEY_DEFAULT_BONUS = "default_bonus_amount"
        private const val KEY_TARGET_AMOUNT = "monthly_target_amount"
        private const val KEY_DAILY_REMINDER = "daily_reminder_enabled"
        private const val KEY_WAGE_MODE = "wage_mode"
        private const val KEY_FLAT_DAILY_AMOUNT = "flat_daily_amount"
        private const val KEY_WAGE_PERIOD = "wage_period"
        private const val KEY_PAYDAY_DATE = "payday_date"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
