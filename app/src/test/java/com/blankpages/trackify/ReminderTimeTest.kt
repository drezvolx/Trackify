package com.blankpages.trackify

import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.BackupSettings
import com.blankpages.trackify.domain.EngineResult
import com.blankpages.trackify.domain.ReminderTime
import com.blankpages.trackify.domain.TrackifyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeTest {

    @Test
    fun defaultKeepsLegacyEightPm() {
        assertEquals(20, ReminderTime.DEFAULT.hour)
        assertEquals(0, ReminderTime.DEFAULT.minute)
        assertEquals("20.00", ReminderTime.DEFAULT.label)
    }

    @Test
    fun ofClampsOutOfRangeValues() {
        val time = ReminderTime.of(25, -7)
        assertEquals(23, time.hour)
        assertEquals(0, time.minute)
        assertEquals("07.05", ReminderTime.of(7, 5).label)
    }

    @Test
    fun backupRoundTripKeepsReminderTime() {
        val backup = BackupPayload(
            logs = emptyList(),
            locations = emptyList(),
            notes = emptyList(),
            settings = BackupSettings(reminderTime = ReminderTime.of(6, 45)),
        )
        val encoded = TrackifyEngine.encodeBackup("2026-11-24", backup)
        assertTrue(encoded is EngineResult.Ok)
        val raw = (encoded as EngineResult.Ok).value

        val decoded = TrackifyEngine.decodeBackup(raw, "2026-11-24")
        assertTrue(decoded is EngineResult.Ok)
        assertEquals(ReminderTime.of(6, 45), (decoded as EngineResult.Ok).value.settings.reminderTime)
    }

    @Test
    fun legacyBackupWithoutReminderKeysFallsBackToEightPm() {
        val raw = """{"app":"Trackify","schema":7,"exportedAt":"2026-11-24","settings":{"defaultRate":2500.0}}"""
        val decoded = TrackifyEngine.decodeBackup(raw, "2026-11-24")
        assertTrue(decoded is EngineResult.Ok)
        assertEquals(ReminderTime.DEFAULT, (decoded as EngineResult.Ok).value.settings.reminderTime)
    }
}
