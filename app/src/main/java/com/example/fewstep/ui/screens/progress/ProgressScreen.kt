package com.example.fewstep.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: HomeViewModel, onBackClick: () -> Unit) {
    val allLogs by viewModel.allLogs.collectAsState()
    val habits by viewModel.habits.collectAsState()
    
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val logsForSelectedDate = allLogs.filter { 
        it.date == sdf.format(selectedDate.time) && it.completed 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consistency Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            // Calendar Header (Month Switcher)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = currentMonthCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentMonthCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                        }
                        
                        Text(
                            text = monthYearFormat.format(currentMonthCalendar.time),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E)
                        )

                        IconButton(onClick = {
                            val newCal = currentMonthCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentMonthCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Days of Week Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val days = listOf("S", "M", "T", "W", "T", "F", "S")
                        days.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    CalendarGrid(
                        currentMonth = currentMonthCalendar,
                        allLogs = allLogs,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                }
            }

            // Results Section
            Text(
                text = "Activities for ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(selectedDate.time)}",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF455A64)
            )

            if (logsForSelectedDate.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No habits completed on this day 🌬️", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(logsForSelectedDate.size) { index ->
                        val log = logsForSelectedDate[index]
                        val habitName = habits.find { it.id == log.habitId }?.title ?: "Unknown Habit"
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(habitName, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar,
    allLogs: List<com.example.fewstep.data.model.HabitLog>,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthStartCal = currentMonth.clone() as Calendar
    monthStartCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = monthStartCal.get(Calendar.DAY_OF_WEEK) - 1

    val totalSlots = 42 // 6 rows of 7
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val dates = (0 until totalSlots).map { i ->
        val dateCal = monthStartCal.clone() as Calendar
        dateCal.add(Calendar.DAY_OF_MONTH, i - firstDayOfWeek)
        dateCal
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(280.dp),
        userScrollEnabled = false
    ) {
        items(dates) { dateCal ->
            val isCurrentMonth = dateCal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
            val dateStr = sdf.format(dateCal.time)
            val hasLogs = allLogs.any { it.date == dateStr && it.completed }
            val isSelected = sdf.format(selectedDate.time) == dateStr
            val isToday = sdf.format(Date()) == dateStr

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> Color(0xFF1A237E)
                            isToday -> Color(0xFFE8EAF6)
                            else -> Color.Transparent
                        }
                    )
                    .clickable { onDateSelected(dateCal) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateCal.get(Calendar.DAY_OF_MONTH).toString(),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> Color.White
                            !isCurrentMonth -> Color.LightGray
                            isToday -> Color(0xFF1A237E)
                            else -> Color.Black
                        }
                    )
                    if (hasLogs && !isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFF4CAF50), CircleShape))
                    }
                }
            }
        }
    }
}
