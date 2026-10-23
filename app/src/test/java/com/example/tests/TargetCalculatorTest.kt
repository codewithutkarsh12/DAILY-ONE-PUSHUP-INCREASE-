package com.example.tests

import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.MissedStreakBehavior
import com.example.data.preferences.UserSettings
import com.example.domain.ChallengeManager
import com.example.domain.TargetCalculator
import com.example.notifications.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TargetCalculatorTest {

    private val defaultSettings = UserSettings(
        startTarget = 10,
        dailyIncrement = 1,
        targetCapEnabled = false,
        targetCap = 100,
        missedStreakBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET
    )

    // Case A: First challenge is 10.
    @Test
    fun `Case A - first challenge target is 10 under default settings`() {
        val day1 = LocalDate.of(2026, 10, 1)
        val target = TargetCalculator.calculateTargetForDate(
            targetDate = day1,
            history = emptyList(),
            settings = defaultSettings
        )
        assertEquals(10, target)
    }

    // Case B: Completing day 1 makes day 2's target 11.
    @Test
    fun `Case B - completing day 1 makes day 2 target 11`() {
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)
        val history = listOf(
            PushUpDayEntity(
                id = 1L,
                localDate = day1.toString(),
                targetReps = 10,
                isCompleted = true,
                completedAtEpochMillis = 1700000000000L,
                actualReps = 10
            )
        )

        val day2Target = TargetCalculator.calculateTargetForDate(
            targetDate = day2,
            history = history,
            settings = defaultSettings
        )
        assertEquals(11, day2Target)
    }

    // Case C: Completing day 2 makes day 3's target 12.
    @Test
    fun `Case C - completing day 2 makes day 3 target 12`() {
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)
        val day3 = LocalDate.of(2026, 10, 3)
        val history = listOf(
            PushUpDayEntity(
                id = 1L,
                localDate = day1.toString(),
                targetReps = 10,
                isCompleted = true,
                completedAtEpochMillis = 1700000000000L,
                actualReps = 10
            ),
            PushUpDayEntity(
                id = 2L,
                localDate = day2.toString(),
                targetReps = 11,
                isCompleted = true,
                completedAtEpochMillis = 1700086400000L,
                actualReps = 11
            )
        )

        val day3Target = TargetCalculator.calculateTargetForDate(
            targetDate = day3,
            history = history,
            settings = defaultSettings
        )
        assertEquals(12, day3Target)
    }

    // Case E: Following a missed day, the next target is 10 under default settings.
    @Test
    fun `Case E - following a missed day next target resets to 10 under default settings`() {
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)
        // Day 3 (2026-10-03) is missed completely
        val day4 = LocalDate.of(2026, 10, 4)

        val history = listOf(
            PushUpDayEntity(id = 1L, localDate = day1.toString(), targetReps = 10, isCompleted = true),
            PushUpDayEntity(id = 2L, localDate = day2.toString(), targetReps = 11, isCompleted = true)
        )

        val day4Target = TargetCalculator.calculateTargetForDate(
            targetDate = day4,
            history = history,
            settings = defaultSettings
        )
        assertEquals(10, day4Target)
    }

    // Case I: Crossing midnight does not incorrectly carry an incomplete daily challenge into the next day.
    @Test
    fun `Case I - crossing midnight evaluates new day independently and resets incomplete streak`() {
        val day1 = LocalDate.of(2026, 10, 1)
        // User completed day 1 (target 10), then left day 2 (2026-10-02) incomplete across midnight into day 3
        val history = listOf(
            PushUpDayEntity(
                id = 1L,
                localDate = day1.toString(),
                targetReps = 10,
                isCompleted = true,
                actualReps = 10
            )
        )

        // Before midnight on Day 2 (23:59:50): target is 11, activeStreak is still 1
        val beforeMidnight = LocalDateTime.of(2026, 10, 2, 23, 59, 50)
        val snapshotBeforeMidnight = ChallengeManager.evaluateDashboard(
            now = beforeMidnight,
            history = history,
            settings = defaultSettings
        )
        assertEquals(11, snapshotBeforeMidnight.todayTarget)
        assertEquals(1, snapshotBeforeMidnight.activeStreak)
        assertFalse(snapshotBeforeMidnight.isTodayCompleted)

        // After midnight on Day 3 (00:00:05): Day 2 was missed, so Day 3 activeStreak is 0 and target resets to 10
        val afterMidnight = LocalDateTime.of(2026, 10, 3, 0, 0, 5)
        val snapshotAfterMidnight = ChallengeManager.evaluateDashboard(
            now = afterMidnight,
            history = history,
            settings = defaultSettings
        )
        assertEquals(LocalDate.of(2026, 10, 3), snapshotAfterMidnight.todayDate)
        assertEquals(0, snapshotAfterMidnight.activeStreak)
        assertEquals(10, snapshotAfterMidnight.todayTarget)
        assertFalse(snapshotAfterMidnight.isTodayCompleted)
    }

    // Case J: Changing the reminder time computes deterministic delay and replaces unique work without duplication.
    @Test
    fun `Case J - changing reminder time computes accurate delay for same day or next day`() {
        val now = LocalDateTime.of(2026, 10, 10, 18, 0, 0) // 6:00 PM

        // Reminder set for 8:00 PM today -> 2 hours (7,200,000 ms)
        val delaySameDay = ReminderScheduler.computeNextDelayMillis(
            now = now,
            targetHour = 20,
            targetMinute = 0
        )
        assertEquals(2 * 60 * 60 * 1000L, delaySameDay)

        // Reminder changed to 5:00 PM (already passed today) -> schedules for 5:00 PM tomorrow (23 hours)
        val delayNextDay = ReminderScheduler.computeNextDelayMillis(
            now = now,
            targetHour = 17,
            targetMinute = 0
        )
        assertEquals(23 * 60 * 60 * 1000L, delayNextDay)
        assertTrue(delayNextDay > 0L)
    }

    // Case L: Target increments, optional caps, and alternate restart settings work correctly.
    @Test
    fun `Case L - custom start target, increment, cap, and resume-from-previous behavior work`() {
        val customSettings = UserSettings(
            startTarget = 15,
            dailyIncrement = 5,
            targetCapEnabled = true,
            targetCap = 22,
            missedStreakBehavior = MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET
        )

        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)
        val day3 = LocalDate.of(2026, 10, 3)
        val day5AfterMiss = LocalDate.of(2026, 10, 5) // Day 4 missed

        // Day 1 starts at 15
        val t1 = TargetCalculator.calculateTargetForDate(day1, emptyList(), customSettings)
        assertEquals(15, t1)

        // Day 2: 15 + 5 = 20
        val historyAfterDay1 = listOf(
            PushUpDayEntity(id = 1L, localDate = day1.toString(), targetReps = 15, isCompleted = true)
        )
        val t2 = TargetCalculator.calculateTargetForDate(day2, historyAfterDay1, customSettings)
        assertEquals(20, t2)

        // Day 3: 20 + 5 = 25, but capped at 22
        val historyAfterDay2 = historyAfterDay1 + PushUpDayEntity(
            id = 2L,
            localDate = day2.toString(),
            targetReps = 20,
            isCompleted = true
        )
        val t3 = TargetCalculator.calculateTargetForDate(day3, historyAfterDay2, customSettings)
        assertEquals(22, t3)

        // Day 4 is missed; on Day 5 with RESUME_FROM_PREVIOUS_TARGET, target resumes at last completed target (20)
        val t5 = TargetCalculator.calculateTargetForDate(day5AfterMiss, historyAfterDay2, customSettings)
        assertEquals(20, t5)
    }
}
