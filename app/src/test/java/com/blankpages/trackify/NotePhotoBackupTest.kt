package com.blankpages.trackify

import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.EngineResult
import com.blankpages.trackify.domain.NotePhoto
import com.blankpages.trackify.domain.TrackifyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePhotoBackupTest {

    @Test
    fun backupRoundTripKeepsNotePhotos() {
        val note = CourierNote(id = 3, title = "Titipan", content = "isi", updatedAt = "2026-11-24")
        val payload = BackupPayload(
            logs = emptyList(),
            locations = emptyList(),
            notes = listOf(note),
            notePhotos = listOf(
                NotePhoto(noteId = 3, path = "/data/note_photos/a.jpg", sortOrder = 0),
                NotePhoto(noteId = 3, path = "/data/note_photos/b.jpg", sortOrder = 1),
            ),
        )
        val encoded = TrackifyEngine.encodeBackup("2026-11-24", payload)
        assertTrue(encoded is EngineResult.Ok)
        val raw = (encoded as EngineResult.Ok).value

        val decoded = TrackifyEngine.decodeBackup(raw, "2026-11-24")
        assertTrue(decoded is EngineResult.Ok)
        val restored = (decoded as EngineResult.Ok).value
        assertEquals(2, restored.notePhotos.size)
        assertEquals("/data/note_photos/a.jpg", restored.notePhotos[0].path)
        assertEquals(3, restored.notePhotos[0].noteId)
        assertEquals(1, restored.notePhotos[1].sortOrder)
    }

    @Test
    fun schemaSevenWithoutPhotosIsStillAccepted() {
        val raw = """{"app":"Trackify","schema":7,"exportedAt":"2026-11-24",""" +
            """"courier_notes":[{"id":1,"title":"Lama","content":"isi","isPinned":false,""" +
            """"updatedAt":"2026-11-23","linkedLocationId":null,"isResolved":false}]}"""
        val decoded = TrackifyEngine.decodeBackup(raw, "2026-11-24")
        assertTrue(decoded is EngineResult.Ok)
        val restored = (decoded as EngineResult.Ok).value
        assertEquals(1, restored.notes.size)
        assertTrue(restored.notePhotos.isEmpty())
    }
}
