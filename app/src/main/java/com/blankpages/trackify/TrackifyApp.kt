package com.blankpages.trackify

import android.app.Application
import android.util.Log
import com.blankpages.trackify.data.performance.PerformanceRuntime
import com.blankpages.trackify.performance.TpgFrameLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackifyApp : Application() {
    private val frameLogger by lazy { TpgFrameLogger(PerformanceRuntime.governor) }

    override fun onCreate() {
        super.onCreate()

        // TPG > Bind profil perangkat sekali sebelum database dan UI dibuat.
        PerformanceRuntime.bind(this)

        // TPG > Observasi grade frame: satu log hanya saat grade berubah.
        frameLogger.attach()

        // Crash handler > Delegate to the previous handler so the OS still reports it.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TrackifyCrash", "FATAL EXCEPTION in thread ${thread.name}", throwable)
            throwable.printStackTrace()
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
