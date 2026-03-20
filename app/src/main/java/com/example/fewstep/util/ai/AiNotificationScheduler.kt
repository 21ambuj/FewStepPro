package com.example.fewstep.util.ai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AiNotificationScheduler {

    private const val AI_NOTIFICATION_ID = 1000
    private const val BROADCAST_NOTIFICATION_ID = 2000
    private const val TAG = "AiNotification"

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AiNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AI_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val targetHours = listOf(0, 8, 9, 12, 14, 17, 20, 21)
        var nextHour = targetHours.firstOrNull { it > currentHour || (it == currentHour && currentMinute < 1) }
        
        if (nextHour == null) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, targetHours.first())
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, nextHour)
        }
        
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            val strictlyNext = targetHours.firstOrNull { it > currentHour }
            if (strictlyNext == null) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, targetHours.first())
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, strictlyNext)
            }
            calendar.set(Calendar.MINUTE, 0)
        }

        Log.d(TAG, "⏰ Scheduling next AI Coach session for: ${java.util.Date(calendar.timeInMillis)}")
        setAlarmInternal(context, alarmManager, calendar.timeInMillis, pendingIntent)
    }

    /**
     * Schedules a specific alarm for a custom Admin Broadcast.
     * This ensures the app wakes up exactly when the admin wants the message delivered.
     */
    fun scheduleBroadcast(context: Context, timestamp: Long) {
        // Safety truncation: Reset seconds/ms to ensure exact minute trigger
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val truncatedTimestamp = cal.timeInMillis

        if (truncatedTimestamp <= System.currentTimeMillis()) {
            Log.d(TAG, "⏭️ Truncated timestamp $truncatedTimestamp is in the past, skipping special alarm.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AiNotificationReceiver::class.java).apply {
            action = "com.example.fewstep.CUSTOM_BROADCAST"
        }
        
        // Use a unique ID based on the timestamp to allow multiple future broadcasts
        val requestCode = BROADCAST_NOTIFICATION_ID + (truncatedTimestamp % 10000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "📢 Scheduling SPECIFIC broadcast alarm for: ${java.util.Date(truncatedTimestamp)}")
        setAlarmInternal(context, alarmManager, truncatedTimestamp, pendingIntent)
    }

    fun scheduleTest(context: Context, delayMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AiNotificationReceiver::class.java).apply {
            action = "com.example.fewstep.TEST_SCHEDULER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AI_NOTIFICATION_ID + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, delayMinutes)
        
        Log.d(TAG, "🧪 Diagnostic: Scheduling test alarm for: ${java.util.Date(calendar.timeInMillis)}")
        setAlarmInternal(context, alarmManager, calendar.timeInMillis, pendingIntent)
    }

    private fun setAlarmInternal(context: Context, alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            timeInMillis,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, com.example.fewstep.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "✅ Alarm set successfully for ${java.util.Date(timeInMillis)}")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ setAlarmClock failed: ${e.message}. Falling back...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        }
    }

    fun startInitial(context: Context) {
        scheduleNext(context)
    }
}
