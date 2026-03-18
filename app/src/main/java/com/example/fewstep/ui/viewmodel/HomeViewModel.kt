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

class HomeViewModel(private val repository: HabitRepository) : ViewModel() {
    
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
            
            // Observe streak for milestones
            userData.collect { user ->
                val streak = user?.currentStreak ?: 0
                val milestones = listOf(7, 15, 30, 50, 100)
                if (streak in milestones) {
                    // Check if we already celebrated this streak today
                    // For now, we simple trigger it when the value matches
                    _newMilestone.value = streak
                }
            }
        }
    }

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
            val totalForDay = allHabitsRaw.value.count { it.scheduledDays.contains(dayOfWeek) }
            
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

    // Habit Stats: Total completions for each habit
    val habitStats: StateFlow<Map<String, Int>> = allLogs.map { logs ->
        logs.filter { it.completed }
            .groupBy { it.habitId }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun completeHabit(habit: Habit) {
        viewModelScope.launch {
            val dateToMark = getDateStrForDayOfWeek(selectedDayOfWeek.value)
            repository.markHabitAsCompleted(habit, dateToMark)
        }
    }
    fun addHabit(context: Context, title: String, category: String, frequency: String, scheduledDays: List<Int>, reminderTime: String) {
        viewModelScope.launch {
            val habitId = repository.insertHabit(
                Habit(
                    title = title,
                    category = category,
                    frequency = frequency,
                    scheduledDays = scheduledDays,
                    reminderTime = reminderTime
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
}

data class DaySummary(
    val dayName: String,
    val dateStr: String,
    val completedCount: Int,
    val totalCount: Int,
    val isToday: Boolean
)

