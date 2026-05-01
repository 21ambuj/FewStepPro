package com.example.fewstep.data.local

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository that wraps StepRecordDao.
 * All DB operations run on the calling coroutine dispatcher (use IO).
 */
class StepHistoryRepository(context: Context) {

    private val dao = StepDatabase.getInstance(context).stepRecordDao()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayName = SimpleDateFormat("EEE", Locale.getDefault())

    /** Save (or update) today's step count. Called frequently from the tracking service. */
    suspend fun saveTodaySteps(steps: Int) {
        val today = sdf.format(Date())
        dao.upsert(StepRecord(dateStr = today, steps = steps))
    }

    /** Returns today's saved step count (0 if no record). */
    suspend fun getTodaySteps(): Int {
        val today = sdf.format(Date())
        return dao.getByDate(today)?.steps ?: 0
    }

    /**
     * Returns the last [days] days as a list of (dayLabel, steps).
     * Today is always live from the parameter, older days come from DB.
     */
    suspend fun getHistory(days: Int = 7, todayLiveSteps: Int = 0): List<Pair<String, Int>> {
        val records = dao.getRecentDays(days)
            .reversed()                        // oldest → newest
            .associateBy { it.dateStr }

        val result = mutableListOf<Pair<String, Int>>()
        val cal = Calendar.getInstance()

        for (i in days - 1 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            val label = if (i == 0) "Today" else dayName.format(cal.time)
            val steps = if (i == 0) todayLiveSteps else (records[dateStr]?.steps ?: 0)
            result.add(label to steps)
        }

        return result
    }

    /** Returns the total steps for the last 7 days. */
    suspend fun getWeeklyTotal(): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val fromDate = sdf.format(cal.time)
        return dao.getTotalStepsSince(fromDate) ?: 0
    }

    /** Prune records older than 90 days to keep DB lean. */
    suspend fun pruneOldRecords() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -90)
        val cutoff = sdf.format(cal.time)
        dao.deleteOlderThan(cutoff)
    }
}
