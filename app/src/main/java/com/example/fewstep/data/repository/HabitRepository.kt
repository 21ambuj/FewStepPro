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
        val batch = firestore.batch()
        
        // Delete the habit document
        val habitRef = firestore.collection("users").document(uid).collection("habits").document(habit.id)
        batch.delete(habitRef)
        
        // Find and delete all logs for this habit
        try {
            val logsSnapshot = firestore.collection("users").document(uid).collection("logs")
                .whereEqualTo("habitId", habit.id)
                .get()
                .await()
            
            for (doc in logsSnapshot.documents) {
                batch.delete(doc.reference)
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error fetching logs for deletion: ${e.message}")
        }
        
        batch.commit().await()
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




    suspend fun markHabitAsCompleted(habit: Habit, date: String, xpAward: Long = 50L): Int? {
        val uid = userId ?: return null
        val userRef = firestore.collection("users").document(uid)
        val logsRef = firestore.collection("users").document(uid).collection("logs")
        
        return try {
            val newStreakToReturn = firestore.runTransaction { transaction ->
                // 1. PERFORM ALL READS FIRST
                val logId = "${uid}_${habit.id}_$date"
                val logRef = logsRef.document(logId)
                val logSnapshot = transaction.get(logRef)
                val userSnapshot = transaction.get(userRef)

                // 2. CHECK CONDITIONS
                if (logSnapshot.exists()) {
                    val existingLog = logSnapshot.toObject(HabitLog::class.java)
                    if (existingLog?.completed == true) {
                        return@runTransaction null // Already completed
                    }
                }

                // 3. PERFORM ALL WRITES
                val newLog = HabitLog(id = logId, habitId = habit.id, date = date, completed = true)
                transaction.set(logRef, newLog)

                val currentXp = userSnapshot.getLong("xp") ?: 0L
                val newXp = currentXp + xpAward // Use the passed xpAward
                val newLevel = User.calculateLevel(newXp)

                val userUpdates = hashMapOf<String, Any>(
                    "xp" to newXp,
                    "level" to newLevel
                )
                
                // Backfill details if missing
                if (userSnapshot.getString("name").isNullOrEmpty()) {
                    auth.currentUser?.displayName?.let { userUpdates["name"] = it }
                }
                if (userSnapshot.getString("email").isNullOrEmpty()) {
                    auth.currentUser?.email?.let { userUpdates["email"] = it }
                }
                if (userSnapshot.getString("uid").isNullOrEmpty()) {
                    userUpdates["uid"] = uid
                }

                transaction.set(userRef, userUpdates, SetOptions.merge())

                val currentStreak = userSnapshot.getLong("currentStreak")?.toInt() ?: 0
                val lastUpdate = userSnapshot.getString("lastStreakUpdate") ?: ""

                var returnedStreak: Int? = null

                if (lastUpdate != date) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val today = sdf.format(java.util.Date())
                    
                    // Only update streak if the completion is for TODAY
                    if (date == today) {
                        var consumedFreeze = false
                        val availableFreezes = userSnapshot.getLong("availableFreezes")?.toInt() ?: 0

                        val newStreak = if (lastUpdate.isEmpty()) {
                            1 // First time ever
                        } else {
                            try {
                                val todayDate = sdf.parse(date)
                                val lastUpdateDate = sdf.parse(lastUpdate)
                                val diffInMillies = Math.abs(todayDate.time - lastUpdateDate.time)
                                val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)
                                
                                if (diffInDays <= 3) {
                                    // 1 day (yesterday), 2 days (missed 1), 3 days (missed 2)
                                    currentStreak + 1
                                } else {
                                    // Missed 3 or more days
                                    if (availableFreezes > 0) {
                                        consumedFreeze = true
                                        currentStreak + 1
                                    } else {
                                        1 // No freezes, reset back to 1
                                    }
                                }
                            } catch (e: Exception) {
                                1
                            }
                        }

                        if (newStreak > currentStreak) {
                            returnedStreak = newStreak
                        }

                        transaction.update(userRef, "currentStreak", newStreak)
                        transaction.update(userRef, "lastStreakUpdate", date)
                        if (consumedFreeze) {
                            transaction.update(userRef, "availableFreezes", availableFreezes - 1)
                        }
                    }
                }
                returnedStreak
            }.await()
            newStreakToReturn
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "markHabitAsCompleted failed: ${e.message}")
            null
        }
    }
    suspend fun addExperiencePoints(xpAward: Long) {
        val uid = userId ?: return
        val userRef = firestore.collection("users").document(uid)
        
        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val currentXp = userSnapshot.getLong("xp") ?: 0L
            val newXp = currentXp + xpAward
            val newLevel = User.calculateLevel(newXp)
            
            transaction.update(userRef, "xp", newXp)
            transaction.update(userRef, "level", newLevel)
        }.await()
    }

    suspend fun buyStreakFreeze(cost: Long): Boolean {
        val uid = userId ?: return false
        val userRef = firestore.collection("users").document(uid)
        
        return try {
            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                val currentXp = userSnapshot.getLong("xp") ?: 0L
                val currentFreezes = userSnapshot.getLong("availableFreezes")?.toInt() ?: 0
                
                if (currentXp >= cost) {
                    val newXp = currentXp - cost
                    val newLevel = User.calculateLevel(newXp)
                    
                    transaction.update(userRef, "xp", newXp)
                    transaction.update(userRef, "level", newLevel)
                    transaction.update(userRef, "availableFreezes", currentFreezes + 1)
                    true
                } else {
                    false
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "buyStreakFreeze failed: ${e.message}")
            false
        }
    }

    suspend fun updateUserName(newName: String) {
        val uid = userId ?: return
        firestore.collection("users").document(uid)
            .update("name", newName)
            .await()
    }

    suspend fun syncUserProfile() {
        val firebaseUser = auth.currentUser ?: return
        val userRef = firestore.collection("users").document(firebaseUser.uid)
        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {
            val newUser = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Champion",
                email = firebaseUser.email ?: "",
                xp = 0,
                level = 1,
                currentStreak = 0
            )
            userRef.set(newUser).await()
        } else {
            // Document exists, but check if name/email is missing
            val updates = mutableMapOf<String, Any>()
            if (snapshot.getString("name").isNullOrEmpty() && !firebaseUser.displayName.isNullOrEmpty()) {
                updates["name"] = firebaseUser.displayName!!
            }
            if (snapshot.getString("email").isNullOrEmpty() && !firebaseUser.email.isNullOrEmpty()) {
                updates["email"] = firebaseUser.email!!
            }
            if (snapshot.getString("uid").isNullOrEmpty()) {
                updates["uid"] = firebaseUser.uid
            }
            
            if (updates.isNotEmpty()) {
                userRef.update(updates).await()
            }
        }
    }

    fun getLeaderboard(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("xp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log error to console (visible in logcat)
                    android.util.Log.e("Leaderboard", "Firestore error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(User::class.java)
                        } catch (e: Exception) {
                            android.util.Log.e("Leaderboard", "Mapping error: ${e.message}")
                            null
                        }
                    }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }
}
