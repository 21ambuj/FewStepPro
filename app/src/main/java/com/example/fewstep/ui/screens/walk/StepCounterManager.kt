package com.example.fewstep.ui.screens.walk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fewstep.data.local.StepHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StepCounterManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val counterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val prefs = context.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    // Room-backed repository for persistent history
    private val repository = StepHistoryRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    init {
        loadSteps()
    }

    fun start() {
        counterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        detectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                handleCounterUpdate(event.values[0].toInt())
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    incrementStepManually()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun handleCounterUpdate(totalStepsSinceBoot: Int) {
        val today = getTodayDate()
        val savedDate = prefs.getString("last_saved_date", "")

        if (savedDate != today) {
            // New day: save yesterday's final count, reset for today
            prefs.edit()
                .putString("last_saved_date", today)
                .putInt("base_steps", totalStepsSinceBoot)
                .putInt("daily_steps", 0)
                .apply()
            _steps.value = 0
            return
        }

        val baseSteps = prefs.getInt("base_steps", -1)
        if (baseSteps == -1 || totalStepsSinceBoot < baseSteps) {
            // Reboot detected — reset base
            prefs.edit().putInt("base_steps", totalStepsSinceBoot).apply()
        }

        val currentBase = prefs.getInt("base_steps", totalStepsSinceBoot)
        val dailySteps = (totalStepsSinceBoot - currentBase).coerceAtLeast(0)

        if (dailySteps > _steps.value) {
            _steps.value = dailySteps
            persistSteps(dailySteps)
        }
    }

    private fun incrementStepManually() {
        _steps.value += 1
        persistSteps(_steps.value)
    }

    /**
     * Saves steps to both SharedPreferences (fast, for cold-start reads)
     * and Room database (reliable, for history queries).
     */
    private fun persistSteps(count: Int) {
        val today = getTodayDate()
        // SharedPreferences for quick in-process access
        prefs.edit()
            .putInt("daily_steps", count)
            .putInt("steps_$today", count)
            .apply()

        // Room DB for persistent history
        scope.launch {
            repository.saveTodaySteps(count)
        }
    }

    private fun loadSteps() {
        val today = getTodayDate()
        val savedDate = prefs.getString("last_saved_date", "")
        _steps.value = if (savedDate == today) prefs.getInt("daily_steps", 0) else 0
    }

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
