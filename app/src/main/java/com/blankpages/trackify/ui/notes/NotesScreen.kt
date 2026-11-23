package com.blankpages.trackify.ui.notes
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.SavedLocation
import com.blankpages.trackify.domain.TrackifyEngine
import com.blankpages.trackify.domain.noteTemplates
import com.blankpages.trackify.ui.theme.ScreenTitle
import com.blankpages.trackify.ui.theme.TrackifyCard
import com.blankpages.trackify.ui.theme.TrackifyDialog
import com.blankpages.trackify.ui.theme.TrackifyIcons
import com.blankpages.trackify.ui.theme.trackifyFieldColors
import java.time.LocalDate

@Composable
fun NotesScreen(
    notes: List<CourierNote>,
    locations: List<SavedLocation>,
    onSaveNote: (CourierNote, Boolean) -> Unit,
    onDeleteNote: (CourierNote) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    var query by rememberSaveable { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableIntStateOf(0) }
    var editingNote by remember { mutableStateOf<CourierNote?>(null) }
    var isNewNote by remember { mutableStateOf(false) }

    val locationsById = remember(locations) { locations.associateBy { it.id } }
    val filteredNotes = remember(notes, query, filterMode) {
        TrackifyEngine.filterNotes(notes, query, filterMode)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "notes_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScreenTitle(
                        text = "Catatan",
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            isNewNote = true
                            editingNote = CourierNote(
                                title = "",
                                content = "",
                                updatedAt = LocalDate.now().toString(),
                            )
                        },
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(TrackifyIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Templat:",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    noteTemplates.forEach { template ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                isNewNote = true
                                editingNote = CourierNote(
                                    title = template.title,
                                    content = template.content,
                                    updatedAt = LocalDate.now().toString(),
                                )
                            },
                            label = {
                                Text(
                                    text = template.title,
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    onClick = {
                        isNewNote = false
                        editingNote = note
                    },
                    borderColor = if (note.isPinned) scheme.primary else scheme.outlineVariant,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                            Checkbox(
                                checked = note.isResolved,
                                onCheckedChange = { checked ->
                                    onSaveNote(
                                        note.copy(
                                            isResolved = checked,
                                            updatedAt = LocalDate.now().toString(),
                                        ),
                                        false,
                                    )
                                },
                                modifier = Modifier.size(28.dp),
                            )
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

                        IconButton(
                            onClick = {
                                onSaveNote(
                                    note.copy(isPinned = !note.isPinned),
                                    false,
                                )
                            },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) TrackifyIcons.PinFilled else TrackifyIcons.PinOutlined,
                                contentDescription = "Pin",
                                tint = if (note.isPinned) scheme.primary else scheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textDecoration = if (note.isResolved) TextDecoration.LineThrough else null,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                            TextButton(
                                onClick = { openNoteLocationMap(context, linkedLoc) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
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
            onDismiss = { editingNote = null },
            onSave = { saved ->
                onSaveNote(saved, isNewNote)
                editingNote = null
            },
            onDelete = {
                onDeleteNote(target)
                editingNote = null
            },
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: CourierNote,
    isNew: Boolean,
    locations: List<SavedLocation>,
    onDismiss: () -> Unit,
    onSave: (CourierNote) -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var title by remember { mutableStateOf(initial.title) }
    var content by remember { mutableStateOf(initial.content) }
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
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Isi") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = trackifyFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            content = content.trim(),
                            isPinned = isPinned,
                            linkedLocationId = linkedId,
                            updatedAt = LocalDate.now().toString(),
                        ),
                    )
                },
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
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
