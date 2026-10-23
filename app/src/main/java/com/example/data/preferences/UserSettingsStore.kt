package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reprise_user_settings")

enum class MissedStreakBehavior {
    RESTART_AT_START_TARGET,
    RESUME_FROM_PREVIOUS_TARGET
}

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class UserSettings(
    val startTarget: Int = 10,
    val dailyIncrement: Int = 1,
    val targetCapEnabled: Boolean = false,
    val targetCap: Int = 100,
    val missedStreakBehavior: MissedStreakBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20, // Default 8:00 PM
    val reminderMinute: Int = 0,
    val secondReminderEnabled: Boolean = false,
    val secondReminderHour: Int = 22, // Default 10:30 PM
    val secondReminderMinute: Int = 30,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val hapticFeedbackEnabled: Boolean = true
)

class UserSettingsStore(private val context: Context) {

    private object Keys {
        val START_TARGET = intPreferencesKey("start_target")
        val DAILY_INCREMENT = intPreferencesKey("daily_increment")
        val TARGET_CAP_ENABLED = booleanPreferencesKey("target_cap_enabled")
        val TARGET_CAP = intPreferencesKey("target_cap")
        val MISSED_BEHAVIOR = stringPreferencesKey("missed_behavior")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SECOND_REMINDER_ENABLED = booleanPreferencesKey("second_reminder_enabled")
        val SECOND_REMINDER_HOUR = intPreferencesKey("second_reminder_hour")
        val SECOND_REMINDER_MINUTE = intPreferencesKey("second_reminder_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val startTarget = (prefs[Keys.START_TARGET] ?: 10).coerceIn(1, 1000)
            val dailyIncrement = (prefs[Keys.DAILY_INCREMENT] ?: 1).coerceIn(1, 100)
            val targetCapEnabled = prefs[Keys.TARGET_CAP_ENABLED] ?: false
            val targetCap = (prefs[Keys.TARGET_CAP] ?: 100).coerceAtLeast(startTarget)

            val missedBehavior = runCatching {
                MissedStreakBehavior.valueOf(
                    prefs[Keys.MISSED_BEHAVIOR] ?: MissedStreakBehavior.RESTART_AT_START_TARGET.name
                )
            }.getOrDefault(MissedStreakBehavior.RESTART_AT_START_TARGET)

            val themeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name)
            }.getOrDefault(AppThemeMode.DARK)

            UserSettings(
                startTarget = startTarget,
                dailyIncrement = dailyIncrement,
                targetCapEnabled = targetCapEnabled,
                targetCap = targetCap,
                missedStreakBehavior = missedBehavior,
                reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
                reminderHour = (prefs[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23),
                reminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
                secondReminderEnabled = prefs[Keys.SECOND_REMINDER_ENABLED] ?: false,
                secondReminderHour = (prefs[Keys.SECOND_REMINDER_HOUR] ?: 22).coerceIn(0, 23),
                secondReminderMinute = (prefs[Keys.SECOND_REMINDER_MINUTE] ?: 30).coerceIn(0, 59),
                themeMode = themeMode,
                hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true
            )
        }

    suspend fun updateChallengeConfig(
        startTarget: Int,
        dailyIncrement: Int,
        targetCapEnabled: Boolean,
        targetCap: Int,
        missedStreakBehavior: MissedStreakBehavior
    ) {
        val safeStart = startTarget.coerceIn(1, 1000)
        val safeIncrement = dailyIncrement.coerceIn(1, 100)
        val safeCap = targetCap.coerceIn(safeStart, 5000)

        context.dataStore.edit { prefs ->
            prefs[Keys.START_TARGET] = safeStart
            prefs[Keys.DAILY_INCREMENT] = safeIncrement
            prefs[Keys.TARGET_CAP_ENABLED] = targetCapEnabled
            prefs[Keys.TARGET_CAP] = safeCap
            prefs[Keys.MISSED_BEHAVIOR] = missedStreakBehavior.name
        }
    }

    suspend fun updatePrimaryReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
            prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun updateSecondReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SECOND_REMINDER_ENABLED] = enabled
            prefs[Keys.SECOND_REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.SECOND_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun resetSettingsToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
