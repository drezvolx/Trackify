package com.blankpages.trackify.ui.locations

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.blankpages.trackify.data.location.DeviceLocationSource
import com.blankpages.trackify.data.location.MapLinkResolver
import com.blankpages.trackify.data.performance.DevicePerformance
import com.blankpages.trackify.domain.MapUrlParser
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyButton
import com.blankpages.trackify.ui.theme.TrackifyCard
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyGradients
import com.blankpages.trackify.ui.theme.TrackifyIconButton
import com.blankpages.trackify.ui.theme.TrackifyIcons
import com.blankpages.trackify.ui.theme.TrackifyTextButton
import com.blankpages.trackify.ui.theme.trackifyFieldColors
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun SavedLocation.displayTitle(): String = customerName.trim().ifBlank { "Pelanggan #${id.coerceAtLeast(1)}" }

private fun SavedLocation.displaySubtitle(): String = landmarkNote.trim().ifBlank {
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}

@Composable
fun LocationsScreen(
    locations: List<SavedLocation>,
    onSaveLocation: (SavedLocation, Boolean) -> Unit,
    onDeleteLocation: (SavedLocation) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val perf = DevicePerformance.config(context)

    var query by rememberSaveable { mutableStateOf("") }
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
    var pendingDelete by remember { mutableStateOf<SavedLocation?>(null) }

    fun applyFix(loc: Location) {
        currentLat = loc.latitude
        currentLon = loc.longitude
        lastFixTime = loc.time
        lastAccuracy = loc.accuracy
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionGranted = granted
        if (granted) {
            gpsEnabled = DeviceLocationSource.isGpsEnabled(context)
            DeviceLocationSource.requestFresh(context) { loc ->
                if (loc != null) {
                    applyFix(loc)
                    Toast.makeText(
                        context,
                        "GPS live aktif (±${loc.accuracy.roundToInt()} m)",
                        Toast.LENGTH_SHORT,
                    ).show()
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
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
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
            val stopUpdates = DeviceLocationSource.startContinuous(context, perf) { loc ->
                gpsEnabled = DeviceLocationSource.isGpsEnabled(context)
                applyFix(loc)
            }
            onDispose { stopUpdates() }
        }
    }

    val filtered = remember(locations, query, currentLat, currentLon) {
        TrackifyEngine.filterLocations(locations, query, currentLat, currentLon)
    }

    fun openEditor(target: SavedLocation?) {
        detailEntry = null
        editingTarget = target
        showEditor = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TrackifyGradients.background()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "loc_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(
                    text = "Lokasi",
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackifyIconButton(
                        onClick = { refreshLocation() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = TrackifyIcons.Gps,
                            contentDescription = "Perbarui GPS",
                            tint = scheme.onBackground,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    TrackifyButton(
                        onClick = { openEditor(null) },
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(
                            TrackifyIcons.Plus,
                            contentDescription = null,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item(key = "loc_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TrackifyCard(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when {
                        !permissionGranted -> Text(
                            text = "Izin lokasi belum diberikan — ketuk ikon GPS di kanan atas",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )

                        !gpsEnabled -> {
                            Text(
                                text = "GPS perangkat masih nonaktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                            TrackifyTextButton(
                                onClick = { DeviceLocationSource.openGpsSettings(context) },
                                contentPadding = PaddingValues(0.dp),
                            ) { Text("Aktifkan GPS", style = MaterialTheme.typography.labelSmall) }
                        }

                        currentLat == null || currentLon == null -> Text(
                            text = "Mencari fix lokasi...",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )

                        else -> {
                            Text(
                                text = "GPS live: ${"%.5f".format(currentLat)}, ${"%.5f".format(currentLon)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val accuracyText = lastAccuracy?.roundToInt()?.let { "Akurasi ±$it m" }
                            val ageText = lastFixTime?.let {
                                "fix ${(System.currentTimeMillis() - it) / 1000L}s lalu"
                            }
                            Text(
                                text = listOfNotNull(accuracyText, ageText).ifEmpty { listOf("Menunggu fix GPS") }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
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
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        when {
            locations.isEmpty() -> {
                item(key = "loc_empty_all") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, scheme.outline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = TrackifyIcons.MapPin,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "Belum ada lokasi tersimpan. Simpan rumah penerima langganan supaya " +
                                    "tinggal sekali ketuk untuk navigasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            TrackifyButton(
                                onClick = { openEditor(null) },
                                shape = RoundedCornerShape(12.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Tidak ada lokasi yang cocok.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant,
                            )
                            TrackifyTextButton(
                                onClick = { query = "" },
                            ) {
                                Text(
                                    "Reset pencarian",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                item(key = "loc_nearby") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (currentLat != null && currentLon != null) "Terdekat" else "Lokasi tersimpan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground,
                            )
                            Text(
                                text = "${filtered.size} tempat",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filtered, key = { it.first.id }) { entry ->
                                LocationNearbyCard(
                                    entry = entry,
                                    onNavigate = { openMapNavigation(context, entry.first) },
                                    onDetail = { detailEntry = entry },
                                    onShare = { LocationSharing.share(context, entry.first) },
                                )
                            }
                        }
                    }
                }

                item(key = "loc_all") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Semua lokasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground,
                            )
                            Text(
                                text = "${filtered.size} tempat",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = scheme.surface,
                            border = BorderStroke(1.dp, scheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                filtered.forEachIndexed { index, entry ->
                                    if (index > 0) {
                                        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                                    }
                                    val loc = entry.first
                                    val initialCode = loc.displayTitle().take(
                                        1,
                                    ).uppercase(Locale.forLanguageTag("id-ID"))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { detailEntry = entry }
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(scheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = initialCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = scheme.onSurface,
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = loc.displayTitle(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = scheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = loc.displaySubtitle(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = scheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                        entry.second?.let { meters ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = scheme.surfaceVariant.copy(alpha = 0.65f),
                                            ) {
                                                Text(
                                                    text = TrackifyEngine.formatDistance(meters),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = scheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(scheme.surfaceVariant)
                                                .border(1.dp, scheme.outlineVariant, CircleShape)
                                                .clickable { openMapNavigation(context, loc) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "↗",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = scheme.onSurface,
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
        val detailPhoto = remember(target.photoPath) { LocationPhotoStore.load(target.photoPath, perf) }
        TrackifyDialog(
            onDismissRequest = { detailEntry = null },
            title = {
                Text(
                    target.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (detailPhoto != null) {
                        Image(
                            bitmap = detailPhoto,
                            contentDescription = "Foto lokasi",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        text = "Koordinat: $coordStr",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurfaceVariant,
                    )
                    entry.second?.let { meters ->
                        Text(
                            text = "Jarak dari GPS: ${TrackifyEngine.formatDistance(meters)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (target.landmarkNote.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.surfaceVariant)
                                .padding(12.dp),
                        ) {
                            Text(
                                target.landmarkNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurface,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TrackifyTextButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Koordinat", coordStr))
                                Toast.makeText(context, "Koordinat disalin", Toast.LENGTH_SHORT).show()
                            },
                        ) { Text("Salin") }
                        TrackifyTextButton(onClick = { LocationSharing.share(context, target) }) { Text("Bagikan") }
                        TrackifyTextButton(onClick = { openEditor(target) }) { Text("Edit") }
                        TrackifyTextButton(
                            onClick = {
                                pendingDelete = target
                                detailEntry = null
                            },
                        ) { Text("Hapus") }
                    }
                }
            },
            confirmButton = {
                TrackifyButton(
                    onClick = {
                        openMapNavigation(context, target)
                        detailEntry = null
                    },
                ) { Text("Navigasi ke Google Maps") }
            },
            dismissButton = {
                TrackifyTextButton(onClick = { detailEntry = null }) { Text("Tutup") }
            },
        )
    }

    if (showEditor) {
        val base = editingTarget ?: SavedLocation(
            customerName = "",
            landmarkNote = "",
            latitude = currentLat ?: -6.4025,
            longitude = currentLon ?: 106.7942,
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
                pendingDelete = base
                showEditor = false
                editingTarget = null
            },
        )
    }

    pendingDelete?.let { target ->
        TrackifyDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus lokasi?", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Lokasi ${target.displayTitle()} akan dihapus. Catatan yang tertaut tetap tersimpan " +
                        "tanpa tautan lokasi.",
                )
            },
            confirmButton = {
                TrackifyButton(
                    onClick = {
                        LocationPhotoStore.delete(target.photoPath)
                        onDeleteLocation(target)
                        pendingDelete = null
                    },
                    contentColor = scheme.error,
                ) { Text("Hapus") }
            },
            dismissButton = {
                TrackifyTextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            },
        )
    }

    if (showPermissionRationale) {
        TrackifyDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Izin lokasi diperlukan", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Trackify memakai lokasi untuk mengurutkan rumah terdekat dan mengisi koordinat otomatis. " +
                        "Izin sudah ditolak permanen, jadi aktifkan lewat pengaturan aplikasi.",
                )
            },
            confirmButton = {
                TrackifyButton(
                    onClick = {
                        showPermissionRationale = false
                        DeviceLocationSource.openAppSettings(context)
                    },
                ) { Text("Buka setelan") }
            },
            dismissButton = {
                TrackifyTextButton(onClick = { showPermissionRationale = false }) { Text("Nanti") }
            },
        )
    }
}

@Composable
private fun LocationNearbyCard(
    entry: Pair<SavedLocation, Double?>,
    onNavigate: () -> Unit,
    onDetail: () -> Unit,
    onShare: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val perf = DevicePerformance.config(LocalContext.current)
    val loc = entry.first
    val coordText = String.format(Locale.US, "%.6f, %.6f", loc.latitude, loc.longitude)
    val nearPhoto = remember(loc.photoPath) { LocationPhotoStore.load(loc.photoPath, perf) }

    Surface(
        modifier = Modifier.width(310.dp),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(scheme.surfaceVariant),
            ) {
                if (nearPhoto != null) {
                    Image(
                        bitmap = nearPhoto,
                        contentDescription = "Foto lokasi",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entry.second?.let { meters ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(scheme.background)
                                .border(1.dp, scheme.outlineVariant, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = TrackifyEngine.formatDistance(meters),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = loc.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = coordText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (loc.landmarkNote.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = loc.landmarkNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrackifyButton(
                        onClick = onNavigate,
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                    ) {
                        Icon(
                            TrackifyIcons.CompassArrow,
                            contentDescription = null,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Rute", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    TrackifyTextButton(
                        onClick = onDetail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 30.dp),
                    ) {
                        Text("Detail", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    TrackifyIconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                        Icon(
                            TrackifyIcons.ShareNode,
                            contentDescription = "Bagikan",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
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
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val perf = DevicePerformance.config(context)
    var name by remember(initial.id, isNew) { mutableStateOf(initial.customerName) }
    var landmark by remember(initial.id, isNew) { mutableStateOf(initial.landmarkNote) }
    var mapUrl by remember(initial.id, isNew) {
        mutableStateOf(
            if (isNew) {
                ""
            } else {
                "https://www.google.com/maps/search/?api=1&query=" +
                    String.format(Locale.US, "%.6f,%.6f", initial.latitude, initial.longitude)
            },
        )
    }
    var photoPath by remember(initial.id, isNew) { mutableStateOf(initial.photoPath) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val photoBitmap = remember(photoPath) { LocationPhotoStore.load(photoPath, perf) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                scope.launch {
                    val saved = LocationPhotoStore.persist(context, uri, perf)
                    if (saved != null) {
                        photoPath = saved
                    } else {
                        Toast.makeText(context, "Gagal menyimpan foto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = LocationPhotoStore.createGalleryImageUri(context)
            pendingCameraUri = uri
            if (uri != null) {
                takePictureLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Gagal menyiapkan kamera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk ambil foto", Toast.LENGTH_SHORT).show()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = LocationPhotoStore.persist(context, uri, perf)
                if (saved != null) {
                    photoPath = saved
                } else {
                    Toast.makeText(context, "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    TrackifyDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isNew) "Lokasi Baru" else "Edit Lokasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!isNew) {
                    TrackifyIconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            TrackifyIcons.Trash,
                            contentDescription = "Hapus",
                            tint = scheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama / Tujuan") },
                    placeholder = { Text("Nama penerima") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Patokan / Catatan") },
                    placeholder = { Text("Contoh: dekat warung, titip satpam") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto lokasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = "Foto lokasi",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TrackifyTextButton(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        ) { Text("Ambil foto") }
                        TrackifyTextButton(onClick = { galleryLauncher.launch("image/*") }) { Text("Dari galeri") }
                        if (photoPath.isNotBlank()) {
                            TrackifyTextButton(
                                onClick = {
                                    LocationPhotoStore.delete(photoPath)
                                    photoPath = ""
                                },
                                contentColor = scheme.error,
                            ) { Text("Hapus foto") }
                        }
                    }
                }
                OutlinedTextField(
                    value = mapUrl,
                    onValueChange = { mapUrl = it },
                    label = { Text("URL Maps") },
                    placeholder = { Text("Tempel link Google Maps di sini") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(12.dp),
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (currentLat != null && currentLon != null) {
                    TrackifyTextButton(
                        onClick = {
                            mapUrl = "https://www.google.com/maps/search/?api=1&query=" +
                                String.format(Locale.US, "%.6f,%.6f", currentLat, currentLon)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Pakai Koordinat GPS Saat Ini",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TrackifyButton(
                onClick = {
                    val cleanName = name.trim()
                    val cleanLandmark = landmark.trim()
                    if (cleanName.isBlank() && cleanLandmark.isBlank()) {
                        Toast.makeText(context, "Isi nama penerima atau patokan", Toast.LENGTH_SHORT).show()
                        return@TrackifyButton
                    }
                    val pasted = mapUrl.trim()
                    scope.launch {
                        val resolved = if (MapLinkResolver.isShortLink(pasted)) {
                            withContext(Dispatchers.IO) { MapLinkResolver.resolve(pasted) }
                        } else {
                            pasted
                        }
                        val coords = MapUrlParser.parse(resolved ?: pasted)
                        if (coords == null) {
                            Toast.makeText(
                                context,
                                "URL Maps tidak dikenali. Tempel link yang memuat titik lokasi.",
                                Toast.LENGTH_LONG,
                            ).show()
                            return@launch
                        }
                        onSave(
                            initial.copy(
                                customerName = cleanName.ifBlank { "Pelanggan" },
                                landmarkNote = cleanLandmark,
                                latitude = coords.latitude,
                                longitude = coords.longitude,
                                photoPath = photoPath,
                            ),
                        )
                    }
                },
            ) {
                Text(if (isNew) "Simpan lokasi" else "Simpan perubahan")
            }
        },
        dismissButton = {
            TrackifyTextButton(onClick = onDismiss) { Text("Batal") }
        },
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
