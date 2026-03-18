package com.example.fewstep.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceReminderService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var textToSpeak: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "voice_reminder_service"
    private val NOTIFICATION_ID = 99

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val habitTitle = intent?.getStringExtra("HABIT_TITLE") ?: "Habit"
        val scheduledTime = intent?.getStringExtra("HABIT_TIME") ?: ""
        
        textToSpeak = "Hello! It's time for your habit: $habitTitle. You set this for $scheduledTime. Let's stay on track!"
        
        startForegroundServiceLocal()
        acquireWakeLock()
        
        return START_NOT_STICKY
    }

    private fun startForegroundServiceLocal() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Voice Reminder", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Reminder")
            .setContentText("Speaking habit reminder...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FewStep:VoiceWakeLock")
        wakeLock?.acquire(3 * 60 * 1000L /* 3 minutes */)
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported")
                stopSelf()
            } else {
                speak()
            }
        } else {
            Log.e("TTS", "Initialization failed")
            stopSelf()
        }
    }

    private fun speak() {
        textToSpeak?.let {
            tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "HabitReminderID")
            
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    Log.d("TTS", "Finished speaking")
                    stopSelf()
                }
                override fun onError(utteranceId: String?) {
                    stopSelf()
                }
            })
        } ?: stopSelf()
    }

    override fun onDestroy() {
        tts?.let {
            it.stop()
            it.shutdown()
        }
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
