package com.example.fewstep.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationScheduler {

    fun scheduleHabitReminder(context: Context, habitId: Int, habitTitle: String, category: String, timeString: String, scheduledDays: List<Int>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Android 12+ check for exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If we can't schedule exact alarms, we can't do much here 
                // but usually the user is prompted in MainActivity or we fall back to inexact
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.example.fewstep.ACTION_HABIT_REMINDER"
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_TITLE", habitTitle)
            putExtra("HABIT_CATEGORY", category)
            putExtra("HABIT_TIME", timeString)
            putExtra("SCHEDULED_DAYS", scheduledDays.toIntArray())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            try {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                val parsedTime = sdf.parse(timeString)
                if (parsedTime != null) {
                    val timeCalendar = Calendar.getInstance().apply { time = parsedTime }
                    set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                } else {
                    return // Cannot schedule with invalid time
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, com.example.fewstep.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            // Fallback for missing permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelHabitReminder(context: Context, habitId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.example.fewstep.ACTION_HABIT_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showWalkProgressNotification(context: Context, steps: Int, goal: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "walk_progress"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Walk Progress",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily step progress updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val remaining = (goal - steps).coerceAtLeast(0)
        val message = if (remaining > 0) {
            "You have completed $steps steps, only $remaining steps to complete today's goal."
        } else {
            "Goal reached! You've completed $steps steps today. Amazing work! 🚶🔥"
        }

        val intent = Intent(context, com.example.fewstep.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.fewstep.R.drawable.ic_notification)
            .setContentTitle("Keep Moving! 🚶")
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}
