package com.blankpages.trackify.data.performance

import android.util.Log
import androidx.room.RoomDatabase
import com.blankpages.trackify.performance.TrackifyGovernor

/**
 * Menghubungkan `RoomDatabase.QueryCallback` ke TPG. Room 2.7.1 hanya
 * menyediakan `onQuery(sqlQuery, bindArgs)` tanpa waktu mulai/selesai, jadi
 * guard ini melaporkan **bentuk query berisiko**, bukan durasi. Callback
 * dijalankan pada thread query; pekerjaannya dibatasi satu klasifikasi dan
 * satu log yang sudah di-de-duplikasi governor.
 */
class RoomQueryGuard(
    private val governor: TrackifyGovernor,
    private val onVerdict: (String) -> Unit = { Log.i(TAG, it) },
) : RoomDatabase.QueryCallback {

    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
        val verdict = governor.recordQueryShape(sqlQuery) ?: return
        val suggestion = verdict.suggestion ?: return
        onVerdict("$POTENTIAL_PREFIX ${verdict.signature} -> $suggestion")
    }

    private companion object {
        const val TAG = "TPG/Room"
        const val POTENTIAL_PREFIX = "Query berpotensi lambat"
    }
}
