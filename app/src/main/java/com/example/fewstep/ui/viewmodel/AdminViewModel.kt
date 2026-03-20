package com.example.fewstep.ui.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.fewstep.data.model.UserQuery
import com.example.fewstep.data.model.AdminBroadcast

class AdminViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _queries = MutableStateFlow<List<UserQuery>>(emptyList())
    val queries: StateFlow<List<UserQuery>> = _queries.asStateFlow()

    private val _scheduledBroadcasts = MutableStateFlow<List<AdminBroadcast>>(emptyList())
    val scheduledBroadcasts: StateFlow<List<AdminBroadcast>> = _scheduledBroadcasts.asStateFlow()

    init {
        fetchUsers()
        fetchQueries()
        fetchScheduledBroadcasts()
    }

    private fun fetchUsers() {
        firestore.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _allUsers.value = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                }
            }
    }

    private fun fetchQueries() {
        firestore.collection("queries")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _queries.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserQuery::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    private fun fetchScheduledBroadcasts() {
        android.util.Log.d("AdminViewModel", "📡 fetchScheduledBroadcasts: Starting listener...")
        firestore.collection("admin_broadcasts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AdminViewModel", "❌ snapshotListener ERROR: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    android.util.Log.d("AdminViewModel", "📊 snapshotListener: Found ${snapshot.size()} broadcast documents")
                    _scheduledBroadcasts.value = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(AdminBroadcast::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("AdminViewModel", "❌ toObject FAILED for doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                }
            }
    }

    fun toggleBlockUser(context: Context, user: User) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(user.uid)
                    .update("isBlocked", !user.isBlocked)
                    .await()
                Toast.makeText(context, if (user.isBlocked) "User Unblocked!" else "User Blocked!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleAdminStatus(context: Context, user: User) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(user.uid)
                    .update("isAdmin", !user.isAdmin)
                    .await()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteUser(context: Context, uid: String) {
        viewModelScope.launch {
            try {
                // Deep Delete: Remove habits sub-collection first
                val habits = firestore.collection("users").document(uid).collection("habits").get().await()
                habits.documents.forEach { doc ->
                    doc.reference.delete().await()
                }
                
                // Finally delete the user document
                firestore.collection("users").document(uid).delete().await()
                Toast.makeText(context, "User & all data permanently erased. 🧹", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Deep deletion failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendBroadcast(context: Context, message: String, scheduledTime: Long = System.currentTimeMillis()) {
        if (message.isBlank()) return
        viewModelScope.launch {
            try {
                val broadcast = hashMapOf(
                    "message" to message,
                    "timestamp" to scheduledTime,
                    "type" to "ADMIN_GLOBAL",
                    "active" to true
                )
                firestore.collection("admin_broadcasts").add(broadcast).await()
                Toast.makeText(context, "Broadcast Scheduled! 📢", Toast.LENGTH_SHORT).show()
                
                // 1. Schedule a specific future alarm if needed
                if (scheduledTime > System.currentTimeMillis()) {
                    com.example.fewstep.util.ai.AiNotificationScheduler.scheduleBroadcast(context, scheduledTime)
                }

                // 2. Immediate local check ONLY if broadcast is for NOW or past
                if (scheduledTime <= System.currentTimeMillis() + 1000L) {
                    com.example.fewstep.util.ai.AiNotificationReceiver.checkAndNotify(context)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteBroadcast(context: Context, broadcastId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("admin_broadcasts").document(broadcastId).delete().await()
                Toast.makeText(context, "Broadcast Cancelled.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun triggerTestNotification(context: Context) {
        android.util.Log.d("AdminViewModel", "🧪 Triggering TEST notification...")
        com.example.fewstep.util.ai.AiNotificationReceiver.checkAndNotify(context)
        Toast.makeText(context, "Test Triggered! Check Logcat & Notifications. 🧪", Toast.LENGTH_LONG).show()
    }

    fun scheduleTestNotification(context: Context, minutes: Int) {
        android.util.Log.d("AdminViewModel", "⏰ Requesting test alarm for $minutes minutes...")
        com.example.fewstep.util.ai.AiNotificationScheduler.scheduleTest(context, minutes)
        Toast.makeText(context, "Alarm scheduled for $minutes min from now! ⏰", Toast.LENGTH_LONG).show()
    }
}
