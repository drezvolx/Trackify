package com.blankpages.trackify.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.blankpages.trackify.data.local.AppPreferences
import java.util.Calendar

object DailyReminderScheduler {
    private const val CHANNEL_ID = "trackify_daily_reminder"
    private const val REQUEST_CODE_REMINDER = 1001
    private const val REQUEST_CODE_OPEN_APP = 1002

    fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Pengingat harian",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Pengingat untuk mencatat rekap harian Trackify"
            },
        )
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, REQUEST_CODE_REMINDER, intent, flags)
    }

    fun schedule(context: Context, enabled: Boolean) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = pendingIntent(context)
        if (!enabled) {
            alarm.cancel(intent)
            return
        }
        ensureChannel(context)

        // Jam milik user. Scheduler membaca prefs setiap kali dijadwalkan ulang,
        // jadi perubahan jam langsung berlaku tanpa reinstall/force-stop.
        val reminderTime = AppPreferences(context).reminderTime
        val now = Calendar.getInstance()
        val fireAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Alarm > Inexact but wakes on idle; no SCHEDULE_EXACT_ALARM permission needed.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.timeInMillis, intent)
    }

    fun notify(context: Context) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        // Launcher > Resolve via package manager; :data must not depend on :app.
        val openApp = (
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val contentIntent = PendingIntent.getActivity(context, REQUEST_CODE_OPEN_APP, openApp, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Jangan lupa catat harian")
            .setContentText("Luangkan 1 menit untuk mengisi rekap paket hari ini.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(REQUEST_CODE_REMINDER, notification)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyReminderScheduler.notify(context)
        // Reschedule > One-shot alarm repeats tomorrow only while reminders are on.
        DailyReminderScheduler.schedule(context, AppPreferences(context).dailyReminderEnabled)
    }
}

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyReminderScheduler.schedule(context, AppPreferences(context).dailyReminderEnabled)
        }
    }
}
