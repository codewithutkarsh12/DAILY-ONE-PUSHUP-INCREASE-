package com.example.domain

import android.annotation.SuppressLint
import com.example.data.local.PushUpDayEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakMilestone(
    val thresholdDays: Int,
    val badgeLabel: String,
    val title: String,
    val isUnlocked: Boolean
)

data class StreakStats(
    val activeStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val totalLoggedPushUps: Int,
    val isTodayCompleted: Boolean,
    val milestones: List<StreakMilestone> = emptyList()
)

/**
 * Pure, deterministic calculator for active streak, longest-ever streak, milestone achievements, and aggregate totals.
 */
@SuppressLint("NewApi")
object StreakCalculator {

    private val MILESTONE_DEFINITIONS = listOf(
        7 to "7-Day Spark",
        14 to "14-Day Forge",
        30 to "30-Day Monthly",
        100 to "100-Day Centurion"
    )

    fun evaluateMilestones(longestStreak: Int): List<StreakMilestone> {
        return MILESTONE_DEFINITIONS.map { (days, title) ->
            StreakMilestone(
                thresholdDays = days,
                badgeLabel = "${days}d",
                title = title,
                isUnlocked = longestStreak >= days
            )
        }
    }

    fun calculateStats(
        today: LocalDate,
        history: List<PushUpDayEntity>
    ): StreakStats {
        // Deduplicate by LocalDate defensively, keeping completed records
        val completedByDate = history
            .filter { it.isCompleted && LocalDate.parse(it.localDate) <= today }
            .associateBy { LocalDate.parse(it.localDate) }

        val completedDatesSorted = completedByDate.keys.sorted()

        val isTodayCompleted = completedByDate.containsKey(today)

        // Calculate active streak
        val activeStreak = calculateActiveStreak(today, completedByDate)

        // Calculate longest streak ever
        val longestStreak = maxOf(calculateLongestStreak(completedDatesSorted), activeStreak)

        // Totals
        val uniqueRecords = history.distinctBy { it.localDate }
        val totalCompletedDays = uniqueRecords.count { it.isCompleted }
        val totalLoggedPushUps = uniqueRecords.sumOf { it.effectiveLoggedReps }

        return StreakStats(
            activeStreak = activeStreak,
            longestStreak = longestStreak,
            totalCompletedDays = totalCompletedDays,
            totalLoggedPushUps = totalLoggedPushUps,
            isTodayCompleted = isTodayCompleted,
            milestones = evaluateMilestones(longestStreak)
        )
    }

    private fun calculateActiveStreak(
        today: LocalDate,
        completedByDate: Map<LocalDate, PushUpDayEntity>
    ): Int {
        var cursorDate = when {
            completedByDate.containsKey(today) -> today
            completedByDate.containsKey(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        while (completedByDate.containsKey(cursorDate)) {
            streak++
            cursorDate = cursorDate.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(sortedUniqueCompletedDates: List<LocalDate>): Int {
        if (sortedUniqueCompletedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentRun = 1

        for (i in 1 until sortedUniqueCompletedDates.size) {
            val prev = sortedUniqueCompletedDates[i - 1]
            val curr = sortedUniqueCompletedDates[i]
            val daysBetween = ChronoUnit.DAYS.between(prev, curr)
            when (daysBetween) {
                0L -> Unit
                1L -> {
                    currentRun++
                    if (currentRun > maxStreak) {
                        maxStreak = currentRun
                    }
                }
                else -> {
                    currentRun = 1
                }
            }
        }
        return maxStreak
    }
}
