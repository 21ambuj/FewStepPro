package com.example.fewstep.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.model.Habit
import com.example.fewstep.data.model.HabitLog
import com.example.fewstep.data.repository.HabitRepository
import com.example.fewstep.util.NotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.fewstep.ui.screens.home.DayState


class HomeViewModel(
    private val repository: HabitRepository,
    private val focusRepository: com.example.fewstep.data.repository.FocusHistoryRepository
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            repository.syncUserProfile()
        }
    }

    // Live stream of ALL habits (unfiltered by day) — for analytics
    val allHabitsRaw: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDayOfWeek = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    val habits: StateFlow<List<Habit>> = allHabitsRaw.combine(selectedDayOfWeek) { habits, selectedDay ->
        habits.filter { habit -> habit.scheduledDays.contains(selectedDay) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<HabitLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sortedHabits: StateFlow<List<Habit>> = habits.combine(
        allLogs.combine(selectedDayOfWeek) { logs, day ->
            val targetDate = getDateStrForDayOfWeek(day)
            logs.filter { it.date == targetDate && it.completed }.map { it.habitId }.toSet()
        }
    ) { habitList, completedIds ->
        habitList.sortedWith(
            compareBy<Habit> { it.id in completedIds }
                .thenBy { it.reminderTime }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayStartMs: StateFlow<Long> = selectedDayOfWeek.map { day ->
        val cal = Calendar.getInstance()
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diff = day - currentDayOfWeek
        cal.add(Calendar.DAY_OF_YEAR, diff)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val selectedDayEndMs: StateFlow<Long> = selectedDayStartMs.map { startMs ->
        startMs + 24 * 60 * 60 * 1000L - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val selectedDayState: StateFlow<DayState> = selectedDayOfWeek.map { selectedDay ->
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dayMapping = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, 
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )
        val selectedIndex = dayMapping.indexOf(selectedDay)
        val todayIndex = dayMapping.indexOf(currentDayOfWeek)
        when {
            selectedIndex < todayIndex -> DayState.PAST
            selectedIndex > todayIndex -> DayState.FUTURE
            else -> DayState.TODAY
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayState.TODAY)



    val userData: StateFlow<com.example.fewstep.data.model.User?> = repository.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _newMilestone = MutableStateFlow<Int?>(null)
    val newMilestone: StateFlow<Int?> = _newMilestone.asStateFlow()

    fun dismissMilestone() {
        _newMilestone.value = null
    }

    init {
        viewModelScope.launch {
            repository.syncUserProfile()
        }
    }

    // Observe streak for milestones reactively via a dedicated Flow instead of init collection
    val milestoneEvent: Flow<Int> = userData
        .filterNotNull()
        .map { it.currentStreak }
        .distinctUntilChanged()
        .filter { it in listOf(7, 15, 30, 50, 100) }
        .onEach { _newMilestone.value = it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        .map { it } // Just to keep it as a Flow

    fun selectDay(day: Int) {
        _selectedDayOfWeek.value = day
    }

    // Derived State: Today's Date
    private val todayStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Helper to get date string for a specific day of the current week
    fun getDateStrForDayOfWeek(dayOfWeek: Int): String {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        cal.add(Calendar.DAY_OF_YEAR, dayOfWeek - currentDay)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    // Derived State: Completed Habit IDs for the Selected Day
    val completedHabitIdsForSelectedDay: StateFlow<Set<String>> = allLogs.combine(selectedDayOfWeek) { logs, day ->
        val targetDate = getDateStrForDayOfWeek(day)
        logs.filter { it.date == targetDate && it.completed }
            .map { it.habitId }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())


    // History Recap Logic: Last 7 days with completion counts
    val historyRecap: StateFlow<List<DaySummary>> = allLogs.map { logs ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        (0 until 7).map { i ->
            val date = cal.time
            val dateStr = sdf.format(date)
            // Get day of week for this specific past date (1-7)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            
            val dailyLogs = logs.filter { it.date == dateStr }
            val completedCount = dailyLogs.count { it.completed }
            
            // Count how many habits are scheduled for this specific day of week
            // IMPORTANT: Only count habits that have actually 'started' (respecting startDate or createdAt)
            val totalForDay = allHabitsRaw.value.filter { it.scheduledDays.contains(dayOfWeek) }
                .count { habit ->
                    val effectiveStartTime = habit.startDate ?: habit.createdAt
                    // Compare effective start time with the end of the day being calculated (23:59:59) 
                    // to ensure it's not 'missed' if created today
                    effectiveStartTime <= date.time || dateStr == todayStr
                }
            
            val summary = DaySummary(
                dayName = if (i == 0) "Today" else dayFormat.format(date),
                dateStr = dateStr,
                completedCount = completedCount,
                totalCount = totalForDay,
                isToday = i == 0
            )
            cal.add(Calendar.DAY_OF_YEAR, -1)
            summary
        }.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Focus History Logic: Last 7 days with focus minutes
    val focusSessions: StateFlow<List<com.example.fewstep.data.model.FocusSession>> = focusRepository.getRecentSessions(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusHistory: StateFlow<List<Pair<String, Int>>> = focusSessions.map { sessions ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        (0 until 7).map { i ->
            val dateStr = sdf.format(cal.time)
            val dayName = if (i == 0) "TODAY" else dayFormat.format(cal.time).uppercase()
            
            // Sum minutes for this day
            val minutes = sessions.filter { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.timestamp) == dateStr 
            }.sumOf { it.durationSeconds } / 60
            
            val pair = dayName to minutes
            cal.add(Calendar.DAY_OF_YEAR, -1)
            pair
        }.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Comeback Mode Logic: True if yesterday had habits but wasn't fully completed
    val isComebackMode: StateFlow<Boolean> = historyRecap.map { recap ->
        if (recap.size < 2) return@map false
        val yesterday = recap[recap.size - 2]
        val today = recap.last()
        // Comeback is active IF yesterday was missed AND no habits completed today yet
        (yesterday.totalCount > 0 && yesterday.completedCount < yesterday.totalCount) && (today.completedCount == 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Habit Stats: Total completions for each habit (Restored)
    val habitStats: StateFlow<Map<String, Int>> = allLogs.map { logs ->
        logs.filter { it.completed }
            .groupBy { it.habitId }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _streakIncreasedEvent = MutableSharedFlow<Int>()
    val streakIncreasedEvent = _streakIncreasedEvent.asSharedFlow()

    fun completeHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                val dateToMark = getDateStrForDayOfWeek(selectedDayOfWeek.value)
                // Bonus XP if in Comeback Mode
                val xpToAward = if (isComebackMode.value) 100L else 50L
                val increasedStreak = repository.markHabitAsCompleted(habit, dateToMark, xpToAward)
                if (increasedStreak != null) {
                    _streakIncreasedEvent.emit(increasedStreak)
                }
            } catch (e: Exception) {
                // Log and ignore to prevent crash, or handle via UI if needed
                android.util.Log.e("HomeViewModel", "Error completing habit: ${e.message}")
            }
        }
    }
    
    fun buyStreakFreeze(cost: Long, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.buyStreakFreeze(cost)
                if (success) {
                    onSuccess()
                } else {
                    onError()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error buying streak freeze: ${e.message}")
                onError()
            }
        }
    }
    fun addHabit(
        context: Context, 
        title: String, 
        description: String,
        category: String, 
        frequency: String, 
        scheduledDays: List<Int>, 
        reminderTime: String,
        startDate: Long? = null,
        endDate: Long? = null
    ) {
        viewModelScope.launch {
            val habitId = repository.insertHabit(
                Habit(
                    title = title,
                    description = description,
                    category = category,
                    frequency = frequency,
                    scheduledDays = scheduledDays,
                    reminderTime = reminderTime,
                    startDate = startDate,
                    endDate = endDate
                )
            )
            NotificationScheduler.scheduleHabitReminder(context, habitId.hashCode(), title, category, reminderTime, scheduledDays)
        }
    }

    fun deleteHabit(context: Context, habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            NotificationScheduler.cancelHabitReminder(context, habit.id.hashCode())
        }
    }

    fun updateHabit(context: Context, habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            NotificationScheduler.cancelHabitReminder(context, habit.id.hashCode())
            NotificationScheduler.scheduleHabitReminder(context, habit.id.hashCode(), habit.title, habit.category, habit.reminderTime, habit.scheduledDays)
        }
    }

    // Centralized Analytics Data Flow (Lazy)
    val analyticsData: StateFlow<com.example.fewstep.ui.screens.analytics.AnalyticsData?> = combine(
        allHabitsRaw, allLogs
    ) { habits, logs ->
        if (habits.isEmpty()) return@combine null

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        
        val totalComps = logs.count { it.completed }
        val doneToday = logs.count { it.completed && it.date == today }

        val categoryMap = habits.groupBy { it.category }
        val catEntries = categoryMap.map { (cat, catHabits) ->
            val completedCount = logs.count { log ->
                log.completed && catHabits.any { it.id == log.habitId }
            }
            Triple(cat, catHabits.size, completedCount)
        }

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val sevenDays = (0..6).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dateStr = sdf.format(cal.time)
            val dayName = dayFormat.format(cal.time)
            val completed = logs.count { it.completed && it.date == dateStr }
            Pair(dayName, completed)
        }.reversed()

        val habitCompCounts = logs.filter { it.completed }
            .groupBy { it.habitId }
            .mapValues { it.value.size }
            
        val topHabitIds = habitCompCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
            
        val topHabList = topHabitIds.mapNotNull { id -> habits.find { it.id == id } }

        com.example.fewstep.ui.screens.analytics.AnalyticsData(
            totalHabits = habits.size,
            totalCompletions = totalComps,
            completedToday = doneToday,
            categoryEntries = catEntries,
            last7Days = sevenDays,
            habitCompletionCounts = habitCompCounts,
            topHabits = topHabList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            repository.updateUserName(newName)
        }
    }
}

data class DaySummary(
    val dayName: String,
    val dateStr: String,
    val completedCount: Int,
    val totalCount: Int,
    val isToday: Boolean
)

