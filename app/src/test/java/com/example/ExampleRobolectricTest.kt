package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.MissedStreakBehavior
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsStore
import com.example.data.repository.PushUpRepository
import com.example.domain.ChallengeManager
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RepRise", appName)
    }

    @Test
    fun `Case J Robolectric - changing reminder time replaces scheduled alarm without duplicates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val now = LocalDateTime.of(2026, 10, 20, 12, 0)
        val initialSettings = UserSettings(
            reminderEnabled = true,
            reminderHour = 20,
            reminderMinute = 0,
            secondReminderEnabled = false
        )
        ReminderScheduler.syncReminders(context, initialSettings, now)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Change reminder time to 21:30 and re-sync -> still only 1 scheduled alarm (no duplicates)
        val updatedSettings = initialSettings.copy(reminderHour = 21, reminderMinute = 30)
        ReminderScheduler.syncReminders(context, updatedSettings, now)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())
    }

    @Test
    fun `Case K Robolectric - showing notification when permission is not granted does not crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ReminderScheduler.showReminderNotificationIfPermitted(
            context = context,
            targetReps = 14,
            isSecondReminder = false
        )
    }

    @Test
    fun `Settings persist across reads in UserSettingsStore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = UserSettingsStore(context)

        store.updateChallengeConfig(
            startTarget = 12,
            dailyIncrement = 2,
            targetCapEnabled = true,
            targetCap = 50,
            missedStreakBehavior = MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET
        )
        store.updatePrimaryReminder(enabled = true, hour = 19, minute = 45)
        store.updateThemeMode(AppThemeMode.LIGHT)

        val persisted = store.settingsFlow.first()
        assertEquals(12, persisted.startTarget)
        assertEquals(2, persisted.dailyIncrement)
        assertTrue(persisted.targetCapEnabled)
        assertEquals(50, persisted.targetCap)
        assertEquals(MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET, persisted.missedStreakBehavior)
        assertEquals(19, persisted.reminderHour)
        assertEquals(45, persisted.reminderMinute)
        assertEquals(AppThemeMode.LIGHT, persisted.themeMode)

        // Reset back to defaults for test isolation
        store.resetSettingsToDefaults()
    }

    @Test
    fun `Full Room + Repository integration - partial progress, settings change, completion, undo, CSV export, and clear`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val store = UserSettingsStore(context)
        store.resetSettingsToDefaults()
        val repo = PushUpRepository(db.pushUpDayDao(), store)

        val today = LocalDate.of(2026, 10, 20)

        // 1. Log partial progress (5 / 10 reps)
        val partialSaved = repo.recordPartialRepsForDate(date = today, targetReps = 10, actualReps = 5)
        assertTrue(partialSaved)

        var snapshot = ChallengeManager.evaluateDashboard(
            now = today.atTime(14, 0),
            history = repo.getAllDaysSnapshot(),
            settings = store.settingsFlow.first()
        )
        assertFalse(snapshot.isTodayCompleted)
        assertEquals(5, snapshot.todayActualReps)
        assertEquals(0, snapshot.activeStreak)

        // 2. User changes startTarget in Settings to 8 before completing today
        store.updateChallengeConfig(
            startTarget = 8,
            dailyIncrement = 1,
            targetCapEnabled = false,
            targetCap = 100,
            missedStreakBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET
        )

        // 3. Complete today with 8 reps (matches updated target of 8)
        val completed = repo.completeChallengeForDate(
            date = today,
            targetReps = 8,
            actualReps = 8,
            completedAtMillis = 1700000000000L
        )
        assertTrue(completed)

        snapshot = ChallengeManager.evaluateDashboard(
            now = today.atTime(15, 0),
            history = repo.getAllDaysSnapshot(),
            settings = store.settingsFlow.first()
        )
        assertTrue(snapshot.isTodayCompleted)
        assertEquals(1, snapshot.activeStreak)
        assertEquals(1, snapshot.totalCompletedDays)
        assertEquals(8, snapshot.totalLoggedPushUps)
        assertEquals(9, snapshot.nextDayTarget)

        // 4. Verify CSV export contains header and today's record
        val csv = repo.buildCsvExport(repo.getAllDaysSnapshot())
        assertTrue(csv.contains("local_date,target_reps,is_completed,actual_reps,completed_at_epoch_millis"))
        assertTrue(csv.contains("2026-10-20,8,true,8,1700000000000"))

        // 5. Undo today's completion
        repo.undoCompletionForDate(today)
        snapshot = ChallengeManager.evaluateDashboard(
            now = today.atTime(15, 5),
            history = repo.getAllDaysSnapshot(),
            settings = store.settingsFlow.first()
        )
        assertFalse(snapshot.isTodayCompleted)
        assertEquals(0, snapshot.activeStreak)
        assertEquals(0, snapshot.totalCompletedDays)

        // 6. Clear all history
        repo.clearAllHistory()
        assertTrue(repo.getAllDaysSnapshot().isEmpty())

        store.resetSettingsToDefaults()
        db.close()
    }
}
