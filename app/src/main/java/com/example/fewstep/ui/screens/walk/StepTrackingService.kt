package com.example.fewstep.ui.screens.walk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fewstep.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StepTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "step_tracking_service"
        const val NOTIF_ID = 202
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private val binder = StepBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var stepManager: StepCounterManager

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps

    inner class StepBinder : Binder() {
        fun getService(): StepTrackingService = this@StepTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        stepManager = StepCounterManager(this)
        createNotificationChannel()
        
        scope.launch {
            stepManager.steps.collect { steps ->
                _currentSteps.value = steps
                // checkMilestones only posts an ALERT notification at specific thresholds
                checkMilestones(steps)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification())
                stepManager.start()
            }
            ACTION_STOP -> {
                stepManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Step Tracking", NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Tracking your daily steps reliably"
                setSound(null, null)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Tracking Active")
            .setContentText("FewStep is counting your steps in the background.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun checkMilestones(currentSteps: Int) {
        val prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        val goal = prefs.getInt("step_goal", 10000).coerceAtLeast(1)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val currentPercentage = currentSteps.toFloat() / goal
        
        val milestones = listOf(0.5f, 0.7f, 1.0f)
        
        for (milestone in milestones) {
            val milestoneKey = "notified_${(milestone * 100).toInt()}_date"
            val lastNotifiedDate = prefs.getString(milestoneKey, "")
            
            if (currentPercentage >= milestone && lastNotifiedDate != today) {
                prefs.edit().putString(milestoneKey, today).apply()
                
                // Trigger the alert notification
                com.example.fewstep.util.NotificationScheduler.showWalkProgressNotification(
                    this,
                    currentSteps,
                    goal
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stepManager.stop()
        super.onDestroy()
    }
}
