package com.blankpages.trackify.domain
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.MonthlySummary
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.toMonthlySummary
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

sealed interface EngineResult<out T> {
    data class Ok<T>(val value: T) : EngineResult<T>
    data class Err(val message: String) : EngineResult<Nothing>
}

/** Pengaturan yang ikut cadangan. `themeMode` sengaja tidak disertakan: tema selalu mengikuti sistem. */
data class BackupSettings(
    val defaultRate: Double = 2500.0,
    val defaultBonus: Double = 0.0,
    val targetAmount: Double = 3_600_000.0,
    val dailyReminderEnabled: Boolean = true,
)

data class BackupPayload(
    val logs: List<DailyLog>,
    val locations: List<SavedLocation>,
    val notes: List<CourierNote>,
    val settings: BackupSettings = BackupSettings(),
)

data class PeriodBreakdown(
    val p1WorkDays: Int = 0,
    val p1Delivered: Int = 0,
    val p1GrossIncome: Double = 0.0,
    val p1Expense: Double = 0.0,
    val p1NetIncome: Double = 0.0,
    val p1HalfPayout: Double = 0.0,
    val p1HalfCarryover: Double = 0.0,
    val p2WorkDays: Int = 0,
    val p2Delivered: Int = 0,
    val p2GrossIncome: Double = 0.0,
    val p2Expense: Double = 0.0,
    val p2NetIncome: Double = 0.0,
    val p2TotalPayout: Double = 0.0,
    val avgDeliveredPerWorkDay: Double = 0.0,
    val deliveryEfficiencyPct: Int = 0,
    val periodDiffPackages: Int = 0,
)

data class TieredBonusSummary(
    val tier1Target: Int = 2500,
    val tier1Bonus: Double = 0.0,
    val tier1Reached: Boolean = false,
    val tier1Remaining: Int = 2500,
    val tier2Target: Int = 3000,
    val tier2Bonus: Double = 0.0,
    val tier2Reached: Boolean = false,
    val tier2Remaining: Int = 3000,
    val earnedBonus: Double = 0.0,
)

data class DailyChartBar(
    val day: Int,
    val dateStr: String,
    val delivered: Int,
    val carried: Int,
    val expense: Double,
    val netIncome: Double,
    val isWorkingDay: Boolean,
    val hasLog: Boolean,
)

data class MonthlyChartSummary(
    val bars: List<DailyChartBar> = emptyList(),
    val maxDelivered: Int = 0,
    val bestDay: DailyChartBar? = null,
    val avgDelivered: Double = 0.0,
)

object TrackifyEngine {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private val reportLocale = Locale.forLanguageTag("id-ID")

    fun grossIncomeOf(log: DailyLog): Double {
        val clean = log.sanitized()
        if (!clean.isWorkingDay) return 0.0
        return (clean.safeDelivered * clean.ratePerPackage.coerceAtLeast(0.0)) + clean.bonus.coerceAtLeast(0.0)
    }

    fun buildMonthlyChart(month: YearMonth, logs: List<DailyLog>): MonthlyChartSummary {
        val byDate = logs.associateBy { it.date }
        val daysCount = month.lengthOfMonth()
        val bars = ArrayList<DailyChartBar>(daysCount)
        var maxDlv = 0
        var best: DailyChartBar? = null
        var workDays = 0
        var totalDlv = 0

        for (day in 1..daysCount) {
            val dateStr = month.atDay(day).toString()
            val log = byDate[dateStr]?.sanitized()
            val bar = if (log != null) {
                DailyChartBar(
                    day = day,
                    dateStr = dateStr,
                    delivered = log.safeDelivered,
                    carried = log.safeCarried,
                    expense = log.expense,
                    netIncome = log.netIncome,
                    isWorkingDay = log.isWorkingDay,
                    hasLog = true,
                )
            } else {
                DailyChartBar(
                    day = day,
                    dateStr = dateStr,
                    delivered = 0,
                    carried = 0,
                    expense = 0.0,
                    netIncome = 0.0,
                    isWorkingDay = false,
                    hasLog = false,
                )
            }
            bars.add(bar)
            if (bar.hasLog && bar.isWorkingDay) {
                workDays++
                totalDlv += bar.delivered
                if (bar.delivered > maxDlv) {
                    maxDlv = bar.delivered
                    best = bar
                }
            }
        }

        val avg = if (workDays > 0) totalDlv.toDouble() / workDays else 0.0
        return MonthlyChartSummary(
            bars = bars,
            maxDelivered = maxDlv,
            bestDay = best,
            avgDelivered = avg,
        )
    }

    fun computePeriodBreakdown(logs: List<DailyLog>, workDays: Int): PeriodBreakdown {
        if (logs.isEmpty()) return PeriodBreakdown()
        var p1Days = 0
        var p1Dlv = 0
        var p1Gross = 0.0
        var p1Exp = 0.0
        var p1Net = 0.0

        var p2Days = 0
        var p2Dlv = 0
        var p2Gross = 0.0
        var p2Exp = 0.0
        var p2Net = 0.0

        var totalCarriedWork = 0
        var totalDeliveredWork = 0

        for (raw in logs) {
            val log = raw.sanitized()
            val dayOfMonth = log.date.takeLast(2).toIntOrNull() ?: 1
            val gross = grossIncomeOf(log)
            val exp = if (log.isWorkingDay) log.expense else 0.0
            val net = log.netIncome

            if (log.isWorkingDay) {
                totalCarriedWork += log.safeCarried
                totalDeliveredWork += log.safeDelivered
            }

            if (dayOfMonth <= 15) {
                if (log.isWorkingDay) p1Days++
                p1Dlv += log.safeDelivered
                p1Gross += gross
                p1Exp += exp
                p1Net += net
            } else {
                if (log.isWorkingDay) p2Days++
                p2Dlv += log.safeDelivered
                p2Gross += gross
                p2Exp += exp
                p2Net += net
            }
        }

        val p1Half = p1Net * 0.5
        val p1Rem = p1Net - p1Half
        val p2Payout = p1Rem + p2Net
        val totalDlv = p1Dlv + p2Dlv
        val avgPerDay = if (workDays > 0) totalDlv.toDouble() / workDays else 0.0
        val effPct = if (totalCarriedWork > 0) {
            ((totalDeliveredWork.toDouble() / totalCarriedWork.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        return PeriodBreakdown(
            p1WorkDays = p1Days,
            p1Delivered = p1Dlv,
            p1GrossIncome = p1Gross,
            p1Expense = p1Exp,
            p1NetIncome = p1Net,
            p1HalfPayout = p1Half,
            p1HalfCarryover = p1Rem,
            p2WorkDays = p2Days,
            p2Delivered = p2Dlv,
            p2GrossIncome = p2Gross,
            p2Expense = p2Exp,
            p2NetIncome = p2Net,
            p2TotalPayout = p2Payout,
            avgDeliveredPerWorkDay = avgPerDay,
            deliveryEfficiencyPct = effPct,
            periodDiffPackages = p2Dlv - p1Dlv,
        )
    }

    fun formatMonthlyWhatsappReport(
        monthLabel: String,
        logs: List<DailyLog>,
        summary: MonthlySummary,
        breakdown: PeriodBreakdown,
        tieredBonus: TieredBonusSummary = TieredBonusSummary(),
    ): String = buildString(448) {
        val totalGross = breakdown.p1GrossIncome + breakdown.p2GrossIncome
        val totalExpense = breakdown.p1Expense + breakdown.p2Expense
        val totalNetWithBonus = summary.totalNetIncome + tieredBonus.earnedBonus
        val p2WithBonus = breakdown.p2TotalPayout + tieredBonus.earnedBonus
        appendLine("*REKAP BULANAN KURIR - TRACKIFY*")
        appendLine("Bulan      : $monthLabel")
        appendLine("Hari Kerja : ${summary.totalWorkingDays} hari")
        appendLine(
            "Terkirim   : ${summary.totalDelivered} paket (Avg ${"%.1f".format(
                reportLocale,
                breakdown.avgDeliveredPerWorkDay,
            )}/hr • ${breakdown.deliveryEfficiencyPct}%)",
        )
        appendLine(
            "Gajian 1 (1-15 ÷ 2) : ${breakdown.p1HalfPayout.toRupiah()} (Full 1-15: ${breakdown.p1NetIncome.toRupiah()})",
        )
        appendLine(
            "Gajian 2 (16-Akh)   : ${p2WithBonus.toRupiah()} (Sisa P1: ${breakdown.p1HalfCarryover.toRupiah()} + P2: ${breakdown.p2NetIncome.toRupiah()})",
        )
        if (tieredBonus.earnedBonus > 0.0) {
            appendLine("Bonus Tier : +${tieredBonus.earnedBonus.toRupiah()}")
        }
        appendLine("Kotor      : ${totalGross.toRupiah()}")
        appendLine("Potongan   : -${totalExpense.toRupiah()}")
        append("Bersih     : ${totalNetWithBonus.toRupiah()}")
    }

    fun encodeMonthlyCsv(logs: List<DailyLog>): String = buildString(logs.size * 96 + 192) {
        appendLine(
            "Tanggal,Status,Paket_Dibawa,Paket_Terkirim,Sisa_Paket,Tarif_Satuan,Bonus,Pendapatan_Kotor,Potongan,Pendapatan_Bersih,Catatan",
        )
        var sumCarried = 0
        var sumDelivered = 0
        var sumRemaining = 0
        var sumBonus = 0
        var sumGross = 0
        var sumExpense = 0
        var sumNet = 0
        logs.sortedBy { it.date }.forEach { raw ->
            val l = raw.sanitized()
            val status = if (l.isWorkingDay) "MASUK" else "LIBUR"
            val gross = grossIncomeOf(l)
            val cleanNotes = l.notes.replace(",", ";").replace("\n", " ").trim()
            sumCarried += l.safeCarried
            sumDelivered += l.safeDelivered
            sumRemaining += l.remainingPackages
            sumBonus += l.bonus.toInt()
            sumGross += gross.toInt()
            sumExpense += l.expense.toInt()
            sumNet += l.netIncome.toInt()
            appendLine(
                "${l.date},$status,${l.safeCarried},${l.safeDelivered},${l.remainingPackages},${l.ratePerPackage.toInt()},${l.bonus.toInt()},${gross.toInt()},${l.expense.toInt()},${l.netIncome.toInt()},\"$cleanNotes\"",
            )
        }
        appendLine(
            "TOTAL_BULAN,-,$sumCarried,$sumDelivered,$sumRemaining,-,$sumBonus,$sumGross,$sumExpense,$sumNet,\"\"",
        )
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat * 0.5).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon * 0.5).pow(2)
        return 2.0 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun formatDistance(meters: Double): String =
        if (meters < 1000.0) "${meters.roundToInt()} m" else "%.1f km".format(meters / 1000.0)

    fun filterLocations(
        items: List<SavedLocation>,
        query: String,
        currentLat: Double?,
        currentLon: Double?,
    ): List<Pair<SavedLocation, Double?>> {
        if (items.isEmpty()) return emptyList()
        val q = query.trim()
        val hasCoords = currentLat != null && currentLon != null
        val result = ArrayList<Pair<SavedLocation, Double?>>(items.size)
        for (loc in items) {
            if (q.isNotEmpty() &&
                !loc.customerName.contains(q, ignoreCase = true) &&
                !loc.landmarkNote.contains(q, ignoreCase = true)
            ) {
                continue
            }
            val dist = if (hasCoords) distanceMeters(currentLat!!, currentLon!!, loc.latitude, loc.longitude) else null
            result.add(loc to dist)
        }
        if (hasCoords && result.size > 1) {
            result.sortWith { a, b -> (a.second ?: Double.MAX_VALUE).compareTo(b.second ?: Double.MAX_VALUE) }
        }
        return result
    }

    fun filterNotes(
        items: List<CourierNote>,
        query: String,
        filterMode: Int = 0,
        locations: List<SavedLocation> = emptyList(),
    ): List<CourierNote> {
        if (items.isEmpty()) return emptyList()
        val q = query.trim()
        val locationNames = locations.associate { it.id to it.customerName }
        val result = ArrayList<CourierNote>(items.size)
        for (note in items) {
            val matchesStatus = when (filterMode) {
                1 -> !note.isResolved
                2 -> note.isResolved
                else -> true
            }
            if (!matchesStatus) continue
            if (q.isNotEmpty() &&
                !note.title.contains(q, ignoreCase = true) &&
                !note.content.contains(q, ignoreCase = true) &&
                locationNames[note.linkedLocationId]?.contains(q, ignoreCase = true) != true
            ) {
                continue
            }
            result.add(note)
        }
        return result
    }

    fun encodeBackup(today: String, payload: BackupPayload): EngineResult<String> = runCatching {
        JSONObject().apply {
            put("app", "Trackify")
            put("schema", 7)
            put("exportedAt", today)
            put(
                "settings",
                JSONObject().apply {
                    put("defaultRate", payload.settings.defaultRate)
                    put("defaultBonus", payload.settings.defaultBonus)
                    put("targetAmount", payload.settings.targetAmount)
                    put("dailyReminderEnabled", payload.settings.dailyReminderEnabled)
                },
            )
            put(
                "daily_logs",
                JSONArray().apply {
                    payload.logs.forEach { rawLog ->
                        val l = rawLog.sanitized()
                        put(
                            JSONObject().apply {
                                put("date", l.date)
                                put("isWorkingDay", l.isWorkingDay)
                                put("packagesCarried", l.packagesCarried)
                                put("packagesDelivered", l.packagesDelivered)
                                put("ratePerPackage", l.ratePerPackage)
                                put("bonus", l.bonus)
                                put("expense", l.expense)
                                put("notes", l.notes)
                            },
                        )
                    }
                },
            )
            put(
                "saved_locations",
                JSONArray().apply {
                    payload.locations.forEach { loc ->
                        put(
                            JSONObject().apply {
                                put("id", loc.id)
                                put("customerName", loc.customerName)
                                put("landmarkNote", loc.landmarkNote)
                                put("latitude", loc.latitude)
                                put("longitude", loc.longitude)
                                put("photoPath", loc.photoPath)
                            },
                        )
                    }
                },
            )
            put(
                "courier_notes",
                JSONArray().apply {
                    payload.notes.forEach { n ->
                        put(
                            JSONObject().apply {
                                put("id", n.id)
                                put("title", n.title)
                                put("content", n.content)
                                put("isPinned", n.isPinned)
                                put("updatedAt", n.updatedAt)
                                put("linkedLocationId", n.linkedLocationId ?: JSONObject.NULL)
                                put("isResolved", n.isResolved)
                            },
                        )
                    }
                },
            )
        }.toString(2)
    }.fold(
        onSuccess = { EngineResult.Ok(it) },
        onFailure = { EngineResult.Err("Encode gagal: ${it.localizedMessage}") },
    )

    fun decodeBackup(raw: String, fallbackDate: String): EngineResult<BackupPayload> = runCatching {
        val root = JSONObject(raw)
        require(root.optString("app") == "Trackify") { "Header file tidak cocok (bukan backup Trackify)" }

        val logs = root.optJSONArray("daily_logs").mapObjects { o ->
            val dateStr = o.optString("date").trim()
            require(dateStr.isNotEmpty()) { "Tanggal pada salah satu entri rekap kosong" }
            DailyLog(
                date = dateStr,
                isWorkingDay = o.optBoolean("isWorkingDay", true),
                packagesCarried = o.optInt("packagesCarried").coerceAtLeast(0),
                packagesDelivered = o.optInt("packagesDelivered").coerceAtLeast(0),
                ratePerPackage = o.optDouble("ratePerPackage", 2500.0).coerceAtLeast(0.0),
                bonus = o.optDouble("bonus").coerceAtLeast(0.0),
                expense = o.optDouble("expense").coerceAtLeast(0.0),
                notes = o.optString("notes"),
            ).sanitized()
        }
        val locs = root.optJSONArray("saved_locations").mapObjects { o ->
            SavedLocation(
                id = o.optInt("id"),
                customerName = o.getString("customerName").trim(),
                landmarkNote = o.optString("landmarkNote").trim(),
                latitude = o.getDouble("latitude"),
                longitude = o.getDouble("longitude"),
                photoPath = o.optString("photoPath"),
            )
        }
        val notes = root.optJSONArray("courier_notes").mapObjects { o ->
            CourierNote(
                id = o.optInt("id"),
                title = o.getString("title").trim(),
                content = o.getString("content").trim(),
                isPinned = o.optBoolean("isPinned"),
                updatedAt = o.optString("updatedAt", fallbackDate),
                linkedLocationId = if (o.isNull("linkedLocationId")) null else o.optInt("linkedLocationId"),
                isResolved = o.optBoolean("isResolved", false),
            )
        }
        // Legacy backup > Schema 5 has no settings; fall back to defaults.
        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj == null) {
            BackupSettings()
        } else {
            BackupSettings(
                defaultRate = settingsObj.optDouble("defaultRate", 2500.0).coerceAtLeast(0.0),
                defaultBonus = settingsObj.optDouble("defaultBonus", 0.0).coerceAtLeast(0.0),
                targetAmount = settingsObj.optDouble("targetAmount", 3_600_000.0).coerceAtLeast(0.0),
                dailyReminderEnabled = settingsObj.optBoolean("dailyReminderEnabled", true),
            )
        }
        BackupPayload(logs, locs, notes, settings)
    }.fold(
        onSuccess = { EngineResult.Ok(it) },
        onFailure = { EngineResult.Err(it.localizedMessage ?: "Format JSON rusak") },
    )

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else List(length()) { transform(getJSONObject(it)) }
}
