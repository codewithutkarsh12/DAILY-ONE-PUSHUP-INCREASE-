package com.example.domain

import android.annotation.SuppressLint
import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.MissedStreakBehavior
import com.example.data.preferences.UserSettings
import java.time.LocalDate

/**
 * Pure, deterministic calculator for daily push-up targets.
 *
 * Rules:
 * 1. If [targetDate] already has a completed record, its target is locked at [PushUpDayEntity.targetReps]
 *    so changing settings later never corrupts historical completed targets.
 * 2. If yesterday was completed, today's target = yesterday.targetReps + settings.dailyIncrement
 *    (capped at [settings.targetCap] if [settings.targetCapEnabled] is true).
 * 3. If yesterday was NOT completed (either no prior history or at least one full day was missed):
 *    - Under [MissedStreakBehavior.RESTART_AT_START_TARGET] (default): target is [settings.startTarget] (default 10).
 *    - Under [MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET]: if there is a previous completed day,
 *      target resumes at the last completed day's targetReps (capped if cap enabled), or [settings.startTarget]
 *      if no day was ever completed.
 */
@SuppressLint("NewApi")
object TargetCalculator {

    fun calculateTargetForDate(
        targetDate: LocalDate,
        history: List<PushUpDayEntity>,
        settings: UserSettings
    ): Int {
        val byDate = history.associateBy { LocalDate.parse(it.localDate) }

        // Rule 1: Preserve completed record's target for targetDate if already completed
        val existingToday = byDate[targetDate]
        if (existingToday != null && existingToday.isCompleted) {
            return existingToday.targetReps
        }

        val yesterday = targetDate.minusDays(1)
        val yesterdayRecord = byDate[yesterday]

        val rawTarget = if (yesterdayRecord != null && yesterdayRecord.isCompleted) {
            // Consecutive day following a completed yesterday
            yesterdayRecord.targetReps + settings.dailyIncrement
        } else {
            // First day ever OR a missed day broke the streak
            when (settings.missedStreakBehavior) {
                MissedStreakBehavior.RESTART_AT_START_TARGET -> settings.startTarget
                MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET -> {
                    val lastCompletedBeforeToday = byDate.entries
                        .asSequence()
                        .filter { (date, record) -> record.isCompleted && date.isBefore(targetDate) }
                        .maxByOrNull { it.key }
                        ?.value

                    lastCompletedBeforeToday?.targetReps ?: settings.startTarget
                }
            }
        }

        return applyCap(rawTarget, settings)
    }

    private fun applyCap(target: Int, settings: UserSettings): Int {
        val safeTarget = target.coerceAtLeast(1)
        return if (settings.targetCapEnabled) {
            safeTarget.coerceAtMost(settings.targetCap.coerceAtLeast(settings.startTarget))
        } else {
            safeTarget
        }
    }
}
