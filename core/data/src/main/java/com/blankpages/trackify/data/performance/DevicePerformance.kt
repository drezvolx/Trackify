package com.blankpages.trackify.data.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.blankpages.trackify.domain.PerformanceConfig
import com.blankpages.trackify.domain.PerformancePolicy
import com.blankpages.trackify.domain.PerformanceTier

/**
 * Profil perangkat dihitung sekali saat aplikasi menyentuhnya pertama kali.
 * Hasilnya di-cache karena `ActivityManager` dan `Runtime.maxMemory()` tidak berubah
 * selama proses hidup, dan membacanya berulang di list foto mahal.
 */
object DevicePerformance {
    @Volatile
    private var cached: PerformanceConfig? = null

    fun config(context: Context): PerformanceConfig {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: detect(context).also { cached = it }
        }
    }

    internal fun detect(context: Context): PerformanceConfig {
        val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.isLowRamDevice == true
        val maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L)
        val tier = when {
            lowRam || maxMemoryMb < 128L -> PerformanceTier.LOW
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O || maxMemoryMb < 256L -> PerformanceTier.BALANCED
            else -> PerformanceTier.HIGH
        }
        return PerformancePolicy.configFor(tier)
    }
}
