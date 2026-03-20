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
        // If it takes more than 5 seconds and still empty, might be truly empty
        kotlinx.coroutines.delay(5000)
        if (topUsers.isEmpty()) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)

                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Global Champions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching Champions...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (topUsers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No champions found yet.", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Be the first to reach the top!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top 3 Podium
                    item {
                        LeaderboardPodium(topUsers.take(3))
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // REST OF TOP 100
                    if (topUsers.size > 3) {
                        itemsIndexed(topUsers.drop(3)) { index, user ->
                            LeaderboardItem(index + 4, user)
                        }
                    }
                    
                    item {
                        AdMobBanner()
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardPodium(top3: List<User>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // Silver (2nd)
        if (top3.size > 1) {
            PodiumItem(top3[1], 2, Color(0xFFC0C0C0), 140.dp)
        }
        
        // Gold (1st)
        if (top3.isNotEmpty()) {
            PodiumItem(top3[0], 1, Color(0xFFFFD700), 180.dp)
        }

        // Bronze (3rd) -> Diamond as requested
        if (top3.size > 2) {
            // User requested "Diamond Gold Silver" structure. Gold is center.
            PodiumItem(top3[2], 3, Color(0xFFB9F2FF), 120.dp)
        }
    }
}

@Composable
fun PodiumItem(user: User, rank: Int, color: Color, height: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, color)
            ) {
                Icon(
                    Icons.Default.Star, 
                    null, 
                    tint = color, 
                    modifier = Modifier.padding(12.dp)
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                shape = CircleShape,
                color = color
            ) {
                Text(
                    "#$rank", 
                    color = Color.White, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            user.name.take(10), 
            fontWeight = FontWeight.Black, 
            fontSize = 14.sp, 
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "${user.xp} XP", 
            fontSize = 12.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height / 2)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color, 
                            color.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        )
    }
}

@Composable
fun LeaderboardItem(rank: Int, user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                rank.toString(), 
                modifier = Modifier.width(32.dp), 
                fontWeight = FontWeight.Black, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF5722), modifier = Modifier.size(12.dp))
                    Text("${user.currentStreak} day streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${user.xp} XP", fontWeight = FontWeight.Black, color = Color(0xFFFFA000), fontSize = 14.sp)
                Text("LVL ${user.level}", fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}
