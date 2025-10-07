package com.example.voicevibe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.voicevibe.presentation.navigation.NavGraph
import com.example.voicevibe.presentation.navigation.Screen
import com.example.voicevibe.ui.theme.VoiceVibeTheme
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.presentation.system.MaintenanceBanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity of the VoiceVibe app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var splashScreenStateFlow: MutableStateFlow<Boolean>
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    // Global microphone permission launcher
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // No-op; screens react to permission state when granted
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition {
            splashScreenStateFlow.value
        }
        
        // Request microphone permission globally at app start if not granted yet
        val notGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val notAskedYet = !tokenManager.wasMicPermissionAsked()
        if (notGranted && notAskedYet) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            // Remember that we have prompted once to avoid nagging on every launch
            lifecycleScope.launch { tokenManager.setMicPermissionAsked(true) }
        }
        
        setContent {
            // Force app-wide dark theme and disable dynamic color
            VoiceVibeTheme(darkTheme = false, dynamicColor = true) {
                VoiceVibeApp()
            }
        }
    }
}

@Composable
fun VoiceVibeApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showNetworkIssues = currentRoute != Screen.Login.route && currentRoute != Screen.Register.route
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            MaintenanceBanner(showNetworkIssues = showNetworkIssues)
            NavGraph(navController = navController)
        }
    }
}