package com.example.fewstep.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

import kotlinx.coroutines.tasks.await

import com.example.fewstep.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            rescheduleAllHabits(context)
            return
        }

        val habitId = intent.getIntExtra("HABIT_ID", 0)
        val habitTitle = intent.getStringExtra("HABIT_TITLE") ?: "Habit Reminder"
        val category = intent.getStringExtra("HABIT_CATEGORY") ?: "Task"
        val scheduledTime = intent.getStringExtra("HABIT_TIME") ?: ""
        val scheduledDays = intent.getIntArrayExtra("SCHEDULED_DAYS")?.toList() ?: listOf(1, 2, 3, 4, 5, 6, 7)
        
        val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

        if (scheduledDays.contains(currentDay)) {
            showNotification(context, habitTitle, category, scheduledTime)
        }

        // Always schedule for the next day to keep the cycle going
        NotificationScheduler.scheduleHabitReminder(
            context, habitId, habitTitle, category, scheduledTime, scheduledDays
        )
    }

    private fun rescheduleAllHabits(context: Context) {
        // Fetch all habits from Firestore and reschedule
        // Since we are in a receiver, we should be quick. 
        // We'll use a temporary coroutine scope or a WorkManager task for better reliability.
        val repository = com.example.fewstep.data.repository.HabitRepository()
        
        // We use a global scope here for simplicity in this specific boot scenario, 
        // though WorkManager is preferred for longer tasks.
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.allHabits.collect { habits ->
                habits.forEach { habit ->
                    NotificationScheduler.scheduleHabitReminder(
                        context, habit.id.hashCode(), habit.title, habit.category, habit.reminderTime, habit.scheduledDays
                    )
                }
                // Stop collecting after first batch to avoid infinite loops/leaks
                this.cancel()
            }
        }
    }

    private fun showNotification(context: Context, title: String, category: String, time: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your habits and tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
            .setContentTitle("⏰ Time for: $title")
            .setContentText("Focus: $category • Scheduled for $time")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's time for your $category habit: $title. Keep up the great momentum! 🚀"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF1A237E.toInt())
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        notificationManager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), 
            builder.build()
        )

        // Trigger Voice Reminder if not on silent
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) {
            // Start voice immediately with a default name so there's zero delay.
            // This is the main fix: we don't wait for Firestore.
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val displayName = auth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Champion"
            
            val voiceIntent = Intent(context, VoiceReminderService::class.java).apply {
                putExtra(VoiceReminderService.EXTRA_HABIT_TITLE, title)
                putExtra(VoiceReminderService.EXTRA_HABIT_TIME, time)
                putExtra(VoiceReminderService.EXTRA_USER_NAME, displayName)
                putExtra(VoiceReminderService.EXTRA_LOCALE, "en")
            }
            ContextCompat.startForegroundService(context, voiceIntent)
        }
    }
}
