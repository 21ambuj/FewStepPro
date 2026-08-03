package com.example.fewstep

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.theme.FewStepTheme
import com.example.fewstep.ui.screens.splash.SplashScreen
import com.example.fewstep.ui.screens.auth.LoginScreen
import com.example.fewstep.ui.screens.auth.SignupScreen

import com.example.fewstep.data.repository.HabitRepository
import com.example.fewstep.ui.viewmodel.HomeViewModel
import com.example.fewstep.ui.viewmodel.AuthViewModel
import com.example.fewstep.ui.screens.habit.AddHabitScreen
import com.example.fewstep.ui.screens.home.HomeScreen
import com.example.fewstep.ui.screens.profile.ProfileScreen
import com.example.fewstep.ui.screens.profile.LevelRanksScreen
import com.example.fewstep.ui.screens.profile.StreakScreen
import com.example.fewstep.ui.screens.home.StoreScreen
import com.example.fewstep.ui.screens.profile.MoreOptionsScreen
import com.example.fewstep.ui.screens.profile.more.AccountSettingsScreen
import com.example.fewstep.ui.screens.progress.ProgressScreen
import com.example.fewstep.ui.screens.habit.EditHabitScreen
import com.example.fewstep.ui.screens.focus.FocusTimerScreen
import com.example.fewstep.ui.screens.analytics.AnalyticsScreen
import com.example.fewstep.ui.screens.walk.WalkScreen
import com.example.fewstep.ui.screens.walk.WalkViewModel
import com.example.fewstep.ui.screens.aicoach.AiCoachScreen
import com.example.fewstep.ui.screens.leaderboard.LeaderboardScreen
import com.example.fewstep.ui.screens.leaderboard.LeaderboardViewModel
import com.example.fewstep.data.model.Habit
import com.example.fewstep.ui.viewmodel.AiCoachViewModel
import com.example.fewstep.ui.viewmodel.ThemeViewModel
import com.example.fewstep.ui.screens.profile.more.*
import com.example.fewstep.ui.screens.profile.NotificationsScreen
import com.example.fewstep.ui.screens.admin.AdminDashboardScreen
import com.example.fewstep.ui.screens.admin.BlockedScreen
import com.example.fewstep.ui.screens.auth.DeletionPendingScreen
import com.example.fewstep.ui.viewmodel.AdminViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.Icons.AutoMirrored.Filled.TrendingUp
import androidx.compose.material.Icons.AutoMirrored.Filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import com.google.android.gms.ads.MobileAds

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        MobileAds.initialize(this) { 
            android.util.Log.d("AdMob", "✅ MobileAds SDK Initialized")
        }

        // Initialize Start.io Ads
        com.startapp.sdk.adsbase.StartAppSDK.init(this, BuildConfig.STARTAPP_ID, true)
        com.startapp.sdk.adsbase.StartAppAd.disableSplash() // Disable the intrusive Start.io splash by default
        
        // 🚨 CRASH CATCHER FOR DIAGNOSING HOME SCREEN CRASH 🚨
        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        
        if (lastCrash != null) {
            setContent {
                FewStepTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Oops! The app crashed last time.", fontSize = 20.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = lastCrash,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                prefs.edit().remove("last_crash").apply()
                                recreate()
                            }) {
                                Text("Clear Exception & Restart")
                            }
                        }
                    }
                }
            }
            return
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = android.util.Log.getStackTraceString(throwable)
            prefs.edit().putString("last_crash", trace).commit()
            kotlin.system.exitProcess(1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 102)
            }
        }
        
        // Manual Dependency Injection
        val repository = HabitRepository()
        val focusRepository = com.example.fewstep.data.repository.FocusHistoryRepository()
        val homeViewModel = HomeViewModel(repository, focusRepository)
        val authViewModel = AuthViewModel()
        val aiCoachViewModel = AiCoachViewModel()
        val leaderboardViewModel = LeaderboardViewModel(repository)
        val themeViewModel = ThemeViewModel(this)
        val adminViewModel = AdminViewModel()
        val walkViewModel = WalkViewModel(application)
        
        authViewModel.checkCurrentUser()
         // Start AI Notification Engine
        com.example.fewstep.util.ai.AiNotificationScheduler.startInitial(this)

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            
            FewStepTheme(darkTheme = isDarkMode) {
                enableEdgeToEdge()
                
                // --- GLOBAL AUTH TOAST/FEEDBACK COLLECTOR ---
                val context = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(Unit) {
                    authViewModel.uiEvents.collect { event: com.example.fewstep.ui.viewmodel.AuthUiEvent ->
                        when(event) {
                            is com.example.fewstep.ui.viewmodel.AuthUiEvent.ShowToast -> {
                                android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            is com.example.fewstep.ui.viewmodel.AuthUiEvent.ShowSnackbar -> {
                                // Optional Snackbar logic if needed
                            }
                        }
                    }
                }
                
                // --- IN-APP UPDATE CHECK ---
                var pendingUpdate by remember { mutableStateOf<com.example.fewstep.util.update.UpdateInfo?>(null) }
                LaunchedEffect(Unit) {
                    val currentVersionCode = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionCode
                    pendingUpdate = com.example.fewstep.util.update.AppUpdateChecker.checkForUpdate(currentVersionCode)
                }
                if (pendingUpdate != null) {
                    com.example.fewstep.util.update.UpdateDialog(
                        updateInfo = pendingUpdate!!,
                        onDismiss = { pendingUpdate = null }
                    )
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navigationStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
                    val currentScreen by remember { derivedStateOf { navigationStack.lastOrNull() ?: Screen.Splash } }
                    val authState by authViewModel.authState.collectAsState()

                    // --- HANDLE MAGIC LINK SIGN-IN ---
                    LaunchedEffect(intent) {
                        intent?.let { handleAuthIntent(it, authViewModel) }
                    }

                    // Navigation Helper Functions
                    fun navigateTo(screen: Screen, clearStack: Boolean = false) {
                        if (clearStack) navigationStack.clear()
                        navigationStack.add(screen)
                    }

                    // ... existing navigation functions ...
                    fun navigateToTab(screen: Screen) {
                        navigationStack.clear()
                        navigationStack.add(Screen.Home)
                        if (screen != Screen.Home) {
                            navigationStack.add(screen)
                        }
                    }

                    fun popBack() {
                        if (navigationStack.size > 1) {
                            navigationStack.removeAt(navigationStack.size - 1)
                        } else {
                            if (currentScreen is Screen.Home || currentScreen is Screen.Login || currentScreen is Screen.Signup) {
                                this@MainActivity.finish()
                            }
                        }
                    }

                    val user by homeViewModel.userData.collectAsState()
                    LaunchedEffect(user?.isBlocked, user?.isDeleted, currentScreen) {
                        if (user?.isDeleted == true && currentScreen !is Screen.DeletionPending) {
                            navigateTo(Screen.DeletionPending, clearStack = true)
                        } else if (user?.isBlocked == true && user?.isDeleted != true && currentScreen !is Screen.Blocked) {
                            navigateTo(Screen.Blocked, clearStack = true)
                        } else if ((currentScreen is Screen.DeletionPending || currentScreen is Screen.Blocked) && 
                                   (user?.isDeleted != true && user?.isBlocked != true)) {
                            // AUTOMATIC REDIRECT AFTER RESTORATION 🛡️🚀
                            navigateTo(Screen.Home, clearStack = true)
                        }
                    }
                    
                    // Auto-redirect to Home if already logged in via Magic Link/Success
                    LaunchedEffect(authState) {
                        if (authState is com.example.fewstep.ui.viewmodel.AuthState.Success && currentScreen is Screen.Login) {
                            navigateTo(Screen.Home, clearStack = true)
                        } else if (authState is com.example.fewstep.ui.viewmodel.AuthState.Success && currentScreen is Screen.Signup) {
                            navigateTo(Screen.Home, clearStack = true)
                        } else if (authState is com.example.fewstep.ui.viewmodel.AuthState.Idle && currentScreen !is Screen.Login && currentScreen !is Screen.Signup && currentScreen !is Screen.Splash) {
                            // GLOBAL LOGOUT REDIRECT 🔑🚪
                            navigateTo(Screen.Login, clearStack = true)
                        }
                    }

                    // --- MAINTENANCE MODE CHECK ---
                    var isMaintenanceMode by remember { mutableStateOf(false) }
                    var maintenanceEndTime by remember { mutableStateOf<Long?>(null) }
                    
                    LaunchedEffect(Unit) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("config").document("app_settings")
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null && snapshot.exists()) {
                                    isMaintenanceMode = snapshot.getBoolean("isMaintenanceMode") ?: false
                                    val endTimeNum = snapshot.get("maintenanceEndTime") as? Number
                                    maintenanceEndTime = endTimeNum?.toLong()
                                }
                            }
                    }

                    val userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                    val isAdmin = (user?.isAdmin == true) || (userEmail == "fewstep@gmail.com")

                    LaunchedEffect(isMaintenanceMode, isAdmin, currentScreen, authState) {
                        if (isMaintenanceMode && !isAdmin && authState is com.example.fewstep.ui.viewmodel.AuthState.Success && currentScreen !is Screen.Maintenance) {
                            navigateTo(Screen.Maintenance, clearStack = true)
                        } else if (!isMaintenanceMode && currentScreen is Screen.Maintenance) {
                            navigateTo(Screen.Splash, clearStack = true)
                        }
                    }


                    BackHandler { popBack() }

                    val showBottomBar = currentScreen is Screen.Home || 
                                       currentScreen is Screen.Analytics || 
                                       currentScreen is Screen.Walk || 
                                       currentScreen is Screen.FocusTimer || 
                                       currentScreen is Screen.Profile

                    if (currentScreen is Screen.Splash) {
                        SplashScreen(onTimeout = {
                            if (authState is com.example.fewstep.ui.viewmodel.AuthState.Success) {
                                // Double check security flags on timeout
                                if (user?.isDeleted == true) {
                                    navigateTo(Screen.DeletionPending, clearStack = true)
                                } else if (user?.isBlocked == true) {
                                    navigateTo(Screen.Blocked, clearStack = true)
                                } else {
                                    navigateTo(Screen.Home, clearStack = true)
                                }
                            } else {
                                navigateTo(Screen.Login, clearStack = true)
                            }
                        })
                    } else {
                        Scaffold(
                            floatingActionButton = {
                                if (showBottomBar) {
                                    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                                    val glowColor = if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF00E5FF) else androidx.compose.ui.graphics.Color(0xFF6200EA)
                                    val aiGradient = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFF6200EA), 
                                            androidx.compose.ui.graphics.Color(0xFF00B8D4)
                                        )
                                    )
                                    
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val waveScale by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.6f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "waveScale"
                                    )
                                    val waveAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.6f,
                                        targetValue = 0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "waveAlpha"
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(bottom = if (currentScreen is Screen.Home) 88.dp else 16.dp)
                                            .size(72.dp)
                                    ) {
                                        // The wave
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .graphicsLayer {
                                                    scaleX = waveScale
                                                    scaleY = waveScale
                                                    alpha = waveAlpha
                                                }
                                                .background(brush = aiGradient, shape = androidx.compose.foundation.shape.CircleShape)
                                        )
                                        // The actual button
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shadow(
                                                    elevation = 12.dp, 
                                                    shape = androidx.compose.foundation.shape.CircleShape,
                                                    ambientColor = glowColor,
                                                    spotColor = glowColor
                                                )
                                                .background(brush = aiGradient, shape = androidx.compose.foundation.shape.CircleShape)
                                                .clickable { navigateTo(Screen.AiCoach) }
                                        ) {
                                            androidx.compose.material3.Icon(
                                                Icons.Default.AutoAwesome, 
                                                contentDescription = "AI Coach", 
                                                modifier = Modifier.size(24.dp),
                                                tint = androidx.compose.ui.graphics.Color.White
                                            )
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                if (showBottomBar) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        tonalElevation = 8.dp,
                                        modifier = Modifier
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .drawWithContent {
                                                drawContent()
                                                drawLine(
                                                    color = Color.LightGray.copy(alpha = 0.3f),
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                                    strokeWidth = 1f
                                                )
                                            }
                                    ) {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Home") },
                                            selected = currentScreen is Screen.Home,
                                            onClick = { navigateToTab(Screen.Home) }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Analytics") },
                                            label = { Text("Stats") },
                                            selected = currentScreen is Screen.Analytics,
                                            onClick = { navigateToTab(Screen.Analytics) }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Walk") },
                                            label = { Text("Walk") },
                                            selected = currentScreen is Screen.Walk,
                                            onClick = { navigateToTab(Screen.Walk) }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Timer, contentDescription = "Focus") },
                                            label = { Text("Focus") },
                                            selected = currentScreen is Screen.FocusTimer,
                                            onClick = { navigateToTab(Screen.FocusTimer) }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                            label = { Text("Profile") },
                                            selected = currentScreen is Screen.Profile,
                                            onClick = { navigateToTab(Screen.Profile) }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentScreen) {
                                    is Screen.Maintenance -> {
                                        com.example.fewstep.ui.screens.admin.MaintenanceScreen(
                                            endTime = maintenanceEndTime,
                                            onLogoutClick = { authViewModel.logout() }
                                        )
                                    }
                                    is Screen.Login -> {
                                        LoginScreen(
                                            viewModel = authViewModel,
                                            onLoginSuccess = { navigateTo(Screen.Home, clearStack = true) },
                                            onNavigateToSignup = { navigateTo(Screen.Signup, clearStack = true) }
                                        )
                                    }
                                    is Screen.Signup -> {
                                        SignupScreen(
                                            viewModel = authViewModel,
                                            onSignupSuccess = { navigateTo(Screen.Home, clearStack = true) },
                                            onNavigateToLogin = { navigateTo(Screen.Login, clearStack = true) }
                                        )
                                    }
                                    is Screen.Home -> {
                                        val userName = (authState as? com.example.fewstep.ui.viewmodel.AuthState.Success)?.user?.displayName ?: "Champion"
                                        HomeScreen(
                                            userName = userName,
                                            viewModel = homeViewModel,
                                            onAddHabitClick = { navigateTo(Screen.AddHabit) },
                                            onProfileClick = { navigateTo(Screen.Profile) },
                                            onProgressClick = { navigateTo(Screen.Progress) },
                                            onFocusClick = { navigateTo(Screen.FocusTimer) },
                                            onAnalyticsClick = { navigateTo(Screen.Analytics) },
                                            onEditClick = { habit -> navigateTo(Screen.EditHabit(habit)) },
                                            onAiCoachClick = { navigateTo(Screen.AiCoach) },
                                            onNotificationsClick = { navigateTo(Screen.Notifications) },
                                            onStreakClick = { navigateTo(Screen.Streak) },
                                            onLevelClick = { navigateTo(Screen.LevelRanks) },
                                            onStoreClick = { navigateTo(Screen.Store) }
                                        )
                                    }
                                    is Screen.AiCoach -> {
                                        val habits by homeViewModel.allHabitsRaw.collectAsState()
                                        val user by homeViewModel.userData.collectAsState()
                                        AiCoachScreen(
                                            user = user,
                                            habits = habits,
                                            viewModel = aiCoachViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.AddHabit -> {
                                        AddHabitScreen(
                                            viewModel = homeViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.EditHabit -> {
                                        EditHabitScreen(
                                            habit = (currentScreen as Screen.EditHabit).habit,
                                            viewModel = homeViewModel,
                                            onBackClick = { popBack() },
                                            onNavigateHome = { navigateTo(Screen.Home, clearStack = true) }
                                        )
                                    }
                                    is Screen.Profile -> {
                                        ProfileScreen(
                                            authViewModel = authViewModel,
                                            homeViewModel = homeViewModel,
                                            themeViewModel = themeViewModel,
                                            onLogout = { 
                                                authViewModel.logout()
                                                navigateTo(Screen.Login, clearStack = true)
                                            },
                                            onLeaderboardClick = { navigateTo(Screen.Leaderboard) },
                                            onMoreOptionsClick = { navigateTo(Screen.MoreOptions) },
                                            onLevelRanksClick = { navigateTo(Screen.LevelRanks) },
                                            onBackClick = { popBack() },
                                            onStreakClick = { navigateTo(Screen.Streak) }
                                        )
                                    }
                                    is Screen.LevelRanks -> {
                                        LevelRanksScreen(onBackClick = { popBack() })
                                    }
                                    is Screen.Streak -> {
                                        val user by homeViewModel.userData.collectAsState()
                                        StreakScreen(
                                            currentStreak = user?.currentStreak ?: 0,
                                            onNavigateBack = { popBack() }
                                        )
                                    }
                                    is Screen.Store -> {
                                        StoreScreen(
                                            viewModel = homeViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.Progress -> {
                                        ProgressScreen(
                                            viewModel = homeViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.Walk -> {
                                        WalkScreen(
                                            viewModel = walkViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.FocusTimer -> {
                                        FocusTimerScreen(
                                            viewModel = homeViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.Analytics -> {
                                        AnalyticsScreen(
                                            viewModel = homeViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.MoreOptions -> {
                                        val user by homeViewModel.userData.collectAsState()
                                        val userEmail = user?.email?.ifEmpty { null } 
                                            ?: (authState as? com.example.fewstep.ui.viewmodel.AuthState.Success)?.user?.email 
                                            ?: ""
                                        val isAdmin = (user?.isAdmin ?: false) || (userEmail == "fewstep@gmail.com")
                                        MoreOptionsScreen(
                                            authViewModel = authViewModel,
                                            onBackClick = { popBack() },
                                            onAiCoachClick = { navigateTo(Screen.AiCoach) },
                                            onContactClick = { navigateTo(Screen.ContactUs) },
                                            onPrivacyClick = { navigateTo(Screen.PrivacyPolicy) },
                                            onTermsClick = { navigateTo(Screen.TermsConditions) },
                                            onDeveloperClick = { navigateTo(Screen.Developer) },
                                            onAboutClick = { navigateTo(Screen.AboutUs) },
                                            onAccountSettingsClick = { navigateTo(Screen.AccountSettings) },
                                            isAdmin = isAdmin,
                                            onAdminClick = { navigateTo(Screen.AdminDashboard) }
                                        )
                                    }
                                    is Screen.AccountSettings -> {
                                        AccountSettingsScreen(
                                            authViewModel = authViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.AdminDashboard -> {
                                        AdminDashboardScreen(
                                            onBack = { popBack() },
                                            viewModel = adminViewModel
                                        )
                                    }
                                    is Screen.ContactUs -> ContactUsScreen(onBackClick = { popBack() })
                                    is Screen.PrivacyPolicy -> PrivacyPolicyScreen(onBackClick = { popBack() })
                                    is Screen.TermsConditions -> TermsConditionsScreen(onBackClick = { popBack() })
                                    is Screen.Developer -> DeveloperScreen(onBackClick = { popBack() })
                                    is Screen.AboutUs -> AboutUsScreen(onBackClick = { popBack() })
                                    is Screen.Notifications -> NotificationsScreen(onBackClick = { popBack() })
                                    is Screen.Leaderboard -> {
                                        LeaderboardScreen(
                                            viewModel = leaderboardViewModel,
                                            onBackClick = { popBack() }
                                        )
                                    }
                                    is Screen.Blocked -> {
                                        BlockedScreen(onLogout = {
                                            authViewModel.logout()
                                            navigateTo(Screen.Login, clearStack = true)
                                        })
                                    }
                                    is Screen.DeletionPending -> {
                                        DeletionPendingScreen(
                                            onCancelDeletion = { authViewModel.cancelDeletionRequest() },
                                            onLogout = {
                                                authViewModel.logout()
                                                navigateTo(Screen.Login, clearStack = true)
                                            }
                                        )
                                    }
                                    is Screen.Splash -> { /* Handled outside */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleAuthIntent(intent: android.content.Intent, viewModel: AuthViewModel) {
        val link = intent.data?.toString() ?: ""
        if (com.google.firebase.auth.FirebaseAuth.getInstance().isSignInWithEmailLink(link)) {
            val email = intent.data?.getQueryParameter("email") ?: ""
            viewModel.completeEmailLinkSignIn(email, link)
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Signup : Screen()
    object Home : Screen()
    object AddHabit : Screen()
    data class EditHabit(val habit: Habit) : Screen()
    object Profile : Screen()
    object Progress : Screen()
    object FocusTimer : Screen()
    object Analytics : Screen()
    object Walk : Screen()
    object AiCoach : Screen()
    object Leaderboard : Screen()
    object MoreOptions : Screen()
    object AccountSettings : Screen()
    object ContactUs : Screen()
    object PrivacyPolicy : Screen()
    object TermsConditions : Screen()
    object Developer : Screen()
    object AboutUs : Screen()
    object AdminDashboard : Screen()
    object Notifications : Screen()
    object Blocked : Screen()
    object DeletionPending : Screen()
    object LevelRanks : Screen()
    object Streak : Screen()
    object Store : Screen()
    object Maintenance : Screen()
}
