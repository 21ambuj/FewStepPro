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
    onBackClick: () -> Unit
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Power Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            item {
                ProfileHeaderCard(
                    name = userName,
                    email = userEmail,
                    level = user?.level ?: 1,
                    rankTitle = user?.rankTitle ?: "NOVICE",
                    onEditClick = { showEditNameDialog = true }
                )
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
                ProfileMenuCard(
                    title = "Global Leaderboard",
                    subtitle = "See where you stand globally",
                    icon = Icons.Default.EmojiEvents,
                    iconColor = Color(0xFFFBC02D),
                    iconBgColor = Color(0xFFFFF9C4),
                    onClick = onLeaderboardClick
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

            // More Section
            item {
                ProfileMenuCard(
                    title = "More Options",
                    subtitle = "Privacy, Terms, About & AI Coach",
                    icon = Icons.Default.LinearScale,
                    iconColor = Color(0xFF673AB7),
                    iconBgColor = Color(0xFFEDE7F6),
                    onClick = onMoreOptionsClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

    }
}

@Composable
fun ProfileHeaderCard(name: String, email: String, level: Int, rankTitle: String, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp).padding(start = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                "Edit Name",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        email,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "LVL $level",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        rankTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                
                Text(
                    "Account Verified",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        }
    }
}

