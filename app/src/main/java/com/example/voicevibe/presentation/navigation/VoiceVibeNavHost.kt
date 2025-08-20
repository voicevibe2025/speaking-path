package com.example.voicevibe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicevibe.presentation.screens.auth.login.LoginScreen
import com.example.voicevibe.presentation.screens.auth.register.RegisterScreen
import com.example.voicevibe.presentation.screens.auth.splash.SplashScreen
import com.example.voicevibe.presentation.screens.auth.onboarding.OnboardingScreen
import com.example.voicevibe.presentation.screens.auth.forgot.ForgotPasswordScreen
import com.example.voicevibe.presentation.screens.main.MainScreen

/**
 * Main navigation host for the VoiceVibe app
 */
@Composable
fun VoiceVibeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Screen
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Forgot Password Screen
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Main App Screen (with bottom navigation)
        composable(route = Screen.Home.route) {
            MainScreen()
        }

        // Other main screens handled by bottom navigation
        composable(route = Screen.Practice.route) {
            MainScreen()
        }

        composable(route = Screen.LearningPaths.route) {
            MainScreen()
        }

        composable(route = Screen.Achievements.route) {
            MainScreen()
        }

        composable(route = Screen.Profile.route) {
            MainScreen()
        }

        // Session Practice Screen
        composable(route = Screen.SessionPractice.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            // SessionPracticeScreen(sessionId = sessionId)
        }

        // Session Result Screen
        composable(route = Screen.SessionResult.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            // SessionResultScreen(sessionId = sessionId)
        }

        // Add more screens as needed...
    }
}
