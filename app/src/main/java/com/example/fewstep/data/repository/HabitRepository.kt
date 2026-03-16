package com.example.fewstep.data.repository

import com.example.fewstep.data.model.Habit
import com.example.fewstep.data.model.HabitLog
import com.example.fewstep.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll

class HabitRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val userId: String? get() = auth.currentUser?.uid

    private val authStateFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allHabits: Flow<List<Habit>> = authStateFlow.flatMapLatest { uid ->
        if (uid == null) return@flatMapLatest flowOf(emptyList<Habit>())
        callbackFlow {
            val listener = firestore.collection("users").document(uid).collection("habits")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val habits = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Habit::class.java)?.copy(id = doc.id)
                        }
                        trySend(habits)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allLogs: Flow<List<HabitLog>> = authStateFlow.flatMapLatest { uid ->
        if (uid == null) return@flatMapLatest flowOf(emptyList<HabitLog>())
        callbackFlow {
            val listener = firestore.collection("users").document(uid).collection("logs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val logs = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(HabitLog::class.java)?.copy(id = doc.id)
                        }
                        trySend(logs)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun insertHabit(habit: Habit): String {
        val uid = userId ?: throw Exception("User not authenticated")
        val docRef = firestore.collection("users").document(uid).collection("habits").document()
        val newHabit = habit.copy(id = docRef.id)
        docRef.set(newHabit).await()
        return docRef.id
    }

    suspend fun deleteHabit(habit: Habit) {
        val uid = userId ?: return
        firestore.collection("users").document(uid).collection("habits").document(habit.id).delete().await()
    }

    suspend fun updateHabit(habit: Habit) {
        val uid = userId ?: return
        firestore.collection("users").document(uid).collection("habits").document(habit.id).set(habit).await()
    }

    fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid).collection("logs")
            .whereEqualTo("habitId", habitId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(HabitLog::class.java)?.copy(id = doc.id)
                    }
                    trySend(logs)
                }
            }
        awaitClose { listener.remove() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userData: Flow<User?> = authStateFlow.flatMapLatest { uid ->
        if (uid == null) return@flatMapLatest flowOf(null)
        callbackFlow {
            val listener = firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(User::class.java))
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun awardXp(xpAmount: Long, reason: String) {
        val uid = userId ?: return
        val userRef = firestore.collection("users").document(uid)
        val xpLogsRef = firestore.collection("users").document(uid).collection("xp_logs")
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentXp = snapshot.getLong("xp") ?: 0L
            val newXp = currentXp + xpAmount
            val newLevel = User.calculateLevel(newXp)
            
            // Use set with Merge to ensure document is created if it doesn't exist
            val userUpdates = hashMapOf(
                "xp" to newXp,
                "level" to newLevel
            )
            transaction.set(userRef, userUpdates, SetOptions.merge())
            
            val logRef = xpLogsRef.document()
            val log = com.example.fewstep.data.model.XpLog(
                id = logRef.id,
                userId = uid,
                amount = xpAmount,
                reason = reason,
                timestamp = System.currentTimeMillis()
            )
            transaction.set(logRef, log)
        }.await()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val xpHistory: Flow<List<com.example.fewstep.data.model.XpLog>> = authStateFlow.flatMapLatest { uid ->
        if (uid == null) return@flatMapLatest flowOf(emptyList<com.example.fewstep.data.model.XpLog>())
        callbackFlow {
            val listener = firestore.collection("users").document(uid).collection("xp_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val logs = snapshot?.documents?.mapNotNull { it.toObject(com.example.fewstep.data.model.XpLog::class.java) } ?: emptyList()
                    trySend(logs)
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun updateStreakLogic(date: String) {
        val uid = userId ?: return
        val userRef = firestore.collection("users").document(uid)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
            val lastUpdate = snapshot.getString("lastStreakUpdate") ?: ""
            
            if (lastUpdate == date) return@runTransaction // Already updated today
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterday = sdf.format(cal.time)
            
            val newStreak = when (lastUpdate) {
                yesterday -> currentStreak + 1
                else -> 1 // Reset if missed a day or first time
            }
            
            val updates = hashMapOf(
                "currentStreak" to newStreak,
                "lastStreakUpdate" to date
            )
            transaction.set(userRef, updates, SetOptions.merge())
        }.await()
    }

    suspend fun markHabitAsCompleted(habit: Habit, date: String) {
        val uid = userId ?: return
        val userRef = firestore.collection("users").document(uid)
        val logsRef = firestore.collection("users").document(uid).collection("logs")
        val xpLogsRef = firestore.collection("users").document(uid).collection("xp_logs")
        
        firestore.runTransaction { transaction ->
            // 1. PERFORM ALL READS FIRST
            val logId = "${uid}_${habit.id}_$date"
            val logRef = logsRef.document(logId)
            val logSnapshot = transaction.get(logRef)
            val userSnapshot = transaction.get(userRef)

            // 2. CHECK CONDITIONS
            if (logSnapshot.exists()) {
                val existingLog = logSnapshot.toObject(HabitLog::class.java)
                if (existingLog?.completed == true) {
                    return@runTransaction // Already completed
                }
            }

            // 3. PERFORM ALL WRITES
            val newLog = HabitLog(id = logId, habitId = habit.id, date = date, completed = true)
            transaction.set(logRef, newLog)

            val currentXp = userSnapshot.getLong("xp") ?: 0L
            val newXp = currentXp + 50L
            val newLevel = User.calculateLevel(newXp)

            val userUpdates = hashMapOf(
                "xp" to newXp,
                "level" to newLevel
            )
            transaction.set(userRef, userUpdates, SetOptions.merge())

            val xpLogRef = xpLogsRef.document()
            val xpLog = com.example.fewstep.data.model.XpLog(
                id = xpLogRef.id,
                userId = uid,
                amount = 50L,
                reason = "Completed: ${habit.title}",
                timestamp = System.currentTimeMillis()
            )
            transaction.set(xpLogRef, xpLog)

            val currentStreak = userSnapshot.getLong("currentStreak")?.toInt() ?: 0
            val lastUpdate = userSnapshot.getString("lastStreakUpdate") ?: ""

            if (lastUpdate != date) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = sdf.format(java.util.Date())
                
                // Only update streak if the completion is for TODAY
                if (date == today) {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val yesterday = sdf.format(cal.time)

                    val newStreak = when (lastUpdate) {
                        yesterday -> currentStreak + 1
                        "" -> 1 // First time
                        else -> 1 // Missed days, reset to 1 because we completed today
                    }

                    transaction.update(userRef, "currentStreak", newStreak)
                    transaction.update(userRef, "lastStreakUpdate", date)
                }
            }
        }.await()
    }
}
