package com.blankpages.trackify.ui.settings

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blankpages.trackify.data.notification.DailyReminderScheduler
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyIcons
import java.text.NumberFormat
import java.util.Locale

private val setIdLocale = Locale.forLanguageTag("id-ID")
private fun Double.setIdr(): String {
    val nf = NumberFormat.getCurrencyInstance(setIdLocale)
    nf.maximumFractionDigits = 0
    nf.minimumFractionDigits = 0
    return nf.format(this).replace("Rp", "Rp").replace("\u00a0", "")
}

@Composable
fun SettingsScreen(
    defaultRate: Double,
    defaultBonus: Double = 0.0,
    targetAmount: Double = 3_600_000.0,
    dailyReminderEnabled: Boolean = true,
    onBack: () -> Unit,
    onUpdateRate: (Double) -> Unit,
    onUpdateBonus: (Double) -> Unit = {},
    onUpdateTarget: (Double) -> Unit = {},
    onToggleReminder: (Boolean) -> Unit = {},
    onExportCsv: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
    onBackupData: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
    onRestoreData: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
    onResetData: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var rateVal by rememberSaveable(defaultRate) { mutableDoubleStateOf(defaultRate) }
    var bonusVal by rememberSaveable(defaultBonus) { mutableDoubleStateOf(defaultBonus) }
    var targetVal by rememberSaveable(targetAmount) { mutableDoubleStateOf(targetAmount) }
    var remVal by rememberSaveable(dailyReminderEnabled) { mutableStateOf(dailyReminderEnabled) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            remVal = true
            onToggleReminder(true)
        } else {
            remVal = false
            onToggleReminder(false)
            Toast.makeText(context, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExportCsv(uri) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) onBackupData(uri) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onRestoreData(uri) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(scheme.surface)
                        .border(1.dp, scheme.outlineVariant, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TrackifyIcons.ChevronLeft,
                        contentDescription = "Kembali",
                        tint = scheme.onSurface,
                    )
                }
                ScreenTitle(text = "Setelan")
            }
        }

        // Section > Pay scheme
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Skema gaji",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        MockupStepperRow(
                            title = "Rate per paket",
                            subtitle = "Dikali jumlah terkirim",
                            valueText = rateVal.setIdr(),
                            onDec = {
                                rateVal = (rateVal - 100.0).coerceAtLeast(0.0)
                                onUpdateRate(rateVal)
                            },
                            onInc = {
                                rateVal += 100.0
                                onUpdateRate(rateVal)
                            },
                        )
                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                        MockupStepperRow(
                            title = "Bonus masuk",
                            subtitle = "Per hari kerja",
                            valueText = bonusVal.setIdr(),
                            onDec = {
                                bonusVal = (bonusVal - 1000.0).coerceAtLeast(0.0)
                                onUpdateBonus(bonusVal)
                            },
                            onInc = {
                                bonusVal += 1000.0
                                onUpdateBonus(bonusVal)
                            },
                        )
                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                        MockupStepperRow(
                            title = "Target bulanan",
                            subtitle = "Pendapatan bersih",
                            valueText = targetVal.setIdr(),
                            onDec = {
                                targetVal = (targetVal - 100_000.0).coerceAtLeast(0.0)
                                onUpdateTarget(targetVal)
                            },
                            onInc = {
                                targetVal += 100_000.0
                                onUpdateTarget(targetVal)
                            },
                        )
                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Pembagian gajian",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface,
                                )
                                Text("Tgl 15 dan akhir bulan", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, scheme.outline, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    "2x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section > Reminders & data
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pengingat",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ingatkan catat harian",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = scheme.onSurface,
                            )
                            Text("Notifikasi setiap malam 20.00", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = remVal,
                            onCheckedChange = { enabled ->
                                if (enabled && !DailyReminderScheduler.hasNotificationPermission(context)) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    remVal = enabled
                                    onToggleReminder(enabled)
                                }
                            },
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Data",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Ekspor CSV",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface,
                                )
                                Text("Rekap bulan yang dipilih", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(scheme.surfaceVariant)
                                    .clickable { csvLauncher.launch("Trackify_Rekap.csv") }
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    "Ekspor",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Backup data",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface,
                                )
                                Text("Simpan semua log dan lokasi", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(scheme.surfaceVariant)
                                    .clickable { backupLauncher.launch("Trackify_Backup.json") }
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    "Backup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Pulihkan data",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface,
                                )
                                Text("Dari file backup", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(scheme.surfaceVariant)
                                    .clickable { restoreLauncher.launch(arrayOf("application/json", "text/*")) }
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    "Pulihkan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Hapus semua data",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.error,
                                )
                                Text(
                                    "Log, lokasi, catatan, dan setelan",
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(scheme.error.copy(alpha = 0.12f))
                                    .clickable { showResetConfirm = true }
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                            ) {
                                Text("Hapus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        TrackifyDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Hapus semua data?") },
            text = {
                Text(
                    "Seluruh log harian, lokasi, catatan, dan pengaturan akan dihapus permanen " +
                        "dari perangkat. Sebaiknya buat backup dulu.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.error,
                        contentColor = scheme.onError,
                    ),
                ) { Text("Hapus permanen") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun MockupStepperRow(title: String, subtitle: String, valueText: String, onDec: () -> Unit, onInc: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = scheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = scheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(scheme.surface)
                    .clickable(onClick = onDec),
                contentAlignment = Alignment.Center,
            ) {
                Text("−", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            }
            Text(
                text = valueText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 82.dp),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(scheme.surface)
                    .clickable(onClick = onInc),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            }
        }
    }
}
