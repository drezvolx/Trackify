package com.blankpages.trackify.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Satu-satunya tempat yang berbicara langsung ke [LocationManager].
 * UI hanya menerima [Location] + metadata (akurasi, waktu fix).
 */
object DeviceLocationSource {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isGpsEnabled(context: Context): Boolean =
        (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

    fun openGpsSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openAppSettings(context: Context) {
        runCatching {
            val uri = Uri.fromParts("package", context.packageName, null)
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun manager(context: Context): LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private fun bestProvider(context: Context): String? {
        val mgr = manager(context) ?: return null
        return when {
            mgr.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        val mgr = manager(context) ?: return null
        var best: Location? = null
        for (provider in mgr.getProviders(true)) {
            val loc = runCatching { mgr.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || loc.accuracy < best.accuracy) best = loc
        }
        return best
    }

    /** Permintaan satu fix segar. Fallback ke last-known setelah 10 detik. */
    @SuppressLint("MissingPermission")
    fun requestFresh(context: Context, onResult: (Location?) -> Unit) {
        val mgr = manager(context)
        val provider = bestProvider(context)
        if (!hasPermission(context) || mgr == null || provider == null) {
            onResult(lastKnown(context))
            return
        }

        val timeout = Handler(Looper.getMainLooper())
        var listener: LocationListener? = null
        val finish: (Location?) -> Unit = { result ->
            listener?.let { active -> runCatching { mgr.removeUpdates(active) } }
            timeout.removeCallbacksAndMessages(null)
            onResult(result)
        }
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = finish(location)
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = finish(null)
        }

        runCatching {
            mgr.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }.onFailure {
            finish(lastKnown(context))
            return
        }
        timeout.postDelayed({ finish(lastKnown(context)) }, 10_000L)
    }

    /** Updates berkelanjutan selama layar lokasi aktif. Returns fungsi untuk berhenti. */
    @SuppressLint("MissingPermission")
    fun startContinuous(context: Context, onResult: (Location) -> Unit): () -> Unit {
        val mgr = manager(context)
        val provider = bestProvider(context)
        if (!hasPermission(context) || mgr == null || provider == null) return {}

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onResult(location)
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        runCatching {
            mgr.requestLocationUpdates(provider, 5_000L, 10f, listener, Looper.getMainLooper())
        }
        lastKnown(context)?.let(onResult)
        return { runCatching { mgr.removeUpdates(listener) } }
    }
}
