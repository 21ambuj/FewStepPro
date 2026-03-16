package com.example.fewstep.ui.screens.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

// Timer mode: focus or break
enum class TimerMode { FOCUS, SHORT_BREAK, LONG_BREAK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val habits by viewModel.habits.collectAsState()
    val completedIds: Set<String> by viewModel.completedHabitIdsForSelectedDay.collectAsState()

    // Timer State
    var timerMode by remember { mutableStateOf(TimerMode.FOCUS) }
    val totalSeconds by remember(timerMode) {
        derivedStateOf {
            when (timerMode) {
                TimerMode.FOCUS -> 25 * 60
                TimerMode.SHORT_BREAK -> 5 * 60
                TimerMode.LONG_BREAK -> 15 * 60
            }
        }
    }

    var secondsLeft by remember(timerMode) { mutableStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var sessionsCompleted by remember { mutableStateOf(0) }
    var selectedHabitId by remember { mutableStateOf<String?>(null) }
    var showCompletionBanner by remember { mutableStateOf(false) }

    // Countdown engine
    LaunchedEffect(isRunning, timerMode) {
        while (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        if (isRunning && secondsLeft == 0) {
            isRunning = false
            if (timerMode == TimerMode.FOCUS) {
                sessionsCompleted++
                // Auto-complete the selected habit after a focus session
                if (selectedHabitId != null && selectedHabitId !in completedIds) {
                    val selectedHabit = habits.find { it.id == selectedHabitId }
                    if (selectedHabit != null) {
                        viewModel.completeHabit(selectedHabit)
                        showCompletionBanner = true
                    }
                }
            }
        }
    }

    // Auto-hide banner
    LaunchedEffect(showCompletionBanner) {
        if (showCompletionBanner) {
            delay(3000L)
            showCompletionBanner = false
        }
    }

    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    // Pulsing animation when running
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val ringAlpha by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B4B))
            )
        },
        containerColor = Color(0xFF0D1B4B)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Completion Banner
            if (showCompletionBanner) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF43A047),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text("Habit marked complete! +50 XP 🎉", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mode Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2D6B), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimerMode.values().forEach { mode ->
                        val label = when (mode) {
                            TimerMode.FOCUS -> "Focus"
                            TimerMode.SHORT_BREAK -> "Short Break"
                            TimerMode.LONG_BREAK -> "Long Break"
                        }
                        val isSelected = timerMode == mode
                        Surface(
                            onClick = {
                                if (!isRunning) {
                                    timerMode = mode
                                    secondsLeft = when (mode) {
                                        TimerMode.FOCUS -> 25 * 60
                                        TimerMode.SHORT_BREAK -> 5 * 60
                                        TimerMode.LONG_BREAK -> 15 * 60
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF3D5AFE) else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // TIMER RING
            item {
                val ringColor = when (timerMode) {
                    TimerMode.FOCUS -> Color(0xFF3D5AFE)
                    TimerMode.SHORT_BREAK -> Color(0xFF43A047)
                    TimerMode.LONG_BREAK -> Color(0xFFFF7043)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(260.dp)
                        .drawBehind {
                            val stroke = 18.dp.toPx()
                            val inset = stroke / 2f
                            // Track
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // Progress arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(ringColor.copy(alpha = if (isRunning) ringAlpha else 1f), ringColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = when (timerMode) {
                                TimerMode.FOCUS -> "Stay focused 🎯"
                                TimerMode.SHORT_BREAK -> "Short break ☕"
                                TimerMode.LONG_BREAK -> "Long break 🌿"
                            },
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Session chips
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sessions: ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    repeat(4) { i ->
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (i < sessionsCompleted % 4) Color(0xFF3D5AFE) else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )
                    }
                    if (sessionsCompleted > 0) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Total: $sessionsCompleted",
                            color = Color(0xFFFFD600),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Control buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset
                    OutlinedIconButton(
                        onClick = {
                            isRunning = false
                            secondsLeft = totalSeconds
                        },
                        modifier = Modifier.size(56.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        ),
                        shape = CircleShape,
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                    }

                    // Play/Pause - large
                    FloatingActionButton(
                        onClick = { isRunning = !isRunning },
                        containerColor = Color(0xFF3D5AFE),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Habit selector
            if (timerMode == TimerMode.FOCUS && habits.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A2D6B), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "Focus on a Habit:",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        habits.forEach { habit ->
                            val isCompleted = habit.id in completedIds
                            val isSelected = habit.id == selectedHabitId
                            Surface(
                                onClick = { if (!isCompleted) selectedHabitId = if (isSelected) null else habit.id },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isCompleted -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                                    isSelected -> Color(0xFF3D5AFE).copy(alpha = 0.4f)
                                    else -> Color.White.copy(alpha = 0.05f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = when {
                                            isCompleted -> Color(0xFF66BB6A)
                                            isSelected -> Color(0xFF82B1FF)
                                            else -> Color.White.copy(alpha = 0.3f)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            habit.title,
                                            color = if (isCompleted) Color(0xFF66BB6A) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                        if (isCompleted) {
                                            Text("Completed today ✓", color = Color(0xFF66BB6A), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tip card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A2D6B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Work for 25 min, then take a 5-min break. After 4 sessions take a long break.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
