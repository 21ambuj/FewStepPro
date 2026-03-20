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
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.fewstep.ui.screens.profile.MoreOptionsScreen
import com.example.fewstep.ui.screens.progress.ProgressScreen
import com.example.fewstep.ui.screens.habit.EditHabitScreen
import com.example.fewstep.ui.screens.focus.FocusTimerScreen
import com.example.fewstep.ui.screens.analytics.AnalyticsScreen
import com.example.fewstep.ui.screens.aicoach.AiCoachScreen
import com.example.fewstep.ui.screens.leaderboard.LeaderboardScreen
import com.example.fewstep.ui.screens.leaderboard.LeaderboardViewModel
import com.example.fewstep.data.model.Habit
import com.example.fewstep.ui.viewmodel.AiCoachViewModel
import com.example.fewstep.ui.viewmodel.ThemeViewModel
import com.example.fewstep.ui.screens.profile.more.*
import com.example.fewstep.ui.screens.admin.AdminDashboardScreen
import com.example.fewstep.ui.screens.admin.BlockedScreen
import com.example.fewstep.ui.viewmodel.AdminViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        MobileAds.initialize(this) { status ->
            val states = status.adapterStatusMap
            for (adapterClass in states.keys) {
                val state = states[adapterClass]
                android.util.Log.d("AdMob", String.format("Adapter name: %s, Description: %s, Latency: %d",
                    adapterClass, state?.description, state?.latency))
            }
            android.util.Log.d("AdMob", "✅ MobileAds SDK Initialized")
        }
        
        // ðŸš¨ CRASH CATCHER FOR DIAGNOSING HOME SCREEN CRASH ðŸš¨
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
            // Exit immediately to prevent ANR popup block
            kotlin.system.exitProcess(1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Manual Dependency Injection for now
        val repository = HabitRepository()
        val homeViewModel = HomeViewModel(repository)
        val authViewModel = AuthViewModel()
        val aiCoachViewModel = AiCoachViewModel()
        val leaderboardViewModel = LeaderboardViewModel(repository)
        val themeViewModel = ThemeViewModel(this)
        val adminViewModel = AdminViewModel()
        
        authViewModel.checkCurrentUser()
        
        // Start AI Notification Engine (Every 3 hours funny reminders)
        com.example.fewstep.util.ai.AiNotificationScheduler.startInitial(this)

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            
            FewStepTheme(darkTheme = isDarkMode) {
                // Enable Edge-to-Edge inside the theme block common for modern apps
                enableEdgeToEdge(
                    statusBarStyle = androidx.activity.SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode },
                    navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode }
                )
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navigationStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
                    val currentScreen by remember { derivedStateOf { navigationStack.lastOrNull() ?: Screen.Splash } }
                    val authState by authViewModel.authState.collectAsState()

                    // Navigation Helper Functions
                    fun navigateTo(screen: Screen, clearStack: Boolean = false) {
                        if (clearStack) navigationStack.clear()
                        navigationStack.add(screen)
                    }

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
                            // If we're at the root (Home, Login, or Signup) and press back, close app
                            if (currentScreen is Screen.Home || currentScreen is Screen.Login || currentScreen is Screen.Signup) {
                                this@MainActivity.finish()
                            }
                        }
                    }

                    val user by homeViewModel.userData.collectAsState()
                    LaunchedEffect(user?.isBlocked) {
                        if (user?.isBlocked == true && currentScreen !is Screen.Blocked) {
                            navigateTo(Screen.Blocked, clearStack = true)
                        }
                    }

                    BackHandler { popBack() }

                    val showBottomBar = currentScreen is Screen.Home || 
                                       currentScreen is Screen.Analytics || 
                                       currentScreen is Screen.FocusTimer || 
                                       currentScreen is Screen.Profile

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home") },
                                        selected = currentScreen is Screen.Home,
                                        onClick = { navigateToTab(Screen.Home) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Analytics") },
                                        label = { Text("Stats") },
                                        selected = currentScreen is Screen.Analytics,
                                        onClick = { navigateToTab(Screen.Analytics) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Timer, contentDescription = "Focus") },
                                        label = { Text("Focus") },
                                        selected = currentScreen is Screen.FocusTimer,
                                        onClick = { navigateToTab(Screen.FocusTimer) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                        label = { Text("Profile") },
                                        selected = currentScreen is Screen.Profile,
                                        onClick = { navigateToTab(Screen.Profile) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentScreen) {
                                is Screen.Splash -> {
                                    SplashScreen(onTimeout = {
                                        if (authState is com.example.fewstep.ui.viewmodel.AuthState.Success) {
                                            navigateTo(Screen.Home, clearStack = true)
                                        } else {
                                            navigateTo(Screen.Login, clearStack = true)
                                        }
                                    })
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
                                        onAiCoachClick = { navigateTo(Screen.AiCoach) }
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
                                        onBackClick = { popBack() }
                                    )
                                }
                                is Screen.Progress -> {
                                    ProgressScreen(
                                        viewModel = homeViewModel,
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
                                    val isAdmin = (user?.isAdmin ?: false) || (userEmail == "ambuj20maurya@gmail.com")

                                    MoreOptionsScreen(
                                        onBackClick = { popBack() },
                                        onAiCoachClick = { navigateTo(Screen.AiCoach) },
                                        onContactClick = { navigateTo(Screen.ContactUs) },
                                        onPrivacyClick = { navigateTo(Screen.PrivacyPolicy) },
                                        onTermsClick = { navigateTo(Screen.TermsConditions) },
                                        onDeveloperClick = { navigateTo(Screen.Developer) },
                                        onAboutClick = { navigateTo(Screen.AboutUs) },
                                        isAdmin = isAdmin,
                                        onAdminClick = { navigateTo(Screen.AdminDashboard) }
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
                            }
                        }
                    }
                }
            }
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
    object AiCoach : Screen()
    object Leaderboard : Screen()
    object MoreOptions : Screen()
    object ContactUs : Screen()
    object PrivacyPolicy : Screen()
    object TermsConditions : Screen()
    object Developer : Screen()
    object AboutUs : Screen()
    object AdminDashboard : Screen()
    object Blocked : Screen()
}
