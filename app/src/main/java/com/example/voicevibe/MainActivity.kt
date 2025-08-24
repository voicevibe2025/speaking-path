package com.example.voicevibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.voicevibe.presentation.navigation.NavGraph
import com.example.voicevibe.ui.theme.VoiceVibeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Main activity of the VoiceVibe app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var splashScreenStateFlow: MutableStateFlow<Boolean>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition {
            splashScreenStateFlow.value
        }
        
        setContent {
            VoiceVibeTheme {
                VoiceVibeApp()
            }
        }
    }
}

@Composable
fun VoiceVibeApp() {
    val navController = rememberNavController()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavGraph(navController = navController)
    }
}