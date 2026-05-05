package com.fuck13.mobileide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fuck13.mobileide.data.preferences.UserPreferences
import com.fuck13.mobileide.ui.screens.DashboardScreen
import com.fuck13.mobileide.ui.screens.EditorScreen
import com.fuck13.mobileide.ui.screens.FileManagerScreen
import com.fuck13.mobileide.ui.screens.ProjectManagerScreen
import com.fuck13.mobileide.ui.screens.TerminalScreen
import com.fuck13.mobileide.ui.screens.SettingsScreen
import com.fuck13.mobileide.ui.theme.MobileIDETheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val darkMode by UserPreferences.darkMode.collectAsStateWithLifecycle(initial = true)
            val dynamicColors by UserPreferences.dynamicColors.collectAsStateWithLifecycle(initial = false)
            
            MobileIDETheme(
                darkTheme = darkMode,
                dynamicColor = dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToEditor = { navController.navigate("editor") },
                onNavigateToFiles = { navController.navigate("files") },
                onNavigateToProjects = { navController.navigate("projects") },
                onNavigateToTerminal = { navController.navigate("terminal") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("editor") {
            EditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("files") {
            FileManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("projects") {
            ProjectManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("terminal") {
            TerminalScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
