package com.blankpages.trackify.ui.home
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.WageCalculator
import com.blankpages.trackify.domain.WageMode
import com.blankpages.trackify.domain.WagePeriod
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TabularFigures
import com.blankpages.trackify.ui.theme.TrackifyButton
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyGradients
import com.blankpages.trackify.ui.theme.TrackifyIconButton
import com.blankpages.trackify.ui.theme.TrackifyIcons
import com.blankpages.trackify.ui.theme.TrackifyTextButton
import com.blankpages.trackify.ui.theme.trackifyFieldColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val homeIdLocale = Locale.forLanguageTag("id-ID")
private val homeShortDayFmt = DateTimeFormatter.ofPattern("EEE", homeIdLocale)

private fun Double.homeIdr(): String {
    val nf = NumberFormat.getCurrencyInstance(homeIdLocale)
    nf.maximumFractionDigits = 0
    nf.minimumFractionDigits = 0
    return nf.format(this).replace("Rp", "Rp").replace("\u00a0", "")
}

@Composable
fun HomeScreen(
    todayLog: DailyLog,
    monthlyLogs: List<DailyLog>,
    summary: MonthlySummary,
    savedLocations: List<SavedLocation>,
    targetAmount: Double = 3_600_000.0,
    wageMode: WageMode = WageMode.PER_PACKAGE,
    flatDailyAmount: Double = 2_500.0,
    wagePeriod: WagePeriod = WagePeriod.MONTHLY,
    paydayDate: Int = 25,
    onOpenSettings: () -> Unit,
    onOpenRekap: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenNotes: () -> Unit,
    onUpdateTodayLog: (DailyLog) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activeToday = todayLog.sanitized()
    val todayObj = runCatching { LocalDate.parse(activeToday.date) }.getOrElse { LocalDate.now() }

    val logsByDate = remember(monthlyLogs) { monthlyLogs.associate { it.date to it.sanitized() } }
    val avgPerDay = if (summary.totalWorkingDays > 0) {
        summary.totalDelivered.toDouble() / summary.totalWorkingDays
    } else {
        0.0
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

    val sentPct = if (activeToday.isWorkingDay && activeToday.safeCarried > 0) {
        ((activeToday.safeDelivered.toFloat() / activeToday.safeCarried) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        0
    }

    val periodTargetAmount = WageCalculator.targetForPeriod(targetAmount, wagePeriod)
    val periodIncome = remember(monthlyLogs, todayObj, wageMode, flatDailyAmount, wagePeriod) {
        WageCalculator.incomeForPeriod(monthlyLogs, todayObj, wageMode, flatDailyAmount, wagePeriod)
    }
    val targetPct = if (periodTargetAmount > 0.0) {
        ((periodIncome / periodTargetAmount) * 100.0).roundToInt().coerceIn(0, 100)
    } else {
        0
    }

    val nextLoc = savedLocations.firstOrNull()
    val last7Days = remember(todayObj, logsByDate) {
        (-6L..0L).map { offset ->
            val dt = todayObj.plusDays(offset)
            val l = logsByDate[dt.toString()]
            val sentVal = if (l != null && l.isWorkingDay) l.safeDelivered else 0
            val lbl = dt.format(homeShortDayFmt).replaceFirstChar { it.titlecase(homeIdLocale) }
            Triple(dt.toString(), lbl, sentVal)
        }
    }
    val max7 = last7Days.maxOfOrNull { it.third }?.coerceAtLeast(1) ?: 1
    var selectedBarIndex by rememberSaveable { mutableIntStateOf(last7Days.lastIndex) }
    val selectedBar = last7Days.getOrNull(selectedBarIndex) ?: last7Days.last()
    var pendingDelta by remember { mutableStateOf<Int?>(null) }
    var pendingPause by remember { mutableStateOf(false) }
    var carriedText by remember { mutableStateOf(activeToday.safeCarried.toString()) }
    var deliveredText by remember { mutableStateOf(activeToday.safeDelivered.toString()) }

    LaunchedEffect(activeToday.safeCarried, activeToday.safeDelivered) {
        carriedText = activeToday.safeCarried.toString()
        deliveredText = activeToday.safeDelivered.toString()
    }

    fun commitPackageCounts() {
        val carried = carriedText.trim().toIntOrNull()?.coerceAtLeast(0) ?: activeToday.safeCarried
        val delivered = deliveredText.trim().toIntOrNull()?.coerceAtLeast(0) ?: activeToday.safeDelivered
        onUpdateTodayLog(
            activeToday.copy(
                isWorkingDay = true,
                packagesCarried = carried,
                packagesDelivered = delivered.coerceAtMost(carried),
            ).sanitized(),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TrackifyGradients.background()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header > Home
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    ScreenTitle(text = "Trackify")
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(scheme.surface)
                        .border(1.dp, scheme.outlineVariant, CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TrackifyIcons.Settings,
                        contentDescription = "Setelan",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Hero > Today card
        item {
            // Hero memakai gradien khusus; teksnya selalu putih di kedua tema.
            val hero = scheme.copy(
                onSurface = TrackifyGradients.HeroInk,
                onSurfaceVariant = TrackifyGradients.HeroMute,
                surface = TrackifyGradients.HeroChipInk,
                surfaceVariant = TrackifyGradients.HeroTrack,
                outline = TrackifyGradients.HeroLine,
                outlineVariant = TrackifyGradients.HeroLine,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrackifyGradients.hero(), RoundedCornerShape(20.dp))
                    .border(1.dp, TrackifyGradients.HeroLine, RoundedCornerShape(20.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Terkirim hari ini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = hero.onSurfaceVariant,
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (activeToday.isWorkingDay) hero.onSurface else Color.Transparent)
                                .then(
                                    if (!activeToday.isWorkingDay) {
                                        Modifier.border(1.dp, hero.outline, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    if (activeToday.isWorkingDay) {
                                        pendingPause = true
                                    } else {
                                        onUpdateTodayLog(activeToday.copy(isWorkingDay = true).sanitized())
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (activeToday.isWorkingDay) "Masuk" else "Libur",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeToday.isWorkingDay) hero.surface else hero.onSurfaceVariant,
                            )
                        }
                    }

                    if (activeToday.isWorkingDay) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "${activeToday.safeDelivered}",
                                style = TabularFigures,
                                fontSize = 54.sp,
                                lineHeight = 56.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-1.8).sp,
                                color = hero.onSurface,
                            )
                            Text(
                                text = "dari ${activeToday.safeCarried} paket",
                                fontSize = 13.sp,
                                color = hero.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(hero.surfaceVariant),
                        ) {
                            val frac = (sentPct / 100f).coerceIn(0f, 1f)
                            if (frac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(frac)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(hero.outline, hero.onSurface),
                                            ),
                                        ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "$sentPct% terkirim",
                                fontSize = 12.sp,
                                color = hero.onSurfaceVariant,
                            )
                            Text(
                                text = "Sisa ${activeToday.remainingPackages} paket",
                                fontSize = 12.sp,
                                color = hero.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(
                            text = "Libur",
                            fontSize = 54.sp,
                            lineHeight = 56.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-1.8).sp,
                            color = hero.onSurface,
                        )
                        Text(
                            text = "Paket tidak dihitung hari ini.",
                            fontSize = 13.sp,
                            color = hero.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = hero.outlineVariant)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = WageCalculator.netIncomeOf(activeToday, wageMode, flatDailyAmount).homeIdr(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hero.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Bersih hari ini",
                                fontSize = 12.sp,
                                color = hero.onSurfaceVariant,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                text = "${activeToday.remainingPackages}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hero.onSurface,
                            )
                            Text(
                                text = "Sisa paket",
                                fontSize = 12.sp,
                                color = hero.onSurfaceVariant,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                text = "%.1f".format(homeIdLocale, avgPerDay),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hero.onSurface,
                            )
                            Text(
                                text = "Rata-rata/hari",
                                fontSize = 12.sp,
                                color = hero.onSurfaceVariant,
                            )
                        }
                    }

                    if (activeToday.isWorkingDay) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = carriedText,
                                onValueChange = { value -> carriedText = value.filter { c -> c.isDigit() } },
                                label = { Text("Dibawa") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = trackifyFieldColors(),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = deliveredText,
                                onValueChange = { value -> deliveredText = value.filter { c -> c.isDigit() } },
                                label = { Text("Dikirim") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = trackifyFieldColors(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TrackifyTextButton(
                            onClick = { commitPackageCounts() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Simpan paket",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("-1", "+1", "+5", "+10").forEach { deltaStr ->
                                val delta = deltaStr.toInt()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(hero.surfaceVariant)
                                        .border(1.dp, hero.outlineVariant, RoundedCornerShape(11.dp))
                                        .clickable { pendingDelta = delta }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = deltaStr,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = hero.onSurface,
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(hero.onSurface)
                                .border(1.dp, hero.outlineVariant, RoundedCornerShape(11.dp))
                                .clickable {
                                    onUpdateTodayLog(activeToday.copy(isWorkingDay = true).sanitized())
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Mulai kerja hari ini",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hero.surface,
                            )
                        }
                    }
                }
            }
        }

        // Section > Twice-a-month pay & monthly target
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ringkasan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(scheme.surfaceVariant)
                            .border(1.dp, scheme.outlineVariant, CircleShape)
                            .clickable(onClick = onOpenRekap)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Buka",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val totG = (abs(gajian1) + abs(gajian2)).coerceAtLeast(1.0)
                        val w1 = (abs(gajian1) / totG).toFloat().coerceIn(0.25f, 0.75f)
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
                                    text = "Periode 1",
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
                                    text = "Periode 2",
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gajian1.homeIdr(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Tanggal 15",
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gajian2.homeIdr(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Akhir Bulan",
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Target ${WageCalculator.labelOf(wagePeriod)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    text = "$targetPct%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                            }
                            Text(
                                text = "${periodIncome.homeIdr()} dari ${periodTargetAmount.homeIdr()}" +
                                    if (wagePeriod == WagePeriod.MONTHLY) " • gajian tgl $paydayDate" else "",
                                fontSize = 11.sp,
                                color = scheme.onSurfaceVariant,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(scheme.surfaceVariant),
                            ) {
                                val tFrac = (targetPct / 100f).coerceIn(0f, 1f)
                                if (tFrac > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(tFrac)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(scheme.onSurface),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section > Next goal
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Lokasi",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant)
                                .border(1.dp, scheme.outlineVariant, CircleShape)
                                .clickable(onClick = onOpenNotes)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "Catatan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant)
                                .border(1.dp, scheme.outlineVariant, CircleShape)
                                .clickable(onClick = onOpenLocations)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "Buka",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    if (nextLoc != null) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(scheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "LOK".take(3).uppercase(homeIdLocale),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurfaceVariant,
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = nextLoc.customerName.ifBlank { "Pelanggan" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = nextLoc.landmarkNote.ifBlank { "${nextLoc.latitude}, ${nextLoc.longitude}" },
                                    fontSize = 13.sp,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scheme.onSurface)
                                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .clickable {
                                            val query = "${nextLoc.latitude},${nextLoc.longitude}"
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(
                                                    "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(
                                                        query,
                                                    )}&travelmode=two-wheeler",
                                                ),
                                            )
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Navigasi",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.surface,
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Belum ada lokasi tersimpan.",
                                fontSize = 13.sp,
                                color = scheme.onSurfaceVariant,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(scheme.surfaceVariant)
                                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenLocations)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "Simpan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section > Last 7 days
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Grafik",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            last7Days.forEachIndexed { index, (dStr, _, sentVal) ->
                                val isToday = dStr == activeToday.date
                                val frac = if (sentVal > 0) {
                                    (sentVal.toFloat() / max7).coerceIn(0.08f, 1f)
                                } else {
                                    0.03f
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { selectedBarIndex = index },
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(frac)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 7.dp,
                                                    topEnd = 7.dp,
                                                    bottomStart = 3.dp,
                                                    bottomEnd = 3.dp,
                                                ),
                                            )
                                            .background(
                                                when {
                                                    selectedBarIndex == index -> scheme.primary
                                                    isToday -> scheme.onSurface
                                                    else -> scheme.surfaceVariant
                                                },
                                            ),
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            last7Days.forEachIndexed { index, (_, lbl, _) ->
                                val isSelected = selectedBarIndex == index
                                Text(
                                    text = lbl,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Text(
                            text = "${selectedBar.second} ${selectedBar.first.takeLast(5)} • " +
                                "${selectedBar.third} paket",
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    val delta = pendingDelta
    if (delta != null) {
        TrackifyDialog(
            onDismissRequest = { pendingDelta = null },
            title = { Text("Perbarui paket hari ini?") },
            text = {
                Text(
                    if (delta > 0) {
                        "Tambah $delta paket terkirim ke catatan hari ini."
                    } else {
                        "Kurangi ${-delta} paket terkirim dari catatan hari ini."
                    },
                )
            },
            confirmButton = {
                TrackifyTextButton(
                    onClick = {
                        pendingDelta = null
                        val nextS = (activeToday.safeDelivered + delta).coerceAtLeast(0)
                        val nextC = activeToday.safeCarried.coerceAtLeast(nextS)
                        onUpdateTodayLog(
                            activeToday.copy(
                                isWorkingDay = true,
                                packagesCarried = nextC,
                                packagesDelivered = nextS,
                            ).sanitized(),
                        )
                    },
                ) { Text("Ya, perbarui") }
            },
            dismissButton = {
                TrackifyTextButton(onClick = { pendingDelta = null }) { Text("Batal") }
            },
        )
    }

    if (pendingPause) {
        TrackifyDialog(
            onDismissRequest = { pendingPause = false },
            title = { Text("Tandai libur hari ini?") },
            text = { Text("Paket dan pengeluaran hari ini akan dikosongkan. Data hari lain tidak berubah.") },
            confirmButton = {
                TrackifyTextButton(
                    onClick = {
                        pendingPause = false
                        onUpdateTodayLog(
                            activeToday.copy(
                                isWorkingDay = false,
                                packagesCarried = 0,
                                packagesDelivered = 0,
                                expense = 0.0,
                            ).sanitized(),
                        )
                    },
                ) { Text("Ya, liburkan") }
            },
            dismissButton = {
                TrackifyTextButton(onClick = { pendingPause = false }) { Text("Batal") }
            },
        )
    }
}
