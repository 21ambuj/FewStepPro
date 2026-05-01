package com.example.fewstep.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepRecordDao {

    /** Insert or update a step record for a given date. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: StepRecord)

    /** Get the step record for a specific date. */
    @Query("SELECT * FROM step_records WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getByDate(dateStr: String): StepRecord?

    /**
     * Get the last [days] days of step records, ordered from oldest to newest.
     * Results are sorted ascending so the UI can display them left-to-right.
     */
    @Query(
        "SELECT * FROM step_records ORDER BY dateStr DESC LIMIT :days"
    )
    suspend fun getRecentDays(days: Int): List<StepRecord>

    /** Get the total steps in the current week (last 7 days). */
    @Query(
        "SELECT SUM(steps) FROM step_records WHERE dateStr >= :fromDate"
    )
    suspend fun getTotalStepsSince(fromDate: String): Int?

    /** Delete all records older than the given date to keep the DB lean. */
    @Query("DELETE FROM step_records WHERE dateStr < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)
}
