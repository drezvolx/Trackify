package com.blankpages.trackify.domain

/**
 * Jam pengingat harian yang dipilih user. Nilai sudah tervalidasi, jadi scheduler
 * hanya perlu membacanya tanpa harus memikirkan batas jam/menit lagi.
 */
data class ReminderTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "Jam harus 0..23" }
        require(minute in 0..59) { "Menit harus 0..59" }
    }

    /** Format untuk Settings, misal `20.00` atau `07.05`. */
    val label: String get() = hour.toString().padStart(2, '0') + "." + minute.toString().padStart(2, '0')

    companion object {
        val DEFAULT = ReminderTime(20, 0)

        /** Versi aman untuk nilai dari preferensi/backup yang mungkin di luar rentang. */
        fun of(hour: Int, minute: Int): ReminderTime = ReminderTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }
}
