package com.blankpages.trackify

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackifyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Simpan handler bawaan sistem supaya crash tetap ditangani normal
        // (dialog Force Close / pelaporan dari OS), bukan cuma dicetak ke log.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TrackifyCrash", "FATAL EXCEPTION in thread ${thread.name}", throwable)
            throwable.printStackTrace()
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
