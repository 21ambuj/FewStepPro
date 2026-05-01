package com.example.fewstep.ui.screens.profile.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.screens.profile.MoreOptionTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    authViewModel: com.example.fewstep.ui.viewmodel.AuthViewModel,
    onBackClick: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? com.example.fewstep.ui.viewmodel.AuthState.Success)?.user?.email ?: ""

    var showDeleteDialog by remember { mutableStateOf(false) }
    var understoodDataLoss by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Account Deletion 🛡️", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(
                        "Deleting your account is PERMANENT. You will lose your habits, streaks, level, and all progress forever.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Checkbox(
                                checked = understoodDataLoss,
                                onCheckedChange = { understoodDataLoss = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "I understand that my data will be deleted permanently and cannot be recovered.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount()
                    },
                    enabled = understoodDataLoss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("REQUEST DELETION", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    understoodDataLoss = false
                }) { Text("CANCEL") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    "Account Settings", 
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            MoreOptionTile(
                title = "Reset Password",
                subtitle = "Send a reset link to your email",
                icon = Icons.Default.LockReset,
                iconColor = Color(0xFFF57C00),
                onClick = { authViewModel.sendPasswordResetEmail(userEmail) }
            )

            MoreOptionTile(
                title = "Delete Account",
                subtitle = "Permanent removal of your data",
                icon = Icons.Default.DeleteForever,
                iconColor = Color.Red,
                onClick = { showDeleteDialog = true }
            )
        }
    }
    }
}
