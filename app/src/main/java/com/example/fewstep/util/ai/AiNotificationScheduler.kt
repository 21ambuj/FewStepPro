package com.example.fewstep.util.ai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AiNotificationScheduler {

    private const val AI_NOTIFICATION_ID = 1000

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

        // Target hours for professional behavioral nudges
        val targetHours = listOf(0, 8, 9, 12, 14, 17, 20, 21)
        
        // Find the next target hour
        var nextHour = targetHours.firstOrNull { it > currentHour || (it == currentHour && currentMinute < 1) }
        
        if (nextHour == null) {
            // No more target hours today, schedule for the first one tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, targetHours.first())
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, nextHour)
        }
        
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // FINAL CHECK: If calculation resulted in a past time (e.g., currently 14:01 and we set 14:00),
        // we must find the next slot to avoid silent failure.
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            // Re-run the search but strictly for hours > currentHour
            val strictlyNext = targetHours.firstOrNull { it > currentHour }
            if (strictlyNext == null) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, targetHours.first())
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, strictlyNext)
            }
            calendar.set(Calendar.MINUTE, 0)
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
        } catch (e: Exception) {
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

    fun startInitial(context: Context) {
        scheduleNext(context)
    }
}
