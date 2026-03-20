package com.example.fewstep.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fewstep.data.model.User
import com.example.fewstep.data.model.UserQuery
import com.example.fewstep.data.model.AdminBroadcast
import com.example.fewstep.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.allUsers.collectAsState()
    val queries by viewModel.queries.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Users", "Queries", "Broadcast")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> UserListTab(users, viewModel)
                1 -> QueryListTab(queries)
                2 -> {
                    val scheduled by viewModel.scheduledBroadcasts.collectAsState()
                    BroadcastTab(viewModel, scheduled)
                }
            }
        }
    }
}

@Composable
fun UserListTab(users: List<User>, viewModel: AdminViewModel) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(users) { user ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(user.email, fontSize = 12.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (user.isAdmin) {
                                Badge(containerColor = Color(0xFF6366F1), contentColor = Color.White) {
                                    Text("ADMIN", modifier = Modifier.padding(horizontal = 4.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            if (user.isBlocked) {
                                Badge(containerColor = Color.Red, contentColor = Color.White) {
                                    Text("BLOCKED", modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                        }
                    }
                    
                    Row {
                        IconButton(onClick = { viewModel.toggleBlockUser(context, user) }) {
                            Icon(
                                if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                contentDescription = "Block/Unblock",
                                tint = if (user.isBlocked) Color.Green else Color.Red
                            )
                        }
                        IconButton(onClick = { viewModel.toggleAdminStatus(context, user) }) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Toggle Admin",
                                tint = if (user.isAdmin) Color(0xFF6366F1) else Color.Gray
                            )
                        }
                        
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                        
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete User?") },
                                text = { Text("Are you sure you want to permanently delete ${user.name}? This action cannot be undone.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteUser(context, user.uid)
                                        showDeleteDialog = false
                                    }) { Text("DELETE", color = Color.Red) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { Text("CANCEL") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueryListTab(queries: List<UserQuery>) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (queries.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No queries found.", color = Color.Gray)
                }
            }
        }
        items(queries) { query ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(query.userName, fontWeight = FontWeight.Bold)
                        Text(sdf.format(Date(query.timestamp)), fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(query.userEmail, fontSize = 12.sp, color = Color.Gray)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(query.query, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun BroadcastTab(viewModel: AdminViewModel, scheduled: List<AdminBroadcast>) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    // Custom Time State
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Section: New Broadcast
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Schedule New AI Broadcast", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message for AI Coach") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time:", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Button(
                        onClick = {
                            val now = Calendar.getInstance()
                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                calendar.set(Calendar.YEAR, y)
                                calendar.set(Calendar.MONTH, m)
                                calendar.set(Calendar.DAY_OF_MONTH, d)
                                
                                android.app.TimePickerDialog(context, { _, hh, mm ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hh)
                                    calendar.set(Calendar.MINUTE, mm)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    selectedTimestamp = calendar.timeInMillis
                                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
                            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(sdf.format(Date(selectedTimestamp)))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        viewModel.sendBroadcast(context, message, selectedTimestamp)
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = message.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Set Broadcast")
                }

                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { viewModel.triggerTestNotification(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test Notification Now 🧪")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.scheduleTestNotification(context, 1) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Schedule 1-Min Background Test ⏰")
                }
            }
        }

        // Section: Scheduled List
        Text("Scheduled Notifications", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (scheduled.isEmpty()) {
                item { Text("No pending broadcasts.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
            }
            items(scheduled) { broadcast ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(broadcast.message, fontSize = 14.sp, maxLines = 2)
                            Text(
                                "Scheduled: ${sdf.format(Date(broadcast.timestamp))}",
                                fontSize = 11.sp,
                                color = if (broadcast.timestamp < System.currentTimeMillis()) Color.Red else Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.deleteBroadcast(context, broadcast.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
