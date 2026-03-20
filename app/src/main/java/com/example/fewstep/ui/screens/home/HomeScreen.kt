package com.example.fewstep.ui.screens.home

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.R
import com.example.fewstep.data.model.User
import com.example.fewstep.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import com.example.fewstep.ui.components.AdMobBanner

enum class DayState { PAST, TODAY, FUTURE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    viewModel: HomeViewModel,
    onAddHabitClick: () -> Unit,
    onProfileClick: () -> Unit,
    onProgressClick: () -> Unit,
    onFocusClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onEditClick: (com.example.fewstep.data.model.Habit) -> Unit,
    onAiCoachClick: () -> Unit

) {
    val user by viewModel.userData.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val historyRecap by viewModel.historyRecap.collectAsState()
    val habitStats by viewModel.habitStats.collectAsState()
    val selectedDay by viewModel.selectedDayOfWeek.collectAsState()
    val completedIdsForDay by viewModel.completedHabitIdsForSelectedDay.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var processingIds by remember { mutableStateOf(setOf<String>()) }

    var showCelebrate by remember { mutableStateOf(false) }
    var celebratoryStreak by remember { mutableStateOf(0) }
    var isMilestone by remember { mutableStateOf(false) }

    val newMilestone by viewModel.newMilestone.collectAsState()

    // Persistent celebration flag
    val prefs = remember { context.getSharedPreferences("FewStepPrefs", Context.MODE_PRIVATE) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Reorder: Pending first, Completed last. Both sub-sections sorted by time.
    val sortedHabits = habits.sortedWith(
        compareBy<com.example.fewstep.data.model.Habit> { it.id in completedIdsForDay }
            .thenBy { it.reminderTime }
    )

    // Clear processing IDs once they are confirmed by the server
    LaunchedEffect(completedIdsForDay) {
        processingIds = processingIds.filter { it !in completedIdsForDay }.toSet()
    }
    
    // Trigger celebration once per day when the streak is updated
    LaunchedEffect(user?.lastStreakUpdate, user?.currentStreak) {
        val lastUpdate = user?.lastStreakUpdate ?: ""
        val currentStreak = user?.currentStreak ?: 0
        val lastCelebrated = prefs.getString("lastCelebratedDate", "")

        if (lastUpdate == today && lastCelebrated != today && currentStreak > 0) {
            celebratoryStreak = currentStreak
            isMilestone = listOf(7, 15, 30, 50, 100).contains(currentStreak)
            showCelebrate = true
        }
    }

    // Explicit Milestone trigger from ViewModel
    LaunchedEffect(newMilestone) {
        newMilestone?.let { milestone ->
            val lastCelebratedMilestone = prefs.getInt("lastMilestone_$milestone", 0)
            if (lastCelebratedMilestone != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                celebratoryStreak = milestone
                isMilestone = true
                showCelebrate = true
            }
        }
    }
    
    val daysOfWeek = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    val dayMapping: List<Int> = listOf(
        java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY, 
        java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY, java.util.Calendar.SATURDAY
    )
    val selectedIndex: Int = dayMapping.indexOf(selectedDay)
    

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Hello, ${user?.name ?: userName}!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All cards same weight for uniform size
                        HeaderBadgeCard(
                            modifier = Modifier.weight(1f),
                            label = "Streak",
                            value = "${user?.currentStreak ?: 0}d",
                            icon = Icons.Default.Whatshot,
                            iconColor = Color(0xFFFF5722),
                            bgColor = Color(0xFFFFE0B2)
                        )
                        HeaderBadgeCard(
                            modifier = Modifier.weight(1f),
                            label = "Level",
                            value = "${user?.level ?: 1}",
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFFFFA000),
                            bgColor = Color(0xFFFFF9C4)
                        )
                        HeaderBadgeCard(
                            modifier = Modifier.weight(1f),
                            label = "Cals",
                            value = "Log",
                            icon = Icons.Default.DateRange,
                            iconColor = Color(0xFF1E88E5),
                            bgColor = Color(0xFFE3F2FD),
                            onClick = onProgressClick
                        )
                    }
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = onAiCoachClick,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.padding(bottom = 12.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach", modifier = Modifier.size(24.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = onAddHabitClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Habit", modifier = Modifier.size(28.dp))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    user?.let {
                        XpProgressBar(it.xp, it.level)
                    }
                }

                item {
                    WeeklyRecapBar(historyRecap)
                }

                
                item {
                    val statusText = if (selectedDay == java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) "Today's Missions" else "${daysOfWeek[selectedIndex]}'s Missions"
                    Text(
                        text = statusText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    val todayIndex = dayMapping.indexOf(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK))
                    val dayState = when {
                        selectedIndex < todayIndex -> DayState.PAST
                        selectedIndex > todayIndex -> DayState.FUTURE
                        else -> DayState.TODAY
                    }

                    WeeklyTabRow(
                        days = daysOfWeek,
                        selectedIndex = selectedIndex,
                        onDaySelected = { viewModel.selectDay(dayMapping[it]) }
                    )
                }

                if (sortedHabits.isNotEmpty()) {
                    itemsIndexed(sortedHabits, key = { _, habit -> habit.id }) { index, habit ->
                        val isDone = habit.id in completedIdsForDay
                        val isProcessing = habit.id in processingIds
                        
                        val todayIndex = dayMapping.indexOf(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK))
                        val dayState = when {
                            selectedIndex < todayIndex -> DayState.PAST
                            selectedIndex > todayIndex -> DayState.FUTURE
                            else -> DayState.TODAY
                        }

                        // Calculate the start and end of the selected day
                        val selectedDayStartMs = java.util.Calendar.getInstance().apply {
                            val currentDayOfWeek = get(java.util.Calendar.DAY_OF_WEEK)
                            val diff = dayMapping[selectedIndex] - currentDayOfWeek
                            add(java.util.Calendar.DAY_OF_YEAR, diff)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val selectedDayEndMs = selectedDayStartMs + 24 * 60 * 60 * 1000L - 1

                        // Visibility Logic based on duration
                        val isStarted = habit.startDate == null || habit.startDate <= selectedDayEndMs
                        val isFinished = habit.endDate != null && selectedDayStartMs > habit.endDate

                        // Hide if not started yet or if past its end date (unless it's today and we want to show it as finished)
                        if (!isStarted) return@itemsIndexed
                        
                        // Always show for today if it's finished, but hide for future days if finished
                        if (isFinished && dayState == DayState.FUTURE) return@itemsIndexed

                        HabitItem(
                            habit = habit,
                            isCompleted = isDone || isProcessing,
                            statusFinished = isFinished,
                            totalCompletions = habitStats[habit.id] ?: 0,
                            dayState = dayState,
                            onCompleteClick = { 
                                if (dayState == DayState.TODAY && !isDone && !isProcessing) {
                                    processingIds = processingIds + habit.id
                                    viewModel.completeHabit(habit) 
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Mission Accomplished! +50 XP 🚀")
                                    }
                                }
                            },
                            onDeleteClick = { 
                                viewModel.deleteHabit(context, habit) 
                                scope.launch {
                                    snackbarHostState.showSnackbar("Mission Terminated. 🗑️")
                                }
                            },
                            onEditClick = { onEditClick(habit) }
                        )
                    }
                } else {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No missions for this day!", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                Text("Consistency is key! ✨", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                item {
                    AdMobBanner()
                }
            }
        }

        if (showCelebrate) {
            StreakAchievementOverlay(
                streak = celebratoryStreak,
                isMilestone = isMilestone,
                onDismiss = { 
                    showCelebrate = false 
                    if (isMilestone) {
                        prefs.edit().putInt("lastMilestone_$celebratoryStreak", Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).apply()
                        viewModel.dismissMilestone()
                    } else {
                        prefs.edit().putString("lastCelebratedDate", today).apply()
                    }
                }
            )
        }
    }



@Composable
fun StreakAchievementOverlay(streak: Int, isMilestone: Boolean = false, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotate by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotate"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isMilestone) {
                Text(
                    text = "🏆 MILESTONE UNLOCKED 🏆",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD600),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(0.dp))
            }

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, rotationZ = rotate),
                contentAlignment = Alignment.Center
            ) {
                // Multi-layered Glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isMilestone) Color(0xFFFFD600) else Color(0xFFFF5722)).copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 1.2f
                    )
                }
                
                Icon(
                    imageVector = if (isMilestone) Icons.Default.EmojiEvents else Icons.Default.Whatshot,
                    contentDescription = null,
                    modifier = Modifier.size(130.dp),
                    tint = if (isMilestone) Color(0xFFFFD600) else Color(0xFFFF5722)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isMilestone) "INCREDIBLE $streak DAYS!" else "$streak DAY STREAK!",
                fontSize = if (isMilestone) 36.sp else 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )

            val subtitle = when {
                streak >= 100 -> "Legendary Status! You are a master of habit. 👑"
                streak >= 30 -> "Absolute Beast Mode! 30 days of pure fire. 🔥⚡"
                streak >= 7 -> "One week down! The habit is taking root. 🌱"
                else -> "You're on fire! Keep the momentum going! 🚀"
            }

            Text(
                text = subtitle,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (isMilestone) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = Color(0xFFFFD600).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600))
                ) {
                    Text(
                        "+${streak * 10} BONUS XP RECEIVED 🌟",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFFFFD600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMilestone) Color(0xFFFFD600) else Color(0xFFFF5722)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = if (isMilestone) "CLAIM REWARD 🏆" else "KEEP CLIMBING 🚀",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isMilestone) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun LevelBadge(level: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "LVL $level",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StreakBadge(streak: Int) {
    Surface(
        color = Color(0xFFFBE9E7),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "$streak",
                color = Color(0xFFD84315),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun XpProgressBar(xp: Long, level: Int) {
    val currentLevelStart = if (level <= 1) 0L else com.example.fewstep.data.model.User.xpToNextLevel(level - 1)
    val nextLevelTarget = com.example.fewstep.data.model.User.xpToNextLevel(level)
    val progress = (xp - currentLevelStart).toFloat() / (nextLevelTarget - currentLevelStart).toFloat()
    
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                "Power Progress", 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Black, 
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Text(
                "${xp - currentLevelStart} / ${nextLevelTarget - currentLevelStart} XP", 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun WeeklyRecapBar(recap: List<com.example.fewstep.ui.viewmodel.DaySummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            recap.forEach { summary ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = summary.dayName,
                        fontSize = 10.sp,
                        color = if (summary.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (summary.isToday) FontWeight.Black else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when {
                                    summary.completedCount > 0 && summary.completedCount >= summary.totalCount -> Color(0xFF10B981)
                                    summary.completedCount > 0 -> Color(0xFF10B981).copy(alpha = 0.6f)
                                    summary.isToday -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (summary.completedCount > 0) {
                            Text(
                                "✔", 
                                color = Color.White, 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        } else if (summary.isToday) {
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun HabitItem(
    habit: com.example.fewstep.data.model.Habit,
    isCompleted: Boolean,
    statusFinished: Boolean = false,
    totalCompletions: Int,
    dayState: DayState,
    onCompleteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isCompleted) 0.9f else 1f
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCompleted) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, "Done", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isCompleted) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = habit.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${habit.reminderTime}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🏆 $totalCompletions lifetime wins",
                    fontSize = 11.sp,
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            val buttonLabel = when {
                isCompleted -> "COMPLETED"
                statusFinished -> "FINISHED"
                dayState == DayState.PAST -> "MISSED"
                dayState == DayState.FUTURE -> "UPCOMING"
                else -> "DONE"
            }

            Button(
                onClick = onCompleteClick,
                enabled = !isCompleted && !statusFinished && dayState == DayState.TODAY,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isCompleted -> Color(0xFFD1FAE5)
                        statusFinished -> MaterialTheme.colorScheme.surfaceVariant
                        dayState == DayState.PAST -> Color(0xFFFEE2E2)
                        dayState == DayState.FUTURE -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color(0xFF10B981)
                    },
                    contentColor = when {
                        isCompleted -> Color(0xFF059669)
                        statusFinished -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        dayState == DayState.PAST -> Color(0xFFEF4444)
                        dayState == DayState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color.White
                    },
                    disabledContainerColor = when {
                        isCompleted -> Color(0xFFD1FAE5)
                        statusFinished -> MaterialTheme.colorScheme.surfaceVariant
                        dayState == DayState.PAST -> Color(0xFFFEE2E2)
                        dayState == DayState.FUTURE -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color(0xFFD1FAE5)
                    },
                    disabledContentColor = when {
                        isCompleted -> Color(0xFF059669)
                        statusFinished -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        dayState == DayState.PAST -> Color(0xFFEF4444)
                        dayState == DayState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color(0xFF059669)
                    }
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isCompleted || dayState != DayState.TODAY) 0.dp else 6.dp)
            ) {
                Text(
                    text = buttonLabel, 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.LightGray
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun WeeklyTabRow(days: List<String>, selectedIndex: Int, onDaySelected: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = MaterialTheme.colorScheme.primary,
                height = 3.dp
            )
        }
    ) {
        days.forEachIndexed { index, day ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onDaySelected(index) },
                text = {
                    Text(
                        text = day,
                        fontSize = 13.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
fun HeaderBadgeCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    bgColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(bgColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
