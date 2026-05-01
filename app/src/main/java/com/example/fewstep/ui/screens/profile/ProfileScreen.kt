package com.example.fewstep.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.fewstep.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    themeViewModel: ThemeViewModel,
    onLogout: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onLevelRanksClick: () -> Unit,
    onBackClick: () -> Unit,
    onStreakClick: () -> Unit = {}
) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val user by homeViewModel.userData.collectAsState()
    
    val userName = user?.name?.ifEmpty { null } 
        ?: (authState as? AuthState.Success)?.user?.displayName 
        ?: "Champion"
        
    val userEmail = user?.email?.ifEmpty { null } 
        ?: (authState as? AuthState.Success)?.user?.email 
        ?: "user@example.com"

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }

    // Sync tempName when userName changes externally
    LaunchedEffect(userName) {
        tempName = userName
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Update Username", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Choose a name that reflects your power.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            homeViewModel.updateUserName(tempName)
                            showEditNameDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE CHANGES")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from your session?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("YES, LOGOUT", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Power Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                        .clickable { showLogoutDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout, 
                        null, 
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Logout", 
                        color = Color.Red, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        val context = androidx.compose.ui.platform.LocalContext.current
        val shareProgress = {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                val shareText = """
                    🚀 My FewStep Power Progress!
                    👤 Champion: $userName
                    🔥 Current Streak: ${user?.currentStreak ?: 0} Days
                    ⭐ Level: ${user?.level ?: 1} (${user?.rankTitle ?: "NOVICE"})
                    💎 Total XP Earned: ${user?.xp ?: 0}
                    
                    Join me on FewStep and let's conquer our goals together! 🎯
                    👉 https://21ambuj.github.io/FewStep-/
                """.trimIndent()
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share My Progress"))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Consolidated Power Card
            item {
                UnifiedPowerCard(
                    name = userName,
                    email = userEmail,
                    level = user?.level ?: 1,
                    rankTitle = user?.rankTitle ?: "NOVICE",
                    streak = user?.currentStreak ?: 0,
                    xp = user?.xp ?: 0,
                    onEditClick = { showEditNameDialog = true },
                    onShareClick = shareProgress,
                    onLevelClick = onLevelRanksClick,
                    onStreakClick = onStreakClick
                )
            }

            // Theme Toggle Section
            item {
                ProfileMenuCard(
                    title = if (isDarkMode) "Dark Mode" else "Light Mode",
                    subtitle = "Switch app theme",
                    icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    iconColor = if (isDarkMode) Color(0xFF818CF8) else Color(0xFFFFA000),
                    iconBgColor = if (isDarkMode) Color(0xFF312E81) else Color(0xFFFFF3E0),
                    isToggle = true,
                    isToggled = isDarkMode,
                    onToggle = { themeViewModel.toggleTheme() }
                )
            }

            // Leaderboard Section
            item {
                ProfileMenuCard(
                    title = "Global Leaderboard",
                    subtitle = "See where you stand globally",
                    icon = Icons.Default.EmojiEvents,
                    iconColor = Color(0xFFFBC02D),
                    iconBgColor = Color(0xFFFFF9C4),
                    onClick = onLeaderboardClick
                )
            }

            // Settings & App Center Section
            item {
                ProfileMenuCard(
                    title = "Settings & App Center",
                    subtitle = "Privacy, Terms, About & AI Coach",
                    icon = Icons.Default.LinearScale,
                    iconColor = Color(0xFF673AB7),
                    iconBgColor = Color(0xFFEDE7F6),
                    onClick = onMoreOptionsClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    }
}

@Composable
fun UnifiedPowerCard(
    name: String,
    email: String,
    level: Int,
    rankTitle: String,
    streak: Int,
    xp: Long,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onLevelClick: () -> Unit,
    onStreakClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Top Section: User Identity & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            name.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        Button(
                            onClick = onEditClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        email, 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Level & Rank Section (Pill/Circular Badge Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val rankColor = when {
                        level >= 101 -> Color(0xFFFF3D00)
                        level >= 100 -> Color(0xFFFFCC00)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(Icons.Default.EmojiEvents, null, tint = rankColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        rankTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = rankColor,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Surface(
                    color = Color.Transparent, // Removed background
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.clickable { onLevelClick() }
                ) {
                    Text(
                        "Level - $level", // Full word used
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = if (level >= 101) Color(0xFFFF3D00) else MaterialTheme.colorScheme.primary, // Themed color
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid (Floating Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .clickable { onStreakClick() }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF5722), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("$streak Days", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Current Streak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // XP Card
                

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFFFA000).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .clickable { onLevelClick() }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("$xp", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Total XP Earned", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Share Button
            Button(
                onClick = onShareClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.IosShare, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SHARE YOUR SUCCESS", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ProfileMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    onClick: (() -> Unit)? = null,
    isToggle: Boolean = false,
    isToggled: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = { if (!isToggle) onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconBgColor
            ) {
                Icon(
                    icon, 
                    null, 
                    tint = iconColor, 
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isToggle) {
                Switch(
                    checked = isToggled,
                    onCheckedChange = { onToggle?.invoke(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        }
    }
}

