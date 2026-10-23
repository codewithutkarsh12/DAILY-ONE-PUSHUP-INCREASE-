package com.example.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.preferences.UserSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Manages local daily reminders using Android's native [AlarmManager] with deterministic request codes
 * and [PendingIntent.FLAG_UPDATE_CURRENT] so changing reminder times or rebooting the device replaces
 * existing scheduled alarms cleanly and never duplicates notifications.
 *
 * Uses [AlarmManager.setWindow] (inexact window) to respect Android battery restrictions without
 * requiring restricted exact-alarm special permissions.
 */
@SuppressLint("NewApi")
object ReminderScheduler {

    const val CHANNEL_ID = "reprise_daily_reminder_channel"
    const val EXTRA_IS_SECOND_REMINDER = "extra_is_second_reminder"

    const val REQUEST_CODE_PRIMARY = 1001
    const val REQUEST_CODE_SECONDARY = 1002
    private const val NOTIFICATION_ID_PRIMARY = 1001
    private const val NOTIFICATION_ID_SECONDARY = 1002
    private const val WINDOW_LENGTH_MILLIS = 10 * 60 * 1000L // 10-minute delivery window

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Push-up Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to complete your daily push-up target if not yet completed."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Synchronizes both primary and optional secondary reminders based on [settings].
     * Cancels and replaces any existing PendingIntent with the same request code so zero duplicates occur.
     */
    fun syncReminders(
        context: Context,
        settings: UserSettings,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val currentMillis = System.currentTimeMillis()

        // Primary reminder
        val primaryPendingIntent = buildReminderPendingIntent(
            context = context,
            requestCode = REQUEST_CODE_PRIMARY,
            isSecondReminder = false
        )
        alarmManager.cancel(primaryPendingIntent)

        if (settings.reminderEnabled) {
            val delayMillis = computeNextDelayMillis(
                now = now,
                targetHour = settings.reminderHour,
                targetMinute = settings.reminderMinute
            )
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                currentMillis + delayMillis,
                WINDOW_LENGTH_MILLIS,
                primaryPendingIntent
            )
        }

        // Secondary reminder before deadline
        val secondaryPendingIntent = buildReminderPendingIntent(
            context = context,
            requestCode = REQUEST_CODE_SECONDARY,
            isSecondReminder = true
        )
        alarmManager.cancel(secondaryPendingIntent)

        if (settings.reminderEnabled && settings.secondReminderEnabled) {
            val delayMillis = computeNextDelayMillis(
                now = now,
                targetHour = settings.secondReminderHour,
                targetMinute = settings.secondReminderMinute
            )
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                currentMillis + delayMillis,
                WINDOW_LENGTH_MILLIS,
                secondaryPendingIntent
            )
        }
    }

    private fun buildReminderPendingIntent(
        context: Context,
        requestCode: Int,
        isSecondReminder: Boolean
    ): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_IS_SECOND_REMINDER, isSecondReminder)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelTodayNotifications(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(NOTIFICATION_ID_PRIMARY)
        manager.cancel(NOTIFICATION_ID_SECONDARY)
    }

    /**
     * Pure helper for calculating the delay in milliseconds until the next occurrence of [targetHour]:[targetMinute].
     */
    fun computeNextDelayMillis(
        now: LocalDateTime,
        targetHour: Int,
        targetMinute: Int
    ): Long {
        val targetTime = LocalTime.of(targetHour.coerceIn(0, 23), targetMinute.coerceIn(0, 59))
        var nextTrigger = LocalDateTime.of(now.toLocalDate(), targetTime)
        if (!nextTrigger.isAfter(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }
        return Duration.between(now, nextTrigger).toMillis().coerceAtLeast(1_000L)
    }

    internal fun showReminderNotificationIfPermitted(
        context: Context,
        targetReps: Int,
        isSecondReminder: Boolean
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isSecondReminder) NOTIFICATION_ID_SECONDARY else NOTIFICATION_ID_PRIMARY,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSecondReminder) {
            "Final Call Before Midnight"
        } else {
            "RepRise Daily Challenge"
        }

        val body = if (isSecondReminder) {
            "Don't lose your streak! Today's challenge: $targetReps push-ups before midnight."
        } else {
            "Today's challenge: $targetReps push-ups. Keep your streak alive!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                if (isSecondReminder) NOTIFICATION_ID_SECONDARY else NOTIFICATION_ID_PRIMARY,
                notification
            )
        } catch (_: SecurityException) {
            // Gracefully handle runtime permission revocation
        }
    }
}
