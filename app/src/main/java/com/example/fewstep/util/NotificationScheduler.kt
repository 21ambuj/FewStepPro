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
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
