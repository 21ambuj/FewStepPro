package com.example.fewstep.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    onBackClick: () -> Unit,
    onAiCoachClick: () -> Unit,
    onContactClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    onAboutClick: () -> Unit,
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More Options", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MoreOptionTile(
                    title = "AI Coach",
                    subtitle = "Get personalized guidance",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFF6200EE),
                    onClick = onAiCoachClick
                )
            }
            
            item {
                MoreOptionTile(
                    title = "Contact Us",
                    subtitle = "Need help? Reach out to us",
                    icon = Icons.Default.Email,
                    iconColor = Color(0xFF1E88E5),
                    onClick = onContactClick
                )
            }
            
            item {
                MoreOptionTile(
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFF43A047),
                    onClick = onPrivacyClick
                )
            }
            
            item {
                MoreOptionTile(
                    title = "Terms & Conditions",
                    subtitle = "Rules of the platform",
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFFFFB300),
                    onClick = onTermsClick
                )
            }
            
            item {
                MoreOptionTile(
                    title = "Developer",
                    subtitle = "About the creators",
                    icon = Icons.Default.Code,
                    iconColor = Color(0xFFE91E63),
                    onClick = onDeveloperClick
                )
            }
            
            item {
                MoreOptionTile(
                    title = "About Us",
                    subtitle = "Learn more about FewStep",
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF757575),
                    onClick = onAboutClick
                )
            }

            if (isAdmin) {
                item {
                    MoreOptionTile(
                        title = "Admin Dashboard",
                        subtitle = "Control Center & User Management",
                        icon = Icons.Default.AdminPanelSettings,
                        iconColor = Color(0xFFD32F2F),
                        onClick = onAdminClick
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Version 1.0.0",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "FewStep",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
