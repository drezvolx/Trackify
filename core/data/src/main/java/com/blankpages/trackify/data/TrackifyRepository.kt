package com.blankpages.trackify.data

import androidx.room.Transaction
import com.blankpages.trackify.data.local.AppPreferences
import com.blankpages.trackify.data.local.TrackifyDao
import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.BackupSettings
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.NotePhoto
import com.blankpages.trackify.domain.ReminderTime
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.WageMode
import com.blankpages.trackify.domain.WagePeriod
import java.io.File
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

const val MAX_PHOTOS_PER_NOTE = 5

data class TrackifySettings(
    val themeMode: Int = 0,
    val defaultBonus: Double = 0.0,
    val targetAmount: Double = 3_600_000.0,
    val dailyReminderEnabled: Boolean = true,
    val wageMode: WageMode = WageMode.PER_PACKAGE,
    val flatDailyAmount: Double = 2_500.0,
    val wagePeriod: WagePeriod = WagePeriod.MONTHLY,
    val paydayDate: Int = 25,
    val reminderTime: ReminderTime = ReminderTime.DEFAULT,
)

/**
 * Satu pintu masuk untuk data aplikasi. UI/ViewModels tidak lagi berbicara
 * langsung ke DAO maupun SharedPreferences.
 */
@Singleton
class TrackifyRepository @Inject constructor(private val dao: TrackifyDao, private val prefs: AppPreferences) {
    fun logByDate(date: String): Flow<DailyLog?> = dao.getLogByDate(date)
    fun logsByMonth(yearMonth: String): Flow<List<DailyLog>> {
        val month = runCatching { YearMonth.parse(yearMonth) }.getOrNull() ?: return flowOf(emptyList())
        return dao.getLogsByRange(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())
    }
    fun allLocations(): Flow<List<SavedLocation>> = dao.getAllLocations()
    fun allNotes(): Flow<List<CourierNote>> = dao.getAllNotes()
    fun allNotePhotos(): Flow<List<NotePhoto>> = dao.getAllNotePhotos()

    val defaultRate: Double get() = prefs.defaultRatePerPackage

    fun readSettings() = TrackifySettings(
        themeMode = prefs.themeMode,
        defaultBonus = prefs.defaultBonusAmount,
        targetAmount = prefs.monthlyTargetAmount,
        dailyReminderEnabled = prefs.dailyReminderEnabled,
        wageMode = prefs.wageMode,
        flatDailyAmount = prefs.flatDailyAmount,
        wagePeriod = prefs.wagePeriod,
        paydayDate = prefs.paydayDate,
        reminderTime = prefs.reminderTime,
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

    fun setWageMode(value: WageMode) {
        prefs.wageMode = value
    }

    fun setFlatDailyAmount(value: Double) {
        prefs.flatDailyAmount = value
    }

    fun setWagePeriod(value: WagePeriod) {
        prefs.wagePeriod = value
    }

    fun setPaydayDate(value: Int) {
        prefs.paydayDate = value
    }

    fun setReminderTime(value: ReminderTime) {
        prefs.reminderTime = value
    }

    /** Dipakai VM saat menyusun backup JSON. */
    fun readBackupSettings() = BackupSettings(
        defaultRate = defaultRate,
        defaultBonus = prefs.defaultBonusAmount,
        targetAmount = prefs.monthlyTargetAmount,
        dailyReminderEnabled = prefs.dailyReminderEnabled,
        wageMode = prefs.wageMode,
        flatDailyAmount = prefs.flatDailyAmount,
        wagePeriod = prefs.wagePeriod,
        paydayDate = prefs.paydayDate,
        reminderTime = prefs.reminderTime,
    )

    /** Dipakai VM saat memulihkan backup JSON. */
    fun restoreBackupSettings(settings: BackupSettings) {
        prefs.defaultRatePerPackage = settings.defaultRate
        prefs.defaultBonusAmount = settings.defaultBonus
        prefs.monthlyTargetAmount = settings.targetAmount
        prefs.dailyReminderEnabled = settings.dailyReminderEnabled
        prefs.wageMode = settings.wageMode
        prefs.flatDailyAmount = settings.flatDailyAmount
        prefs.wagePeriod = settings.wagePeriod
        prefs.paydayDate = settings.paydayDate
        prefs.reminderTime = settings.reminderTime
    }

    fun resetPreferences() = prefs.resetAll()

    suspend fun upsertLog(log: DailyLog) = dao.upsertLog(log)
    suspend fun deleteLog(log: DailyLog) = dao.deleteLog(log)

    suspend fun saveLocation(location: SavedLocation, isEdit: Boolean) {
        if (isEdit) dao.updateLocation(location) else dao.insertLocation(location)
    }

    @Transaction
    suspend fun deleteLocation(location: SavedLocation) {
        dao.unlinkNotesFromLocation(location.id)
        dao.deleteLocation(location)
    }

    @Transaction
    suspend fun upsertNoteWithPhotos(note: CourierNote, paths: List<String>): Int {
        val noteId = dao.upsertNote(note).toInt()
        dao.deleteNotePhotos(noteId)
        val safePaths = paths.distinct().filter { it.isNotBlank() }.take(MAX_PHOTOS_PER_NOTE)
        if (safePaths.isNotEmpty()) {
            dao.upsertNotePhotos(
                safePaths.mapIndexed { index, path ->
                    NotePhoto(noteId = noteId, path = path, sortOrder = index)
                },
            )
        }
        return noteId
    }

    /** File dihapus setelah commit DB berhasil, supaya kegagalan transaksi tidak kehilangan foto. */
    suspend fun deleteNoteWithPhotos(note: CourierNote, paths: List<String>) {
        deleteNoteFromDatabase(note)
        paths.forEach { runCatching { File(it).delete() } }
    }

    @Transaction
    internal suspend fun deleteNoteFromDatabase(note: CourierNote) {
        dao.deleteNotePhotos(note.id)
        dao.deleteNote(note)
    }

    suspend fun dumpLogs(): List<DailyLog> = dao.dumpLogs()
    suspend fun dumpLocations(): List<SavedLocation> = dao.dumpLocations()
    suspend fun dumpNotes(): List<CourierNote> = dao.dumpNotes()
    suspend fun dumpNotePhotos(): List<NotePhoto> = dao.dumpNotePhotos()

    suspend fun restoreSnapshot(payload: BackupPayload) = dao.restoreSnapshot(payload)

    @Transaction
    suspend fun clearAllData() {
        dao.clearLogs()
        dao.clearLocations()
        dao.clearNotePhotos()
        dao.clearNotes()
    }
}
