package com.example.fewstep.ui.screens.walk

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.local.StepHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WalkViewModel(application: Application) : AndroidViewModel(application) {
    private val habitRepository = com.example.fewstep.data.repository.HabitRepository()
    private val stepHistoryRepo = StepHistoryRepository(application)
    private val prefs = application.getSharedPreferences("step_prefs", android.content.Context.MODE_PRIVATE)

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()
    
    private val _stepGoal = MutableStateFlow(prefs.getInt("step_goal", 10000))
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _claimedMilestones = MutableStateFlow(loadClaimedMilestones())
    val claimedMilestones: StateFlow<Set<Int>> = _claimedMilestones.asStateFlow()

    // --- Room-backed history & weekly total ---
    private val _walkHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val walkHistory: StateFlow<List<Pair<String, Int>>> = _walkHistory.asStateFlow()

    private val _weeklyTotal = MutableStateFlow(0)
    val weeklyTotal: StateFlow<Int> = _weeklyTotal.asStateFlow()

    private var stepService: StepTrackingService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as StepTrackingService.StepBinder).getService()
            stepService = service
            isBound = true
            
            // Observe steps from service
            viewModelScope.launch {
                service.currentSteps.collect { currentSteps ->
                    _steps.value = currentSteps
                    // Refresh history every time steps update so the chart stays live
                    refreshHistory()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stepService = null
            isBound = false
        }
    }

    init {
        startAndBindService()
        // Load initial steps from prefs in case service is not yet bound
        _steps.value = prefs.getInt("daily_steps", 0)
        // Load history from DB immediately
        refreshHistory()
    }

    fun onPermissionGranted() {
        startAndBindService()
    }

    private fun startAndBindService() {
        val application = getApplication<Application>()
        
        // Only start the foreground service if we have the required permission
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                application, 
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val intent = Intent(application, StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_START
        }

        if (hasPermission) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        }
        
        // We can still bind to the service to listen for updates if it's running
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /** Reload 7-day history and weekly total from Room DB. */
    fun refreshHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _walkHistory.value = stepHistoryRepo.getHistory(
                days = 7,
                todayLiveSteps = _steps.value
            )
            _weeklyTotal.value = stepHistoryRepo.getWeeklyTotal()
        }
    }

    fun updateGoal(newGoal: Int) {
        prefs.edit().putInt("step_goal", newGoal).apply()
        _stepGoal.value = newGoal
    }

    fun claimXp(milestone: Int) {
        val currentClaimed = _claimedMilestones.value
        val allMilestones = (1000..milestone step 1000).filter { it !in currentClaimed }
        val xpToGain = allMilestones.size * 10L
        
        if (xpToGain <= 0) return

        val updated = currentClaimed.toMutableSet().apply { addAll(allMilestones) }
        saveClaimedMilestones(updated)
        _claimedMilestones.value = updated

        viewModelScope.launch {
            try {
                habitRepository.addExperiencePoints(xpToGain)
            } catch (e: Exception) {
                android.util.Log.e("WalkViewModel", "Error claiming XP: ${e.message}")
            }
        }
    }

    private fun loadClaimedMilestones(): Set<Int> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val savedDate = prefs.getString("xp_claim_date", "")
        if (savedDate != today) return emptySet()
        
        val set = prefs.getStringSet("claimed_milestones", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun saveClaimedMilestones(milestones: Set<Int>) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        prefs.edit()
            .putString("xp_claim_date", today)
            .putStringSet("claimed_milestones", milestones.map { it.toString() }.toSet())
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }

    fun getDistanceKm(currentSteps: Int): Double = (currentSteps * 0.762) / 1000.0

    fun getCaloriesBurned(currentSteps: Int): Int = (currentSteps * 0.04).toInt()

    /**
     * Legacy helper — still available for the WalkHistoryDialog which passes
     * a snapshot of data. The live version is [walkHistory] StateFlow.
     */
    fun get7DayHistory(): List<Pair<String, Int>> = _walkHistory.value
}
