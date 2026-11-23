package com.blankpages.trackify.ui.spreadsheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.MonthlySummary
import com.blankpages.trackify.domain.PeriodBreakdown
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.domain.WageCalculator
import com.blankpages.trackify.domain.WageMode
import com.blankpages.trackify.domain.WagePeriod
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyGradients
import com.blankpages.trackify.ui.theme.TrackifyIcons
import com.blankpages.trackify.ui.theme.trackifyFieldColors
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val idLocale = Locale.forLanguageTag("id-ID")
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)

private fun sanitizeDecimalInput(value: String): String {
    val normalized = value.replace(',', '.')
    val firstDot = normalized.indexOf('.')
    if (firstDot < 0) return normalized.filter { it.isDigit() }
    val before = normalized.substring(0, firstDot).filter { it.isDigit() }
    val after = normalized.substring(firstDot + 1).filter { it.isDigit() }
    return "$before.$after"
}
private val shortMonthFmt = DateTimeFormatter.ofPattern("MMM", idLocale)
private val shortDayFmt = DateTimeFormatter.ofPattern("EEE", idLocale)

private fun Double.formatIdr(): String {
    val nf = NumberFormat.getCurrencyInstance(idLocale)
    nf.maximumFractionDigits = 0
    nf.minimumFractionDigits = 0
    return nf.format(this).replace("Rp", "Rp").replace("\u00a0", "")
}

private fun Double.formatCompactK(): String {
    val rounded = this.roundToInt()
    val sign = if (rounded < 0) "-" else ""
    val absVal = abs(rounded)
    return if (absVal >= 1000) {
        val kVal = absVal / 1000.0
        val formatted = if (kVal % 1.0 == 0.0) kVal.toInt().toString() else "%.1f".format(idLocale, kVal)
        "${sign}${formatted}k"
    } else {
        "${sign}$absVal"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetScreen(
    todayLog: DailyLog,
    yesterdayRemaining: Int,
    defaultRate: Double,
    selectedMonth: YearMonth,
    monthlyLogs: List<DailyLog>,
    summary: MonthlySummary,
    targetAmount: Double = 3_600_000.0,
    wageMode: WageMode = WageMode.PER_PACKAGE,
    flatDailyAmount: Double = 2_500.0,
    wagePeriod: WagePeriod = WagePeriod.MONTHLY,
    onUpdateTarget: (Double) -> Unit = {},
    onShiftMonth: (Long) -> Unit,
    onCarryoverYesterday: () -> Unit,
    onExportCsv: (Uri, (String) -> Unit) -> Unit,
    onUpdateLog: (DailyLog) -> Unit,
    onDeleteLog: (DailyLog) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var viewMode by rememberSaveable { mutableIntStateOf(0) }
    var sortDesc by rememberSaveable { mutableStateOf(true) }
    var filterMode by rememberSaveable { mutableIntStateOf(0) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var editingLog by remember { mutableStateOf<DailyLog?>(null) }
    var pendingDeleteLog by remember { mutableStateOf<DailyLog?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTargetSheet by remember { mutableStateOf(false) }
    var targetRupiah by remember(targetAmount) { mutableDoubleStateOf(targetAmount) }

    val csvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            onExportCsv(uri) { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val monthLabel = remember(selectedMonth) {
        selectedMonth.atDay(1).format(monthFmt).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(idLocale) else it.toString()
        }
    }

    val breakdown = remember(monthlyLogs, summary.totalWorkingDays) {
        TrackifyEngine.computePeriodBreakdown(monthlyLogs, summary.totalWorkingDays)
    }

    val chartSummary = remember(selectedMonth, monthlyLogs) {
        TrackifyEngine.buildMonthlyChart(selectedMonth, monthlyLogs)
    }

    val logsByDate = remember(monthlyLogs) {
        monthlyLogs.associate { it.date to it.sanitized() }
    }

    val grossTotal = remember(monthlyLogs, wageMode, flatDailyAmount) {
        monthlyLogs.sumOf { raw -> WageCalculator.grossIncomeOf(raw, wageMode, flatDailyAmount) }
    }

    val expenseTotal = remember(monthlyLogs) {
        monthlyLogs.sumOf { raw ->
            val l = raw.sanitized()
            if (l.isWorkingDay) l.expense else 0.0
        }
    }

    val p1Net = remember(monthlyLogs, wageMode, flatDailyAmount) {
        monthlyLogs.sumOf { raw ->
            val l = raw.sanitized()
            val d = l.date.takeLast(2).toIntOrNull() ?: 1
            if (l.isWorkingDay && d <= 15) WageCalculator.netIncomeOf(l, wageMode, flatDailyAmount) else 0.0
        }
    }

    val p2Net = remember(monthlyLogs, wageMode, flatDailyAmount) {
        monthlyLogs.sumOf { raw ->
            val l = raw.sanitized()
            val d = l.date.takeLast(2).toIntOrNull() ?: 1
            if (l.isWorkingDay && d > 15) WageCalculator.netIncomeOf(l, wageMode, flatDailyAmount) else 0.0
        }
    }

    val gajian1 = p1Net * 0.5
    val gajian2 = p2Net + (p1Net * 0.5)
    val avgPerWorkDay = if (summary.totalWorkingDays > 0) {
        summary.totalDelivered.toDouble() / summary.totalWorkingDays
    } else {
        0.0
    }

    val targetPct = if (targetRupiah > 0.0) {
        ((summary.totalNetIncome / targetRupiah).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val filteredLogs = remember(monthlyLogs, filterMode, sortDesc, searchQuery) {
        val q = searchQuery.trim()
        monthlyLogs
            .map { it.sanitized() }
            .filter { log ->
                val matchesFilter = when (filterMode) {
                    1 -> log.isWorkingDay
                    2 -> !log.isWorkingDay
                    3 -> log.remainingPackages > 0
                    else -> true
                }
                if (!matchesFilter) return@filter false
                if (q.isEmpty()) return@filter true
                val dayNum = log.date.takeLast(2).toIntOrNull()
                val dayLabel = if (dayNum != null && dayNum in 1..selectedMonth.lengthOfMonth()) {
                    val dt = selectedMonth.atDay(dayNum)
                    "${dt.format(shortDayFmt)}, $dayNum ${dt.format(shortMonthFmt)}"
                } else {
                    ""
                }
                log.date.contains(q, ignoreCase = true) ||
                    dayLabel.contains(q, ignoreCase = true) ||
                    log.notes.contains(q, ignoreCase = true) ||
                    log.safeDelivered.toString().contains(q)
            }
            .let { list ->
                if (sortDesc) list.sortedByDescending { it.date } else list.sortedBy { it.date }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TrackifyGradients.background()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(
                    text = "Rekap",
                    modifier = Modifier.weight(1f),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetPillAction(text = "Tanggal", onClick = { showDatePicker = true })
                    SheetPillAction(
                        text = "+ Log",
                        onClick = {
                            val defaultDate = if (YearMonth.now() == selectedMonth) {
                                LocalDate.now().toString()
                            } else {
                                selectedMonth.atDay(1).toString()
                            }
                            editingLog = logsByDate[defaultDate] ?: DailyLog(
                                date = defaultDate,
                                ratePerPackage = defaultRate,
                            )
                        },
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SheetCircleIconButton(onClick = { onShiftMonth(-1L) }) {
                        Icon(
                            imageVector = TrackifyIcons.ChevronLeft,
                            contentDescription = "Bulan sebelumnya",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Text(
                        text = monthLabel,
                        fontSize = 19.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )

                    SheetCircleIconButton(onClick = { onShiftMonth(1L) }) {
                        Icon(
                            imageVector = TrackifyIcons.ChevronRight,
                            contentDescription = "Bulan berikutnya",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    listOf("Ringkasan", "Grafik").forEachIndexed { idx, title ->
                        val active = subTab == idx
                        Column(
                            modifier = Modifier
                                .clickable { subTab = idx }
                                .padding(top = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (active) scheme.onSurface else scheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(2.dp)
                                    .background(if (active) scheme.onSurface else Color.Transparent),
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
            }
        }

        if (subTab == 0) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ringkasan",
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )

                    SheetCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SheetStatItem(
                                    "${summary.totalWorkingDays}",
                                    "Hari kerja",
                                    modifier = Modifier.weight(1f),
                                )
                                SheetStatItem("${summary.totalDelivered}", "Terkirim", modifier = Modifier.weight(1f))
                                SheetStatItem(
                                    "%.1f".format(idLocale, avgPerWorkDay),
                                    "Rata-rata/hari",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SheetStatItem(grossTotal.formatIdr(), "Kotor", modifier = Modifier.weight(1f))
                                SheetStatItem(expenseTotal.formatIdr(), "Potongan", modifier = Modifier.weight(1f))
                                SheetStatItem(
                                    summary.totalNetIncome.formatIdr(),
                                    "Bersih",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Estimasi",
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onBackground,
                        )
                        Text(
                            text = "Total ${summary.totalNetIncome.formatIdr()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSurfaceVariant,
                        )
                    }

                    SheetCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val totalSplit = (abs(gajian1) + abs(gajian2)).coerceAtLeast(1.0)
                            val w1 = (abs(gajian1) / totalSplit).toFloat().coerceIn(0.25f, 0.75f)
                            val w2 = (1f - w1).coerceAtLeast(0.25f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(w1)
                                        .fillMaxHeight()
                                        .background(scheme.onSurface)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        "Periode 1",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.surface,
                                        maxLines = 1,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(w2)
                                        .fillMaxHeight()
                                        .background(scheme.surfaceVariant)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        "Periode 2",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SheetStatItem(
                                    value = gajian1.formatIdr(),
                                    label = "Tanggal 15 (${breakdown.p1Delivered} pkt)",
                                    large = true,
                                    modifier = Modifier.weight(1f),
                                )
                                SheetStatItem(
                                    value = gajian2.formatIdr(),
                                    label = "Akhir Bulan (${breakdown.p2Delivered} pkt)",
                                    large = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Target",
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onBackground,
                        )
                        SheetPillAction(text = "Atur", onClick = { showTargetSheet = true })
                    }

                    SheetCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = summary.totalNetIncome.formatIdr(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    text = "dari ${targetRupiah.formatIdr()} • ${WageCalculator.labelOf(wagePeriod)}",
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            }

                            SheetProgressBar(progress = targetPct, gradient = false)

                            val pctInt = (targetPct * 100).roundToInt()
                            val dayNow = LocalDate.now().dayOfMonth.coerceAtLeast(1)
                            val dim = selectedMonth.lengthOfMonth()
                            val showProj = YearMonth.now() == selectedMonth && summary.totalNetIncome > 0.0
                            val projVal = (summary.totalNetIncome / dayNow) * dim

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("$pctInt% tercapai", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                if (showProj) {
                                    Text("${projVal.formatIdr()}", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paket terkirim per hari",
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )

                    SheetCard {
                        val workingDays = monthlyLogs.map { it.sanitized() }.filter { it.isWorkingDay }
                        if (workingDays.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Grafik muncul setelah ada log hari kerja.",
                                    fontSize = 13.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            val maxDelivered = workingDays.maxOf { it.safeDelivered }.coerceAtLeast(1)
                            val peakDay = chartSummary.bestDay
                            val daysCount = selectedMonth.lengthOfMonth()

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    for (d in 1..daysCount) {
                                        val dateStr = selectedMonth.atDay(d).toString()
                                        val log = logsByDate[dateStr]
                                        val sentVal = if (log != null && log.isWorkingDay) log.safeDelivered else 0
                                        val ratio = (sentVal.toFloat() / maxDelivered).coerceIn(0f, 1f)
                                        val isPeak = peakDay != null && peakDay.dateStr == dateStr && sentVal > 0

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.BottomCenter,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(
                                                        if (sentVal >
                                                            0
                                                        ) {
                                                            ratio.coerceAtLeast(0.06f)
                                                        } else {
                                                            0.02f
                                                        },
                                                    )
                                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                                    .background(
                                                        if (isPeak) scheme.onSurface else scheme.surfaceVariant,
                                                    ),
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SheetStatItem(
                                        value = "%.1f paket".format(idLocale, avgPerWorkDay),
                                        label = "Rata-rata per hari",
                                        large = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    SheetStatItem(
                                        value = if (peakDay != null) {
                                            "tgl ${peakDay.dateStr.takeLast(
                                                2,
                                            ).toIntOrNull() ?: 1} · ${peakDay.delivered}"
                                        } else {
                                            "-"
                                        },
                                        label = "Hari Puncak",
                                        large = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (YearMonth.now() == selectedMonth) {
            item {
                val activeToday = todayLog.sanitized()
                val dateObj = runCatching { LocalDate.parse(activeToday.date) }.getOrElse { LocalDate.now() }
                val dayHeader = "${dateObj.format(shortDayFmt).replaceFirstChar {
                    it.titlecase(idLocale)
                }}, ${dateObj.dayOfMonth} ${dateObj.format(shortMonthFmt)}"

                var carriedText by remember(activeToday.date, activeToday.safeCarried) {
                    mutableStateOf(activeToday.safeCarried.toString())
                }
                var deliveredText by remember(activeToday.date, activeToday.safeDelivered) {
                    mutableStateOf(activeToday.safeDelivered.toString())
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Hari ini",
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )

                    SheetCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    dayHeader,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(scheme.surfaceVariant)
                                        .padding(2.dp),
                                ) {
                                    listOf(true to "Masuk", false to "Libur").forEach { (stateVal, labelTxt) ->
                                        val sel = activeToday.isWorkingDay == stateVal
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (sel) scheme.onSurface else Color.Transparent)
                                                .clickable {
                                                    val next = if (stateVal) {
                                                        activeToday.copy(isWorkingDay = true)
                                                    } else {
                                                        activeToday.copy(
                                                            isWorkingDay = false,
                                                            packagesCarried = 0,
                                                            packagesDelivered = 0,
                                                            expense = 0.0,
                                                        )
                                                    }.sanitized()
                                                    onUpdateLog(next)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 5.dp),
                                        ) {
                                            Text(
                                                text = labelTxt,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (sel) scheme.surface else scheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            if (!activeToday.isWorkingDay) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "Libur hari ini. Paket tidak dihitung.",
                                        fontSize = 13.sp,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                if (yesterdayRemaining > 0 && activeToday.safeCarried == 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(scheme.surfaceVariant)
                                            .clickable { onCarryoverYesterday() }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Bawa sisa kemarin ($yesterdayRemaining paket)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = scheme.onSurface,
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(scheme.surfaceVariant)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            "Dibawa",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = scheme.onSurfaceVariant,
                                        )
                                        SheetBigNumberInput(
                                            value = carriedText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }
                                                carriedText = digits
                                                val nextC = digits.toIntOrNull() ?: 0
                                                val nextS = activeToday.safeDelivered.coerceAtMost(nextC)
                                                onUpdateLog(
                                                    activeToday.copy(
                                                        isWorkingDay = true,
                                                        packagesCarried = nextC,
                                                        packagesDelivered = nextS,
                                                    ).sanitized(),
                                                )
                                            },
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(scheme.surfaceVariant)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            "Terkirim",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = scheme.onSurfaceVariant,
                                        )
                                        SheetBigNumberInput(
                                            value = deliveredText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }
                                                deliveredText = digits
                                                val parsedS = digits.toIntOrNull() ?: 0
                                                val nextC = activeToday.safeCarried.coerceAtLeast(parsedS)
                                                onUpdateLog(
                                                    activeToday.copy(
                                                        isWorkingDay = true,
                                                        packagesCarried = nextC,
                                                        packagesDelivered = parsedS,
                                                    ).sanitized(),
                                                )
                                            },
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf("-1", "+1", "+5", "+10", "0").forEach { btnLabel ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(scheme.surfaceVariant)
                                                .clickable {
                                                    val nextS = if (btnLabel == "0") {
                                                        0
                                                    } else {
                                                        val delta = btnLabel.toIntOrNull() ?: 0
                                                        (activeToday.safeDelivered + delta).coerceAtLeast(0)
                                                    }
                                                    val nextC = if (btnLabel == "0") {
                                                        activeToday.safeCarried
                                                    } else {
                                                        activeToday.safeCarried.coerceAtLeast(nextS)
                                                    }
                                                    onUpdateLog(
                                                        activeToday.copy(
                                                            isWorkingDay = true,
                                                            packagesCarried = nextC,
                                                            packagesDelivered = nextS,
                                                        ).sanitized(),
                                                    )
                                                }
                                                .padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                btnLabel,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = scheme.onSurface,
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SheetStatItem("${activeToday.remainingPackages}", "Sisa hari ini", large = true)
                                    SheetPillAction(text = "Edit", onClick = { editingLog = activeToday })
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val firstDay = selectedMonth.atDay(1)
            val daysInMonth = selectedMonth.lengthOfMonth()
            val leadingBlanks = ((firstDay.dayOfWeek.value + 6) % 7)
            val totalCells = leadingBlanks + daysInMonth
            val rowsCount = (totalCells + 6) / 7
            val todayStr = LocalDate.now().toString()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kalender",
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )

                SheetCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { dow ->
                                Text(
                                    text = dow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        for (r in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                for (c in 0 until 7) {
                                    val idx = r * 7 + c
                                    val dayNum = idx - leadingBlanks + 1
                                    if (dayNum < 1 || dayNum > daysInMonth) {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val dateStr = selectedMonth.atDay(dayNum).toString()
                                        val logItem = logsByDate[dateStr]
                                        val isToday = dateStr == todayStr
                                        val isOffDay = logItem != null && !logItem.isWorkingDay

                                        val cellBg = when {
                                            isToday -> scheme.onSurface
                                            isOffDay -> Color.Transparent
                                            else -> scheme.surfaceVariant
                                        }
                                        val cellFg = if (isToday) scheme.surface else scheme.onSurface
                                        val subFg = if (isToday) {
                                            scheme.surface.copy(
                                                alpha = 0.72f,
                                            )
                                        } else {
                                            scheme.onSurfaceVariant
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(cellBg)
                                                .then(
                                                    if (isOffDay && !isToday) {
                                                        Modifier.border(
                                                            1.dp,
                                                            scheme.outlineVariant,
                                                            RoundedCornerShape(11.dp),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .clickable {
                                                    editingLog =
                                                        logItem
                                                            ?: DailyLog(date = dateStr, ratePerPackage = defaultRate)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    dayNum.toString(),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = cellFg,
                                                )
                                                Text(
                                                    text = when {
                                                        logItem == null -> " "
                                                        logItem.isWorkingDay -> "${logItem.safeDelivered}"
                                                        else -> "LBR"
                                                    },
                                                    fontSize = 10.sp,
                                                    lineHeight = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = subFg,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Riwayat",
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SheetPillAction(
                            text = "Salin",
                            onClick = {
                                val report = TrackifyEngine.formatMonthlyWhatsappReport(
                                    monthLabel = monthLabel,
                                    logs = monthlyLogs,
                                    summary = summary,
                                    breakdown = breakdown,
                                )
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Rekap Trackify", report))
                                Toast.makeText(context, "Laporan disalin", Toast.LENGTH_SHORT).show()
                            },
                        )
                        SheetPillAction(text = "CSV", onClick = { csvExporter.launch("Trackify_$selectedMonth.csv") })
                    }
                }

                SheetCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(scheme.surfaceVariant)
                                    .padding(2.dp),
                            ) {
                                listOf(0 to "Kartu", 1 to "Grid").forEach { (idx, lbl) ->
                                    val sel = viewMode == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(if (sel) scheme.onSurface else Color.Transparent)
                                            .clickable { viewMode = idx }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            lbl,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sel) scheme.surface else scheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            SheetPillAction(text = if (sortDesc) "Terbaru" else "Terlama", onClick = {
                                sortDesc =
                                    !sortDesc
                            })

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (showSearch) scheme.onSurface else scheme.surfaceVariant)
                                    .clickable {
                                        showSearch = !showSearch
                                        if (!showSearch) searchQuery = ""
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = TrackifyIcons.Search,
                                    contentDescription = "Cari",
                                    tint = if (showSearch) scheme.surface else scheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        if (showSearch) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Cari tanggal, misal 12 Nov",
                                        fontSize = 13.sp,
                                        color = scheme.onSurfaceVariant,
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = trackifyFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("Semua", "Masuk", "Libur", "Sisa").forEachIndexed { idx, chipLabel ->
                                val active = filterMode == idx
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (active) scheme.onSurface else scheme.surfaceVariant)
                                        .clickable { filterMode = idx }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        chipLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) scheme.surface else scheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)

                        if (monthlyLogs.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "Belum ada log di $monthLabel. Mulai dari tanggal 1 atau pilih " +
                                        "tanggal di kalender.",
                                    fontSize = 13.sp,
                                    color = scheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = {
                                        val d1 = selectedMonth.atDay(1).toString()
                                        editingLog = logsByDate[d1] ?: DailyLog(date = d1, ratePerPackage = defaultRate)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = scheme.onSurface,
                                        contentColor = scheme.surface,
                                    ),
                                ) {
                                    Text("Tambah log", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else if (filteredLogs.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Tidak ada log yang cocok dengan filter.",
                                    fontSize = 13.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                                SheetPillAction(
                                    text = "Reset filter",
                                    onClick = {
                                        filterMode = 0
                                        searchQuery = ""
                                    },
                                )
                            }
                        } else if (viewMode == 0) {
                            Column {
                                filteredLogs.forEachIndexed { idx, item ->
                                    if (idx > 0) HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                                    val dayNum = item.date.takeLast(2).toIntOrNull() ?: 1
                                    val dt = selectedMonth.atDay(dayNum.coerceIn(1, selectedMonth.lengthOfMonth()))
                                    val rowTitle = "${dt.format(shortDayFmt).replaceFirstChar {
                                        it.titlecase(idLocale)
                                    }}, ${dt.dayOfMonth} ${dt.format(shortMonthFmt)}"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editingLog = item }
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    rowTitle,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = scheme.onSurface,
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (item.isWorkingDay) {
                                                                scheme.onSurface
                                                            } else {
                                                                Color.Transparent
                                                            },
                                                        )
                                                        .then(
                                                            if (!item.isWorkingDay) {
                                                                Modifier.border(
                                                                    1.dp,
                                                                    scheme.outline,
                                                                    CircleShape,
                                                                )
                                                            } else {
                                                                Modifier
                                                            },
                                                        )
                                                        .padding(horizontal = 9.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        text = if (item.isWorkingDay) "Masuk" else "Libur",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (item.isWorkingDay) {
                                                            scheme.surface
                                                        } else {
                                                            scheme.onSurfaceVariant
                                                        },
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (item.isWorkingDay) {
                                                    val remPart = if (item.remainingPackages >
                                                        0
                                                    ) {
                                                        " · sisa ${item.remainingPackages}"
                                                    } else {
                                                        ""
                                                    }
                                                    "${item.safeDelivered} dari ${item.safeCarried} paket$remPart"
                                                } else {
                                                    "Tidak ada paket"
                                                },
                                                fontSize = 13.sp,
                                                color = scheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = WageCalculator.netIncomeOf(
                                                item,
                                                wageMode,
                                                flatDailyAmount,
                                            ).formatIdr(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = scheme.onSurface,
                                        )
                                    }
                                }
                            }
                        } else {
                            val totC = filteredLogs.sumOf { if (it.isWorkingDay) it.safeCarried else 0 }
                            val totS = filteredLogs.sumOf { if (it.isWorkingDay) it.safeDelivered else 0 }
                            val totR = filteredLogs.sumOf { if (it.isWorkingDay) it.remainingPackages else 0 }
                            val totE = filteredLogs.sumOf { if (it.isWorkingDay) it.expense else 0.0 }
                            val totN = filteredLogs.sumOf { WageCalculator.netIncomeOf(it, wageMode, flatDailyAmount) }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                    Text(
                                        "Tgl",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.9f),
                                    )
                                    Text(
                                        "Bawa",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "Kirim",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "Sisa",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.9f),
                                    )
                                    Text(
                                        "Pot.",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "Bersih",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.1f),
                                    )
                                }

                                filteredLogs.forEach { item ->
                                    val dNum = item.date.takeLast(2).toIntOrNull() ?: 1
                                    HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editingLog = item }
                                            .padding(vertical = 9.dp),
                                    ) {
                                        Text(
                                            "$dNum",
                                            fontSize = 12.sp,
                                            color = scheme.onSurface,
                                            modifier = Modifier.weight(0.9f),
                                        )
                                        Text(
                                            "${item.safeCarried}",
                                            fontSize = 12.sp,
                                            color = scheme.onSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "${item.safeDelivered}",
                                            fontSize = 12.sp,
                                            color = scheme.onSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "${item.remainingPackages}",
                                            fontSize = 12.sp,
                                            color = scheme.onSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.9f),
                                        )
                                        Text(
                                            item.expense.formatCompactK(),
                                            fontSize = 12.sp,
                                            color = scheme.onSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            WageCalculator.netIncomeOf(
                                                item,
                                                wageMode,
                                                flatDailyAmount,
                                            ).formatCompactK(),
                                            fontSize = 12.sp,
                                            color = scheme.onSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1.1f),
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 2.dp, color = scheme.onSurface)
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 9.dp)) {
                                    Text(
                                        "TOTAL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        modifier = Modifier.weight(0.9f),
                                    )
                                    Text(
                                        "$totC",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "$totS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "$totR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.9f),
                                    )
                                    Text(
                                        totE.formatCompactK(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        totN.formatCompactK(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialDate = runCatching {
            LocalDate.parse(editingLog?.date ?: LocalDate.now().toString())
        }.getOrElse {
            if (YearMonth.now() == selectedMonth) LocalDate.now() else selectedMonth.atDay(1)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val dateStr = Instant.ofEpochMilli(
                                millis,
                            ).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            editingLog = logsByDate[dateStr] ?: DailyLog(date = dateStr, ratePerPackage = defaultRate)
                        }
                        showDatePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTargetSheet) {
        var draftTarget by remember(targetRupiah) { mutableDoubleStateOf(targetRupiah) }
        TrackifyDialog(
            onDismissRequest = { showTargetSheet = false },
            title = { Text("Atur target bulan", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Target bersih", fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SheetPillAction("−") { draftTarget = (draftTarget - 100_000.0).coerceAtLeast(0.0) }
                        Text(draftTarget.formatIdr(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        SheetPillAction("+") { draftTarget += 100_000.0 }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetRupiah = draftTarget
                        onUpdateTarget(draftTarget)
                        showTargetSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.onSurface,
                        contentColor = scheme.surface,
                    ),
                ) { Text("Simpan target") }
            },
            dismissButton = { TextButton(onClick = { showTargetSheet = false }) { Text("Batal") } },
        )
    }

    editingLog?.let { current ->
        SheetLogEditorDialog(
            log = current,
            defaultRate = defaultRate,
            onDismiss = { editingLog = null },
            onSave = { updated ->
                onUpdateLog(updated)
                editingLog = null
            },
            onDelete = { target ->
                pendingDeleteLog = target
                editingLog = null
            },
        )
    }

    pendingDeleteLog?.let { target ->
        TrackifyDialog(
            onDismissRequest = { pendingDeleteLog = null },
            title = { Text("Hapus log?", fontWeight = FontWeight.SemiBold) },
            text = { Text("Log ${target.date} akan dihapus. Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLog(target)
                        pendingDeleteLog = null
                    },
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLog = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun SheetCard(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SheetStatItem(value: String, label: String, large: Boolean = false, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            fontSize = if (large) 20.sp else 15.sp,
            lineHeight = if (large) 24.sp else 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetPillAction(text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun SheetCircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SheetProgressBar(progress: Float, gradient: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(scheme.surfaceVariant),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        if (gradient) {
                            Brush.horizontalGradient(listOf(scheme.outline, scheme.onSurface))
                        } else {
                            Brush.horizontalGradient(listOf(scheme.onSurface, scheme.onSurface))
                        },
                    ),
            )
        }
    }
}

@Composable
private fun SheetBigNumberInput(value: String, onValueChange: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    )
}

@Composable
private fun SheetLogEditorDialog(
    log: DailyLog,
    defaultRate: Double,
    onDismiss: () -> Unit,
    onSave: (DailyLog) -> Unit,
    onDelete: (DailyLog) -> Unit,
) {
    val clean = remember(log) { log.sanitized() }
    var isWorking by remember(clean) { mutableStateOf(clean.isWorkingDay) }
    var carried by remember(clean) { mutableIntStateOf(clean.safeCarried) }
    var delivered by remember(clean) { mutableIntStateOf(clean.safeDelivered) }
    var rateText by remember(clean, defaultRate) {
        val r = if (clean.ratePerPackage > 0.0) clean.ratePerPackage else defaultRate
        mutableStateOf(r.toLong().toString())
    }
    var bonusText by remember(clean) { mutableStateOf(clean.bonus.toLong().toString()) }
    var expenseText by remember(clean) { mutableStateOf(clean.expense.toLong().toString()) }
    var noteText by remember(clean) { mutableStateOf(clean.notes) }

    TrackifyDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${clean.date}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Status Masuk", fontSize = 14.sp)
                    Switch(checked = isWorking, onCheckedChange = { isWorking = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dibawa", fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SheetPillAction("−") {
                            carried = (carried - 1).coerceAtLeast(0)
                            delivered = delivered.coerceAtMost(carried)
                        }
                        Text("$carried", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        SheetPillAction("+") { carried += 1 }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Terkirim", fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SheetPillAction("−") { delivered = (delivered - 1).coerceAtLeast(0) }
                        Text("$delivered", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        SheetPillAction("+") {
                            delivered += 1
                            carried = carried.coerceAtLeast(delivered)
                        }
                    }
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = sanitizeDecimalInput(it)
                    },
                    label = {
                        Text("Tarif / Paket (Rp)")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(
                        12.dp,
                    ),
                    colors = trackifyFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                )
                OutlinedTextField(
                    value = bonusText,
                    onValueChange = {
                        bonusText = sanitizeDecimalInput(it)
                    },
                    label = {
                        Text("Bonus (Rp)")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(
                        12.dp,
                    ),
                    colors = trackifyFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                )
                OutlinedTextField(
                    value = expenseText,
                    onValueChange = {
                        expenseText = sanitizeDecimalInput(it)
                    },
                    label = {
                        Text("Potongan (Rp)")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(
                        12.dp,
                    ),
                    colors = trackifyFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                )
                OutlinedTextField(value = noteText, onValueChange = {
                    noteText = it
                }, label = {
                    Text("Catatan")
                }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = trackifyFieldColors())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = clean.copy(
                        isWorkingDay = isWorking,
                        packagesCarried = carried,
                        packagesDelivered = delivered,
                        ratePerPackage = rateText.toDoubleOrNull() ?: defaultRate,
                        bonus = bonusText.toDoubleOrNull() ?: 0.0,
                        expense = expenseText.toDoubleOrNull() ?: 0.0,
                        notes = noteText.trim(),
                    ).sanitized()
                    onSave(updated)
                },
            ) { Text("Simpan log") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(clean) }) { Text("Hapus") }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        },
    )
}
