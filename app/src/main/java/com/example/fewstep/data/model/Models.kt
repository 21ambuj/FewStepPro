package com.example.fewstep.data.model

import kotlin.math.*

data class Habit(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Health",
    val frequency: String = "Daily",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val reminderTime: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
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
    val level: Int = 1,
    val isAdmin: Boolean = false,
    val isBlocked: Boolean = false,
    val lastSeenBroadcastId: String = ""
) {
    val rankTitle: String get() = when {
        level >= 101 -> "UNSTOPPABLE FORCE"
        level >= 51 -> "LEGENDARY CHAMPION"
        level >= 21 -> "MASTER COACH"
        level >= 11 -> "ELITE WARRIOR"
        level >= 6 -> "WARRIOR"
        level >= 3 -> "APPRENTICE"
        else -> "NOVICE"
    }

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

data class UserQuery(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val query: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminBroadcast(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val type: String = "ADMIN_GLOBAL",
    val active: Boolean = true
)
