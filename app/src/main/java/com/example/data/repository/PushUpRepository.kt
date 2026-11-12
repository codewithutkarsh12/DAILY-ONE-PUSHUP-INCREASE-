package com.example.data.repository

import android.annotation.SuppressLint
import com.example.data.local.PushUpDayDao
import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsStore
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@SuppressLint("NewApi")
class PushUpRepository(
    private val dao: PushUpDayDao,
    private val settingsStore: UserSettingsStore
) {
    val allHistoryFlow: Flow<List<PushUpDayEntity>> = dao.observeAllDaysAsc()
    val settingsFlow: Flow<UserSettings> = settingsStore.settingsFlow

    suspend fun getAllDaysSnapshot(): List<PushUpDayEntity> {
        return dao.getAllDaysAsc()
    }

    suspend fun getRecordForDate(date: LocalDate): PushUpDayEntity? {
        return dao.getDayByDate(date.toString())
    }

    /**
     * Completes the given date's challenge atomically.
     * If [actualReps] is null, defaults to [targetReps].
     * If [actualReps] is provided and is less than [targetReps], returns false without completing.
     */
    suspend fun completeChallengeForDate(
        date: LocalDate,
        targetReps: Int,
        actualReps: Int?,
        completedAtMillis: Long
    ): Boolean {
        val resolvedActual = actualReps ?: targetReps
        if (resolvedActual < targetReps) {
            return false
        }
        return dao.markDayCompletedTransactional(
            localDate = date.toString(),
            targetReps = targetReps,
            actualReps = resolvedActual,
            completedAtMillis = completedAtMillis
        )
    }

    /**
     * Saves partial reps toward today's challenge without marking the day complete.
     */
    suspend fun recordPartialRepsForDate(
        date: LocalDate,
        targetReps: Int,
        actualReps: Int
    ): Boolean {
        return dao.recordPartialRepsTransactional(
            localDate = date.toString(),
            targetReps = targetReps,
            actualReps = actualReps
        )
    }

    /**
     * Undoes today's completion (only allowed for today's date immediately after completion).
     */
    suspend fun undoCompletionForDate(date: LocalDate) {
        dao.deleteDayByDate(date.toString())
    }

    suspend fun clearAllHistory() {
        dao.deleteAllDays()
    }

    fun buildCsvExport(records: List<PushUpDayEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("local_date,target_reps,is_completed,actual_reps,completed_at_epoch_millis")
        for (record in records) {
            sb.append(record.localDate).append(',')
                .append(record.targetReps).append(',')
                .append(record.isCompleted).append(',')
                .append(record.actualReps ?: "").append(',')
                .append(record.completedAtEpochMillis ?: "")
                .appendLine()
        }
        return sb.toString()
    }

    /**
     * Parses and restores workout history from a RepRise CSV backup.
     * Validates each row (ISO-8601 date, positive target, valid completion state) and merges into Room.
     * Returns the number of valid records imported.
     */
    suspend fun importCsvHistory(csvContent: String): Int {
        var importedCount = 0
        val lines = csvContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        for (line in lines) {
            if (line.startsWith("local_date", ignoreCase = true)) continue
            val parts = line.split(",")
            if (parts.size < 3) continue

            val parsedDate = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: continue
            val targetReps = parts[1].trim().toIntOrNull()?.takeIf { it in 1..10000 } ?: continue
            val isCompleted = parts[2].trim().toBooleanStrictOrNull() ?: continue
            val actualReps = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
                ?.coerceIn(0, 10000)
            val completedAt = parts.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull()

            val existing = dao.getDayByDate(parsedDate.toString())
            val resolvedActual = when {
                isCompleted && (actualReps == null || actualReps < targetReps) -> targetReps
                else -> actualReps
            }

            dao.upsertDay(
                PushUpDayEntity(
                    id = existing?.id ?: 0L,
                    localDate = parsedDate.toString(),
                    targetReps = targetReps,
                    isCompleted = isCompleted,
                    actualReps = resolvedActual,
                    completedAtEpochMillis = if (isCompleted) (completedAt ?: System.currentTimeMillis()) else null
                )
            )
            importedCount++
        }
        return importedCount
    }
}
