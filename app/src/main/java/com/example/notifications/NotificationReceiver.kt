package com.example.notifications

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserSettingsStore
import com.example.domain.TargetCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * BroadcastReceiver triggered by [ReminderScheduler].
 * Verifies whether today's challenge is still incomplete before showing a notification,
 * and reschedules the next day's reminder.
 */
@SuppressLint("NewApi")
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val isSecondReminder = intent?.getBooleanExtra(
            ReminderScheduler.EXTRA_IS_SECOND_REMINDER,
            false
        ) ?: false

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(appContext)
                val settingsStore = UserSettingsStore(appContext)
                val settings = settingsStore.settingsFlow.first()
                val today = LocalDate.now()
                val history = database.pushUpDayDao().getAllDaysAsc()
                val todayRecord = history.find { it.localDate == today.toString() }

                // Only notify if today's challenge is still incomplete
                if (todayRecord == null || !todayRecord.isCompleted) {
                    val targetReps = TargetCalculator.calculateTargetForDate(today, history, settings)
                    ReminderScheduler.showReminderNotificationIfPermitted(
                        context = appContext,
                        targetReps = targetReps,
                        isSecondReminder = isSecondReminder
                    )
                }

                // Reschedule for tomorrow
                ReminderScheduler.syncReminders(appContext, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
