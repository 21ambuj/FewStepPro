package com.example.fewstep.ui.screens.focus

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
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

import com.example.fewstep.R

class FocusTimerService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "focus_timer_service"
        const val NOTIF_ID = 101
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESET = "ACTION_RESET"
        const val EXTRA_FOCUS_SECS = "EXTRA_FOCUS_SECS"
        const val EXTRA_BREAK_SECS = "EXTRA_BREAK_SECS"
    }

    inner class TimerBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    private val binder = TimerBinder()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var tts: TextToSpeech? = null
    private var pendingSpeech: String? = null
    private var isTtsReady = false
    
    private val focusRepository = com.example.fewstep.data.repository.FocusHistoryRepository()
    private val habitRepository = com.example.fewstep.data.repository.HabitRepository()

    // Public state flows that the UI can observe
    private val _secondsLeft = MutableStateFlow(25 * 60)
    val secondsLeft: StateFlow<Int> = _secondsLeft

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isFocusPhase = MutableStateFlow(true)
    val isFocusPhase: StateFlow<Boolean> = _isFocusPhase

    private val _sessionsCompleted = MutableStateFlow(0)
    val sessionsCompleted: StateFlow<Int> = _sessionsCompleted

    private val _totalSeconds = MutableStateFlow(25 * 60)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private var focusDuration = 25 * 60
    private var breakDuration = 5 * 60

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                focusDuration = intent.getIntExtra(EXTRA_FOCUS_SECS, 25 * 60)
                breakDuration = intent.getIntExtra(EXTRA_BREAK_SECS, 5 * 60)
                _totalSeconds.value = focusDuration
                _secondsLeft.value = focusDuration
                _isFocusPhase.value = true
                startForeground(NOTIF_ID, buildNotification())
                resumeTimer()
            }
            ACTION_PAUSE -> {
                if (_isRunning.value) pauseTimer() else resumeTimer()
            }
            ACTION_STOP -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_RESET -> {
                resetTimer()
            }
        }
        return START_NOT_STICKY
    }

    fun resumeTimer() {
        if (_isRunning.value) return
        _isRunning.value = true
        speak(if (_isFocusPhase.value) "Focus session started... Let's stay sharp! 💪" else "Break time! ☕ Relax and recharge... You earned it!")
        timerJob = scope.launch {
            while (_isRunning.value && _secondsLeft.value > 0) {
                delay(1000L)
                _secondsLeft.value--
                updateNotification()
            }
            if (_isRunning.value && _secondsLeft.value == 0) {
                _isRunning.value = false
                if (_isFocusPhase.value) {
                    _sessionsCompleted.value++
                    saveSession()
                    awardXp()
                    speak("Focus session complete! 🔥 Great work. Starting break now.")
                    switchToBreak()
                } else {
                    speak("Break's over! Ready for the next session? Let's go! 🚀")
                    switchToFocus()
                }
            }
        }
    }



    private fun saveSession() {
        scope.launch(Dispatchers.IO) {
            try {
                val session = com.example.fewstep.data.model.FocusSession(
                    durationSeconds = focusDuration,
                    tag = "Focus Session"
                )
                focusRepository.saveSession(session)
            } catch (e: Exception) {
                Log.e("FocusTimerService", "Error saving session: ${e.message}")
            }
        }
    }

    private fun awardXp() {
        scope.launch(Dispatchers.IO) {
            try {
                // Award 20 XP for a focus session
                habitRepository.addExperiencePoints(20L)
            } catch (e: Exception) {
                Log.e("FocusTimerService", "Error awarding XP: ${e.message}")
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        speak("Timer paused... Take a breath.")
        updateNotification()
    }

    fun stopTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        speak("Timer stopped... Session ended.")
    }

    fun resetTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        _isFocusPhase.value = true
        _totalSeconds.value = focusDuration
        _secondsLeft.value = focusDuration
        speak("Timer reset... Starting fresh!")
        updateNotification()
    }

    private fun switchToBreak() {
        _isFocusPhase.value = false
        _totalSeconds.value = breakDuration
        _secondsLeft.value = breakDuration
        resumeTimer()
    }

    private fun switchToFocus() {
        _isFocusPhase.value = true
        _totalSeconds.value = focusDuration
        _secondsLeft.value = focusDuration
        updateNotification()
    }

    private fun stripEmojis(text: String): String {
        val emojiRegex = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u26FF\\u2700-\\u27BF\\u2300-\\u23FF\\u2B50\\u2B06\\u2194\\u21AA]".toRegex()
        return text.replace(emojiRegex, "").replace("\\s+".toRegex(), " ").trim()
    }

    private fun speak(text: String) {
        if (tts == null || !isTtsReady) {
            pendingSpeech = text
            return
        }
        val cleanText = stripEmojis(text)
        Log.d("FocusTimer", "Original: $text | Speaking: $cleanText")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "focus_tts_${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.US)
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(0.9f)
            isTtsReady = true
            pendingSpeech?.let {
                speak(it)
                pendingSpeech = null
            }
        } else {
            Log.e("FocusTimer", "TTS init failed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Focus Timer", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Focus session timer running in background"
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

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resetIntent = PendingIntent.getService(
            this, 2,
            Intent(this, FocusTimerService::class.java).apply { action = ACTION_RESET },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 3,
            Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mins = _secondsLeft.value / 60
        val secs = _secondsLeft.value % 60
        val phase = if (_isFocusPhase.value) "🎯 Focus" else "☕ Break"
        val state = if (_isRunning.value) "Running" else "Paused"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$phase — $state")
            .setContentText(String.format("%02d:%02d remaining", mins, secs))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (_isRunning.value) "Pause" else "Resume",
                pauseIntent
            )
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        timerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
