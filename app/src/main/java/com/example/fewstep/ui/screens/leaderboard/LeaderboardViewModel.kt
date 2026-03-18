package com.example.fewstep.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.model.User
import com.example.fewstep.data.repository.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LeaderboardViewModel(private val repository: HabitRepository) : ViewModel() {
    val topUsers: StateFlow<List<User>> = repository.getLeaderboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
