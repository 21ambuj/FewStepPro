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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.data.model.User
import com.example.fewstep.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

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

    // Reorder: Pending first, Completed last
    val sortedHabits = habits.sortedBy { it.id in completedIdsForDay }

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
    

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hello, ${user?.name ?: userName}!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                user?.let {
                                    StreakBadge(it.currentStreak)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LevelBadge(it.level)
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onProgressClick) {
                            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = Color(0xFF1A237E))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = onAiCoachClick,
                        containerColor = Color(0xFFE8EAF6),
                        contentColor = Color(0xFF1A237E),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.padding(bottom = 12.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach", modifier = Modifier.size(24.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = onAddHabitClick,
                        containerColor = Color(0xFF1A237E),
                        contentColor = Color.White,
                        shape = CircleShape, // Changed to Circle for classic FAB look
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp) // Extra padding to avoid bottom bar overlap
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
                    .background(Color(0xFFF5F7FA))
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
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E),
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

                        // Calculate the start-of-day timestamp for the selected day this week
                        val selectedDayStartMs = java.util.Calendar.getInstance().apply {
                            // Find the current week's date for the selectedIndex
                            val currentDayOfWeek = get(java.util.Calendar.DAY_OF_WEEK)
                            val diff = dayMapping[selectedIndex] - currentDayOfWeek
                            add(java.util.Calendar.DAY_OF_YEAR, diff)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        // Hide habit if it didn't exist on the selected day
                        val habitExistedOnDay = habit.createdAt <= selectedDayStartMs + 24 * 60 * 60 * 1000L - 1
                        if (!habitExistedOnDay && dayState == DayState.PAST) return@itemsIndexed

                        HabitItem(
                            habit = habit,
                            isCompleted = isDone || isProcessing,
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
                            onDeleteClick = { viewModel.deleteHabit(context, habit) },
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
                Spacer(modifier = Modifier.height(16.dp))
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
        color = Color(0xFF1A237E),
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
                color = Color.White,
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Power Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
            Text("${xp - currentLevelStart} / ${nextLevelTarget - currentLevelStart} XP", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).graphicsLayer(clip = true, shape = RoundedCornerShape(5.dp)),
            color = Color(0xFF1A237E),
            trackColor = Color(0xFFE8EAF6)
        )
    }
}

@Composable
fun WeeklyRecapBar(recap: List<com.example.fewstep.ui.viewmodel.DaySummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = if (summary.isToday) Color(0xFF1A237E) else Color.Gray,
                        fontWeight = if (summary.isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when {
                                    summary.completedCount > 0 && summary.completedCount >= summary.totalCount -> Color(0xFF43A047)
                                    summary.completedCount > 0 -> Color(0xFF81C784)
                                    summary.isToday -> Color(0xFFE8EAF6)
                                    else -> Color(0xFFF5F5F5)
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
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF1A237E), CircleShape))
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
                alpha = if (isCompleted) 0.8f else 1f
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFF1A237E)
                    )
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, "Done", tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isCompleted) Color(0xFFC8E6C9) else Color(0xFFFBE9E7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = habit.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFD84315)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${habit.reminderTime}",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
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
                dayState == DayState.PAST -> "MISSED"
                dayState == DayState.FUTURE -> "UPCOMING"
                else -> "DONE"
            }

            Button(
                onClick = onCompleteClick,
                enabled = !isCompleted && dayState == DayState.TODAY,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isCompleted -> Color(0xFFE8F5E9)
                        dayState == DayState.PAST -> Color(0xFFFFEBEE)
                        dayState == DayState.FUTURE -> Color(0xFFF5F5F5)
                        else -> Color(0xFF43A047)
                    },
                    contentColor = when {
                        isCompleted -> Color(0xFF43A047)
                        dayState == DayState.PAST -> Color(0xFFD32F2F)
                        dayState == DayState.FUTURE -> Color.Gray
                        else -> Color.White
                    },
                    disabledContainerColor = when {
                        isCompleted -> Color(0xFFE8F5E9)
                        dayState == DayState.PAST -> Color(0xFFFFEBEE)
                        dayState == DayState.FUTURE -> Color(0xFFF5F5F5)
                        else -> Color(0xFFE8F5E9)
                    },
                    disabledContentColor = when {
                        isCompleted -> Color(0xFF43A047)
                        dayState == DayState.PAST -> Color(0xFFD32F2F)
                        dayState == DayState.FUTURE -> Color.Gray
                        else -> Color(0xFF43A047)
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
                    modifier = Modifier.background(Color.White)
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
        contentColor = Color(0xFF1A237E),
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = Color(0xFF1A237E),
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
