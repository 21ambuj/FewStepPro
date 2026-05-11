package com.example.fewstep.ui.screens.analytics

import com.example.fewstep.ui.components.StartIoBanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.material.icons.filled.Share


private val categoryColors = listOf(
    Color(0xFF3D5AFE), Color(0xFFFF7043), Color(0xFF43A047),
    Color(0xFFFFD600), Color(0xFFE91E63), Color(0xFF00BCD4)
)

@Composable
fun WeeklyRecapGraphic(userData: com.example.fewstep.data.model.User?, completions: Int, totalHabits: Int) {
    val isDark = !MaterialTheme.colorScheme.surface.let { c ->
        (0.299 * c.red + 0.587 * c.green + 0.114 * c.blue) > 0.5
    }
    val gradientColors = if (isDark)
        listOf(Color(0xFF1A1040), Color(0xFF0D2B45), Color(0xFF1A1040))
    else
        listOf(Color(0xFF6C4FD8), Color(0xFF3D8EF0), Color(0xFF6C4FD8))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(24.dp)
        ) {
            // Decorative blobs
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.07f), radius = 220f, center = Offset(size.width, 0f))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = 160f, center = Offset(0f, size.height))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "⚡ MY FEWSTEP WEEK ⚡",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WeeklyStatItem(label = "Day Streak", value = userData?.currentStreak?.toString() ?: "0", emoji = "🔥", color = Color(0xFFFFD600))
                    WeeklyStatItem(label = "Completions", value = completions.toString(), emoji = "✅", color = Color(0xFF69F0AE))
                    WeeklyStatItem(label = "Level", value = userData?.level?.toString() ?: "1", emoji = "🏆", color = Color(0xFF40C4FF))
                }
            }
        }
    }
}

@Composable
fun WeeklyStatItem(label: String, value: String, emoji: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 30.sp, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: HomeViewModel, onBackClick: () -> Unit) {
    val allHabitsRaw by viewModel.allHabitsRaw.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val userData by viewModel.userData.collectAsState()

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { sdf.format(Date()) }

    // Use the lazy-calculated analytics data from ViewModel
    val analyticsData by viewModel.analyticsData.collectAsState()

    var showTopHabitsDialog by remember { mutableStateOf(false) }
    var showInsightsDialog by remember { mutableStateOf(false) }

    // Pie chart animated sweep
    val animatedSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "pie"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Analytics & Insights", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        if (analyticsData == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (allHabitsRaw.isEmpty()) {
                    // Friendly Empty State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No data available yet.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Please add some habits to track your progress! 🚀",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        } else {
            val data = analyticsData!!
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var isSharing by remember { mutableStateOf(false) }
            val graphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                // === WEEKLY RECAP CARD ===
                item(key = "weekly_recap_share") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.drawWithContent {
                                graphicsLayer.record(androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            }
                        ) {
                            WeeklyRecapGraphic(userData, data.totalCompletions, data.totalHabits)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (isSharing) return@OutlinedButton
                                isSharing = true
                                coroutineScope.launch {
                                    try {
                                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                        com.example.fewstep.util.ShareUtils.shareImage(context, bitmap)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSharing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSharing) "Capturing..." else "Share Weekly Recap", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Stats Row
                item(key = "stats_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total\nHabits",
                            value = data.totalHabits.toString(),
                            icon = Icons.Default.BarChart,
                            color = Color(0xFF3D5AFE)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "All Time\nCompletions",
                            value = data.totalCompletions.toString(),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = Color(0xFF43A047)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Done\nToday",
                            value = data.completedToday.toString(),
    
                            icon = Icons.Default.EmojiEvents,
                            color = Color(0xFFFF7043)
                        )
                    }
                }
    
                // 7-Day Bar Chart
                item(key = "7day_chart") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("7-Day Activity", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(16.dp))
                            val maxVal = (data.last7Days.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                data.last7Days.forEach { (day, count) ->
                                    val heightFraction = count.toFloat() / maxVal.toFloat()
                                    val animHeight by animateFloatAsState(
                                        targetValue = heightFraction,
                                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                                        label = day
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            count.toString(),
                                            fontSize = 10.sp,
                                            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp)
                                                .height((animHeight * 80f).dp.coerceAtLeast(4.dp))
                                                .background(
                                                    if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                                )
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(day, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
    
                // Category Pie Chart
                if (data.categoryEntries.isNotEmpty()) {
                    item(key = "category_chart") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Habit Categories", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pie chart canvas
                                    Box(
                                        modifier = Modifier.size(140.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val totalHabitsForPie = data.categoryEntries.sumOf { it.second }.coerceAtLeast(1)
                                        Canvas(modifier = Modifier.size(140.dp)) {
                                            var startAngle = -90f
                                            data.categoryEntries.forEachIndexed { i, (_, count, _) ->
                                                val sweep = (count.toFloat() / totalHabitsForPie) * animatedSweep
                                                drawArc(
                                                    color = categoryColors[i % categoryColors.size],
                                                    startAngle = startAngle,
                                                    sweepAngle = sweep - 1f,
                                                    useCenter = false,
                                                    topLeft = Offset(16.dp.toPx(), 16.dp.toPx()),
                                                    size = Size(size.width - 32.dp.toPx(), size.height - 32.dp.toPx()),
                                                    style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                                startAngle += sweep
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(data.totalHabits.toString(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("habits", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
    
                                    Spacer(Modifier.width(20.dp))
    
                                    // Legend
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        data.categoryEntries.forEachIndexed { i, (cat, count, completed) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(categoryColors[i % categoryColors.size], CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("$count habits · $completed done", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
    
                    // --- Action Buttons ---
                    item(key = "action_buttons") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showTopHabitsDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Top Habits", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = { showInsightsDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Insights, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Coach Insights", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
    
                item(key = "footer_ad") {
                    StartIoBanner()
                }
            }
        }
    }

    if (showTopHabitsDialog && analyticsData != null) {
        TopHabitsDialog(
            habits = analyticsData!!.topHabits,
            counts = analyticsData!!.habitCompletionCounts,
            onDismiss = { showTopHabitsDialog = false }
        )
    }

    if (showInsightsDialog && analyticsData != null) {
        val currentStreakValue = userData?.currentStreak ?: 0
        val insights = buildInsights(analyticsData!!.totalCompletions, currentStreakValue, analyticsData!!.completedToday, analyticsData!!.totalHabits)
        InsightsDialog(
            insights = insights,
            onDismiss = { showInsightsDialog = false }
        )
    }
}


data class AnalyticsData(
    val totalHabits: Int,
    val totalCompletions: Int,
    val completedToday: Int,
    val categoryEntries: List<Triple<String, Int, Int>>,
    val last7Days: List<Pair<String, Int>>,
    val habitCompletionCounts: Map<String, Int>,
    val topHabits: List<com.example.fewstep.data.model.Habit>
)

data class Insight(val emoji: String, val text: String)

fun buildInsights(totalCompletions: Int, streak: Int, todayDone: Int, totalHabits: Int): List<Insight> {
    val list = mutableListOf<Insight>()
    
    // Streak-based Insights
    when {
        streak >= 30 -> list.add(Insight("👑", "Unstoppable! A $streak-day streak. You've officially mastered these habits and turned them into a lifestyle."))
        streak >= 21 -> list.add(Insight("🧠", "Habit Formed! You've crossed the 21-day mark. Your brain has rewired these actions to be automatic."))
        streak >= 14 -> list.add(Insight("🔥", "Solid Consistency! $streak days in a row. You're now more consistent than 90% of people."))
        streak >= 7 -> list.add(Insight("⚡", "Great Momentum! A full week of consistency. Keep pushing to reach the 21-day habit milestone."))
        streak >= 3 -> list.add(Insight("🌱", "Starting to Bloom! You've kept going for $streak days. Momentum is building!"))
        else -> list.add(Insight("💪", "Day $streak: Consistency is the key to lasting change. Start a streak today!"))
    }

    // Completion-based Insights
    when {
        totalCompletions > 500 -> list.add(Insight("💎", "Habit Legend! Over 500 total completions. Your dedication is truly inspiring."))
        totalCompletions > 100 -> list.add(Insight("🏅", "Century Club! Over 100 total completions. You've built a powerful routine."))
        totalCompletions > 30 -> list.add(Insight("📈", "Rising Star! You've completed habits $totalCompletions times. You're building real momentum!"))
        else -> {
            if (streak < 21) {
                val remaining = 21 - streak
                list.add(Insight("🚀", "The 21/90 Rule: Keep it up for $remaining more days to make this habit automatic!"))
            } else {
                list.add(Insight("🚀", "Keep going! You're building a foundation for a better you every single day."))
            }
        }
    }

    // Daily Progress Insights
    when {
        totalHabits == 0 -> {
            // No habits yet
        }
        todayDone == 0 -> {
            list.add(Insight("⏰", "Fresh Start: You haven't completed any habits today. Start with one small task now!"))
        }
        todayDone == totalHabits -> {
            list.add(Insight("🎯", "Perfect Score! All $totalHabits habits completed today. You're absolutely unstoppable!"))
        }
        else -> {
            list.add(Insight("✅", "Progress: $todayDone/$totalHabits done. You're more than halfway there, keep the momentum!"))
        }
    }

    return list
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
fun TopHabitsDialog(
    habits: List<com.example.fewstep.data.model.Habit>,
    counts: Map<String, Int>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Bold) }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆 Top 3 Habits", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (habits.isEmpty()) {
                    Text("No habits completed yet. Start your journey today! 🚀", textAlign = TextAlign.Center)
                } else {
                    habits.forEachIndexed { index, habit ->
                        val rankEmoji = when(index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> ""
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rankEmoji, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(habit.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${counts[habit.id] ?: 0} completions", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun InsightsDialog(
    insights: List<Insight>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Understood", fontWeight = FontWeight.Bold) }
        },
        title = {
            Text("💡 Behavioral Insights", fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                insights.forEach { insight ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(insight.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(insight.text, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
