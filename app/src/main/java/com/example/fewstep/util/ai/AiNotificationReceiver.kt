package com.example.fewstep.util.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fewstep.data.model.Habit
import com.example.fewstep.data.model.HabitLog
import com.example.fewstep.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.text.SimpleDateFormat
import java.util.*
import com.example.fewstep.R
import com.example.fewstep.util.VoiceReminderService
import androidx.core.content.ContextCompat

class AiNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d("AiNotification", "📥 Broadcast received! Action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            android.util.Log.d("AiNotification", "🔄 Boot detected, rescheduling...")
            AiNotificationScheduler.scheduleNext(context)
            return
        }

        // Trigger notification check
        checkAndNotify(context)

        // Always schedule the next check for the next behavioral time slot
        AiNotificationScheduler.scheduleNext(context)
    }

    companion object {
        fun checkAndNotify(context: Context) {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val uid = auth.currentUser?.uid ?: run {
                android.util.Log.e("AiNotification", "❌ No UID found, stopping.")
                return
            }

            android.util.Log.d("AiNotification", "🚀 Beginning checkAndNotify for UID: $uid")

            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val currentCal = Calendar.getInstance()
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCal.time)
                    val dayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
                    val hour = currentCal.get(Calendar.HOUR_OF_DAY)

                    // 1. Get user data
                    android.util.Log.d("AiNotification", "🔍 Fetching User doc...")
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    if (!userDoc.exists()) {
                        android.util.Log.e("AiNotification", "❌ User doc DOES NOT EXIST in Firestore for $uid")
                        return@launch
                    }
                    val user = userDoc.toObject(User::class.java)
                    android.util.Log.d("AiNotification", "👤 User: ${user?.name}, Blocked: ${user?.isBlocked}, lastSeen: ${user?.lastSeenBroadcastId}")
                    
                    if (user?.isBlocked == true) {
                        android.util.Log.w("AiNotification", "🚫 User is blocked, skipping all notifications.")
                        return@launch
                    }

                    val streak = user?.currentStreak ?: 0

                    // 2. Check for latest valid Admin Broadcast
                    try {
                        val now = System.currentTimeMillis()
                        android.util.Log.d("AiNotification", "📡 Querying for active broadcasts (timestamp <= ${now + 5000})...")
                        val broadcastSnapshot = firestore.collection("admin_broadcasts")
                            .whereEqualTo("active", true)
                            .whereLessThanOrEqualTo("timestamp", now + 5000L) // 5s safety buffer
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get().await()
                        
                        if (!broadcastSnapshot.isEmpty) {
                            val broadcastDoc = broadcastSnapshot.documents.first()
                            val broadcastId = broadcastDoc.id
                            val broadcastMsg = broadcastDoc.get("message")?.toString() ?: ""
                            android.util.Log.d("AiNotification", "📢 Found broadcast: $broadcastId, Msg: $broadcastMsg")
                            
                            if (broadcastId != user?.lastSeenBroadcastId) {
                                android.util.Log.d("AiNotification", "✅ NEW Broadcast found! Showing notification 99...")
                                showNotification(context, "FewStep Master Coach 🎖️", broadcastMsg, 99)
                                
                                firestore.collection("users").document(uid)
                                    .update("lastSeenBroadcastId", broadcastId)
                                    .addOnSuccessListener { android.util.Log.d("AiNotification", "💾 Updated lastSeenBroadcastId to $broadcastId") }
                                    .addOnFailureListener { e -> android.util.Log.e("AiNotification", "❌ Failed to update lastSeenBroadcastId: ${e.message}") }
                                
                                return@launch
                            } else {
                                android.util.Log.d("AiNotification", "💤 Broadcast $broadcastId already seen by user.")
                            }
                        } else {
                            android.util.Log.d("AiNotification", "ℹ️ No active broadcasts found currently.")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AiNotification", "❌ Broadcast query failed. Potential missing index? Error: ${e.message}")
                    }

                    // 3. Get all habits for today
                    android.util.Log.d("AiNotification", "📅 Checking habits for day $dayOfWeek...")
                    val habitsSnapshot = firestore.collection("users").document(uid).collection("habits").get().await()
                    val habits = habitsSnapshot.documents.mapNotNull { it.toObject(Habit::class.java)?.copy(id = it.id) }
                    val todayHabits = habits.filter { it.scheduledDays.contains(dayOfWeek) }
                    android.util.Log.d("AiNotification", "📊 Total habits: ${habits.size}, Today's active: ${todayHabits.size}")

                    if (todayHabits.isEmpty()) {
                        android.util.Log.d("AiNotification", "📭 No habits scheduled for today.")
                        return@launch
                    }

                    // 4. Get today's logs
                    val logsSnapshot = firestore.collection("users").document(uid).collection("logs")
                        .whereEqualTo("date", today)
                        .get().await()
                    val completedHabitIds = logsSnapshot.documents.mapNotNull { it.toObject(HabitLog::class.java)?.habitId }.toSet()
                    android.util.Log.d("AiNotification", "✅ Completed today: ${completedHabitIds.size}/${todayHabits.size}")

                    // 5. Find incomplete habits
                    val incompleteHabits = todayHabits.filter { !completedHabitIds.contains(it.id) }

                    if (incompleteHabits.isNotEmpty()) {
                        val habitToRemind = incompleteHabits.random()
                        android.util.Log.d("AiNotification", "⏰ Reminding for habit: ${habitToRemind.title}")
                        val message = AiNotificationEngine.getMessage(
                            habitToRemind.title, 
                            hour, 
                            streak,
                            user?.name ?: "Champion",
                            user?.rankTitle ?: "Novice"
                        )
                        showNotification(context, "FewStep Coach 🤖", message, 100)
                    } 
                    else {
                        android.util.Log.d("AiNotification", "🎉 All habits done! Showing general motivation.")
                        val message = AiNotificationEngine.getGeneralMotivation(
                            user?.name ?: "Champion",
                            user?.rankTitle ?: "Novice"
                        )
                        showNotification(context, "FewStep Coach 🤖", message, 100)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AiNotification", "💣 FATAL ERROR in checkAndNotify: ${e.message}", e)
                }
            }
        }

        private fun detectLanguage(text: String): String {
            // If it contains Devanagari script, it's strictly Hindi
            val hasHindiScript = text.any { it in '\u0900'..'\u097F' }
            return if (hasHindiScript) "hi" else "en"
        }

        private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "ai_reminders"
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "AI Habit Coaching",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Funny reminders to keep you on track"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setSound(soundUri, android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, com.example.fewstep.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher) 
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setColor(0xFF1A237E.toInt())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))

            notificationManager.notify(notificationId, builder.build())

            // Trigger Voice AI with Smart Language Detection
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) {
                val detectedLocale = detectLanguage(message)
                android.util.Log.d("AiNotification", "🗣️ Starting Voice AI. Message: '$message' | Detected Locale: $detectedLocale")
                val voiceIntent = Intent(context, VoiceReminderService::class.java).apply {
                    putExtra(VoiceReminderService.EXTRA_MESSAGE, message)
                    putExtra(VoiceReminderService.EXTRA_LOCALE, detectedLocale)
                }
                ContextCompat.startForegroundService(context, voiceIntent)
            }
        }
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
