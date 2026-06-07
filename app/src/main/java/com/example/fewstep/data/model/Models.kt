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
    @get:com.google.firebase.firestore.PropertyName("currentStreak")
    @set:com.google.firebase.firestore.PropertyName("currentStreak")
    var currentStreak: Int = 0,
    
    @get:com.google.firebase.firestore.PropertyName("lastStreakUpdate")
    @set:com.google.firebase.firestore.PropertyName("lastStreakUpdate")
    var lastStreakUpdate: String = "", // yyyy-MM-dd
    
    @get:com.google.firebase.firestore.PropertyName("xp")
    @set:com.google.firebase.firestore.PropertyName("xp")
    var xp: Long = 0,
    
    @get:com.google.firebase.firestore.PropertyName("level")
    @set:com.google.firebase.firestore.PropertyName("level")
    var level: Int = 1,
    
    @get:com.google.firebase.firestore.PropertyName("isAdmin")
    @set:com.google.firebase.firestore.PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    
    @get:com.google.firebase.firestore.PropertyName("isBlocked")
    @set:com.google.firebase.firestore.PropertyName("isBlocked")
    var isBlocked: Boolean = false,
    
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    @set:com.google.firebase.firestore.PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    
    @get:com.google.firebase.firestore.PropertyName("lastSeenBroadcastId")
    @set:com.google.firebase.firestore.PropertyName("lastSeenBroadcastId")
    var lastSeenBroadcastId: String = "",
    
    @get:com.google.firebase.firestore.PropertyName("availableFreezes")
    @set:com.google.firebase.firestore.PropertyName("availableFreezes")
    var availableFreezes: Int = 0,
    
    @get:com.google.firebase.firestore.PropertyName("frozenDates")
    @set:com.google.firebase.firestore.PropertyName("frozenDates")
    var frozenDates: List<String> = emptyList(),
    
    @get:com.google.firebase.firestore.PropertyName("createdAt")
    @set:com.google.firebase.firestore.PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
) {
    val rankTitle: String get() = when {
        level >= 101 -> "🔥 PAPA 🔥"
        level >= 100 -> "GODLIKE CHAMPION"
        level >= 91 -> "Mythic Warrior"
        level >= 81 -> "Elite Master"
        level >= 71 -> "Grandmaster"
        level >= 61 -> "Master"
        level >= 51 -> "Mythic"
        level >= 41 -> "Legend"
        level >= 31 -> "Expert"
        level >= 21 -> "Ultra Pro"
        level >= 11 -> "Pro"
        level >= 6 -> "Trainee"
        level >= 2 -> "Novice"
        else -> "Beginner"
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
    val title: String = "",
    val query: String = "",
    val type: String = "Query",
    val timestamp: Long = System.currentTimeMillis(),
    
    @get:com.google.firebase.firestore.PropertyName("resolved")
    @set:com.google.firebase.firestore.PropertyName("resolved")
    var isResolved: Boolean = false
)

data class AdminNotification(
    val id: String = "",
    val receiverId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class AdminBroadcast(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val type: String = "ADMIN_GLOBAL",
    val active: Boolean = true
)

data class DeletionRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false
)
