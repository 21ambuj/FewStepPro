package com.example.fewstep.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

// Palette for pie chart slices
private val categoryColors = listOf(
    Color(0xFF3D5AFE), Color(0xFFFF7043), Color(0xFF43A047),
    Color(0xFFFFD600), Color(0xFFE91E63), Color(0xFF00BCD4)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: HomeViewModel, onBackClick: () -> Unit) {
    val allHabitsRaw by viewModel.allHabitsRaw.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val userData by viewModel.userData.collectAsState()

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())

    // Stats
    val totalHabits = allHabitsRaw.size
    val totalCompletions = allLogs.count { it.completed }
    val completedToday = allLogs.count { it.completed && it.date == today }

    // Category distribution
    val categoryMap = allHabitsRaw.groupBy { it.category }
    val categoryEntries = categoryMap.map { (cat, habits) ->
        val completedCount = allLogs.count { log ->
            log.completed && habits.any { it.id == log.habitId }
        }
        Triple(cat, habits.size, completedCount)
    }

    // Last 7 days completion rate
    val last7Days = (0..6).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val dateStr = sdf.format(cal.time)
        val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
        val completed = allLogs.count { it.completed && it.date == dateStr }
        Pair(dayName, completed)
    }.reversed()

    // Best streak habit
    val habitCompletionCounts = allLogs.filter { it.completed }
        .groupBy { it.habitId }
        .mapValues { it.value.size }
    val topHabitId = habitCompletionCounts.maxByOrNull { it.value }?.key
    val topHabit = allHabitsRaw.find { it.id == topHabitId }

    // Pie chart animated sweep
    val animatedSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "pie"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total\nHabits",
                        value = totalHabits.toString(),
                        icon = Icons.Default.BarChart,
                        color = Color(0xFF3D5AFE)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "All Time\nCompletions",
                        value = totalCompletions.toString(),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = Color(0xFF43A047)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Done\nToday",
                        value = completedToday.toString(),
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFF7043)
                    )
                }
            }

            // 7-Day Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("7-Day Activity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                        }
                        Spacer(Modifier.height(16.dp))
                        val maxVal = (last7Days.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7Days.forEach { (day, count) ->
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
                                        color = if (count > 0) Color(0xFF1A237E) else Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp)
                                            .height((animHeight * 80f).dp.coerceAtLeast(4.dp))
                                            .background(
                                                if (count > 0) Color(0xFF3D5AFE) else Color(0xFFE8EAF6),
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(day, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Category Pie Chart
            if (categoryEntries.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Habit Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
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
                                    val totalHabitsForPie = categoryEntries.sumOf { it.second }.coerceAtLeast(1)
                                    Canvas(modifier = Modifier.size(140.dp)) {
                                        var startAngle = -90f
                                        categoryEntries.forEachIndexed { i, (_, count, _) ->
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
                                        Text(totalHabits.toString(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF1A237E))
                                        Text("habits", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(Modifier.width(20.dp))

                                // Legend
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categoryEntries.forEachIndexed { i, (cat, count, completed) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(categoryColors[i % categoryColors.size], CircleShape)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text("$count habits · $completed done", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Performer Habit
            topHabit?.let { habit ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("🏆 Top Habit", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text(habit.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "${habitCompletionCounts[habit.id] ?: 0} total completions",
                                    color = Color(0xFFFFD600),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Behavioral Insights
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("💡 Behavioral Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                        Spacer(Modifier.height(12.dp))

                        val currentStreakValue = userData?.currentStreak ?: 0
                        val insights = buildInsights(totalCompletions, currentStreakValue, completedToday, totalHabits)
                        insights.forEach { insight ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(insight.emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(insight.text, fontSize = 13.sp, color = Color(0xFF455A64), lineHeight = 18.sp)
                            }
                            if (insight != insights.last()) HorizontalDivider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            }

            // XP & Level Summary
            userData?.let { user ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4FF)),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("⚡ XP & Level", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                XpStatItem("Level", "${user.level}", Color(0xFF3D5AFE))
                                XpStatItem("XP Earned", "${user.xp}", Color(0xFFFF7043))
                                XpStatItem("Streak", "${userData?.currentStreak ?: 0}d 🔥", Color(0xFF43A047))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Insight(val emoji: String, val text: String)

fun buildInsights(totalCompletions: Int, streak: Int, todayDone: Int, totalHabits: Int): List<Insight> {
    val list = mutableListOf<Insight>()
    if (streak >= 7) list.add(Insight("🔥", "You're on a $streak-day streak! You're in the top tier of consistent habit builders."))
    else if (streak >= 3) list.add(Insight("🌱", "You've kept going for $streak days. Momentum is building — don't stop now!"))
    else list.add(Insight("💪", "Start a streak today! Consistency is the key to lasting change."))

    when {
        totalCompletions > 100 -> list.add(Insight("🏅", "Over 100 completions! You've built a powerful routine."))
        totalCompletions > 30 -> list.add(Insight("📈", "You've completed habits $totalCompletions times. You're building real momentum!"))
        else -> list.add(Insight("🚀", "Keep completing habits daily — after 21 days, they become automatic!"))
    }

    if (todayDone == 0 && totalHabits > 0)
        list.add(Insight("⏰", "You haven't completed any habits today yet. Start with the smallest one!"))
    else if (todayDone > 0 && todayDone == totalHabits)
        list.add(Insight("🎯", "Perfect day! All habits completed. You're unstoppable!"))
    else if (todayDone > 0)
        list.add(Insight("✅", "Great progress today — keep going to finish all your habits!"))

    return list
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF1A237E), textAlign = TextAlign.Center)
            Text(title, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

@Composable
fun XpStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}
