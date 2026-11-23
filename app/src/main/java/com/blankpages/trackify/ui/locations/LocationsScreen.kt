package com.blankpages.trackify.ui.locations
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyCard
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyIcons

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.blankpages.trackify.data.location.DeviceLocationSource
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

private fun SavedLocation.displayTitle(): String =
    customerName.trim().ifBlank { "Pelanggan #${id.coerceAtLeast(1)}" }

private fun SavedLocation.displaySubtitle(): String =
    landmarkNote.trim().ifBlank {
        String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

private fun SavedLocation.inferTag(): String {
    val raw = "$customerName $landmarkNote".lowercase(Locale.ROOT)
    return when {
        raw.contains("langganan") -> "Langganan"
        raw.contains("titip") || raw.contains("satpam") || raw.contains("warung") -> "Titip"
        else -> "Biasa"
    }
}

@Composable
fun LocationsScreen(
    locations: List<SavedLocation>,
    onSaveLocation: (SavedLocation, Boolean) -> Unit,
    onDeleteLocation: (SavedLocation) -> Unit,
    onQuickDelivered: (Int) -> Unit = {},
    onCreateLinkedNote: (CourierNote) -> Unit = {}
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    var query by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Semua") }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLon by remember { mutableStateOf<Double?>(null) }
    var lastFixTime by remember { mutableStateOf<Long?>(null) }
    var lastAccuracy by remember { mutableStateOf<Float?>(null) }
    var gpsEnabled by remember { mutableStateOf(DeviceLocationSource.isGpsEnabled(context)) }
    var permissionGranted by remember { mutableStateOf(DeviceLocationSource.hasPermission(context)) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var detailEntry by remember { mutableStateOf<Pair<SavedLocation, Double?>?>(null) }
    var editingTarget by remember { mutableStateOf<SavedLocation?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val tags = listOf("Semua", "Langganan", "Titip", "Biasa")

    fun applyFix(loc: Location) {
        currentLat = loc.latitude
        currentLon = loc.longitude
        lastFixTime = loc.time
        lastAccuracy = loc.accuracy
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionGranted = granted
        if (granted) {
            gpsEnabled = DeviceLocationSource.isGpsEnabled(context)
            DeviceLocationSource.requestFresh(context) { loc ->
                if (loc != null) {
                    applyFix(loc)
                    Toast.makeText(context, "GPS live aktif (±${loc.accuracy.roundToInt()} m)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lokasi belum tersedia", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val activity = context as? Activity
            val permanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            if (permanentlyDenied) {
                showPermissionRationale = true
            } else {
                Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshLocation() {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        gpsEnabled = DeviceLocationSource.isGpsEnabled(context)
        if (!gpsEnabled) {
            Toast.makeText(context, "GPS perangkat masih nonaktif", Toast.LENGTH_SHORT).show()
            DeviceLocationSource.openGpsSettings(context)
            return
        }
        DeviceLocationSource.requestFresh(context) { loc ->
            if (loc != null) {
                applyFix(loc)
                Toast.makeText(context, "GPS diperbarui (±${loc.accuracy.roundToInt()} m)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Lokasi belum tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        gpsEnabled = DeviceLocationSource.isGpsEnabled(context)
        DeviceLocationSource.lastKnown(context)?.let(::applyFix)
    }

    DisposableEffect(permissionGranted) {
        if (!permissionGranted) {
            onDispose {}
        } else {
            val stopUpdates = DeviceLocationSource.startContinuous(context) { loc ->
                gpsEnabled = DeviceLocationSource.isGpsEnabled(context)
                applyFix(loc)
            }
            onDispose { stopUpdates() }
        }
    }

    val filtered = remember(locations, query, selectedTag, currentLat, currentLon) {
        TrackifyEngine.filterLocations(locations, query, currentLat, currentLon)
            .filter { (loc, _) -> selectedTag == "Semua" || loc.inferTag() == selectedTag }
    }

    fun openEditor(target: SavedLocation?) {
        detailEntry = null
        editingTarget = target
        showEditor = true
    }

    fun createLinkedNote(loc: SavedLocation) {
        onCreateLinkedNote(
            CourierNote(
                title = loc.customerName,
                content = loc.landmarkNote,
                updatedAt = LocalDate.now().toString(),
                linkedLocationId = loc.id
            )
        )
        Toast.makeText(context, "Catatan dibuat", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "loc_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenTitle(
                    text = "Lokasi",
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedIconButton(
                        onClick = { refreshLocation() },
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = TrackifyIcons.Gps,
                            contentDescription = "Perbarui GPS",
                            tint = scheme.onBackground,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Button(
                        onClick = { openEditor(null) },
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(TrackifyIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item(key = "loc_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TrackifyCard(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                            !permissionGranted -> Text(
                                text = "Izin lokasi belum diberikan — ketuk ikon GPS di kanan atas",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                            !gpsEnabled -> {
                                Text(
                                    text = "GPS perangkat masih nonaktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { DeviceLocationSource.openGpsSettings(context) },
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Aktifkan GPS", style = MaterialTheme.typography.labelSmall) }
                            }
                            currentLat == null || currentLon == null -> Text(
                                text = "Mencari fix lokasi...",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                            else -> {
                                Text(
                                    text = "GPS live: ${"%.5f".format(currentLat)}, ${"%.5f".format(currentLon)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val accuracyText = lastAccuracy?.roundToInt()?.let { "Akurasi ±${it} m" }
                                val ageText = lastFixTime?.let { "fix ${(System.currentTimeMillis() - it) / 1000L}s lalu" }
                                Text(
                                    text = listOfNotNull(accuracyText, ageText).ifEmpty { listOf("Menunggu fix GPS") }
                                        .joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari nama atau patokan") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        val active = selectedTag == tag
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) scheme.onSurface else scheme.surfaceVariant)
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) scheme.surface else scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        when {
            locations.isEmpty() -> {
                item(key = "loc_empty_all") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, scheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Belum ada lokasi tersimpan. Simpan rumah penerima langganan supaya tinggal sekali ketuk untuk navigasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { openEditor(null) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Simpan lokasi pertama", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            filtered.isEmpty() -> {
                item(key = "loc_empty_filter") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Tidak ada lokasi yang cocok.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    query = ""
                                    selectedTag = "Semua"
                                }
                            ) {
                                Text(
                                    "Reset pencarian",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                item(key = "loc_nearby") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLat != null && currentLon != null) "Terdekat" else "Lokasi tersimpan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground
                            )
                            Text(
                                text = "${filtered.size} tempat",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filtered, key = { it.first.id }) { entry ->
                                LocationNearbyCard(
                                    entry = entry,
                                    onNavigate = { openMapNavigation(context, entry.first) },
                                    onDetail = { detailEntry = entry },
                                    onDelivered = {
                                        onQuickDelivered(1)
                                        Toast.makeText(context, "+1 Terkirim", Toast.LENGTH_SHORT).show()
                                    },
                                    onNote = { createLinkedNote(entry.first) },
                                    onShare = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${entry.first.displayTitle()} (${entry.first.landmarkNote}) - https://maps.google.com/?q=${entry.first.latitude},${entry.first.longitude}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Lokasi"))
                                    }
                                )
                            }
                        }
                    }
                }

                item(key = "loc_all") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Semua lokasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground
                            )
                            Text(
                                text = "${filtered.size} tempat",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = scheme.surface,
                            border = BorderStroke(1.dp, scheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                filtered.forEachIndexed { index, entry ->
                                    if (index > 0) {
                                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                                    }
                                    val loc = entry.first
                                    val tagCode = loc.inferTag().take(3).uppercase(Locale.forLanguageTag("id-ID"))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { detailEntry = entry }
                                            .padding(vertical = 11.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(scheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tagCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = scheme.onSurfaceVariant
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = loc.displayTitle(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = scheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = loc.displaySubtitle(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = scheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        entry.second?.let { meters ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = scheme.surfaceVariant.copy(alpha = 0.65f)
                                            ) {
                                                Text(
                                                    text = TrackifyEngine.formatDistance(meters),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = scheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(scheme.surfaceVariant)
                                                .clickable { openMapNavigation(context, loc) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "↗",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = scheme.onSurface
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

    detailEntry?.let { entry ->
        val target = entry.first
        val coordStr = String.format(Locale.US, "%.6f, %.6f", target.latitude, target.longitude)
        TrackifyDialog(
            onDismissRequest = { detailEntry = null },
            title = {
                Text(
                    target.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Koordinat: $coordStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    entry.second?.let { meters ->
                        Text(
                            text = "Jarak dari GPS: ${TrackifyEngine.formatDistance(meters)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    if (target.landmarkNote.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(target.landmarkNote, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Koordinat", coordStr))
                                Toast.makeText(context, "Koordinat disalin", Toast.LENGTH_SHORT).show()
                            }
                        ) { Text("Salin") }
                        TextButton(onClick = { openEditor(target) }) { Text("Edit") }
                        TextButton(
                            onClick = {
                                onDeleteLocation(target)
                                detailEntry = null
                            }
                        ) { Text("Hapus") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openMapNavigation(context, target)
                        detailEntry = null
                    }
                ) { Text("Navigasi ke Google Maps") }
            },
            dismissButton = {
                TextButton(onClick = { detailEntry = null }) { Text("Tutup") }
            }
        )
    }

    if (showEditor) {
        val base = editingTarget ?: SavedLocation(
            customerName = "",
            landmarkNote = "",
            latitude = currentLat ?: -6.4025,
            longitude = currentLon ?: 106.7942
        )
        LocationEditorDialog(
            initial = base,
            isNew = editingTarget == null,
            currentLat = currentLat,
            currentLon = currentLon,
            onDismiss = {
                showEditor = false
                editingTarget = null
            },
            onSave = { saved ->
                onSaveLocation(saved, editingTarget != null)
                showEditor = false
                editingTarget = null
            },
            onDelete = {
                onDeleteLocation(base)
                showEditor = false
                editingTarget = null
            }
        )
    }

    if (showPermissionRationale) {
        TrackifyDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Izin lokasi diperlukan", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Trackify memakai lokasi untuk mengurutkan rumah terdekat dan mengisi koordinat otomatis. " +
                        "Izin sudah ditolak permanen, jadi aktifkan lewat pengaturan aplikasi."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        DeviceLocationSource.openAppSettings(context)
                    }
                ) { Text("Buka setelan") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Nanti") }
            }
        )
    }
}

@Composable
private fun LocationNearbyCard(
    entry: Pair<SavedLocation, Double?>,
    onNavigate: () -> Unit,
    onDetail: () -> Unit,
    onDelivered: () -> Unit,
    onNote: () -> Unit,
    onShare: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val loc = entry.first
    val coordText = String.format(Locale.US, "%.6f, %.6f", loc.latitude, loc.longitude)

    Surface(
        modifier = Modifier.width(310.dp),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(scheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(scheme.background)
                            .border(1.dp, scheme.outlineVariant, CircleShape)
                            .padding(horizontal = 11.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = loc.inferTag(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                    }
                    entry.second?.let { meters ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(scheme.background)
                                .border(1.dp, scheme.outlineVariant, CircleShape)
                                .padding(horizontal = 11.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = TrackifyEngine.formatDistance(meters),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = loc.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coordText,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (loc.landmarkNote.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.surfaceVariant)
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = loc.landmarkNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigate,
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Icon(TrackifyIcons.CompassArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rute", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = onDetail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 30.dp)
                    ) {
                        Text("Detail", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                        Icon(
                            TrackifyIcons.ShareNode,
                            contentDescription = "Bagikan",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelivered,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 30.dp)
                    ) {
                        Text("+1 Pkt", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = onNote,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 30.dp)
                    ) {
                        Text("Catat", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationEditorDialog(
    initial: SavedLocation,
    isNew: Boolean,
    currentLat: Double?,
    currentLon: Double?,
    onDismiss: () -> Unit,
    onSave: (SavedLocation) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var name by remember(initial.id, isNew) { mutableStateOf(initial.customerName) }
    var landmark by remember(initial.id, isNew) { mutableStateOf(initial.landmarkNote) }
    var latStr by remember(initial.id, isNew) { mutableStateOf(initial.latitude.toString()) }
    var lonStr by remember(initial.id, isNew) { mutableStateOf(initial.longitude.toString()) }
    var category by remember(initial.id, isNew) {
        mutableStateOf(initial.inferTag().takeIf { it != "Biasa" } ?: "Biasa")
    }

    TrackifyDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) "Lokasi Baru" else "Edit Lokasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isNew) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            TrackifyIcons.Trash,
                            contentDescription = "Hapus",
                            tint = scheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama / Tujuan") },
                    placeholder = { Text("Nama penerima") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Patokan / Catatan") },
                    placeholder = { Text("Contoh: dekat warung, titip satpam") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = { lonStr = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (currentLat != null && currentLon != null) {
                    TextButton(
                        onClick = {
                            latStr = currentLat.toString()
                            lonStr = currentLon.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Pakai Koordinat GPS Saat Ini",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Langganan", "Titip", "Biasa").forEach { tag ->
                        val selected = category.equals(tag, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) scheme.onSurface else scheme.surfaceVariant)
                                .clickable { category = tag }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) scheme.surface else scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanLandmark = landmark.trim()
                    if (cleanName.isBlank() && cleanLandmark.isBlank()) {
                        Toast.makeText(context, "Isi nama penerima atau patokan", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val mergedNote = if (category != "Biasa" && !cleanLandmark.contains(category, ignoreCase = true)) {
                        if (cleanLandmark.isBlank()) category else "$cleanLandmark · $category"
                    } else cleanLandmark
                    onSave(
                        initial.copy(
                            customerName = cleanName.ifBlank { "Pelanggan" },
                            landmarkNote = mergedNote,
                            latitude = latStr.trim().toDoubleOrNull() ?: initial.latitude,
                            longitude = lonStr.trim().toDoubleOrNull() ?: initial.longitude
                        )
                    )
                }
            ) {
                Text(if (isNew) "Simpan lokasi" else "Simpan perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun openMapNavigation(context: Context, loc: SavedLocation) {
    val uri = Uri.parse("google.navigation:q=${loc.latitude},${loc.longitude}")
    val navIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (navIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(navIntent)
    } else {
        val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}")
        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
    }
}
