package com.example.fewstep.ui.screens.home

import android.content.Context

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
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
import com.example.fewstep.ui.viewmodel.NotificationsViewModel
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.fewstep.ui.components.StartIoBanner
import com.example.fewstep.ui.components.StreakAchievementOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

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
    onAiCoachClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onStreakClick: () -> Unit = {},
    onLevelClick: () -> Unit = {},
    onStoreClick: () -> Unit = {}
) {
    val user by viewModel.userData.collectAsState()
    val notificationViewModel: NotificationsViewModel = viewModel()
    val unreadNotifications by notificationViewModel.unreadCount.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val historyRecap by viewModel.historyRecap.collectAsState()
    val habitStats by viewModel.habitStats.collectAsState()
    val sortedHabits by viewModel.sortedHabits.collectAsState()
    val selectedDayStartMs by viewModel.selectedDayStartMs.collectAsState()
    val selectedDayEndMs by viewModel.selectedDayEndMs.collectAsState()
    val dayState by viewModel.selectedDayState.collectAsState()
    
    val selectedDay by viewModel.selectedDayOfWeek.collectAsState()
    val completedIdsForDay by viewModel.completedHabitIdsForSelectedDay.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var processingIds by remember { mutableStateOf(setOf<String>()) }
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var showCelebrate by remember { mutableStateOf(false) }
    var celebratoryStreak by remember { mutableStateOf(0) }
    var isMilestone by remember { mutableStateOf(false) }

    val newMilestone by viewModel.newMilestone.collectAsState()

    // Persistent celebration flag - renamed to avoid any hidden conflicts
    val homePrefs = remember { context.getSharedPreferences("FewStepPrefs", Context.MODE_PRIVATE) }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Clear processing IDs once they are confirmed by the server
    LaunchedEffect(completedIdsForDay) {
        processingIds = processingIds.filter { it !in completedIdsForDay }.toSet()
    }
    
    // Unified trigger for both Vibration and Visual Pop-up once per day!
    // This now strictly listens to the deterministic database transaction event stream
    LaunchedEffect(Unit) {
        viewModel.streakIncreasedEvent.collect { newStreak ->
            // FIRE 2x VIBRATION
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(150)
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

            // SHOW VISUAL POPUP
            celebratoryStreak = newStreak
            isMilestone = listOf(7, 15, 30, 50, 100).contains(newStreak)
            showCelebrate = true
        }
    }


    // Explicit Milestone trigger from ViewModel
    LaunchedEffect(newMilestone) {
        newMilestone?.let { milestone ->
            val lastCelebratedMilestone = homePrefs.getInt("lastMilestone_$milestone", 0)
            if (lastCelebratedMilestone != java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)) {
                
                // FIRE 2x VIBRATION for milestone too!
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(150)
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

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
    

    val isComebackMode by viewModel.isComebackMode.collectAsState()

    var showMagicWandDialog by remember { mutableStateOf(false) }
    var magicGoalText by remember { mutableStateOf("") }
    var magicPreferenceText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Share Intent Helper
    fun shareApp(context: Context) {
        val shareText = """
            Hey! 🚀 I'm using FewStep to master my habits and stay focused. 
            It has an AI Coach, step tracking, focus timers, and a rank system! 
            
            Join me and start your journey here: 
            https://21ambuj.github.io/FewStep-/
        """.trimIndent()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share FewStep via"))
    }


    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Row 1: FEWSTEP branding (left) | Cals · Store · Notifications (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // FEWSTEP Branding
                    Text(
                        text = "FewStep",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Right side: Cals badge + Store + Notifications
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Calendar icon button
                        IconButton(onClick = onProgressClick) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(contentAlignment = Alignment.TopEnd) {
                            Row {
                                IconButton(onClick = onStoreClick) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = "Store",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { shareApp(context) }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = onNotificationsClick) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(10.dp)
                                        .background(Color.Red, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Greeting below branding
                Text(
                    text = "Hello, ${user?.name ?: userName}! 👋",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 3: Streak · Level · (Cals removed — now in top row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeaderBadgeCard(
                        modifier = Modifier.weight(1f),
                        label = "Streak",
                        value = "${user?.currentStreak ?: 0}d",
                        icon = Icons.Default.Whatshot,
                        iconColor = Color(0xFFFF5722),
                        bgColor = Color(0xFFFFE0B2),
                        onClick = onStreakClick
                    )
                    HeaderBadgeCard(
                        modifier = Modifier.weight(1f),
                        label = "Level",
                        value = "${user?.level ?: 1}",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFFA000),
                        bgColor = Color(0xFFFFF9C4),
                        onClick = onLevelClick
                    )
                    HeaderBadgeCard(
                        modifier = Modifier.weight(1f),
                        label = "XP",
                        value = "${user?.xp ?: 0}",
                        icon = Icons.Default.EmojiEvents,
                        iconColor = Color(0xFF43A047),
                        bgColor = Color(0xFFE8F5E9),
                        onClick = onLevelClick
                    )
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
            if (dayState != DayState.FUTURE) {
                FloatingActionButton(
                    onClick = onAddHabitClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", modifier = Modifier.size(28.dp))
                }
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
            // UNIFIED CAROUSEL (Information Header)
            item(key = "info_carousel") {
                Column {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 4 })
                    
                    // Auto-scroll logic
                    LaunchedEffect(pagerState) {
                        while (true) {
                            kotlinx.coroutines.delay(5000)
                            val next = (pagerState.currentPage + 1) % 4
                            pagerState.animateScrollToPage(next)
                        }
                    }

                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        pageSpacing = 16.dp,
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        Card(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                            Box(modifier = Modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                                when (page) {
                                    0 -> {
                                        // PAGE 1: Weekly Overview
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            historyRecap.forEach { summary ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = summary.dayName, 
                                                        fontSize = 11.sp, 
                                                        color = if (summary.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
                                                        fontWeight = if (summary.isToday) FontWeight.Black else FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(
                                                                color = when { 
                                                                        summary.completedCount > 0 && summary.completedCount >= summary.totalCount -> Color(0xFF10B981) 
                                                                        summary.completedCount > 0 -> Color(0xFF10B981).copy(alpha = 0.8f) 
                                                                        user?.frozenDates?.contains(summary.dateStr) == true -> Color(0xFF64B5F6) // Snowflake Blue
                                                                        summary.totalCount == 0 && !summary.isToday -> Color.Transparent
                                                                        summary.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) 
                                                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) 
                                                                    }, 
                                                                    shape = CircleShape
                                                                ), 
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (summary.completedCount > 0) {
                                                                Text("✔", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                            } else if (user?.frozenDates?.contains(summary.dateStr) == true) {
                                                                Text("❄", color = Color.White, fontSize = 16.sp)
                                                            } else if (summary.totalCount == 0 && !summary.isToday) {
                                                                Text("-", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                            } else if (summary.isToday) {
                                                                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    1 -> {
                                        // PAGE 2: Share App
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { shareApp(context) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Groups, 
                                                contentDescription = null, 
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer, 
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text("Share the Love! 💖🚀", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text("Help your friends grow with FewStep and build a tribe together.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                            }
                                        }
                                    }
                                    2 -> {
                                        // PAGE 3: Official Feedback Form
                                        val feedbackUrl = "https://forms.gle/kWcUkF8oGZE2nJQz9"
                                        val ctx = LocalContext.current
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(feedbackUrl))
                                                ctx.startActivity(intent)
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text("Your Voice Matters! 🥺🙏", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text("Help us grow by sharing your honest feedback and ideas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
                                            }
                                        }
                                    }
                                    3 -> {
                                        // PAGE 4: Download Update
                                        val downloadUrl = "https://21ambuj.github.io/FewStep-/"
                                        val ctx = LocalContext.current
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                                                ctx.startActivity(intent)
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download, 
                                                contentDescription = null, 
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer, 
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text("Get the Magic! ✨📲", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text("Ensure you're using the latest version for the best experience.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Pager Indicators
                    Row(Modifier.wrapContentHeight().fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(4) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(6.dp))
                        }
                    }
                }
            }

            // STATIC SECTION: Header and Day Selector
            item(key = "day_selector") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val currentDayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                    val statusText = if (selectedDay == currentDayOfWeek) "Today's Missions" else "${daysOfWeek[selectedIndex]}'s Missions"

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (dayState != DayState.FUTURE && sortedHabits.isNotEmpty()) {
                            androidx.compose.material3.ElevatedButton(
                                onClick = { showMagicWandDialog = true },
                                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("AI Generator", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    WeeklyTabRow(
                        days = daysOfWeek,
                        selectedIndex = selectedIndex,
                        onDaySelected = { viewModel.selectDay(dayMapping[it]) }
                    )
                }
            }

            // COMEBACK MODE BANNER
            if (isComebackMode && selectedDay == java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
                item(key = "comeback_banner") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("COMEBACK MISSION UNLOCKED!", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFE65100))
                                Text("Complete any task today for 2x XP bonus!", fontSize = 10.sp, color = Color(0xFFE65100).copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            if (sortedHabits.isNotEmpty()) {
                itemsIndexed(sortedHabits, key = { _, habit -> habit.id }) { index, habit ->
                    val isDone = habit.id in completedIdsForDay
                    val isProcessing = habit.id in processingIds
                    val isStarted = habit.startDate == null || habit.startDate <= selectedDayEndMs
                    val isFinished = habit.endDate != null && selectedDayStartMs > habit.endDate

                    // Hide if not started yet or if past its end date
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
                                    val msg = if (isComebackMode) "COMEBACK COMPLETE! +100 XP 🔥" else "Mission Accomplished! +50 XP 🚀"
                                    snackbarHostState.showSnackbar(msg)
                                }
                                // Show Full Screen Interstitial Ad
                                com.startapp.sdk.adsbase.StartAppAd.showAd(context)
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
                item(key = "empty_state") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No missions for this day!", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Consistency is key! ✨", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(32.dp))
                            
                            if (dayState != DayState.FUTURE) {
                                androidx.compose.material3.Button(
                                    onClick = onAddHabitClick,
                                    modifier = Modifier.fillMaxWidth(0.85f).height(54.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Custom Habit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                androidx.compose.material3.Button(
                                    onClick = { showMagicWandDialog = true },
                                    modifier = Modifier.fillMaxWidth(0.85f).height(54.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("✨", fontSize = 20.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("AI Habit Generator", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "footer_ad") {
                StartIoBanner()
            }
        }
        
        if (showCelebrate) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { 
                    showCelebrate = false 
                    com.startapp.sdk.adsbase.StartAppAd.showAd(context)
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                StreakAchievementOverlay(
                    streak = celebratoryStreak,
                    isMilestone = isMilestone,
                    onDismiss = { 
                        showCelebrate = false 
                        // Save to prefs to prevent duplicate popups on same day if needed
                        if (isMilestone) {
                            homePrefs.edit().putInt("lastMilestone_$celebratoryStreak", java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)).apply()
                            viewModel.dismissMilestone()
                        } else {
                            homePrefs.edit().putString("lastCelebratedDate", todayDateStr).apply()
                        }
                        com.startapp.sdk.adsbase.StartAppAd.showAd(context)
                    }
                )
            }
        }
        
        if (showMagicWandDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { 
                    if (!isGenerating) showMagicWandDialog = false 
                },
                title = { Text("✨ AI Habit Generator", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("What is your ultimate goal?", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = magicGoalText,
                            onValueChange = { magicGoalText = it },
                            placeholder = { Text("e.g. Run a 5K, Learn Spanish...") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGenerating
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Any specific time or preference?", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = magicPreferenceText,
                            onValueChange = { magicPreferenceText = it },
                            placeholder = { Text("e.g. Morning only, 7am daily...") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGenerating
                        )
                        if (isGenerating) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Forging your path...", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            if (magicGoalText.isNotBlank()) {
                                isGenerating = true
                                viewModel.generateHabitsFromAi(
                                    goal = magicGoalText,
                                    preferences = magicPreferenceText,
                                    onSuccess = {
                                        isGenerating = false
                                        showMagicWandDialog = false
                                        magicGoalText = ""
                                        magicPreferenceText = ""
                                        scope.launch { snackbarHostState.showSnackbar("✨ Magic habits added!") }
                                        com.startapp.sdk.adsbase.StartAppAd.showAd(context)
                                    },
                                    onError = { error ->
                                        isGenerating = false
                                        scope.launch { snackbarHostState.showSnackbar(error) }
                                    }
                                )
                            }
                        },
                        enabled = magicGoalText.isNotBlank() && !isGenerating
                    ) {
                        Text("Generate")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showMagicWandDialog = false }, enabled = !isGenerating) {
                        Text("Cancel")
                    }
                }
            )
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
                alpha = if (isCompleted) 0.8f else 1f
                scaleX = if (isCompleted) 0.98f else 1f
                scaleY = if (isCompleted) 0.98f else 1f
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = habit.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
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
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                height = 3.dp,
                color = MaterialTheme.colorScheme.primary
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
                        fontSize = 12.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.Black else FontWeight.Bold,
                        color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
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
