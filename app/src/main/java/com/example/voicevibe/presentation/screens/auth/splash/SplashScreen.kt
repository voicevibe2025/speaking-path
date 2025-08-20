package com.example.voicevibe.presentation.screens.auth.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Splash screen composable with animations
 */
@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is SplashNavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                is SplashNavigationEvent.NavigateToLogin -> onNavigateToLogin()
                is SplashNavigationEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    // Initial fade in animation
    var showContent by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "content_alpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .padding(32.dp)
        ) {
            // Animated Logo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VV",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "VoiceVibe",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Master English Speaking\nwith AI-Powered Learning",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }

        // Bottom text
        Text(
            text = "Powered by Advanced AI",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha)
        )
    }
}
