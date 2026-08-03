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
import kotlinx.coroutines.sync.withLock

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
        
        try {
            // ONLY delete the habit document, keep historical logs for UI retention and stats
            // This prevents batch concurrency crashes during mass deletion
            firestore.collection("users").document(uid).collection("habits").document(habit.id).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error deleting habit: ${e.message}")
        }
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




    private val completionMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun markHabitAsCompleted(habit: Habit, date: String, xpAward: Long = 50L): Int? = completionMutex.withLock {
        val uid = userId ?: return null
        val userRef = firestore.collection("users").document(uid)
        val logsRef = firestore.collection("users").document(uid).collection("logs")
        
        return try {
            val logId = "${uid}_${habit.id}_$date"
            val logRef = logsRef.document(logId)
            
            // 1. Fetch current data (Works offline via local cache)
            val logSnapshot = logRef.get().await()
            if (logSnapshot.exists()) {
                val existingLog = logSnapshot.toObject(HabitLog::class.java)
                if (existingLog?.completed == true) {
                    return null // Already completed
                }
            }

            val userSnapshot = userRef.get().await()
            val batch = firestore.batch()

            // 2. Setup the Log write
            val newLog = HabitLog(id = logId, habitId = habit.id, date = date, completed = true)
            batch.set(logRef, newLog)

            // 3. Process XP and Levels
            val currentXp = userSnapshot.getLong("xp") ?: 0L
            val newXp = currentXp + xpAward
            val newLevel = User.calculateLevel(newXp)

            val userUpdates = hashMapOf<String, Any>(
                "xp" to com.google.firebase.firestore.FieldValue.increment(xpAward),
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

            batch.set(userRef, userUpdates, SetOptions.merge())

            // 4. Process Streak logic
            val currentStreak = userSnapshot.getLong("currentStreak")?.toInt() ?: 0
            val lastUpdate = userSnapshot.getString("lastStreakUpdate") ?: ""
            var returnedStreak: Int? = null

            if (lastUpdate != date) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = sdf.format(java.util.Date())
                
                // Only update streak if the completion is for TODAY
                if (date == today) {
                    var freezesToConsume = 0
                    val newlyFrozenDates = mutableListOf<String>()
                    val availableFreezes = userSnapshot.getLong("availableFreezes")?.toInt() ?: 0

                    val newStreak = if (lastUpdate.isEmpty()) {
                        1 // First time ever
                    } else {
                        try {
                            val todayDate = sdf.parse(date)
                            val lastUpdateDate = sdf.parse(lastUpdate)
                            val diffInMillies = Math.abs(todayDate.time - lastUpdateDate.time)
                            val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)
                            
                            when {
                                diffInDays <= 1L -> {
                                    currentStreak + 1
                                }
                                diffInDays == 2L -> {
                                    if (availableFreezes >= 1) {
                                        freezesToConsume = 1
                                        val cal = java.util.Calendar.getInstance()
                                        cal.time = todayDate
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                        newlyFrozenDates.add(sdf.format(cal.time))
                                        currentStreak + 1
                                    } else 1
                                }
                                diffInDays == 3L -> {
                                    if (availableFreezes >= 2) {
                                        freezesToConsume = 2
                                        val cal = java.util.Calendar.getInstance()
                                        cal.time = todayDate
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                        newlyFrozenDates.add(sdf.format(cal.time))
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                        newlyFrozenDates.add(sdf.format(cal.time))
                                        currentStreak + 1
                                    } else 1
                                }
                                else -> 1 
                            }
                        } catch (e: Exception) {
                            1
                        }
                    }

                    // Always return the new streak if it's > 0 (meaning a completion happened today)
                    // This ensures the animation and haptic feedback play even after a streak reset.
                    if (newStreak > 0) {
                        returnedStreak = newStreak
                    }

                    batch.update(userRef, "currentStreak", newStreak)
                    batch.update(userRef, "lastStreakUpdate", date)
                    if (freezesToConsume > 0) {
                        batch.update(userRef, "availableFreezes", availableFreezes - freezesToConsume)
                        // Add the exact skipped dates to the frozenDates array
                        batch.update(userRef, "frozenDates", com.google.firebase.firestore.FieldValue.arrayUnion(*newlyFrozenDates.toTypedArray()))
                    }
                }
            }

            // 5. Commit Batch
            // We do NOT await the batch commit. This allows the function to return instantly,
            // updating the local cache immediately so the UI responds offline!
            batch.commit() 
            
            returnedStreak
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
