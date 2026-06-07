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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // --- Derived ---
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60

    // --- Colors ---
    val isDark = !MaterialTheme.colorScheme.surface.let { color ->
        (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) > 0.5
    }

    val focusColor = MaterialTheme.colorScheme.primary
    val breakColor = MaterialTheme.colorScheme.tertiary
    val ringColor = if (isFocusPhase) focusColor else breakColor
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

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

    // Theme-aware background
    val bgBrush = MaterialTheme.colorScheme.background



    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Focus Mode", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showHistoryDialog = true }) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(bgBrush).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .blur(if (isSettingsExpanded) 15.dp else 0.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(10.dp))

                // === EDIT TIME BUTTON ===
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSettingsExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSettingsExpanded = !isSettingsExpanded }
                        .border(
                            1.dp,
                            if (isSettingsExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = if (isSettingsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Edit Focus Time",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${focusMinutes}m focus · ${breakMinutes}m break",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (isSettingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(56.dp))

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
                            drawArc(
                                color = trackColor,
                                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                                topLeft = Offset(inset, inset), size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
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
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isRunning) "IN PROGRESS" else if (secondsLeft == totalSeconds) "READY" else "PAUSED",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                                    if (i < sessionsCompleted % 4) ringColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
                Spacer(modifier = Modifier.height(24.dp))


                // === CONTROL BUTTONS ===
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        onClick = { if (isBound) timerService?.resetTimer() else sendAction(FocusTimerService.ACTION_RESET) },
                        icon = Icons.Default.Refresh,
                        label = "Reset",
                        size = 60.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(20.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(ringColor, ringColor.copy(alpha = 0.7f)))
                            )
                            .clickable {
                                if (!isBound) startService()
                                else if (isRunning) timerService?.pauseTimer() else timerService?.resumeTimer()
                            }
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    ControlButton(
                        onClick = { sendAction(FocusTimerService.ACTION_STOP) },
                        icon = Icons.Default.Stop,
                        label = "Stop",
                        size = 60.dp,
                        color = Color(0xFFD32F2F).copy(alpha = 0.25f),
                        iconColor = Color(0xFFEF9A9A),
                    )
                }

                Spacer(Modifier.height(64.dp))
            }

            // === OVERLAY MODAL ===
            if (isSettingsExpanded) {
                // Dimming Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { isSettingsExpanded = false }
                )

                // Settings Card
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        AnimatedSettingsCard(
                            expanded = true,
                            focusMinutes = focusMinutes,
                            breakMinutes = breakMinutes,
                            onFocusChange = { focusMinutes = it },
                            onBreakChange = { breakMinutes = it },
                            onApply = {
                                isSettingsExpanded = false
                                if (!isRunning) startService()
                            }
                        )
                    }
                }
            }

            if (showHistoryDialog) {
                FocusHistoryDialog(
                    viewModel = viewModel,
                    onDismiss = { showHistoryDialog = false }
                )
            }
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
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Custom Timing", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Focus time
                Column(modifier = Modifier.weight(1f)) {
                    Text("🎯 Focus (min)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = focusMinutes,
                        onValueChange = { if (it.length <= 3) onFocusChange(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
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
                    Text("☕ Break (min)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = breakMinutes,
                        onValueChange = { if (it.length <= 3) onBreakChange(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.tertiary
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply & Start", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
