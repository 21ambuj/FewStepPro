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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.fewstep.util.SecurityUtils
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    authViewModel: com.example.fewstep.ui.viewmodel.AuthViewModel,
    onBackClick: () -> Unit,
    onAiCoachClick: () -> Unit,
    onContactClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    onAboutClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? com.example.fewstep.ui.viewmodel.AuthState.Success)?.user?.email ?: ""
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false 
                pinInput = ""
                pinError = false
            },
            title = { Text("Admin Unlock") },
            text = {
                Column {
                    Text("Enter your 4-digit PIN to access the dashboard.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN") },
                        isError = pinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (pinError) {
                        Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val prefs = SecurityUtils.getEncryptedPrefs(context)
                    val savedHash = prefs.getString("admin_pin_hash", null)
                    if (savedHash == SecurityUtils.hashPin(pinInput)) {
                        showPinDialog = false
                        pinInput = ""
                        pinError = false
                        onAdminClick()
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPinDialog = false 
                    pinInput = ""
                    pinError = false
                }) {
                    Text("Cancel")
                }
            }
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Settings & App Center", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- ACCOUNT SETTINGS TILE ---
            item {
                MoreOptionTile(
                    title = "Account Settings",
                    subtitle = "Reset password & manage account",
                    icon = Icons.Default.ManageAccounts,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onAccountSettingsClick
                )
            }


            item {
                MoreOptionTile(
                    title = "Contact Us & Report Bug",
                    subtitle = "Queries or Bug Reports",
                    icon = Icons.Default.SupportAgent,
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
                        onClick = {
                            val prefs = SecurityUtils.getEncryptedPrefs(context)
                            val savedHash = prefs.getString("admin_pin_hash", null)
                            if (savedHash != null) {
                                showPinDialog = true
                            } else {
                                onAdminClick()
                            }
                        }
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
                            "Version ${com.example.fewstep.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
