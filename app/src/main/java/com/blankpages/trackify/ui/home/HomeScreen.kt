package com.blankpages.trackify.ui.home
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.MonthlySummary
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.domain.toRupiah
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyIcons

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val homeIdLocale = Locale.forLanguageTag("id-ID")
private val homeFullDateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", homeIdLocale)
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
    onOpenSettings: () -> Unit,
    onOpenRekap: () -> Unit,
    onOpenLocations: () -> Unit,
    onUpdateTodayLog: (DailyLog) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activeToday = todayLog.sanitized()
    val todayObj = runCatching { LocalDate.parse(activeToday.date) }.getOrElse { LocalDate.now() }
    val headerDate = todayObj.format(homeFullDateFmt).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(homeIdLocale) else it.toString()
    }

    val logsByDate = remember(monthlyLogs) { monthlyLogs.associate { it.date to it.sanitized() } }
    val avgPerDay = if (summary.totalWorkingDays > 0) {
        summary.totalDelivered.toDouble() / summary.totalWorkingDays
    } else 0.0

    val p1Net = remember(monthlyLogs) {
        monthlyLogs.sumOf { raw ->
            val l = raw.sanitized()
            val d = l.date.takeLast(2).toIntOrNull() ?: 1
            if (l.isWorkingDay && d <= 15) l.netIncome else 0.0
        }
    }
    val p2Net = remember(monthlyLogs) {
        monthlyLogs.sumOf { raw ->
            val l = raw.sanitized()
            val d = l.date.takeLast(2).toIntOrNull() ?: 1
            if (l.isWorkingDay && d > 15) l.netIncome else 0.0
        }
    }
    val gajian1 = p1Net * 0.5
    val gajian2 = p2Net + (p1Net * 0.5)

    val sentPct = if (activeToday.isWorkingDay && activeToday.safeCarried > 0) {
        ((activeToday.safeDelivered.toFloat() / activeToday.safeCarried) * 100f).roundToInt().coerceIn(0, 100)
    } else 0

    val targetPct = if (targetAmount > 0.0) {
        ((summary.totalNetIncome / targetAmount) * 100.0).roundToInt().coerceIn(0, 100)
    } else 0

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Header Beranda
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
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
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TrackifyIcons.Settings,
                        contentDescription = "Setelan",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Hero Card Hari Ini
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terkirim hari ini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (activeToday.isWorkingDay) scheme.onSurface else Color.Transparent)
                                .then(
                                    if (!activeToday.isWorkingDay) {
                                        Modifier.border(1.dp, scheme.outline, CircleShape)
                                    } else Modifier
                                )
                                .clickable {
                                    val next = if (activeToday.isWorkingDay) {
                                        activeToday.copy(
                                            isWorkingDay = false,
                                            packagesCarried = 0,
                                            packagesDelivered = 0,
                                            expense = 0.0
                                        )
                                    } else {
                                        activeToday.copy(isWorkingDay = true)
                                    }.sanitized()
                                    onUpdateTodayLog(next)
                                }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (activeToday.isWorkingDay) "Masuk" else "Libur",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeToday.isWorkingDay) scheme.surface else scheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeToday.isWorkingDay) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${activeToday.safeDelivered}",
                                fontSize = 54.sp,
                                lineHeight = 56.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-1.8).sp,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "dari ${activeToday.safeCarried} paket",
                                fontSize = 13.sp,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant)
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
                                                listOf(scheme.outline, scheme.onSurface)
                                            )
                                        )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$sentPct% terkirim",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sisa ${activeToday.remainingPackages} paket",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Libur",
                            fontSize = 54.sp,
                            lineHeight = 56.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-1.8).sp,
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Paket tidak dihitung hari ini.",
                            fontSize = 13.sp,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeToday.netIncome.homeIdr(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Bersih hari ini",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = "${activeToday.remainingPackages}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Sisa paket",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = "%.1f".format(homeIdLocale, avgPerDay),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Rata-rata/hari",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeToday.isWorkingDay) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("-1", "+1", "+5", "+10").forEach { deltaStr ->
                                val delta = deltaStr.toInt()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(scheme.surfaceVariant)
                                        .clickable {
                                            val nextS = (activeToday.safeDelivered + delta).coerceAtLeast(0)
                                            val nextC = activeToday.safeCarried.coerceAtLeast(nextS)
                                            onUpdateTodayLog(
                                                activeToday.copy(
                                                    isWorkingDay = true,
                                                    packagesCarried = nextC,
                                                    packagesDelivered = nextS
                                                ).sanitized()
                                            )
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = deltaStr,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(scheme.onSurface)
                                .clickable {
                                    onUpdateTodayLog(activeToday.copy(isWorkingDay = true).sanitized())
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Mulai kerja hari ini",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.surface
                            )
                        }
                    }
                }
            }
        }

        // Section: Gajian 2x + Target Bulan
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ringkasan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(scheme.surfaceVariant)
                            .clickable(onClick = onOpenRekap)
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Buka",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val totG = (abs(gajian1) + abs(gajian2)).coerceAtLeast(1.0)
                        val w1 = (abs(gajian1) / totG).toFloat().coerceIn(0.25f, 0.75f)
                        val w2 = (1f - w1).coerceAtLeast(0.25f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(w1)
                                    .fillMaxHeight()
                                    .background(scheme.onSurface)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Periode 1",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.surface,
                                    maxLines = 1
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(w2)
                                    .fillMaxHeight()
                                    .background(scheme.surfaceVariant)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Periode 2",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gajian1.homeIdr(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tanggal 15",
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gajian2.homeIdr(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Akhir Bulan",
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Target",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "$targetPct%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(scheme.surfaceVariant)
                            ) {
                                val tFrac = (targetPct / 100f).coerceIn(0f, 1f)
                                if (tFrac > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(tFrac)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(scheme.onSurface)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Tujuan berikutnya
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lokasi",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(scheme.surfaceVariant)
                            .clickable(onClick = onOpenLocations)
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Buka",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant)
                ) {
                    if (nextLoc != null) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(scheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LOK".take(3).uppercase(homeIdLocale),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurfaceVariant
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = nextLoc.customerName.ifBlank { "Pelanggan" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = nextLoc.landmarkNote.ifBlank { "${nextLoc.latitude}, ${nextLoc.longitude}" },
                                    fontSize = 13.sp,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scheme.onSurface)
                                        .clickable {
                                            val query = "${nextLoc.latitude},${nextLoc.longitude}"
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(query)}&travelmode=two-wheeler")
                                            )
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Navigasi",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.surface
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Belum ada lokasi tersimpan.",
                                fontSize = 13.sp,
                                color = scheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(scheme.surfaceVariant)
                                    .clickable(onClick = onOpenLocations)
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Simpan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: 7 hari terakhir
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Grafik",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7Days.forEach { (dStr, _, sentVal) ->
                                val isToday = dStr == activeToday.date
                                val frac = if (sentVal > 0) {
                                    (sentVal.toFloat() / max7).coerceIn(0.08f, 1f)
                                } else 0.03f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(frac)
                                            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                            .background(if (isToday) scheme.onSurface else scheme.surfaceVariant)
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            last7Days.forEach { (_, lbl, _) ->
                                Text(
                                    text = lbl,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}