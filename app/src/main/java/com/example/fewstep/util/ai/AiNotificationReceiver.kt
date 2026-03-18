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

class AiNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            AiNotificationScheduler.scheduleNext(context)
            return
        }

        // Trigger notification check
        checkAndNotify(context)

        // Always schedule the next check for the next behavioral time slot
        AiNotificationScheduler.scheduleNext(context)
    }

    private fun checkAndNotify(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                // 1. Get user data for streak awareness
                val userDoc = firestore.collection("users").document(uid).get().await()
                val user = userDoc.toObject(User::class.java)
                val streak = user?.currentStreak ?: 0

                // 2. Get all habits for today
                val habitsSnapshot = firestore.collection("users").document(uid).collection("habits").get().await()
                val habits = habitsSnapshot.documents.mapNotNull { it.toObject(Habit::class.java)?.copy(id = it.id) }
                val todayHabits = habits.filter { it.scheduledDays.contains(dayOfWeek) }

                if (todayHabits.isEmpty()) return@launch

                // 3. Get today's logs
                val logsSnapshot = firestore.collection("users").document(uid).collection("logs")
                    .whereEqualTo("date", today)
                    .get().await()
                val completedHabitIds = logsSnapshot.documents.mapNotNull { it.toObject(HabitLog::class.java)?.habitId }.toSet()

                // 4. Find incomplete habits
                val incompleteHabits = todayHabits.filter { !completedHabitIds.contains(it.id) }

                if (incompleteHabits.isNotEmpty()) {
                    val habitToRemind = incompleteHabits.random()
                    // Use the upgraded engine with phase/streak awareness
                    val message = AiNotificationEngine.getMessage(habitToRemind.title, hour, streak)
                    showNotification(context, "FewStep Coach 🤖", message)
                } 
                else {
                    // All habits for today are DONE! Send a funny praise or general motivation.
                    val message = AiNotificationEngine.getGeneralMotivation()
                    showNotification(context, "FewStep Coach 🤖", message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ai_reminders"

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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFF1A237E.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        notificationManager.notify(99, builder.build())
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
