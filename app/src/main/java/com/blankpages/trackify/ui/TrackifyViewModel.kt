package com.blankpages.trackify.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blankpages.trackify.data.TrackifyRepository
import com.blankpages.trackify.data.notification.DailyReminderScheduler
import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.EngineResult
import com.blankpages.trackify.domain.MonthlySummary
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.domain.toMonthlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class AppSettings(
    /** Legacy: tetap dibaca untuk kompatibilitas backup, tetapi tidak dipakai untuk tema. */
    val themeMode: Int = 0,
    val defaultBonus: Double = 0.0,
    val targetAmount: Double = 3_600_000.0,
    val dailyReminderEnabled: Boolean = true,
)

@Immutable
data class TrackifyUiState(
    val defaultRate: Double = 2500.0,
    val settings: AppSettings = AppSettings(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val todayLog: DailyLog = DailyLog(date = LocalDate.now().toString()),
    val yesterdayRemaining: Int = 0,
    /** Log bulan yang sedang dibuka di tab Rekap. */
    val monthlyLogs: List<DailyLog> = emptyList(),
    val summary: MonthlySummary = MonthlySummary(),
    /** Log bulan berjalan, selalu bulan ini (untuk Beranda), terlepas dari bulan yang dipilih di Rekap. */
    val currentMonthLogs: List<DailyLog> = emptyList(),
    val currentMonthSummary: MonthlySummary = MonthlySummary(),
    val locations: List<SavedLocation> = emptyList(),
    val notes: List<CourierNote> = emptyList(),
    val activeNotesCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrackifyViewModel @Inject constructor(private val repository: TrackifyRepository, app: Application) :
    AndroidViewModel(app) {

    private fun readSettings() = repository.readSettings().let { saved ->
        AppSettings(
            themeMode = saved.themeMode,
            defaultBonus = saved.defaultBonus,
            targetAmount = saved.targetAmount,
            dailyReminderEnabled = saved.dailyReminderEnabled,
        )
    }

    // Startup > Single prefs snapshot; avoid repeated SharedPreferences reads.
    private val initialSettings = readSettings()
    private val initialDefaultRate = repository.defaultRate

    init {
        // Alarm > Schedule off-main so binder calls do not block the first frame.
        viewModelScope.launch(Dispatchers.Default) {
            DailyReminderScheduler.schedule(app, initialSettings.dailyReminderEnabled)
        }
    }

    private val currentDateFlow = MutableStateFlow(LocalDate.now().toString())
    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())
    private val defaultRateFlow = MutableStateFlow(initialDefaultRate)
    private val settingsFlow = MutableStateFlow(initialSettings)
    private val overrideTodayLogFlow = MutableStateFlow<DailyLog?>(null)

    private val todayDbLogFlow: Flow<DailyLog> = currentDateFlow
        .flatMapLatest { dateStr ->
            repository.logByDate(dateStr).map { existing ->
                (
                    existing ?: DailyLog(
                        date = dateStr,
                        ratePerPackage = repository.defaultRate,
                        bonus = repository.readSettings().defaultBonus,
                    )
                    ).sanitized()
            }
        }

    private val activeTodayLogFlow: Flow<DailyLog> = combine(
        todayDbLogFlow,
        overrideTodayLogFlow,
    ) { dbLog, override ->
        when {
            override == null -> dbLog

            override.date != dbLog.date -> dbLog

            dbLog.isWorkingDay == override.isWorkingDay &&
                dbLog.packagesCarried == override.packagesCarried &&
                dbLog.packagesDelivered == override.packagesDelivered -> dbLog

            else -> override.sanitized()
        }
    }

    private val currentMonthLogsFlow: Flow<List<DailyLog>> = currentDateFlow
        .map { it.take(7) }
        .distinctUntilChanged()
        .flatMapLatest { ym -> repository.logsByMonth(ym) }

    val uiState: StateFlow<TrackifyUiState> = combine(
        defaultRateFlow,
        selectedMonthFlow,
        activeTodayLogFlow,
        selectedMonthFlow.flatMapLatest { ym -> repository.logsByMonth(ym.toString()) },
        repository.allLocations(),
        repository.allNotes(),
        settingsFlow,
        currentMonthLogsFlow,
    ) { args ->
        val rate = args[0] as Double
        val month = args[1] as YearMonth
        val today = args[2] as DailyLog

        @Suppress("UNCHECKED_CAST")
        val logs = (args[3] as List<DailyLog>).map { it.sanitized() }
        val sum = logs.toMonthlySummary()

        @Suppress("UNCHECKED_CAST")
        val locs = args[4] as List<SavedLocation>

        @Suppress("UNCHECKED_CAST")
        val notesList = args[5] as List<CourierNote>
        val settings = args[6] as AppSettings

        @Suppress("UNCHECKED_CAST")
        val nowLogs = (args[7] as List<DailyLog>).map { it.sanitized() }

        val yesterdayStr = LocalDate.parse(today.date).minusDays(1).toString()
        val yLog = nowLogs.find { it.date == yesterdayStr } ?: logs.find { it.date == yesterdayStr }
        val yRem = if (yLog != null && yLog.isWorkingDay) yLog.remainingPackages else 0
        val activeNotes = notesList.count { !it.isResolved }

        TrackifyUiState(
            defaultRate = rate,
            settings = settings,
            selectedMonth = month,
            todayLog = today,
            yesterdayRemaining = yRem,
            monthlyLogs = logs,
            summary = sum,
            currentMonthLogs = nowLogs,
            currentMonthSummary = nowLogs.toMonthlySummary(),
            locations = locs,
            notes = notesList,
            activeNotesCount = activeNotes,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackifyUiState(
            defaultRate = initialDefaultRate,
            settings = initialSettings,
        ),
    )

    // Section > Dates & months

    fun syncCurrentDate() {
        val nowStr = LocalDate.now().toString()
        if (currentDateFlow.value != nowStr) {
            overrideTodayLogFlow.value = null
            currentDateFlow.value = nowStr
        }
    }

    fun shiftMonth(deltaMonths: Long) {
        selectedMonthFlow.value = selectedMonthFlow.value.plusMonths(deltaMonths)
    }

    // Section > Settings

    fun updateDefaultRate(newRate: Double) {
        val clean = newRate.coerceAtLeast(0.0)
        repository.setDefaultRate(clean)
        defaultRateFlow.value = clean
    }

    fun updateDefaultBonus(newBonus: Double) {
        repository.setDefaultBonus(newBonus)
        settingsFlow.value = settingsFlow.value.copy(defaultBonus = repository.readSettings().defaultBonus)
    }

    fun updateTargetAmount(newTarget: Double) {
        repository.setTargetAmount(newTarget)
        settingsFlow.value = settingsFlow.value.copy(targetAmount = repository.readSettings().targetAmount)
    }

    fun setDailyReminder(enabled: Boolean) {
        repository.setDailyReminder(enabled)
        settingsFlow.value = settingsFlow.value.copy(dailyReminderEnabled = enabled)
        viewModelScope.launch(Dispatchers.Default) {
            DailyReminderScheduler.schedule(getApplication(), enabled)
        }
    }

    // Section > Daily logs

    fun upsertLog(log: DailyLog) {
        val clean = log.sanitized()
        if (clean.date == currentDateFlow.value) {
            overrideTodayLogFlow.value = clean
        }
        viewModelScope.launch {
            repository.upsertLog(clean)
            // State > Skip equal DB re-emits so pending updates are not overwritten.
            if (overrideTodayLogFlow.value == clean) {
                overrideTodayLogFlow.value = null
            }
        }
    }

    fun deleteLog(log: DailyLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }

    fun incrementTodayDelivered(delta: Int = 1): Boolean {
        val step = delta.coerceAtLeast(1)
        val current = uiState.value.todayLog
        if (!current.isWorkingDay) return false
        val nextDelivered = if (current.packagesCarried > 0) {
            (current.packagesDelivered + step).coerceAtMost(current.packagesCarried)
        } else {
            current.packagesDelivered + step
        }
        val nextCarried = current.packagesCarried.coerceAtLeast(nextDelivered)
        if (nextDelivered == current.packagesDelivered && nextCarried == current.packagesCarried) return false
        upsertLog(
            current.copy(
                packagesCarried = nextCarried,
                packagesDelivered = nextDelivered,
            ),
        )
        return true
    }

    fun carryoverYesterdayRemaining() {
        val rem = uiState.value.yesterdayRemaining
        if (rem <= 0) return
        val current = uiState.value.todayLog
        upsertLog(
            current.copy(
                isWorkingDay = true,
                packagesCarried = current.packagesCarried + rem,
            ),
        )
    }

    // Section > Locations & notes

    fun saveLocation(location: SavedLocation, isEdit: Boolean) {
        viewModelScope.launch {
            repository.saveLocation(location, isEdit)
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    fun upsertNote(note: CourierNote) {
        viewModelScope.launch {
            repository.upsertNote(note)
        }
    }

    fun deleteNote(note: CourierNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Section > Export, backup & reset

    fun exportMonthlyCsv(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = TrackifyEngine.encodeMonthlyCsv(uiState.value.monthlyLogs)
            val ok = runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                } != null
            }.getOrDefault(false)
            onResult(if (ok) "CSV berhasil diekspor" else "Gagal mengekspor CSV")
        }
    }

    fun exportBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            // Backup > Export all data, not only the open month.
            val payload = BackupPayload(
                logs = repository.dumpLogs(),
                locations = repository.dumpLocations(),
                notes = repository.dumpNotes(),
                settings = repository.readBackupSettings(),
            )
            when (val res = TrackifyEngine.encodeBackup(LocalDate.now().toString(), payload)) {
                is EngineResult.Ok -> {
                    val ok = runCatching {
                        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(res.value.toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                    onResult(if (ok) "Backup JSON berhasil disimpan" else "Gagal menulis file backup")
                }

                is EngineResult.Err -> onResult(res.message)
            }
        }
    }

    fun importBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val raw = runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                }
            }.getOrNull()
            if (raw.isNullOrBlank()) {
                onResult("File backup kosong atau tidak terbaca")
                return@launch
            }
            when (val decoded = TrackifyEngine.decodeBackup(raw, LocalDate.now().toString())) {
                is EngineResult.Ok -> {
                    repository.restoreSnapshot(decoded.value)
                    repository.restoreBackupSettings(decoded.value.settings)
                    overrideTodayLogFlow.value = null
                    defaultRateFlow.value = repository.defaultRate
                    settingsFlow.value = readSettings()
                    withContext(Dispatchers.Default) {
                        DailyReminderScheduler.schedule(
                            getApplication(),
                            settingsFlow.value.dailyReminderEnabled,
                        )
                    }
                    onResult("Restore JSON berhasil (${decoded.value.logs.size} log)")
                }

                is EngineResult.Err -> onResult("Restore gagal: ${decoded.message}")
            }
        }
    }

    fun resetAllData(onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.clearAllData()
                repository.resetPreferences()
                overrideTodayLogFlow.value = null
                defaultRateFlow.value = repository.defaultRate
                settingsFlow.value = readSettings()
                withContext(Dispatchers.Default) {
                    DailyReminderScheduler.schedule(
                        getApplication(),
                        settingsFlow.value.dailyReminderEnabled,
                    )
                }
            }.onSuccess {
                onResult("Semua data lokal berhasil dihapus")
            }.onFailure { err ->
                onResult("Gagal mereset data: ${err.message}")
            }
        }
    }
}
