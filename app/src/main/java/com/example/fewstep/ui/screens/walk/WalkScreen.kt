package com.example.fewstep.ui.screens.walk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.Icons.AutoMirrored.Filled.DirectionsWalk
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WalkScreen(
    viewModel: WalkViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentSteps by viewModel.steps.collectAsState()
    val goal by viewModel.stepGoal.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACTIVITY_RECOGNITION
    } else {
        ""
    }

    var hasPermission by remember {
        mutableStateOf(
            if (permission.isEmpty()) true 
            else ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && permission.isNotEmpty()) {
            launcher.launch(permission)
        }
    }

    val isDark = !MaterialTheme.colorScheme.surface.let { color ->
        (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) > 0.5
    }

    val bgBrush = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    "Daily Walk", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bgBrush)
        ) {
            if (!hasPermission && permission.isNotEmpty()) {
                PermissionRequiredView { launcher.launch(permission) }
            } else {
                WalkContent(
                    currentSteps, 
                    goal, 
                    viewModel, 
                    onEditGoal = { showGoalDialog = true },
                    onShowHistory = { showHistoryDialog = true }
                )
            }

            if (showHistoryDialog) {
                val history by viewModel.walkHistory.collectAsState()
                val weeklyTotal by viewModel.weeklyTotal.collectAsState()
                WalkHistoryDialog(
                    history = history,
                    goal = goal,
                    weeklyTotal = weeklyTotal,
                    onDismiss = { showHistoryDialog = false }
                )
            }

            if (showGoalDialog) {
                GoalPickerDialog(
                    currentGoal = goal,
                    onDismiss = { showGoalDialog = false },
                    onConfirm = { newGoal ->
                        viewModel.updateGoal(newGoal)
                        showGoalDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun WalkContent(steps: Int, goal: Int, viewModel: WalkViewModel, onEditGoal: () -> Unit, onShowHistory: () -> Unit) {
    val progress = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "steps_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Manage your activity and reach your goals",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onShowHistory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Walk History", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Button(
                onClick = onEditGoal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Set Goal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // XP Claim Section
        val claimedMilestones by viewModel.claimedMilestones.collectAsState()
        val currentMilestone = (steps / 1000) * 1000
        val unclaimedMilestones = (1000..currentMilestone step 1000).filter { it !in claimedMilestones }
        val pendingXp = unclaimedMilestones.size * 10
        val isClaimable = pendingXp > 0

        androidx.compose.animation.AnimatedVisibility(
            visible = isClaimable,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
        ) {
            Button(
                onClick = { viewModel.claimXp(currentMilestone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Claim $pendingXp XP Bonus!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (currentMilestone > 0 && claimedMilestones.contains(currentMilestone)) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${(currentMilestone / 1000) * 10} XP Reward Collected Today",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Premium Progress Ring
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val gradientColors = listOf(
            primaryColor.copy(alpha = 0.6f),
            primaryColor,
            secondaryColor
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawArc(
                    color = trackColor,
                    startAngle = -225f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Active Progress
                drawArc(
                    brush = Brush.sweepGradient(colors = gradientColors),
                    startAngle = -225f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = String.format("%, d", steps),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Goal: $goal",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WalkStatCard(
                modifier = Modifier.weight(1f),
                label = "Distance",
                value = String.format("%.2f km", viewModel.getDistanceKm(steps)),
                icon = Icons.Default.Route,
                containerColor = MaterialTheme.colorScheme.surface
            )
            WalkStatCard(
                modifier = Modifier.weight(1f),
                label = "Calories",
                value = "${viewModel.getCaloriesBurned(steps)} kcal",
                icon = Icons.Default.Whatshot,
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        Spacer(Modifier.height(16.dp))
        
        // Motivational Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (progress >= 1f) "Goal Reached!" else "Keep Moving!",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (progress >= 1f) "You've smashed your daily target. Incredible job!" else "Only ${goal - steps} steps left to reach your goal.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
            }
        }
    }
}




@Composable
fun GoalPickerDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val goals = listOf(2000, 5000, 8000, 10000, 12000, 15000, 20000)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Step Goal", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                goals.forEach { goal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(goal) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format("%, d steps", goal),
                            fontSize = 16.sp,
                            fontWeight = if (goal == currentGoal) FontWeight.Black else FontWeight.Medium,
                            color = if (goal == currentGoal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (goal == currentGoal) {
                            Icon(
                                Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (goal != goals.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun WalkWeeklyBarChart(
    history: List<Pair<String, Int>>,
    currentSteps: Int,
    goal: Int,
    modifier: Modifier = Modifier,
    showValues: Boolean = false
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    
    // Ensure the last item (today) is updated live
    val currentHistory = remember(history, currentSteps) {
        history.toMutableList().apply {
            if (isNotEmpty()) {
                this[size - 1] = "TODAY" to currentSteps
            }
        }
    }

    val maxSteps = (currentHistory.maxOfOrNull { it.second } ?: goal).coerceAtLeast(goal).toFloat()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weekly Activity",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "Avg: ${String.format("%, d", currentHistory.sumOf { it.second } / 7)}",
                    fontSize = 11.sp,
                    color = primaryColor
                )
            }
            
            Spacer(Modifier.height(if (showValues) 24.dp else 12.dp))
            
            Canvas(modifier = Modifier.fillMaxSize().weight(1f)) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 20.dp.toPx() // Room for labels
                val barWidth = 12.dp.toPx()
                val spacing = (canvasWidth - (currentHistory.size * barWidth)) / (currentHistory.size + 1)
                
                // Draw Goal Line
                val goalY = canvasHeight - (goal.toFloat() / maxSteps * canvasHeight)
                if (goalY in 0f..canvasHeight) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.2f),
                        start = androidx.compose.ui.geometry.Offset(0f, goalY),
                        end = androidx.compose.ui.geometry.Offset(canvasWidth, goalY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                currentHistory.forEachIndexed { index, (day, steps) ->
                    val x = spacing + (index * (barWidth + spacing))
                    val barHeight = (steps.toFloat() / maxSteps * canvasHeight).coerceAtLeast(4.dp.toPx())
                    val top = canvasHeight - barHeight
                    
                    // Bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (day == "TODAY") listOf(primaryColor, secondaryColor) 
                                    else listOf(surfaceVariant, surfaceVariant.copy(alpha = 0.7f))
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Values & Day Labels Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                currentHistory.forEach { (day, steps) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (showValues) {
                            val displaySteps = if (steps >= 1000) String.format("%.1fk", steps / 1000f) else steps.toString()
                            Text(
                                text = displaySteps,
                                fontSize = 8.sp,
                                color = if (day == "TODAY") primaryColor else onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (day == "TODAY") "T" else day.take(1),
                            fontSize = 10.sp,
                            fontWeight = if (day == "TODAY") FontWeight.Bold else FontWeight.Normal,
                            color = if (day == "TODAY") primaryColor else onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.width(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalkHistoryDialog(
    history: List<Pair<String, Int>>,
    goal: Int,
    weeklyTotal: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("7-Day Progress", fontWeight = FontWeight.Black) 
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Embedded Professional Graph with values
                WalkWeeklyBarChart(
                    history = history,
                    currentSteps = history.lastOrNull()?.second ?: 0,
                    goal = goal,
                    showValues = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )

                // Weekly Total from Room DB
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Weekly Total",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = String.format("%,d steps", weeklyTotal),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = String.format("%.1f km", (weeklyTotal * 0.762) / 1000.0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Keep going! Every step is stored and counted. 🚀",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun WalkStatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PermissionRequiredView(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🚶", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Steps Access Needed",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "To count your steps automatically, we need permission to access your phone's activity sensor.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}
