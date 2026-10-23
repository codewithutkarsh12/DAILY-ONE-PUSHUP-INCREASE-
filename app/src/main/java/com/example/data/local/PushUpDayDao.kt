package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PushUpDayDao {

    @Query("SELECT * FROM push_up_days ORDER BY local_date ASC")
    fun observeAllDaysAsc(): Flow<List<PushUpDayEntity>>

    @Query("SELECT * FROM push_up_days ORDER BY local_date ASC")
    suspend fun getAllDaysAsc(): List<PushUpDayEntity>

    @Query("SELECT * FROM push_up_days WHERE local_date = :localDate LIMIT 1")
    suspend fun getDayByDate(localDate: String): PushUpDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(entity: PushUpDayEntity): Long

    @Query("DELETE FROM push_up_days WHERE local_date = :localDate")
    suspend fun deleteDayByDate(localDate: String)

    @Query("DELETE FROM push_up_days")
    suspend fun deleteAllDays()

    /**
     * Enforces one completion record per local date inside a database transaction.
     * Rejects completion if [actualReps] is less than the required target, or if the day
     * is already marked completed.
     * Returns true if the record was newly marked completed, or false otherwise.
     */
    @Transaction
    suspend fun markDayCompletedTransactional(
        localDate: String,
        targetReps: Int,
        actualReps: Int,
        completedAtMillis: Long
    ): Boolean {
        val existing = getDayByDate(localDate)
        if (existing != null && existing.isCompleted) {
            return false
        }
        val effectiveTarget = targetReps.coerceAtLeast(1)
        if (actualReps < effectiveTarget) {
            // Do not allow a count lower than target to be treated as a completed challenge
            return false
        }
        val record = PushUpDayEntity(
            id = existing?.id ?: 0L,
            localDate = localDate,
            targetReps = effectiveTarget,
            isCompleted = true,
            completedAtEpochMillis = completedAtMillis,
            actualReps = actualReps
        )
        upsertDay(record)
        return true
    }

    /**
     * Records partial repetitions for [localDate] when the user logs progress below the daily target.
     * Never marks the challenge completed if [actualReps] < target.
     */
    @Transaction
    suspend fun recordPartialRepsTransactional(
        localDate: String,
        targetReps: Int,
        actualReps: Int
    ): Boolean {
        val existing = getDayByDate(localDate)
        if (existing != null && existing.isCompleted) {
            return false
        }
        val effectiveTarget = targetReps.coerceAtLeast(1)
        val clampedReps = actualReps.coerceIn(0, (effectiveTarget - 1).coerceAtLeast(0))
        val record = PushUpDayEntity(
            id = existing?.id ?: 0L,
            localDate = localDate,
            targetReps = effectiveTarget,
            isCompleted = false,
            completedAtEpochMillis = null,
            actualReps = clampedReps
        )
        upsertDay(record)
        return true
    }
}
