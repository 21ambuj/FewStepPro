package com.example.fewstep.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.viewmodel.AuthViewModel
import com.example.fewstep.ui.viewmodel.AuthState
import com.example.fewstep.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    onLogout: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val user by homeViewModel.userData.collectAsState()
    val userName = user?.name ?: (authState as? AuthState.Success)?.user?.displayName ?: "Champion"
    val userEmail = (authState as? AuthState.Success)?.user?.email ?: "user@example.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Power Profile", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FA)),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                ProfileHeaderCard(userName, userEmail, user?.level ?: 1)
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Current Streak",
                        value = "${user?.currentStreak ?: 0} Days",
                        icon = Icons.Default.Whatshot,
                        color = Color(0xFFFF5722)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total XP",
                        value = "${user?.xp ?: 0}",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFFA000)
                    )
                }
            }

            // Leaderboard Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = onLeaderboardClick
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFF9C4)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents, 
                                null, 
                                tint = Color(0xFFFBC02D), 
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Global Leaderboard", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                            Text("See where you stand globally", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.LightGray)
                    }
                }
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout Session", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

    }
}

@Composable
fun ProfileHeaderCard(name: String, email: String, level: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A237E), Color(0xFF3949AB))
                    )
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.Person, 
                        null, 
                        tint = Color.White, 
                        modifier = Modifier.padding(16.dp).size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(email, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFD600),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "LEVEL $level",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color(0xFF1A237E),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

