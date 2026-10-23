package com.example.domain

import android.annotation.SuppressLint
import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.UserSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class DayChallengeStatus {
    COMPLETED,
    PENDING_TODAY,
    MISSED,
    FUTURE,
    BEFORE_START
}

data class ChartDayPoint(
    val date: LocalDate,
    val actualReps: Int,
    val targetReps: Int,
    val status: DayChallengeStatus
)

data class DailyChallengeSnapshot(
    val todayDate: LocalDate,
    val todayTarget: Int,
    val todayActualReps: Int?,
    val isTodayCompleted: Boolean,
    val completedAtMillis: Long?,
    val nextDayTarget: Int,
    val activeStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val totalLoggedPushUps: Int,
    val remainingMillisToday: Long,
    val motivationalMessage: String,
    val streakBrokenRecently: Boolean
)

/**
 * Coordinates [TargetCalculator] and [StreakCalculator], evaluates calendar day states,
 * 30-day performance chart series, remaining countdown until midnight, and motivational messages.
 */
@SuppressLint("NewApi")
object ChallengeManager {

    fun evaluateDashboard(
        now: LocalDateTime,
        history: List<PushUpDayEntity>,
        settings: UserSettings
    ): DailyChallengeSnapshot {
        val today = now.toLocalDate()
        val todayRecord = history.find { it.localDate == today.toString() }
        val todayTarget = TargetCalculator.calculateTargetForDate(today, history, settings)
        val streakStats = StreakCalculator.calculateStats(today, history)

        // Compute what tomorrow's target will be once today is completed
        val simulatedHistoryIfTodayDone = if (streakStats.isTodayCompleted) {
            history
        } else {
            history.filterNot { it.localDate == today.toString() } + PushUpDayEntity(
                localDate = today.toString(),
                targetReps = todayTarget,
                isCompleted = true,
                actualReps = todayTarget
            )
        }
        val nextDayTarget = TargetCalculator.calculateTargetForDate(
            targetDate = today.plusDays(1),
            history = simulatedHistoryIfTodayDone,
            settings = settings
        )

        val endOfDay = today.plusDays(1).atStartOfDay()
        val remainingMillis = Duration.between(now, endOfDay).toMillis().coerceAtLeast(0L)

        val hasPastCompletions = history.any {
            it.isCompleted && LocalDate.parse(it.localDate).isBefore(today)
        }
        val yesterdayCompleted = history.any {
            it.isCompleted && it.localDate == today.minusDays(1).toString()
        }
        val streakBrokenRecently =
            hasPastCompletions && !yesterdayCompleted && !streakStats.isTodayCompleted

        val motivationalMessage = buildMotivationalMessage(
            isTodayCompleted = streakStats.isTodayCompleted,
            activeStreak = streakStats.activeStreak,
            streakBrokenRecently = streakBrokenRecently,
            todayTarget = todayTarget,
            nextDayTarget = nextDayTarget
        )

        return DailyChallengeSnapshot(
            todayDate = today,
            todayTarget = todayTarget,
            todayActualReps = todayRecord?.effectiveLoggedReps,
            isTodayCompleted = streakStats.isTodayCompleted,
            completedAtMillis = todayRecord?.completedAtEpochMillis,
            nextDayTarget = nextDayTarget,
            activeStreak = streakStats.activeStreak,
            longestStreak = streakStats.longestStreak,
            totalCompletedDays = streakStats.totalCompletedDays,
            totalLoggedPushUps = streakStats.totalLoggedPushUps,
            remainingMillisToday = remainingMillis,
            motivationalMessage = motivationalMessage,
            streakBrokenRecently = streakBrokenRecently
        )
    }

    /**
     * Evaluates a calendar date's status for the History calendar view.
     * Once a user has started using the app (i.e., on or after their earliest recorded date),
     * any past uncompleted date before today is marked as [DayChallengeStatus.MISSED].
     * Future dates are never marked as missed.
     */
    fun getDayStatus(
        date: LocalDate,
        today: LocalDate,
        historyByDate: Map<LocalDate, PushUpDayEntity>,
        firstRecordedDate: LocalDate?
    ): DayChallengeStatus {
        if (date.isAfter(today)) return DayChallengeStatus.FUTURE

        val record = historyByDate[date]
        if (record != null && record.isCompleted) {
            return DayChallengeStatus.COMPLETED
        }

        if (date == today) {
            return DayChallengeStatus.PENDING_TODAY
        }

        return if (firstRecordedDate != null && !date.isBefore(firstRecordedDate)) {
            DayChallengeStatus.MISSED
        } else {
            DayChallengeStatus.BEFORE_START
        }
    }

    /**
     * Builds the 30-day performance dataset for the History screen bar chart (`today - 29 days` .. `today`),
     * strictly derived from persisted database records and deterministic target rules.
     */
    fun buildLast30DaysChartData(
        today: LocalDate,
        history: List<PushUpDayEntity>,
        settings: UserSettings
    ): List<ChartDayPoint> {
        val parsedEntries = history.map { LocalDate.parse(it.localDate) to it }
        val historyByDate = parsedEntries.toMap()
        val firstRecordedDate = parsedEntries
            .asSequence()
            .filter { (_, entity) -> entity.isCompleted || (entity.actualReps ?: 0) > 0 }
            .minOfOrNull { it.first }

        val startDay = today.minusDays(29)
        return (0L..29L).map { offset ->
            val date = startDay.plusDays(offset)
            val record = historyByDate[date]
            val actual = record?.effectiveLoggedReps ?: 0
            val target = if (record != null && record.isCompleted) {
                record.targetReps
            } else {
                val subsetUpToDate = parsedEntries
                    .filter { (d, _) -> !d.isAfter(date) }
                    .map { it.second }
                TargetCalculator.calculateTargetForDate(
                    targetDate = date,
                    history = subsetUpToDate,
                    settings = settings
                )
            }
            val status = getDayStatus(
                date = date,
                today = today,
                historyByDate = historyByDate,
                firstRecordedDate = firstRecordedDate
            )
            ChartDayPoint(
                date = date,
                actualReps = actual,
                targetReps = target,
                status = status
            )
        }
    }

    fun formatCountdown(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    private fun buildMotivationalMessage(
        isTodayCompleted: Boolean,
        activeStreak: Int,
        streakBrokenRecently: Boolean,
        todayTarget: Int,
        nextDayTarget: Int
    ): String {
        return when {
            isTodayCompleted && activeStreak == 1 ->
                "Day 1 complete. Tomorrow's target increases to $nextDayTarget push-ups."
            isTodayCompleted && activeStreak in 2..6 ->
                "$activeStreak consecutive days complete. Tomorrow's target: $nextDayTarget push-ups."
            isTodayCompleted && activeStreak >= 7 ->
                "$activeStreak-day streak locked in. Tomorrow's target: $nextDayTarget push-ups."
            streakBrokenRecently ->
                "Previous streak ended, but your past history is saved. Start fresh today with $todayTarget push-ups."
            activeStreak == 0 ->
                "Complete $todayTarget push-ups today to start your streak."
            else ->
                "Complete $todayTarget push-ups before midnight to reach a ${activeStreak + 1}-day streak."
        }
    }
}
