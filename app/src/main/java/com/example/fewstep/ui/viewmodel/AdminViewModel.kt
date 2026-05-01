package com.example.fewstep.ui.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.fewstep.data.model.UserQuery
import com.example.fewstep.data.model.AdminBroadcast
import com.example.fewstep.data.model.AdminNotification

class AdminViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    val allUsers: StateFlow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(uid = doc.id)
                    }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queries: StateFlow<List<UserQuery>> = callbackFlow {
        val listener = firestore.collection("queries")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserQuery::class.java)?.copy(id = doc.id)
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledBroadcasts: StateFlow<List<AdminBroadcast>> = callbackFlow {
        val listener = firestore.collection("admin_broadcasts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(AdminBroadcast::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleBlockUser(context: Context, user: User) {
        viewModelScope.launch {
            try {
                val newStatus = !user.isBlocked
                val updates = hashMapOf<String, Any>(
                    "isBlocked" to newStatus,
                    "blocked" to com.google.firebase.firestore.FieldValue.delete()
                )
                firestore.collection("users").document(user.uid)
                    .update(updates)
                    .await()
                Toast.makeText(context, if (newStatus) "User Blocked!" else "User Unblocked!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun deleteUser(context: Context, uid: String) {
        viewModelScope.launch {
            try {
                // COST-EFFECTIVE ZERO-BUDGET DELETION (Spark Plan)
                // Instead of deleting from Auth (needs paid Cloud Functions), 
                // we wipe the data and permanently block their UID in Firestore.
                
                // 1. Wipe Habits
                val habits = firestore.collection("users").document(uid).collection("habits").get().await()
                habits.documents.forEach { doc ->
                    doc.reference.delete().await()
                }
                
                // 2. Clear Personal Data & Set Block Flags
                val updates = hashMapOf<String, Any>(
                    "name" to "Deleted Account",
                    "email" to "deleted@fewstep.com",
                    "xp" to 0,
                    "level" to 1,
                    "currentStreak" to 0,
                    "isBlocked" to true,
                    "isDeleted" to true,
                    "blockedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).update(updates).await()
                
                Toast.makeText(context, "User wiped & permanently blocked! 🛡️🧹", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Zero-budget deletion failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                if (scheduledTime > System.currentTimeMillis()) {
                    com.example.fewstep.util.ai.AiNotificationScheduler.scheduleBroadcast(context, scheduledTime)
                }
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
        com.example.fewstep.util.ai.AiNotificationReceiver.checkAndNotify(context)
        Toast.makeText(context, "Test Triggered! Check Logcat & Notifications. 🧪", Toast.LENGTH_LONG).show()
    }

    fun scheduleTestNotification(context: Context, minutes: Int) {
        com.example.fewstep.util.ai.AiNotificationScheduler.scheduleTest(context, minutes)
        Toast.makeText(context, "Alarm scheduled for $minutes min from now! ⏰", Toast.LENGTH_LONG).show()
    }

    fun sendAdminNotification(context: Context, receiverId: String, title: String, message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            try {
                val notification = AdminNotification(
                    receiverId = receiverId,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                firestore.collection("users").document(receiverId)
                    .collection("notifications").add(notification).await()
                Toast.makeText(context, "Message sent to user! 📨", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val deletionRequests: StateFlow<List<com.example.fewstep.data.model.DeletionRequest>> = callbackFlow {
        val listener = firestore.collection("deletion_requests")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.fewstep.data.model.DeletionRequest::class.java)?.copy(id = doc.id)
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleQueryResolution(context: Context, query: UserQuery) {
        viewModelScope.launch {
            try {
                val newStatus = !query.isResolved
                firestore.collection("queries").document(query.id)
                    .update("resolved", newStatus)
                    .await()
                Toast.makeText(context, if (newStatus) "Query Resolved! ✅" else "Query marked as Pending. ⏳", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resolveDeletionRequest(context: Context, requestId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("deletion_requests").document(requestId).delete().await()
                Toast.makeText(context, "Request marked as processed and archived. 🗄️", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
