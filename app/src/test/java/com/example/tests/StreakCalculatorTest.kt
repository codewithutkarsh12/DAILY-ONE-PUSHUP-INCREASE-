package com.example.tests

import com.example.data.local.PushUpDayDao
import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.UserSettings
import com.example.domain.ChallengeManager
import com.example.domain.DayChallengeStatus
import com.example.domain.StreakCalculator
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Fake DAO implementation that mirrors Room's unique index on `local_date` and
 * executes `markDayCompletedTransactional` and `recordPartialRepsTransactional` to test persistence & idempotency.
 */
private class FakePushUpDayDao : PushUpDayDao {
    private val rowsByDate = linkedMapOf<String, PushUpDayEntity>()
    private val flow = MutableStateFlow<List<PushUpDayEntity>>(emptyList())
    private var autoId = 1L

    private fun notifyChange() {
        flow.value = rowsByDate.values.sortedBy { it.localDate }
    }

    override fun observeAllDaysAsc(): Flow<List<PushUpDayEntity>> = flow

    override suspend fun getAllDaysAsc(): List<PushUpDayEntity> =
        rowsByDate.values.sortedBy { it.localDate }

    override suspend fun getDayByDate(localDate: String): PushUpDayEntity? =
        rowsByDate[localDate]

    override suspend fun upsertDay(entity: PushUpDayEntity): Long {
        val assignedId = if (entity.id == 0L) autoId++ else entity.id
        val saved = entity.copy(id = assignedId)
        rowsByDate[entity.localDate] = saved
        notifyChange()
        return assignedId
    }

    override suspend fun deleteDayByDate(localDate: String) {
        rowsByDate.remove(localDate)
        notifyChange()
    }

    override suspend fun deleteAllDays() {
        rowsByDate.clear()
        notifyChange()
    }
}

class StreakCalculatorTest {

    // Case D: Missing a full daily challenge breaks the active streak.
    @Test
    fun `Case D - missing a full daily challenge breaks active streak while keeping longest streak`() {
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)
        val day3 = LocalDate.of(2026, 10, 3)
        // Day 4 (2026-10-04) is missed
        val day5 = LocalDate.of(2026, 10, 5)

        val history = listOf(
            PushUpDayEntity(id = 1L, localDate = day1.toString(), targetReps = 10, isCompleted = true, actualReps = 10),
            PushUpDayEntity(id = 2L, localDate = day2.toString(), targetReps = 11, isCompleted = true, actualReps = 11),
            PushUpDayEntity(id = 3L, localDate = day3.toString(), targetReps = 12, isCompleted = true, actualReps = 12)
        )

        val statsOnDay5 = StreakCalculator.calculateStats(today = day5, history = history)
        assertEquals(0, statsOnDay5.activeStreak)
        assertEquals(3, statsOnDay5.longestStreak)
        assertEquals(3, statsOnDay5.totalCompletedDays)
        assertEquals(33, statsOnDay5.totalLoggedPushUps)
    }

    // Case F: Repeated taps do not create duplicate completions or increase the streak multiple times.
    @Test
    fun `Case F - repeated completion taps do not create duplicates or inflate streak`() = runTest {
        val dao = FakePushUpDayDao()
        val today = LocalDate.of(2026, 10, 10)

        // First tap succeeds
        val firstResult = dao.markDayCompletedTransactional(
            localDate = today.toString(),
            targetReps = 10,
            actualReps = 10,
            completedAtMillis = 1000L
        )
        // Second and third rapid taps on the same date are rejected by transactional check
        val secondResult = dao.markDayCompletedTransactional(
            localDate = today.toString(),
            targetReps = 10,
            actualReps = 15,
            completedAtMillis = 2000L
        )
        val thirdResult = dao.markDayCompletedTransactional(
            localDate = today.toString(),
            targetReps = 11,
            actualReps = 20,
            completedAtMillis = 3000L
        )

        assertTrue(firstResult)
        assertFalse(secondResult)
        assertFalse(thirdResult)

        val persisted = dao.getAllDaysAsc()
        assertEquals(1, persisted.size)
        assertEquals(10, persisted.first().actualReps)

        val stats = StreakCalculator.calculateStats(today = today, history = persisted)
        assertEquals(1, stats.activeStreak)
        assertEquals(1, stats.totalCompletedDays)
        assertEquals(10, stats.totalLoggedPushUps)
    }

    // Case G: Restarting the application preserves today's completion and all past history.
    @Test
    fun `Case G - restarting app re-evaluates persisted history and preserves today completion`() = runTest {
        val dao = FakePushUpDayDao()
        val settings = UserSettings()
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)

        dao.markDayCompletedTransactional(day1.toString(), targetReps = 10, actualReps = 12, completedAtMillis = 100L)
        dao.markDayCompletedTransactional(day2.toString(), targetReps = 11, actualReps = 11, completedAtMillis = 200L)

        // Simulate fresh app restart on Day 2 afternoon by reading persisted list from DAO
        val persistedHistoryOnRestart = dao.getAllDaysAsc()
        val dashboardOnRestart = ChallengeManager.evaluateDashboard(
            now = LocalDateTime.of(2026, 10, 2, 15, 30),
            history = persistedHistoryOnRestart,
            settings = settings
        )

        assertTrue(dashboardOnRestart.isTodayCompleted)
        assertEquals(11, dashboardOnRestart.todayTarget)
        assertEquals(12, dashboardOnRestart.nextDayTarget)
        assertEquals(2, dashboardOnRestart.activeStreak)
        assertEquals(2, dashboardOnRestart.longestStreak)
        assertEquals(2, dashboardOnRestart.totalCompletedDays)
        assertEquals(23, dashboardOnRestart.totalLoggedPushUps)
    }

    // Case H: Opening the app after multiple missed dates calculates the correct target and streak.
    @Test
    fun `Case H - reopening app after multiple missed dates recalculates zero active streak and preserves longest streak`() {
        val settings = UserSettings() // Default: restart at 10
        val history = listOf(
            PushUpDayEntity(id = 1L, localDate = "2026-10-01", targetReps = 10, isCompleted = true, actualReps = 10),
            PushUpDayEntity(id = 2L, localDate = "2026-10-02", targetReps = 11, isCompleted = true, actualReps = 11),
            PushUpDayEntity(id = 3L, localDate = "2026-10-03", targetReps = 12, isCompleted = true, actualReps = 15),
            PushUpDayEntity(id = 4L, localDate = "2026-10-04", targetReps = 13, isCompleted = true, actualReps = 13)
        )

        // App was closed for 10 days and reopened on 2026-10-15
        val reopenedNow = LocalDateTime.of(2026, 10, 15, 9, 0)
        val snapshot = ChallengeManager.evaluateDashboard(
            now = reopenedNow,
            history = history,
            settings = settings
        )

        assertEquals(0, snapshot.activeStreak)
        assertEquals(4, snapshot.longestStreak)
        assertEquals(4, snapshot.totalCompletedDays)
        assertEquals(49, snapshot.totalLoggedPushUps)
        assertEquals(10, snapshot.todayTarget)
        assertTrue(snapshot.streakBrokenRecently)
    }

    // Case K: Notification permission denial does not crash the application.
    @Test
    fun `Case K - dashboard evaluation and reminder delay calculation succeed regardless of notification permission state`() {
        val now = LocalDateTime.of(2026, 10, 20, 12, 0)
        val settings = UserSettings(reminderEnabled = true, reminderHour = 20, reminderMinute = 0)

        val snapshot = ChallengeManager.evaluateDashboard(
            now = now,
            history = emptyList(),
            settings = settings
        )
        val nextDelay = ReminderScheduler.computeNextDelayMillis(
            now = now,
            targetHour = settings.reminderHour,
            targetMinute = settings.reminderMinute
        )

        assertEquals(10, snapshot.todayTarget)
        assertTrue(nextDelay > 0L)
    }

    // Strict Target Validation: Lower count cannot be treated as completed target, but can be recorded as partial progress.
    @Test
    fun `recording actual reps below target does not complete challenge or increment streak`() = runTest {
        val dao = FakePushUpDayDao()
        val today = LocalDate.of(2026, 10, 20)

        // Attempting to mark complete with 6 reps when target is 10 must be rejected
        val completionAttempt = dao.markDayCompletedTransactional(
            localDate = today.toString(),
            targetReps = 10,
            actualReps = 6,
            completedAtMillis = 1000L
        )
        assertFalse(completionAttempt)

        // Saving 6 reps as partial progress stores the reps without completing the day
        val partialSaved = dao.recordPartialRepsTransactional(
            localDate = today.toString(),
            targetReps = 10,
            actualReps = 6
        )
        assertTrue(partialSaved)

        val statsAfterPartial = StreakCalculator.calculateStats(today, dao.getAllDaysAsc())
        assertFalse(statsAfterPartial.isTodayCompleted)
        assertEquals(0, statsAfterPartial.activeStreak)
        assertEquals(0, statsAfterPartial.totalCompletedDays)
        assertEquals(6, statsAfterPartial.totalLoggedPushUps)

        // Now completing with 10 reps upgrades the day to completed
        val completedSuccess = dao.markDayCompletedTransactional(
            localDate = today.toString(),
            targetReps = 10,
            actualReps = 10,
            completedAtMillis = 2000L
        )
        assertTrue(completedSuccess)

        val statsAfterComplete = StreakCalculator.calculateStats(today, dao.getAllDaysAsc())
        assertTrue(statsAfterComplete.isTodayCompleted)
        assertEquals(1, statsAfterComplete.activeStreak)
        assertEquals(1, statsAfterComplete.totalCompletedDays)
        assertEquals(10, statsAfterComplete.totalLoggedPushUps)
    }

    // Last 30 Days Performance Chart: Values match database records and calendar states
    @Test
    fun `last 30 days chart series matches persisted database records and statuses`() {
        val today = LocalDate.of(2026, 10, 20)
        val settings = UserSettings()
        val dayMinus2 = today.minusDays(2) // 2026-10-18 completed (10 reps)
        // dayMinus1 (2026-10-19) missed (0 reps)
        val history = listOf(
            PushUpDayEntity(
                id = 1L,
                localDate = dayMinus2.toString(),
                targetReps = 10,
                isCompleted = true,
                actualReps = 14
            )
        )

        val chartPoints = ChallengeManager.buildLast30DaysChartData(
            today = today,
            history = history,
            settings = settings
        )

        assertEquals(30, chartPoints.size)
        assertEquals(today.minusDays(29), chartPoints.first().date)
        assertEquals(today, chartPoints.last().date)

        val pointBeforeStart = chartPoints[26] // 2026-10-17
        assertEquals(DayChallengeStatus.BEFORE_START, pointBeforeStart.status)
        assertEquals(0, pointBeforeStart.actualReps)

        val pointCompleted = chartPoints[27] // 2026-10-18
        assertEquals(DayChallengeStatus.COMPLETED, pointCompleted.status)
        assertEquals(14, pointCompleted.actualReps)
        assertEquals(10, pointCompleted.targetReps)

        val pointMissed = chartPoints[28] // 2026-10-19
        assertEquals(DayChallengeStatus.MISSED, pointMissed.status)
        assertEquals(0, pointMissed.actualReps)
        assertEquals(11, pointMissed.targetReps)

        val pointToday = chartPoints[29] // 2026-10-20
        assertEquals(DayChallengeStatus.PENDING_TODAY, pointToday.status)
        assertEquals(0, pointToday.actualReps)
        assertEquals(10, pointToday.targetReps)
    }

    @Test
    fun `milestones unlock progressively based on longest streak value`() {
        val noneUnlocked = StreakCalculator.evaluateMilestones(longestStreak = 5)
        assertEquals(4, noneUnlocked.size)
        assertTrue(noneUnlocked.none { it.isUnlocked })

        val sevenUnlocked = StreakCalculator.evaluateMilestones(longestStreak = 7)
        assertTrue(sevenUnlocked.first { it.thresholdDays == 7 }.isUnlocked)
        assertFalse(sevenUnlocked.first { it.thresholdDays == 14 }.isUnlocked)

        val thirtyUnlocked = StreakCalculator.evaluateMilestones(longestStreak = 35)
        assertTrue(thirtyUnlocked.first { it.thresholdDays == 7 }.isUnlocked)
        assertTrue(thirtyUnlocked.first { it.thresholdDays == 14 }.isUnlocked)
        assertTrue(thirtyUnlocked.first { it.thresholdDays == 30 }.isUnlocked)
        assertFalse(thirtyUnlocked.first { it.thresholdDays == 100 }.isUnlocked)

        val allUnlocked = StreakCalculator.evaluateMilestones(longestStreak = 100)
        assertTrue(allUnlocked.all { it.isUnlocked })
    }
}
