package com.blankpages.trackify

import com.blankpages.trackify.data.local.BackupNormalizer
import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.NotePhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupNormalizerTest {

    @Test
    fun normalize_dropsOrphanBlankAndOverQuotaPhotos() {
        val note = CourierNote(id = 1, title = "A", content = "B")
        val payload = BackupPayload(
            logs = listOf(DailyLog(date = "2026-11-25"), DailyLog(date = "2026-11-25")),
            locations = emptyList(),
            notes = listOf(note),
            notePhotos = (0..6).map { NotePhoto(id = it + 1, noteId = 1, path = "p$it", sortOrder = 99) } +
                NotePhoto(id = 90, noteId = 999, path = "orphan") +
                NotePhoto(id = 91, noteId = 1, path = " ") +
                NotePhoto(id = 92, noteId = 0, path = "zero"),
        )

        val result = BackupNormalizer.normalize(payload)

        assertEquals(1, result.logs.size)
        assertEquals(5, result.photos.size)
        assertEquals((0..4).toList(), result.photos.map { it.sortOrder })
        assertEquals(5, result.droppedPhotos)
        assertTrue(result.photos.all { it.id == 0 })
    }

    @Test
    fun normalize_keepsAlreadyValidBackup() {
        val payload = BackupPayload(
            logs = listOf(DailyLog(date = "2026-11-24")),
            locations = emptyList(),
            notes = listOf(CourierNote(id = 7, title = "T", content = "C")),
            notePhotos = listOf(NotePhoto(id = 3, noteId = 7, path = "abc", sortOrder = 0)),
        )

        val result = BackupNormalizer.normalize(payload)

        assertEquals(0, result.droppedPhotos)
        assertEquals(1, result.logs.size)
        assertEquals(1, result.photos.size)
        assertEquals(7, result.photos.first().noteId)
    }
}
