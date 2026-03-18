package com.example.fewstep.ui.screens.focus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fewstep.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val habits by viewModel.habits.collectAsState()
    val completedIds: Set<String> by viewModel.completedHabitIdsForSelectedDay.collectAsState()

    // --- Service State ---
    var timerService by remember { mutableStateOf<FocusTimerService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as FocusTimerService.TimerBinder).getService()
                isBound = true
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
                isBound = false
            }
        }
    }

    // Bind to the service if it's already running
    DisposableEffect(Unit) {
        val intent = Intent(context, FocusTimerService::class.java)
        context.bindService(intent, connection, 0)
        onDispose {
            if (isBound) context.unbindService(connection)
        }
    }

    // Observe live state from service (or use defaults)
    val secondsLeft by (timerService?.secondsLeft ?: MutableStateFlow(25 * 60)).collectAsState()
    val isRunning by (timerService?.isRunning ?: MutableStateFlow(false)).collectAsState()
    val isFocusPhase by (timerService?.isFocusPhase ?: MutableStateFlow(true)).collectAsState()
    val sessionsCompleted by (timerService?.sessionsCompleted ?: MutableStateFlow(0)).collectAsState()
    val totalSeconds by (timerService?.totalSeconds ?: MutableStateFlow(25 * 60)).collectAsState()

    // --- User Configs ---
    var focusMinutes by remember { mutableStateOf("25") }
    var breakMinutes by remember { mutableStateOf("5") }
    var selectedHabitId by remember { mutableStateOf<String?>(null) }
    var isSettingsExpanded by remember { mutableStateOf(false) }

    // --- Derived ---
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60

    // --- Colors ---
    val focusColor = Color(0xFF3D5AFE)
    val breakColor = Color(0xFF00BFA5)
    val ringColor = if (isFocusPhase) focusColor else breakColor

    // --- Animations ---
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val glowAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    // Helper to start/restart service
    fun startService() {
        val focusSecs = (focusMinutes.toIntOrNull() ?: 25).coerceIn(1, 180) * 60
        val breakSecs = (breakMinutes.toIntOrNull() ?: 5).coerceIn(1, 60) * 60
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_FOCUS_SECS, focusSecs)
            putExtra(FocusTimerService.EXTRA_BREAK_SECS, breakSecs)
        }
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun sendAction(action: String) {
        val intent = Intent(context, FocusTimerService::class.java).apply { this.action = action }
        context.startService(intent)
    }

    // Deep indigo gradient background
    val bgBrush = Brush.verticalGradient(listOf(Color(0xFF0A1128), Color(0xFF0D1B4B), Color(0xFF131A3B)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // === HEADER ===
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "FOCUS MODE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.weight(1f))
                // Balance the row visually
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(8.dp))

            // === PHASE PILL ===
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = ringColor.copy(alpha = 0.2f),
                modifier = Modifier.border(1.dp, ringColor.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
            ) {
                Text(
                    text = if (isFocusPhase) "🎯  FOCUS SESSION" else "☕  BREAK TIME",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = ringColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // === CUSTOMIZE TIMER CHIP (always visible) ===
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = if (isSettingsExpanded) Color(0xFF3D5AFE).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isSettingsExpanded) Color(0xFF3D5AFE).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(50.dp)
                    )
                    .clickable { isSettingsExpanded = !isSettingsExpanded }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${focusMinutes}m focus · ${breakMinutes}m break  ${if (isSettingsExpanded) "▲" else "▼"}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // === TIMER RING ===
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(
                        scaleX = if (isRunning) pulseScale else 1f,
                        scaleY = if (isRunning) pulseScale else 1f
                    )
                    .drawBehind {
                        val stroke = 20.dp.toPx()
                        val inset = stroke / 2f
                        val arcSize = Size(size.width - stroke, size.height - stroke)

                        // Glow outer ring
                        if (isRunning) {
                            drawArc(
                                color = ringColor.copy(alpha = glowAlpha * 0.3f),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = Offset(inset - 8, inset - 8),
                                size = Size(arcSize.width + 16, arcSize.height + 16),
                                style = Stroke(width = stroke + 16, cap = StrokeCap.Round)
                            )
                        }
                        // Track
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // Progress
                        if (progress > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(ringColor.copy(alpha = 0.6f), ringColor, ringColor.copy(alpha = 0.9f))
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d:%02d", mins, secs),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) "IN PROGRESS" else if (secondsLeft == totalSeconds) "READY" else "PAUSED",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // === SESSION DOTS ===
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i < sessionsCompleted % 4) 14.dp else 10.dp)
                            .background(
                                if (i < sessionsCompleted % 4) ringColor else Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    )
                    if (i < 3) Spacer(Modifier.width(10.dp))
                }
                if (sessionsCompleted > 0) {
                    Spacer(Modifier.width(14.dp))
                    Surface(
                        color = Color(0xFFFFD600).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "🏆 $sessionsCompleted sessions",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color(0xFFFFD600),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            // === CONTROL BUTTONS ===
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset
                ControlButton(
                    onClick = { if (isBound) timerService?.resetTimer() else sendAction(FocusTimerService.ACTION_RESET) },
                    icon = Icons.Default.Refresh,
                    label = "Reset",
                    size = 60.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    iconColor = Color.White,
                )

                Spacer(Modifier.width(20.dp))

                // Play / Pause (big)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(ringColor, ringColor.copy(alpha = 0.7f)))
                        )
                        .clickable {
                            if (!isBound) {
                                startService()
                            } else {
                                if (isRunning) timerService?.pauseTimer() else timerService?.resumeTimer()
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.width(20.dp))

                // Stop
                ControlButton(
                    onClick = { sendAction(FocusTimerService.ACTION_STOP) },
                    icon = Icons.Default.Stop,
                    label = "Stop",
                    size = 60.dp,
                    color = Color(0xFFD32F2F).copy(alpha = 0.25f),
                    iconColor = Color(0xFFEF9A9A),
                )
            }

            Spacer(Modifier.height(28.dp))

            // === CUSTOM TIMING SETTINGS ===
            AnimatedSettingsCard(
                expanded = isSettingsExpanded,
                focusMinutes = focusMinutes,
                breakMinutes = breakMinutes,
                onFocusChange = { focusMinutes = it },
                onBreakChange = { breakMinutes = it },
                onApply = {
                    isSettingsExpanded = false  // Auto-collapse after apply
                    if (!isRunning) startService()
                }
            )

            Spacer(Modifier.height(20.dp))

            // === HABIT SELECTOR ===
            if (habits.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A2D6B).copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrackChanges, null, tint = ringColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Focus On a Habit", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        habits.forEach { habit ->
                            val isDone = habit.id in completedIds
                            val isSelected = habit.id == selectedHabitId
                            Surface(
                                onClick = { if (!isDone) selectedHabitId = if (isSelected) null else habit.id },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isDone -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                                    isSelected -> ringColor.copy(alpha = 0.3f)
                                    else -> Color.White.copy(alpha = 0.05f)
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint = if (isDone) Color(0xFF66BB6A) else if (isSelected) ringColor else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(habit.title, color = if (isDone) Color(0xFF66BB6A) else Color.White, fontSize = 14.sp)
                                    if (isDone) {
                                        Spacer(Modifier.weight(1f))
                                        Text("✓ Done", color = Color(0xFF66BB6A), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    size: Dp,
    color: Color,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick)
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(size * 0.45f))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AnimatedSettingsCard(
    expanded: Boolean,
    focusMinutes: String,
    breakMinutes: String,
    onFocusChange: (String) -> Unit,
    onBreakChange: (String) -> Unit,
    onApply: () -> Unit
) {
    if (!expanded) return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A2D6B).copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = Color(0xFF82B1FF), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Custom Timing", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Focus time
                Column(modifier = Modifier.weight(1f)) {
                    Text("🎯 Focus (min)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = focusMinutes,
                        onValueChange = { if (it.length <= 3) onFocusChange(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3D5AFE),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF3D5AFE)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Break time
                Column(modifier = Modifier.weight(1f)) {
                    Text("☕ Break (min)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = breakMinutes,
                        onValueChange = { if (it.length <= 3) onBreakChange(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00BFA5)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply & Start", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
