package com.blankpages.trackify.data

import com.blankpages.trackify.data.local.AppPreferences
import com.blankpages.trackify.data.local.TrackifyDao
import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.BackupSettings
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.SavedLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

data class TrackifySettings(
    val themeMode: Int = 0,
    val defaultBonus: Double = 0.0,
    val targetAmount: Double = 3_600_000.0,
    val dailyReminderEnabled: Boolean = true,
)

/**
 * Satu pintu masuk untuk data aplikasi. UI/ViewModels tidak lagi berbicara
 * langsung ke DAO maupun SharedPreferences.
 */
@Singleton
class TrackifyRepository @Inject constructor(private val dao: TrackifyDao, private val prefs: AppPreferences) {
    fun logByDate(date: String): Flow<DailyLog?> = dao.getLogByDate(date)
    fun logsByMonth(yearMonth: String): Flow<List<DailyLog>> = dao.getLogsByMonth(yearMonth)
    fun allLocations(): Flow<List<SavedLocation>> = dao.getAllLocations()
    fun allNotes(): Flow<List<CourierNote>> = dao.getAllNotes()

    val defaultRate: Double get() = prefs.defaultRatePerPackage

    fun readSettings() = TrackifySettings(
        themeMode = prefs.themeMode,
        defaultBonus = prefs.defaultBonusAmount,
        targetAmount = prefs.monthlyTargetAmount,
        dailyReminderEnabled = prefs.dailyReminderEnabled,
    )

    fun setDefaultRate(value: Double) {
        prefs.defaultRatePerPackage = value
    }

    fun setDefaultBonus(value: Double) {
        prefs.defaultBonusAmount = value
    }

    fun setTargetAmount(value: Double) {
        prefs.monthlyTargetAmount = value
    }

    fun setDailyReminder(value: Boolean) {
        prefs.dailyReminderEnabled = value
    }

    /** Dipakai VM saat menyusun backup JSON. */
    fun readBackupSettings() = BackupSettings(
        defaultRate = defaultRate,
        defaultBonus = prefs.defaultBonusAmount,
        targetAmount = prefs.monthlyTargetAmount,
        dailyReminderEnabled = prefs.dailyReminderEnabled,
    )

    /** Dipakai VM saat memulihkan backup JSON. */
    fun restoreBackupSettings(settings: BackupSettings) {
        prefs.defaultRatePerPackage = settings.defaultRate
        prefs.defaultBonusAmount = settings.defaultBonus
        prefs.monthlyTargetAmount = settings.targetAmount
        prefs.dailyReminderEnabled = settings.dailyReminderEnabled
    }

    fun resetPreferences() = prefs.resetAll()

    suspend fun upsertLog(log: DailyLog) = dao.upsertLog(log)
    suspend fun deleteLog(log: DailyLog) = dao.deleteLog(log)

    suspend fun saveLocation(location: SavedLocation, isEdit: Boolean) {
        if (isEdit) dao.updateLocation(location) else dao.insertLocation(location)
    }

    suspend fun deleteLocation(location: SavedLocation) = dao.deleteLocation(location)
    suspend fun upsertNote(note: CourierNote) = dao.upsertNote(note)
    suspend fun deleteNote(note: CourierNote) = dao.deleteNote(note)

    suspend fun dumpLogs(): List<DailyLog> = dao.dumpLogs()
    suspend fun dumpLocations(): List<SavedLocation> = dao.dumpLocations()
    suspend fun dumpNotes(): List<CourierNote> = dao.dumpNotes()

    suspend fun restoreSnapshot(payload: BackupPayload) = dao.restoreSnapshot(payload)

    suspend fun clearAllData() {
        dao.clearLogs()
        dao.clearLocations()
        dao.clearNotes()
    }
}
