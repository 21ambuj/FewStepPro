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
        const val EXTRA_USER_NAME = "USER_NAME"
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val customMessage = intent?.getStringExtra(EXTRA_MESSAGE)
        val habitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"
        val scheduledTime = intent?.getStringExtra(EXTRA_HABIT_TIME) ?: ""
        val userName = intent?.getStringExtra(EXTRA_USER_NAME) ?: "Champion"
        languageCode = intent?.getStringExtra(EXTRA_LOCALE) ?: "en"
        
        textToSpeak = customMessage ?: buildMessage(userName, habitTitle, scheduledTime)
        
        startForegroundServiceLocal()
        acquireWakeLock()
        
        // If TTS is already initialized from a previous call, speak immediately.
        // Otherwise onInit() will call speak() when ready.
        if (tts != null && isTtsReady) {
            speak()
        }
        
        // START_REDELIVER_INTENT ensures the OS re-delivers the intent if the service is killed mid-speech
        return START_REDELIVER_INTENT
    }

    private fun buildMessage(userName: String, habitTitle: String, scheduledTime: String): String {
        return "Hey $userName, time for $habitTitle! You scheduled this for $scheduledTime. Let's go!"
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

    private var isTtsReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val isHindiRequested = languageCode == "hi"
            val hindiLocale = Locale("hi", "IN")
            val englishIndiaLocale = Locale("en", "IN")
            
            var currentLocale = if (isHindiRequested) hindiLocale else Locale.US
            var result = tts?.setLanguage(currentLocale)
            
            Log.d("VoiceAI", "Init Status: SUCCESS. Requested Locale: $languageCode")

            if (isHindiRequested && (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                Log.w("VoiceAI", "Hindi data missing! Falling back to Indian English.")
                currentLocale = englishIndiaLocale
                result = tts?.setLanguage(currentLocale)
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceAI", "Language not supported. Falling back to US English.")
                tts?.setLanguage(Locale.US)
                currentLocale = Locale.US
            }
            
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(if (isHindiRequested) 0.75f else 0.8f)

            // Voice selection
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val voices = tts?.voices
                    val bestVoice = voices?.filter { it.locale.language == currentLocale.language }
                        ?.sortedWith(compareByDescending<Voice> { 
                            (if (it.name.contains("network", true)) 5 else 0) +
                            (if (it.quality >= Voice.QUALITY_VERY_HIGH) 3 else 0) +
                            (if (it.locale.country == currentLocale.country) 2 else 0) +
                            (if (!it.isNetworkConnectionRequired) 1 else 0)
                        } then compareByDescending {
                            it.name.contains("google", true) || it.name.contains("-x-", true)
                        })
                        ?.firstOrNull()

                    bestVoice?.let { 
                        Log.d("VoiceAI", "Final Voice: ${it.name} | Locale: ${it.locale}")
                        tts?.voice = it 
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceAI", "Voice selection error: ${e.message}")
            }
            
            isTtsReady = true
            // Now it's safe to speak — TTS engine is fully configured
            speak()
        } else {
            Log.e("VoiceAI", "TTS Initialization failed with status: $status")
            stopSelf()
        }
    }

    private fun stripEmojis(text: String): String {
        if (text.isBlank()) return ""
        
        // 1. Remove Emojis (Comprehensive regex + Unicode categories)
        val emojiRegex = ("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+" +
                         "|[\\u2600-\\u27BF]+" +
                         "|[\\u2300-\\u23FF]+" +
                         "|[\\u2B50\\u2B06\\u2194\\u21AA]+" +
                         "|\\p{So}").toRegex()
        
        var clean = text.replace(emojiRegex, "")
        
        // 2. Remove formatting characters (*, _, ~, #, `)
        clean = clean.replace(Regex("[*_~#`]"), "")
        
        // 3. Clean up repeated punctuation (!!! -> !, ??? -> ?)
        clean = clean.replace(Regex("!+"), "!")
        clean = clean.replace(Regex("\\?+"), "?")
        clean = clean.replace(Regex("\\.+"), ".")
        
        // 4. Remove trailing/leading whitespace
        return clean.replace("\\s+".toRegex(), " ").trim()
    }

    private fun speak() {
        val rawText = textToSpeak
        if (rawText.isNullOrBlank()) {
            Log.e("VoiceAI", "No text to speak. Stopping.")
            stopSelf()
            return
        }

        val cleanText = stripEmojis(rawText)
        Log.d("VoiceAI", "Speaking: $cleanText")

        // IMPORTANT: Set listener BEFORE calling speak() to avoid race condition
        // where short utterances complete before listener is attached.
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("VoiceAI", "Started speaking")
            }
            override fun onDone(utteranceId: String?) {
                Log.d("VoiceAI", "Finished speaking")
                stopSelf()
            }
            override fun onError(utteranceId: String?) {
                Log.e("VoiceAI", "TTS Error on utteranceId: $utteranceId")
                stopSelf()
            }
        })

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "HabitReminderID")
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
