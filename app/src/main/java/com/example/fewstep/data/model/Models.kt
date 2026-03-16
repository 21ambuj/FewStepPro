package com.example.fewstep.data.model

import kotlin.math.*

data class Habit(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val frequency: String = "Daily",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Sun, 2=Mon, ..., 7=Sat
    val reminderTime: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class HabitLog(
    val id: String = "",
    val habitId: String = "",
    val date: String = "",
    @get:com.google.firebase.firestore.PropertyName("completed")
    val completed: Boolean = false,
    val completedAt: Long = System.currentTimeMillis()
)

data class XpLog(
    val id: String = "",
    val userId: String = "",
    val amount: Long = 0,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val currentStreak: Int = 0,
    val lastStreakUpdate: String = "", // yyyy-MM-dd
    val xp: Long = 0,
    val level: Int = 1
) {
    companion object {
        fun calculateLevel(xp: Long): Int {
            return (kotlin.math.floor(kotlin.math.sqrt(xp.toDouble() / 100)) + 1).toInt()
        }
        
        fun xpToNextLevel(level: Int): Long {
            val nextLevel = level.toDouble()
            return (nextLevel * nextLevel * 100).toLong()
        }
    }
}
