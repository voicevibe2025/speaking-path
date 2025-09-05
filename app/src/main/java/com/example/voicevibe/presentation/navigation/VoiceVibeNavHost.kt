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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.voicevibe.presentation.screens.auth.login.LoginScreen
import com.example.voicevibe.presentation.screens.auth.register.RegisterScreen
import com.example.voicevibe.presentation.screens.auth.splash.SplashScreen
import com.example.voicevibe.presentation.screens.auth.onboarding.OnboardingScreen
import com.example.voicevibe.presentation.screens.auth.forgotpassword.ForgotPasswordScreen
import com.example.voicevibe.presentation.screens.main.home.HomeScreen
import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyScreen
import com.example.voicevibe.presentation.screens.speakingjourney.TopicConversationScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationPracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.PracticeWithAIScreen
import com.example.voicevibe.presentation.screens.speakingjourney.VocabularyLessonScreen

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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Main App Screen (with bottom navigation)
        composable(route = Screen.Home.route) {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.Practice.route)
                },
                onNavigateToPracticeAI = { navController.navigate(Screen.PracticeWithAI.route) },
                onNavigateToLearningPaths = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLearningPath = { _ ->
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                }
            )
        }

        // Other main screens handled by bottom navigation
        composable(route = Screen.Practice.route) {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.Practice.route)
                },
                onNavigateToPracticeAI = { navController.navigate(Screen.PracticeWithAI.route) },
                onNavigateToLearningPaths = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLearningPath = { _ ->
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                }
            )
        }

        composable(route = Screen.LearningPaths.route) {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.Practice.route)
                },
                onNavigateToPracticeAI = { navController.navigate(Screen.PracticeWithAI.route) },
                onNavigateToLearningPaths = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLearningPath = { _ ->
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                }
            )
        }

        composable(route = Screen.Achievements.route) {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.Practice.route)
                },
                onNavigateToPracticeAI = { navController.navigate(Screen.PracticeWithAI.route) },
                onNavigateToLearningPaths = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLearningPath = { _ ->
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                }
            )
        }

        composable(route = Screen.Profile.route) {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.Practice.route)
                },
                onNavigateToPracticeAI = { navController.navigate(Screen.PracticeWithAI.route) },
                onNavigateToLearningPaths = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLearningPath = { _ ->
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                }
            )
        }

        // Speaking-only Journey (beta)
        composable(route = Screen.SpeakingJourney.route) {
            SpeakingJourneyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConversation = { topicId ->
                    navController.navigate(Screen.TopicConversation.createRoute(topicId))
                },
                onNavigateToTopicMaster = { topicId ->
                    navController.navigate(Screen.TopicMaster.createRoute(topicId))
                },
                onNavigateToConversationPractice = { topicId ->
                    navController.navigate(Screen.ConversationPractice.createRoute(topicId))
                },
                onNavigateToVocabularyLesson = { topicId ->
                    navController.navigate(Screen.VocabularyLesson.createRoute(topicId))
                }
            )
        }

        composable(
            route = Screen.TopicConversation.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TopicConversationScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ConversationPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            ConversationPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VocabularyLesson.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            VocabularyLessonScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Practice with AI standalone
        composable(route = Screen.PracticeWithAI.route) {
            PracticeWithAIScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { sessionId ->
                    navController.navigate(Screen.SessionResult.createRoute(sessionId))
                }
            )
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
