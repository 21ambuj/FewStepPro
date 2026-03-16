package com.example.fewstep.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.data.model.User
import com.example.fewstep.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

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
    onEditClick: (com.example.fewstep.data.model.Habit) -> Unit

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

    // Reorder: Pending first, Completed last
    val sortedHabits = habits.sortedBy { it.id in completedIdsForDay }

    // Clear processing IDs once they are confirmed by the server
    LaunchedEffect(completedIdsForDay) {
        processingIds = processingIds.filter { it !in completedIdsForDay }.toSet()
    }
    
    val daysOfWeek = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    val dayMapping: List<Int> = listOf(
        java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY, 
        java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY, java.util.Calendar.SATURDAY
    )
    val selectedIndex: Int = dayMapping.indexOf(selectedDay)
    

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your Journey", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Hello, ${user?.name ?: userName}! 🚀", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                        }
                        user?.let {
                            LevelBadge(it.level)
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
                StreakSummaryCard(user?.currentStreak ?: 0)
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
                    
                    HabitItem(
                        habit = habit,
                        isCompleted = isDone || isProcessing,
                        totalCompletions = habitStats[habit.id] ?: 0,
                        onCompleteClick = { 
                            if (!isDone && !isProcessing) {
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
fun StreakSummaryCard(streak: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1A237E), Color(0xFF3949AB), Color(0xFF5C6BC0)),
                        start = Offset(bgOffset, 0f),
                        end = Offset(bgOffset + 600f, 600f)
                    )
                )
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔥 $streak DAY STREAK",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (streak > 0) "You're unstoppable! ⚡" else "Complete a task to ignite your fire!",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HabitItem(
    habit: com.example.fewstep.data.model.Habit,
    isCompleted: Boolean,
    totalCompletions: Int,
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
            
            Button(
                onClick = onCompleteClick,
                enabled = !isCompleted,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFF43A047),
                    contentColor = if (isCompleted) Color(0xFF43A047) else Color.White,
                    disabledContainerColor = Color(0xFFE8F5E9),
                    disabledContentColor = Color(0xFF43A047)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isCompleted) 0.dp else 6.dp)
            ) {
                Text(
                    text = if (isCompleted) "COMPLETED" else "DONE", 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 1.2.sp,
                    fontSize = 12.sp
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
