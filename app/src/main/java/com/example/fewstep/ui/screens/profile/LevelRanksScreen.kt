package com.example.fewstep.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RankTier(
    val title: String,
    val levelRange: String,
    val xpRequired: String,
    val color: Color,
    val isMilestone: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelRanksScreen(onBackClick: () -> Unit) {
    val rankTiers = listOf(
        RankTier("Beginner", "Lvl 1", "0 XP", Color(0xFF757575)), // Sharper Gray
        RankTier("Novice", "Lvl 2 - 5", "100+ XP", Color(0xFFFF5722)), // Vibrant Deep Orange
        RankTier("Trainee", "Lvl 6 - 10", "2.5k+ XP", Color(0xFFFF9800)), // Strong Orange
        RankTier("Pro", "Lvl 11 - 20", "10k+ XP", Color(0xFF4CAF50)), // Solid Green
        RankTier("Ultra Pro", "Lvl 21 - 30", "40k+ XP", Color(0xFF00C853)), // Lush Green
        RankTier("Expert", "Lvl 31 - 40", "90k+ XP", Color(0xFF00BFA5), true), // Teal
        RankTier("Legend", "Lvl 41 - 50", "160k+ XP", Color(0xFF00B0FF)), // Sky Blue
        RankTier("Mythic", "Lvl 51 - 60", "250k+ XP", Color(0xFF2979FF)), // Royal Blue
        RankTier("Master", "Lvl 61 - 70", "360k+ XP", Color(0xFF6200EA), true), // Deep Purple
        RankTier("Grandmaster", "Lvl 71 - 80", "490k+ XP", Color(0xFFAA00FF)), // Purple
        RankTier("Elite Master", "Lvl 81 - 90", "640k+ XP", Color(0xFFD500F9)), // Pink/Magenta
        RankTier("Mythic Warrior", "Lvl 91 - 99", "810k+ XP", Color(0xFFFF1744)), // Red
        RankTier("GODLIKE CHAMPION", "Lvl 100", "980k+ XP", Color(0xFFFFD600), true), // Pure Gold
        RankTier("🔥 PAPA 🔥", "Lvl 101+", "1.0M+ XP", Color(0xFFFF3D00), true) // Blazing Orange-Red
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
                Text("Rank Progression", fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    "Master your habits to unlock legendary ranks. Each tier represents a major milestone in your productivity journey!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                )
            }
            
            items(rankTiers) { tier ->
                RankTierItem(tier)
            }
        }
    }
}

@Composable
fun RankTierItem(tier: RankTier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, tier.color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tier Badge with stronger gradient
            val badgeBrush = when {
                tier.title.contains("PAPA", ignoreCase = true) -> 
                    Brush.linearGradient(listOf(Color(0xFFFF3D00), Color(0xFFFFEA00)))
                tier.title.contains("GODLIKE", ignoreCase = true) -> 
                    Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFF8F00)))
                else -> 
                    Brush.linearGradient(listOf(tier.color, tier.color.copy(alpha = 0.6f)))
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(brush = badgeBrush, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        tier.title.contains("PAPA", ignoreCase = true) -> Icons.Default.Whatshot
                        tier.isMilestone -> Icons.Default.EmojiEvents
                        else -> Icons.Default.Star
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = if (MaterialTheme.colorScheme.surfaceTint != Color.Unspecified) tier.color else tier.color // Force vibrant color
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = tier.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tier.levelRange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tier.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tier.xpRequired,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
