package com.example.fewstep.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one day's step count stored in the local Room database.
 * [dateStr] is in format "yyyy-MM-dd" and serves as the primary key.
 */
@Entity(tableName = "step_records")
data class StepRecord(
    @PrimaryKey val dateStr: String,   // e.g. "2025-04-25"
    val steps: Int
)
