package com.example.fewstep

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.fewstep.ui.screens.progress.ProgressScreen
import com.example.fewstep.ui.screens.habit.EditHabitScreen
import com.example.fewstep.ui.screens.focus.FocusTimerScreen
import com.example.fewstep.ui.screens.analytics.AnalyticsScreen
import com.example.fewstep.data.model.Habit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
        
        authViewModel.checkCurrentUser()

        setContent {
            FewStepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                    val authState by authViewModel.authState.collectAsState()

                    val showBottomBar = currentScreen is Screen.Home || 
                                       currentScreen is Screen.Analytics || 
                                       currentScreen is Screen.FocusTimer || 
                                       currentScreen is Screen.Profile

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1A237E),
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home") },
                                        selected = currentScreen is Screen.Home,
                                        onClick = { currentScreen = Screen.Home },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF1A237E),
                                            unselectedIconColor = Color.Gray,
                                            indicatorColor = Color(0xFFE8EAF6)
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Analytics") },
                                        label = { Text("Stats") },
                                        selected = currentScreen is Screen.Analytics,
                                        onClick = { currentScreen = Screen.Analytics },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF1A237E),
                                            unselectedIconColor = Color.Gray,
                                            indicatorColor = Color(0xFFE8EAF6)
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Timer, contentDescription = "Focus") },
                                        label = { Text("Focus") },
                                        selected = currentScreen is Screen.FocusTimer,
                                        onClick = { currentScreen = Screen.FocusTimer },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF1A237E),
                                            unselectedIconColor = Color.Gray,
                                            indicatorColor = Color(0xFFE8EAF6)
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                        label = { Text("Profile") },
                                        selected = currentScreen is Screen.Profile,
                                        onClick = { currentScreen = Screen.Profile },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF1A237E),
                                            unselectedIconColor = Color.Gray,
                                            indicatorColor = Color(0xFFE8EAF6)
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
                                            currentScreen = Screen.Home
                                        } else {
                                            currentScreen = Screen.Login 
                                        }
                                    })
                                }
                                is Screen.Login -> {
                                    LoginScreen(
                                        viewModel = authViewModel,
                                        onLoginSuccess = { currentScreen = Screen.Home },
                                        onNavigateToSignup = { currentScreen = Screen.Signup }
                                    )
                                }
                                is Screen.Signup -> {
                                    SignupScreen(
                                        viewModel = authViewModel,
                                        onSignupSuccess = { currentScreen = Screen.Home },
                                        onNavigateToLogin = { currentScreen = Screen.Login }
                                    )
                                }
                                is Screen.Home -> {
                                    val userName = (authState as? com.example.fewstep.ui.viewmodel.AuthState.Success)?.user?.displayName ?: "Champion"
                                    HomeScreen(
                                        userName = userName,
                                        viewModel = homeViewModel,
                                        onAddHabitClick = { currentScreen = Screen.AddHabit },
                                        onProfileClick = { currentScreen = Screen.Profile },
                                        onProgressClick = { currentScreen = Screen.Progress },
                                        onFocusClick = { currentScreen = Screen.FocusTimer },
                                        onAnalyticsClick = { currentScreen = Screen.Analytics },
                                        onEditClick = { habit -> currentScreen = Screen.EditHabit(habit) }
                                    )
                                }
                                is Screen.AddHabit -> {
                                    AddHabitScreen(
                                        viewModel = homeViewModel,
                                        onBackClick = { currentScreen = Screen.Home }
                                    )
                                }
                                is Screen.EditHabit -> {
                                    EditHabitScreen(
                                        habit = (currentScreen as Screen.EditHabit).habit,
                                        viewModel = homeViewModel,
                                        onBackClick = { currentScreen = Screen.Home },
                                        onNavigateHome = { currentScreen = Screen.Home }
                                    )
                                }
                                is Screen.Profile -> {
                                    ProfileScreen(
                                        authViewModel = authViewModel,
                                        homeViewModel = homeViewModel,
                                        onLogout = { 
                                            authViewModel.logout()
                                            currentScreen = Screen.Login
                                        },
                                        onBackClick = { currentScreen = Screen.Home }
                                    )
                                }
                                is Screen.Progress -> {
                                    ProgressScreen(
                                        viewModel = homeViewModel,
                                        onBackClick = { currentScreen = Screen.Home }
                                    )
                                }
                                is Screen.FocusTimer -> {
                                    FocusTimerScreen(
                                        viewModel = homeViewModel,
                                        onBackClick = { currentScreen = Screen.Home }
                                    )
                                }
                                is Screen.Analytics -> {
                                    AnalyticsScreen(
                                        viewModel = homeViewModel,
                                        onBackClick = { currentScreen = Screen.Home }
                                    )
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
}



