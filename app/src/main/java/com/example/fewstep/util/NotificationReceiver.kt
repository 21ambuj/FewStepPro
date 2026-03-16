package com.example.fewstep.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

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
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Time for: $title")
            .setContentText("Focus: $category • Scheduled for $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), 
            builder.build()
        )
    }
}
