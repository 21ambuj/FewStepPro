package com.example.fewstep.ui.screens.leaderboard

import com.example.fewstep.ui.components.AdMobBanner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onBackClick: () -> Unit
) {
    val topUsers by viewModel.topUsers.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(topUsers) {
        if (topUsers.isNotEmpty()) {
            isLoading = false
        }
        kotlinx.coroutines.delay(3000)
        if (topUsers.isEmpty()) {
            isLoading = false
        }
    }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")

                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Global Champions", 
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
        ) {
            if (isLoading) {
                LoadingState()
            } else if (topUsers.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // TOP SECTION: Podium
                    item {
                        PremiumPodium(topUsers.take(3))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // LIST HEADER
                    item {
                        Text(
                            "Top Rankings",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    // REMAINING USERS
                    if (topUsers.size > 3) {
                        itemsIndexed(topUsers.drop(3), key = { _, user -> user.uid }) { index, user ->
                            LeaderboardRow(index + 4, user)
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AdMobBanner()
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(strokeWidth = 4.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ranking the best...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No champions yet!", fontWeight = FontWeight.Black)
        Text("Be the first to claim the throne.", fontSize = 12.sp)
    }
}

@Composable
fun PremiumPodium(top3: List<User>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // SILVER (Rank 2)
            if (top3.size > 1) {
                PodiumMember(top3[1], 2, Color(0xFF9E9E9E), 130.dp, delay = 200)
            }
            
            // GOLD (Rank 1)
            if (top3.isNotEmpty()) {
                PodiumMember(top3[0], 1, Color(0xFFFFC107), 180.dp, delay = 0)
            }

            // BRONZE (Rank 3)
            if (top3.size > 2) {
                PodiumMember(top3[2], 3, Color(0xFFCD7F32), 100.dp, delay = 400)
            }
        }
    }
}

@Composable
fun PodiumMember(user: User, rank: Int, color: Color, podiumHeight: Dp, delay: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(if(rank==1) 80.dp else 64.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(3.dp, color)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            user.name.take(1).uppercase(),
                            fontSize = if(rank==1) 24.sp else 20.sp,
                            fontWeight = FontWeight.Black,
                            color = color
                        )
                    }
                }
                
                // Rank Badge
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                    shape = CircleShape,
                    color = color,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        rank.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(user.name.take(12), fontWeight = FontWeight.Black, fontSize = 13.sp)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF5722), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text("${user.currentStreak}", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            
            Text("${user.xp} XP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // The Podium Pillar
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(podiumHeight)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.2f))
                        )
                    )
            )
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, user: User) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rank.toString(),
                    modifier = Modifier.width(36.dp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF5722), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${user.currentStreak} day streak", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("${user.xp} XP", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Text("LVL ${user.level}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
