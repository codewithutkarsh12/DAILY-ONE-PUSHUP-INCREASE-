package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules daily reminders on device reboot, app update, or timezone/time changes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = UserSettingsStore(appContext).settingsFlow.first()
                    ReminderScheduler.syncReminders(appContext, settings)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
