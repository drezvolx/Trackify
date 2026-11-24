package com.blankpages.trackify.data.local

import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.NotePhoto
import com.blankpages.trackify.domain.SavedLocation

/**
 * Menyiapkan payload backup sebelum ditulis ke Room. Format JSON sengaja tidak
 * diubah: normalisasi terjadi tepat sebelum restore, jadi backup lama tetap
 * bisa dibaca dan kontrak schema 8 tidak berubah.
 */
object BackupNormalizer {
    const val MAX_PHOTOS_PER_NOTE = 5

    data class Result(
        val logs: List<DailyLog>,
        val locations: List<SavedLocation>,
        val notes: List<CourierNote>,
        val photos: List<NotePhoto>,
        val droppedPhotos: Int,
    )

    fun normalize(payload: BackupPayload): Result {
        val logs = payload.logs.associateBy { it.date }.values.toList()
        val locations = payload.locations.associateBy { it.id }.values.toList()
        val notes = payload.notes.associateBy { it.id }.values.toList()
        // id = 0 akan diganti Room saat insert, jadi foto tidak boleh merujuk ke sana.
        val validNoteIds = notes.mapTo(HashSet(notes.size)) { it.id }.filterTo(HashSet()) { it > 0 }

        val perNote = HashMap<Int, Int>(notes.size)
        val photos = ArrayList<NotePhoto>(payload.notePhotos.size)
        var droppedPhotos = 0

        for (photo in payload.notePhotos) {
            val used = perNote[photo.noteId] ?: 0
            val invalidReference = photo.noteId !in validNoteIds
            val invalidPath = photo.path.isBlank()
            val overQuota = used >= MAX_PHOTOS_PER_NOTE
            if (invalidReference || invalidPath || overQuota) {
                droppedPhotos++
                continue
            }
            perNote[photo.noteId] = used + 1
            // id = 0 membiarkan Room memberikan primary key baru; urutan dihitung
            // dari penghitung lokal, bukan count() berulang seperti di engine lama.
            photos += photo.copy(id = 0, sortOrder = used)
        }

        return Result(logs, locations, notes, photos, droppedPhotos)
    }
}
