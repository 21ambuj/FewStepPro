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
import android.speech.tts.Voice
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceReminderService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var textToSpeak: String? = null
    private var languageCode: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "voice_reminder_service"
    private val NOTIFICATION_ID = 99

    companion object {
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_LOCALE = "EXTRA_LOCALE" // "en" or "hi"
        const val EXTRA_HABIT_TITLE = "HABIT_TITLE"
        const val EXTRA_HABIT_TIME = "HABIT_TIME"
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val customMessage = intent?.getStringExtra(EXTRA_MESSAGE)
        val habitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"
        val scheduledTime = intent?.getStringExtra(EXTRA_HABIT_TIME) ?: ""
        languageCode = intent?.getStringExtra(EXTRA_LOCALE) ?: "en"
        
        val currentName = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: "Champion"
        textToSpeak = customMessage ?: "Hello $currentName, this is time for $habitTitle. You have set time of $scheduledTime. All the best! 🚀"
        
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
            val isHindiRequested = languageCode == "hi"
            val hindiLocale = Locale("hi", "IN")
            val englishIndiaLocale = Locale("en", "IN") // Fallback for better accent if Hindi missing
            
            var currentLocale = if (isHindiRequested) hindiLocale else Locale.US
            var result = tts?.setLanguage(currentLocale)
            
            Log.d("VoiceAI", "Init Status: SUCCESS. Requested Locale: $languageCode")

            if (isHindiRequested && (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                Log.w("VoiceAI", "⚠️ Hindi data missing! Attempting Indian English fallback for custom Hindi message.")
                currentLocale = englishIndiaLocale
                result = tts?.setLanguage(currentLocale)
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceAI", "❌ Multiple languages failed. Falling back to US.")
                tts?.setLanguage(Locale.US)
                currentLocale = Locale.US
            }
            
            if (isHindiRequested) {
                // Hindi articulation is clearer at a slightly slower pace
                tts?.setPitch(1.0f)        
                tts?.setSpeechRate(0.8f)   // 0.8f for natural Hindi pronunciation
            } else {
                tts?.setPitch(1.05f) 
                tts?.setSpeechRate(0.9f)   // 0.9f for a calm, professional English coach
            }

            // Stricter Voice Selection
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val voices = tts?.voices
                    val bestVoice = voices?.filter { it.locale.language == currentLocale.language }
                        ?.sortedWith(compareByDescending<Voice> { 
                            // Strong preference for Wavenet / High-Quality
                            (if (it.name.contains("network", true)) 5 else 0) +
                            (if (it.quality >= Voice.QUALITY_VERY_HIGH) 3 else 0) +
                            (if (it.locale.country == currentLocale.country) 2 else 0) +
                            (if (!it.isNetworkConnectionRequired) 1 else 0)
                        } then compareByDescending {
                            it.name.contains("google", true) || it.name.contains("-x-", true)
                        })
                        ?.firstOrNull()

                    bestVoice?.let { 
                        Log.d("VoiceAI", "Final Voice Selection: ${it.name} | Locale: ${it.locale}")
                        tts?.voice = it 
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceAI", "Voice selection error: ${e.message}")
            }
            
            speak()
        } else {
            Log.e("VoiceAI", "Initialization failed")
            stopSelf()
        }
    }

    private fun speak() {
        textToSpeak?.let {
            tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "HabitReminderID")
            
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    Log.d("VoiceAI", "Finished speaking")
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
