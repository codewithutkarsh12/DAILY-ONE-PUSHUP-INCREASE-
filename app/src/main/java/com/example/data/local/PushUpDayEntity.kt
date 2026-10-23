package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single local calendar day's challenge record.
 * [localDate] is stored in ISO-8601 format ("YYYY-MM-DD") and uniquely indexed
 * so duplicate records for the same calendar date are impossible.
 */
@Entity(
    tableName = "push_up_days",
    indices = [Index(value = ["local_date"], unique = true)]
)
data class PushUpDayEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "local_date")
    val localDate: String, // ISO-8601 "YYYY-MM-DD"

    @ColumnInfo(name = "target_reps")
    val targetReps: Int,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    @ColumnInfo(name = "completed_at_epoch_millis")
    val completedAtEpochMillis: Long? = null,

    @ColumnInfo(name = "actual_reps")
    val actualReps: Int? = null
) {
    /**
     * Effective reps recorded for this day:
     * - If actualReps was explicitly recorded, returns actualReps.
     * - Otherwise, if completed, returns targetReps.
     * - Otherwise 0.
     */
    val effectiveLoggedReps: Int
        get() = actualReps ?: if (isCompleted) targetReps else 0
}
