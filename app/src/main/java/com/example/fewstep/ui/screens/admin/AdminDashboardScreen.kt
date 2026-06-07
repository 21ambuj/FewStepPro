package com.example.fewstep.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import android.net.Uri
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.fewstep.util.SecurityUtils
import android.content.Context
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf("MENU") }
    
    val bgBrush = MaterialTheme.colorScheme.background

    androidx.activity.compose.BackHandler(enabled = currentScreen != "MENU") {
        currentScreen = "MENU"
    }

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
                IconButton(onClick = { 
                    if (currentScreen == "MENU") onBack() else currentScreen = "MENU" 
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (currentScreen == "MENU") "Admin Dashboard" else currentScreen, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                
                if (currentScreen == "MENU") {
                    var showSetPinDialog by remember { mutableStateOf(false) }
                    var newPinInput by remember { mutableStateOf("") }
                    var confirmPinInput by remember { mutableStateOf("") }
                    var pinErrorMsg by remember { mutableStateOf("") }
                    val context = LocalContext.current

                    IconButton(onClick = { showSetPinDialog = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "Set PIN", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (showSetPinDialog) {
                        AlertDialog(
                            onDismissRequest = { 
                                showSetPinDialog = false
                                newPinInput = ""
                                confirmPinInput = ""
                                pinErrorMsg = ""
                            },
                            title = { Text("Set Admin PIN") },
                            text = {
                                Column {
                                    Text("Enter a 4-digit PIN to secure the Admin Dashboard on this device.")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = newPinInput,
                                        onValueChange = { if (it.length <= 4) newPinInput = it },
                                        label = { Text("New 4-Digit PIN") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = confirmPinInput,
                                        onValueChange = { if (it.length <= 4) confirmPinInput = it },
                                        label = { Text("Confirm PIN") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true
                                    )
                                    if (pinErrorMsg.isNotEmpty()) {
                                        Text(pinErrorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (newPinInput.length != 4) {
                                        pinErrorMsg = "PIN must be exactly 4 digits."
                                    } else if (newPinInput != confirmPinInput) {
                                        pinErrorMsg = "PINs do not match."
                                    } else {
                                        val prefs = SecurityUtils.getEncryptedPrefs(context)
                                        val hash = SecurityUtils.hashPin(newPinInput)
                                        prefs.edit().putString("admin_pin_hash", hash).apply()
                                        showSetPinDialog = false
                                        newPinInput = ""
                                        confirmPinInput = ""
                                        pinErrorMsg = ""
                                        android.widget.Toast.makeText(context, "PIN Saved securely!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("Save PIN")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showSetPinDialog = false
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    pinErrorMsg = ""
                                }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bgBrush)
                .imePadding()
        ) {
            when (currentScreen) {
                "MENU" -> AdminMenuScreen { target -> currentScreen = target }
                "USERS" -> UserListTab(viewModel)
                "QUERIES" -> QueryListTab(viewModel)
                "BROADCASTS" -> BroadcastTab(viewModel)
                "DELETION REQ" -> DeletionReqListTab(viewModel)
                "ANALYTICS" -> AdminAnalyticsScreen(viewModel)
                "SYSTEM CONTROLS" -> AdminSystemControlsScreen(viewModel)
            }
        }
    }
}

@Composable
fun AdminMenuScreen(onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Control Center", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("Manage your platform.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            AdminMenuTile(
                title = "Manage Users",
                subtitle = "View, block, or delete users",
                icon = Icons.Default.Group,
                color = Color(0xFF1E88E5),
                onClick = { onNavigate("USERS") }
            )
        }
        item {
            AdminMenuTile(
                title = "User Queries",
                subtitle = "Respond to feedback and issues",
                icon = Icons.Default.Forum,
                color = Color(0xFF8E24AA),
                onClick = { onNavigate("QUERIES") }
            )
        }
        item {
            AdminMenuTile(
                title = "Global Broadcasts",
                subtitle = "Send announcements to everyone",
                icon = Icons.Default.Campaign,
                color = Color(0xFFF4511E),
                onClick = { onNavigate("BROADCASTS") }
            )
        }
        item {
            AdminMenuTile(
                title = "Deletion Requests",
                subtitle = "Process account deletion requests",
                icon = Icons.Default.DeleteSweep,
                color = Color(0xFFE53935),
                onClick = { onNavigate("DELETION REQ") }
            )
        }
        item {
            AdminMenuTile(
                title = "Visual Analytics",
                subtitle = "Platform growth & statistics",
                icon = Icons.Default.Insights,
                color = Color(0xFF43A047),
                onClick = { onNavigate("ANALYTICS") }
            )
        }
        item {
            AdminMenuTile(
                title = "System Controls",
                subtitle = "Maintenance mode & Settings",
                icon = Icons.Default.Settings,
                color = Color(0xFF607D8B),
                onClick = { onNavigate("SYSTEM CONTROLS") }
            )
        }
    }
}

@Composable
fun AdminMenuTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DeletionReqListTab(viewModel: AdminViewModel) {
    val requests by viewModel.deletionRequests.collectAsState()
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (requests.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillParentMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No deletion requests found.", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(requests, key = { it.id }) { req ->
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.03f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color.Red.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonRemove, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.userName, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(req.userEmail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        IconButton(
                            onClick = { viewModel.resolveDeletionRequest(context, req.id) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Resolve", tint = Color(0xFF10B981))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.Red.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Requested: ${sdf.format(Date(req.timestamp))}",
                            fontSize = 11.sp,
                            color = Color.Red.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserListTab(viewModel: AdminViewModel) {
    val users by viewModel.allUsers.collectAsState()
    val sortedUsers = users.sortedByDescending { it.isAdmin }
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(sortedUsers, key = { it.uid }) { user ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
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
                                    Text("ADMIN", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            if (user.isBlocked) {
                                Badge(containerColor = Color.Red, contentColor = Color.White) {
                                    Text("BLOCKED", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Badge(containerColor = Color(0xFF10B981), contentColor = Color.White) {
                                    Text("ACTIVE", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    if (!user.isAdmin) {
                        Row {
                            IconButton(onClick = { viewModel.toggleBlockUser(context, user) }) {
                                Icon(
                                    if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                    contentDescription = "Block/Unblock",
                                    tint = if (user.isBlocked) Color.Green else Color.Red
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
}

@Composable
fun QueryListTab(viewModel: AdminViewModel) {
    val queries by viewModel.queries.collectAsState()
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    var showReplyDialog by remember { mutableStateOf<UserQuery?>(null) }
    var replyMessage by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Query") }

    val filteredQueries = queries.filter { it.type == selectedType }

    if (showReplyDialog != null) {
        AlertDialog(
            onDismissRequest = { showReplyDialog = null },
            title = { Text("Reply to ${showReplyDialog?.userName}") },
            text = {
                OutlinedTextField(
                    value = replyMessage,
                    onValueChange = { replyMessage = it },
                    label = { Text("Admin Message") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    showReplyDialog?.let { query ->
                        viewModel.sendAdminNotification(
                            context, 
                            query.userId, 
                            "Reply to your ${query.type}: ${if (query.title.length > 30) query.title.take(27) + "..." else query.title}", 
                            replyMessage
                        )
                        viewModel.toggleQueryResolution(context, query)
                    }
                    showReplyDialog = null
                    replyMessage = ""
                }) { Text("SEND") }
            },
            dismissButton = {
                TextButton(onClick = { showReplyDialog = null }) { Text("CANCEL") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.FilterChip(
                selected = selectedType == "Query",
                onClick = { selectedType = "Query" },
                label = { Text("Queries") }
            )
            androidx.compose.material3.FilterChip(
                selected = selectedType == "Bug",
                onClick = { selectedType = "Bug" },
                label = { Text("Bugs") }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (filteredQueries.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillParentMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.QuestionAnswer, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No user queries yet.", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(filteredQueries, key = { it.id }) { query ->
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    query.userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(query.userName, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(query.userEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (query.isResolved) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        } else {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) {
                                Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            if (query.title.isNotBlank()) {
                                Text(query.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(query.query, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sdf.format(Date(query.timestamp)), fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${query.userEmail}")
                                        putExtra(Intent.EXTRA_SUBJECT, "Reply to your FewStep Query")
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                            }
                            
                            FilledTonalIconButton(
                                onClick = { viewModel.toggleQueryResolution(context, query) },
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (query.isResolved) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check, 
                                    contentDescription = "Mark Resolved", 
                                    modifier = Modifier.size(18.dp),
                                    tint = if (query.isResolved) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Button(
                                onClick = { showReplyDialog = query },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (query.isResolved) Color.Transparent else MaterialTheme.colorScheme.primary,
                                    contentColor = if (query.isResolved) MaterialTheme.colorScheme.primary else Color.White
                                )
                            ) {
                                Icon(Icons.Default.Reply, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun BroadcastTab(viewModel: AdminViewModel) {
    val scheduled by viewModel.scheduledBroadcasts.collectAsState()
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    // Custom Time State
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Section: New Broadcast
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("AI Coach Broadcast", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Enter motivating message for all users...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Schedule Time", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextButton(
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(sdf.format(Date(selectedTimestamp)), fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = { 
                        viewModel.sendBroadcast(context, message, selectedTimestamp)
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = message.isNotBlank(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Broadcast Message", fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.triggerTestNotification(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Quick Test 🧪", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.scheduleTestNotification(context, 1) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("1-Min Test ⏰", fontSize = 11.sp)
                    }
                }
            }
        }

        // Section: Scheduled List
        Text(
            "Scheduled Broadcasts", 
            fontWeight = FontWeight.Black, 
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 16.sp
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (scheduled.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No pending broadcasts.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
            items(scheduled, key = { it.id }) { broadcast ->
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RssFeed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(broadcast.message, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(
                                sdf.format(Date(broadcast.timestamp)),
                                fontSize = 11.sp,
                                color = if (broadcast.timestamp < System.currentTimeMillis()) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { viewModel.deleteBroadcast(context, broadcast.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AdminAnalyticsScreen(viewModel: AdminViewModel) {
    val users by viewModel.allUsers.collectAsState()
    
    val totalUsers = users.size
    val blockedUsers = users.count { it.isBlocked }
    val activeUsers = totalUsers - blockedUsers
    
    val now = System.currentTimeMillis()
    val weeklyUsers = users.count { now - it.createdAt < 7L * 24 * 60 * 60 * 1000 }
    val monthlyUsers = users.count { now - it.createdAt < 30L * 24 * 60 * 60 * 1000 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Visual Analytics", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AdminStatCard(title = "Total Users", value = "$totalUsers", icon = Icons.Default.Group, color = Color(0xFF1E88E5), modifier = Modifier.weight(1f))
                AdminStatCard(title = "Weekly New", value = "$weeklyUsers", icon = Icons.AutoMirrored.Filled.TrendingUp, color = Color(0xFF8E24AA), modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AdminStatCard(title = "Monthly New", value = "$monthlyUsers", icon = Icons.Default.DateRange, color = Color(0xFFF57C00), modifier = Modifier.weight(1f))
                AdminStatCard(title = "Blocked Users", value = "$blockedUsers", icon = Icons.Default.Block, color = Color(0xFFF44336), modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("User Status Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Simple Bar Chart
                    val activeRatio = if (totalUsers > 0) activeUsers.toFloat() / totalUsers else 0f
                    val blockedRatio = if (totalUsers > 0) blockedUsers.toFloat() / totalUsers else 0f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                    ) {
                        if (activeRatio > 0) {
                            Box(modifier = Modifier.weight(activeRatio).fillMaxHeight().background(Color(0xFF4CAF50)))
                        }
                        if (blockedRatio > 0) {
                            Box(modifier = Modifier.weight(blockedRatio).fillMaxHeight().background(Color(0xFFF44336)))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Active ($activeUsers)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFF44336)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Blocked ($blockedUsers)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminSystemControlsScreen(viewModel: AdminViewModel) {
    val context = LocalContext.current
    var isMaintenanceMode by remember { mutableStateOf(false) }
    var maintenanceEndTime by remember { mutableStateOf<Long?>(null) }

    // Fetch Maintenance Mode
    LaunchedEffect(Unit) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("config").document("app_settings").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                isMaintenanceMode = snapshot.getBoolean("isMaintenanceMode") ?: false
                val endTimeNum = snapshot.get("maintenanceEndTime") as? Number
                maintenanceEndTime = endTimeNum?.toLong()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("System Controls", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ) {
                        Icon(Icons.Default.Build, null, tint = Color(0xFFFF9800), modifier = Modifier.padding(12.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Maintenance Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Lock app for regular users", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isMaintenanceMode,
                        onCheckedChange = { newVal ->
                            isMaintenanceMode = newVal
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            
                            val updates = mutableMapOf<String, Any>("isMaintenanceMode" to newVal)
                            if (!newVal) {
                                // If turning off, clear the timer
                                updates["maintenanceEndTime"] = com.google.firebase.firestore.FieldValue.delete()
                            }
                            
                            db.collection("config").document("app_settings")
                                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, "Maintenance Mode updated!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    android.widget.Toast.makeText(context, "Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    // Revert toggle if failed
                                    isMaintenanceMode = !newVal
                                }
                        }
                    )
                }
                
                // Duration Setter UI
                androidx.compose.animation.AnimatedVisibility(visible = isMaintenanceMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text("Expected Duration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val setDuration = { hours: Int ->
                                val endTime = System.currentTimeMillis() + (hours * 60 * 60 * 1000L)
                                db.collection("config").document("app_settings")
                                    .set(mapOf("maintenanceEndTime" to endTime), com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener {
                                        android.widget.Toast.makeText(context, "Timer set for $hours hour(s)!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            }
                            
                            OutlinedButton(
                                onClick = { setDuration(1) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+1H", fontSize = 12.sp) }
                            
                            OutlinedButton(
                                onClick = { setDuration(4) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+4H", fontSize = 12.sp) }
                            
                            OutlinedButton(
                                onClick = { setDuration(24) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+1D", fontSize = 12.sp) }

                            var showCustomDialog by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showCustomDialog = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+Cstm", fontSize = 12.sp) }

                            if (showCustomDialog) {
                                var customHours by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showCustomDialog = false },
                                    title = { Text("Custom Duration") },
                                    text = {
                                        OutlinedTextField(
                                            value = customHours,
                                            onValueChange = { customHours = it.filter { char -> char.isDigit() } },
                                            label = { Text("Hours") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val hrs = customHours.toIntOrNull()
                                            if (hrs != null && hrs > 0) {
                                                setDuration(hrs)
                                            }
                                            showCustomDialog = false
                                        }) { Text("Set") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                        
                        if (maintenanceEndTime != null) {
                            var remainingTimeMs by remember { mutableStateOf(0L) }
                            LaunchedEffect(maintenanceEndTime) {
                                while (true) {
                                    val now = System.currentTimeMillis()
                                    val diff = maintenanceEndTime!! - now
                                    remainingTimeMs = if (diff > 0) diff else 0L
                                    kotlinx.coroutines.delay(1000)
                                }
                            }
                            val formatTime = { ms: Long ->
                                val totalSeconds = ms / 1000
                                val hours = totalSeconds / 3600
                                val minutes = (totalSeconds % 3600) / 60
                                val seconds = totalSeconds % 60
                                String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            val date = java.util.Date(maintenanceEndTime!!)
                            val format = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Ends at: ${format.format(date)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                if (remainingTimeMs > 0) {
                                    Text(formatTime(remainingTimeMs), fontSize = 14.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                                } else {
                                    Text("Time is up!", fontSize = 14.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

