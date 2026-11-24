package com.blankpages.trackify.data.performance

import android.content.Context
import com.blankpages.trackify.performance.TrackifyGovernor

/**
 * Titik sambung TPG untuk runtime Android. [TrackifyGovernor] tetap murni JVM;
 * objek ini hanya menjembatani profil perangkat Fase 10 ke governor.
 */
object PerformanceRuntime {
    val governor: TrackifyGovernor = TrackifyGovernor()

    /** Aman dipanggil dari MainActivity maupun pembuatan database. */
    fun bind(context: Context) {
        governor.bind(DevicePerformance.config(context))
    }
}
