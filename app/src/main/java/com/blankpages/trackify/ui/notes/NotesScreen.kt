package com.blankpages.trackify.ui.notes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.blankpages.trackify.data.MAX_PHOTOS_PER_NOTE
import com.blankpages.trackify.data.performance.DevicePerformance
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.NoteBlock
import com.blankpages.trackify.domain.NoteBlocks
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.domain.noteTemplates
import com.blankpages.trackify.ui.locations.LocationPhotoStore
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyButton
import com.blankpages.trackify.ui.theme.TrackifyCard
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyGradients
import com.blankpages.trackify.ui.theme.TrackifyIconButton
import com.blankpages.trackify.ui.theme.TrackifyIcons
import com.blankpages.trackify.ui.theme.TrackifyTextButton
import com.blankpages.trackify.ui.theme.trackifyFieldColors
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    notes: List<CourierNote>,
    locations: List<SavedLocation>,
    notePhotos: Map<Int, List<String>> = emptyMap(),
    onSaveNote: (CourierNote, Boolean, List<String>) -> Unit,
    onDeleteNote: (CourierNote) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val perf = DevicePerformance.config(context)

    var query by rememberSaveable { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableIntStateOf(0) }
    var sortMode by rememberSaveable { mutableIntStateOf(0) }
    var editingNote by remember { mutableStateOf<CourierNote?>(null) }
    var isNewNote by remember { mutableStateOf(false) }
    var selectedTemplate by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Int>()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    val locationsById = remember(locations) { locations.associateBy { it.id } }
    val filteredNotes = remember(notes, query, filterMode, sortMode, locations) {
        val matched = TrackifyEngine.filterNotes(notes, query, filterMode, locations)
        when (sortMode) {
            1 -> matched.sortedWith(compareByDescending<CourierNote> { it.updatedAt }.thenByDescending { it.id })

            2 -> matched.sortedWith(compareBy<CourierNote> { it.updatedAt }.thenBy { it.id })

            3 -> matched.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { note: CourierNote -> note.title.ifBlank { "Catatan" } }
                    .thenByDescending { it.id },
            )

            else -> matched
        }
    }
    val selectedNotes = remember(notes, selectedIds) { notes.filter { it.id in selectedIds } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TrackifyGradients.background()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "notes_header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScreenTitle(
                        text = "Catatan",
                        modifier = Modifier.weight(1f),
                    )
                    TrackifyIconButton(
                        onClick = {
                            selectionMode = !selectionMode
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (selectionMode) TrackifyIcons.Close else TrackifyIcons.Check,
                            contentDescription = if (selectionMode) "Batal pilih" else "Pilih catatan",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TrackifyButton(
                        onClick = {
                            isNewNote = true
                            editingNote = CourierNote(
                                title = "",
                                content = "",
                                updatedAt = LocalDate.now().toString(),
                            )
                        },
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(
                            imageVector = TrackifyIcons.Plus,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari catatan") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Templat:",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    noteTemplates.forEach { template ->
                        val isSelected = selectedTemplate == template.title
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTemplate = if (isSelected) null else template.title
                                if (!isSelected) {
                                    isNewNote = true
                                    editingNote = CourierNote(
                                        title = template.title,
                                        content = template.content,
                                        updatedAt = LocalDate.now().toString(),
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            shape = RoundedCornerShape(9.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = scheme.onPrimary,
                                containerColor = scheme.surface,
                                labelColor = scheme.onSurfaceVariant,
                            ),
                        )
                    }
                    FilterChip(
                        selected = selectedTemplate == null,
                        onClick = {
                            selectedTemplate = null
                            isNewNote = true
                            editingNote = CourierNote(
                                title = "",
                                content = "",
                                updatedAt = LocalDate.now().toString(),
                            )
                        },
                        label = {
                            Text(
                                text = "Kosong",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        shape = RoundedCornerShape(9.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.primary,
                            selectedLabelColor = scheme.onPrimary,
                            containerColor = scheme.surface,
                            labelColor = scheme.onSurfaceVariant,
                        ),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Semua", "Aktif", "Selesai").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = filterMode == idx,
                            onClick = { filterMode = idx },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (filterMode == idx) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            shape = RoundedCornerShape(9.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = scheme.onPrimary,
                                containerColor = scheme.surface,
                                labelColor = scheme.onSurfaceVariant,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Urut:",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    listOf("Prioritas", "Terbaru", "Terlama", "Judul A–Z").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = sortMode == idx,
                            onClick = { sortMode = idx },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (sortMode == idx) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            shape = RoundedCornerShape(9.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = scheme.onPrimary,
                                containerColor = scheme.surface,
                                labelColor = scheme.onSurfaceVariant,
                            ),
                        )
                    }
                }

                if (selectionMode) {
                    TrackifyCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${selectedIds.size} dipilih",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TrackifyButton(
                                onClick = {
                                    val today = LocalDate.now().toString()
                                    selectedNotes.forEach { note ->
                                        onSaveNote(
                                            note.copy(isResolved = true, updatedAt = today),
                                            false,
                                            notePhotos[note.id].orEmpty(),
                                        )
                                    }
                                    selectionMode = false
                                    selectedIds = emptySet()
                                },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "Selesai",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            TrackifyButton(
                                onClick = { showBulkDeleteConfirm = true },
                                enabled = selectedIds.isNotEmpty(),
                                contentColor = scheme.error,
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "Hapus",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item(key = "notes_empty") {
                TrackifyCard(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Kosong",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                val linkedLoc = note.linkedLocationId?.let { locationsById[it] }
                TrackifyCard(
                    onClick = if (selectionMode) {
                        null
                    } else {
                        {
                            isNewNote = false
                            editingNote = note
                        }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selectedIds = setOf(note.id)
                        } else {
                            selectedIds = if (note.id in selectedIds) {
                                selectedIds - note.id
                            } else {
                                selectedIds + note.id
                            }
                        }
                    },
                    borderColor = if (note.isPinned) scheme.primary else scheme.outlineVariant,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = note.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + note.id
                                        } else {
                                            selectedIds - note.id
                                        }
                                    },
                                    modifier = Modifier.size(28.dp),
                                )
                            } else {
                                Checkbox(
                                    checked = note.isResolved,
                                    onCheckedChange = { checked ->
                                        onSaveNote(
                                            note.copy(
                                                isResolved = checked,
                                                updatedAt = LocalDate.now().toString(),
                                            ),
                                            false,
                                            notePhotos[note.id].orEmpty(),
                                        )
                                    },
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = note.title.ifBlank { "Catatan" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                                textDecoration = if (note.isResolved) TextDecoration.LineThrough else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (!selectionMode) {
                            TrackifyIconButton(
                                onClick = {
                                    onSaveNote(
                                        note.copy(isPinned = !note.isPinned),
                                        false,
                                        notePhotos[note.id].orEmpty(),
                                    )
                                },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    imageVector = if (note.isPinned) {
                                        TrackifyIcons.PinFilled
                                    } else {
                                        TrackifyIcons.PinOutlined
                                    },
                                    contentDescription = "Pin",
                                    tint = if (note.isPinned) scheme.primary else scheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    val previewBlocks = remember(note.id, note.content) {
                        NoteBlocks.parse(note.content).take(4)
                    }
                    if (previewBlocks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            previewBlocks.forEach { block ->
                                val mark = if (block.checked) "[x]" else "[ ]"
                                val rendered = when (block.type) {
                                    NoteBlock.Type.HEADING -> block.text
                                    NoteBlock.Type.BULLET -> "• ${block.text}"
                                    NoteBlock.Type.TASK -> "$mark ${block.text}"
                                    NoteBlock.Type.PARAGRAPH -> block.text
                                }
                                Text(
                                    text = rendered,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (block.type == NoteBlock.Type.HEADING) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    color = scheme.onSurfaceVariant,
                                    textDecoration = if (note.isResolved) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    val cardPhotos = notePhotos[note.id].orEmpty().take(perf.maxPreviewItems)
                    if (cardPhotos.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            cardPhotos.forEach { path ->
                                val bitmap = remember(path) { LocationPhotoStore.load(path, perf) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Foto catatan",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = linkedLoc?.customerName ?: note.updatedAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (linkedLoc != null) {
                            TrackifyTextButton(
                                onClick = { openNoteLocationMap(context, linkedLoc) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 28.dp),
                            ) {
                                Text(
                                    "Rute",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingNote?.let { target ->
        NoteEditorDialog(
            initial = target,
            isNew = isNewNote,
            locations = locations,
            initialPhotos = notePhotos[target.id].orEmpty(),
            onDismiss = { editingNote = null },
            onSave = { saved, paths ->
                onSaveNote(saved, isNewNote, paths)
                editingNote = null
            },
            onDelete = {
                onDeleteNote(target)
                editingNote = null
            },
        )
    }

    if (showBulkDeleteConfirm) {
        TrackifyDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Hapus catatan terpilih?") },
            text = { Text("${selectedIds.size} catatan akan dihapus permanen.") },
            confirmButton = {
                TrackifyButton(
                    onClick = {
                        selectedNotes.forEach(onDeleteNote)
                        showBulkDeleteConfirm = false
                        selectionMode = false
                        selectedIds = emptySet()
                    },
                    contentColor = scheme.error,
                ) { Text("Hapus permanen") }
            },
            dismissButton = {
                TrackifyTextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: CourierNote,
    isNew: Boolean,
    locations: List<SavedLocation>,
    initialPhotos: List<String>,
    onDismiss: () -> Unit,
    onSave: (CourierNote, List<String>) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val perf = DevicePerformance.config(context)
    val scope = rememberCoroutineScope()
    var photoPaths by remember(initial.id) { mutableStateOf(initialPhotos.take(MAX_PHOTOS_PER_NOTE)) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken && pendingCameraUri != null) {
            scope.launch {
                LocationPhotoStore.persist(
                    context,
                    pendingCameraUri!!,
                    perf,
                    directory = "note_photos",
                    prefix = "note_",
                )?.let { path ->
                    photoPaths = (photoPaths + path).take(MAX_PHOTOS_PER_NOTE)
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
            if (uri != null) cameraLauncher.launch(uri)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                LocationPhotoStore.persist(
                    context,
                    uri,
                    perf,
                    directory = "note_photos",
                    prefix = "note_",
                )?.let { path ->
                    photoPaths = (photoPaths + path).take(MAX_PHOTOS_PER_NOTE)
                }
            }
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val uri = LocationPhotoStore.createGalleryImageUri(context)
            pendingCameraUri = uri
            if (uri != null) cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var title by remember { mutableStateOf(initial.title) }
    val blocks = remember(initial.id) {
        NoteBlocks.parse(initial.content)
            .ifEmpty { listOf(NoteBlock(NoteBlock.Type.PARAGRAPH, "", 0)) }
            .toMutableStateList()
    }
    var isPinned by remember { mutableStateOf(initial.isPinned) }
    var linkedId by remember { mutableStateOf(initial.linkedLocationId) }

    TrackifyDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isNew) "Catatan Baru" else "Edit Catatan", fontWeight = FontWeight.SemiBold)
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
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Blok catatan",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "Teks" to NoteBlock.Type.PARAGRAPH,
                        "Judul" to NoteBlock.Type.HEADING,
                        "Daftar" to NoteBlock.Type.BULLET,
                        "Checklist" to NoteBlock.Type.TASK,
                    ).forEach { (label, type) ->
                        FilterChip(
                            selected = false,
                            onClick = { blocks.add(NoteBlock(type, "", blocks.size)) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            shape = RoundedCornerShape(9.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = scheme.surface,
                                labelColor = scheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
                blocks.forEachIndexed { index, block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        when (block.type) {
                            NoteBlock.Type.HEADING -> Text(
                                text = "#",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = scheme.primary,
                                modifier = Modifier.padding(top = 16.dp),
                            )

                            NoteBlock.Type.BULLET -> Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.primary,
                                modifier = Modifier.padding(top = 12.dp),
                            )

                            NoteBlock.Type.TASK -> Checkbox(
                                checked = block.checked,
                                onCheckedChange = { checked -> blocks[index] = block.copy(checked = checked) },
                                modifier = Modifier.size(24.dp),
                            )

                            NoteBlock.Type.PARAGRAPH -> Spacer(Modifier.width(4.dp))
                        }
                        val hint = when (block.type) {
                            NoteBlock.Type.HEADING -> "Judul blok"
                            NoteBlock.Type.BULLET -> "Butir daftar"
                            NoteBlock.Type.TASK -> "Tugas"
                            NoteBlock.Type.PARAGRAPH -> "Tulis sesuatu..."
                        }
                        OutlinedTextField(
                            value = block.text,
                            onValueChange = { value -> blocks[index] = block.copy(text = value) },
                            placeholder = { Text(hint) },
                            minLines = 1,
                            maxLines = 4,
                            textStyle = if (block.type == NoteBlock.Type.HEADING) {
                                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = trackifyFieldColors(),
                            modifier = Modifier.weight(1f),
                        )
                        TrackifyIconButton(
                            onClick = { if (blocks.size > 1) blocks.removeAt(index) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = TrackifyIcons.Trash,
                                contentDescription = "Hapus blok",
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                TrackifyTextButton(onClick = { blocks.add(NoteBlock(NoteBlock.Type.PARAGRAPH, "", blocks.size)) }) {
                    Text("+ Tambah blok")
                }
                Text(
                    text = "Foto catatan (${photoPaths.size}/$MAX_PHOTOS_PER_NOTE)",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                if (photoPaths.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photoPaths.forEachIndexed { index, path ->
                            val bitmap = remember(path) { LocationPhotoStore.load(path, perf) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Foto catatan",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                TrackifyTextButton(
                                    onClick = {
                                        LocationPhotoStore.delete(path)
                                        photoPaths = photoPaths.filterIndexed { i, _ -> i != index }
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                ) {
                                    Text("Hapus", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackifyButton(
                        onClick = ::openCamera,
                        enabled = photoPaths.size < MAX_PHOTOS_PER_NOTE,
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("Ambil foto", style = MaterialTheme.typography.labelSmall) }
                    TrackifyButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        enabled = photoPaths.size < MAX_PHOTOS_PER_NOTE,
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("Dari galeri", style = MaterialTheme.typography.labelSmall) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Pin ke Atas", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
                if (locations.isNotEmpty()) {
                    Text(
                        text = "Tautkan Lokasi",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = linkedId == null,
                            onClick = { linkedId = null },
                            label = { Text("Tanpa Lokasi", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(9.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = scheme.onPrimary,
                                containerColor = scheme.surface,
                                labelColor = scheme.onSurfaceVariant,
                            ),
                        )
                        locations.forEach { loc ->
                            FilterChip(
                                selected = linkedId == loc.id,
                                onClick = { linkedId = loc.id },
                                label = {
                                    Text(
                                        text = loc.customerName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                shape = RoundedCornerShape(9.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = scheme.primary,
                                    selectedLabelColor = scheme.onPrimary,
                                    containerColor = scheme.surface,
                                    labelColor = scheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TrackifyButton(
                onClick = {
                    val savedContent = NoteBlocks.serialize(blocks)
                    val contentChanged = title.trim() != initial.title.trim() ||
                        savedContent != initial.content.trim() ||
                        isPinned != initial.isPinned ||
                        linkedId != initial.linkedLocationId
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            content = savedContent,
                            isPinned = isPinned,
                            linkedLocationId = linkedId,
                            updatedAt = if (isNew || contentChanged) LocalDate.now().toString() else initial.updatedAt,
                        ),
                        photoPaths,
                    )
                },
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TrackifyTextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

private fun openNoteLocationMap(context: Context, loc: SavedLocation) {
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
