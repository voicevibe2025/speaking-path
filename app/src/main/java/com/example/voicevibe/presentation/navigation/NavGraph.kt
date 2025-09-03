package com.example.voicevibe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.voicevibe.presentation.screens.auth.splash.SplashScreen
import com.example.voicevibe.presentation.screens.auth.onboarding.OnboardingScreen
import com.example.voicevibe.presentation.screens.auth.login.LoginScreen
import com.example.voicevibe.presentation.screens.auth.register.RegisterScreen
import com.example.voicevibe.presentation.screens.auth.forgotpassword.ForgotPasswordScreen
import com.example.voicevibe.presentation.screens.main.home.HomeScreen
import com.example.voicevibe.presentation.screens.practice.speaking.SpeakingPracticeScreen
import com.example.voicevibe.presentation.screens.evaluation.EvaluationResultScreen
import com.example.voicevibe.presentation.screens.learning.LearningPathsScreen
import com.example.voicevibe.presentation.screens.learning.LearningPathDetailScreen
import com.example.voicevibe.presentation.screens.learning.LessonDetailScreen
import com.example.voicevibe.presentation.screens.gamification.AchievementsScreen
import com.example.voicevibe.presentation.screens.gamification.LeaderboardScreen
import com.example.voicevibe.presentation.screens.profile.ProfileScreen
import com.example.voicevibe.presentation.screens.profile.SettingsScreen
import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
import com.example.voicevibe.presentation.screens.scenarios.CulturalScenariosScreen
import com.example.voicevibe.presentation.screens.scenarios.ScenarioDetailScreen
import com.example.voicevibe.presentation.screens.analytics.AnalyticsDashboardScreen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyScreen
import com.example.voicevibe.presentation.screens.speakingjourney.TopicConversationScreen
import com.example.voicevibe.presentation.screens.practice.ai.PracticeWithAIScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication Screens
        composable(Screen.Splash.route) {
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

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Main Screens
        composable(Screen.Home.route) {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) {
                        navController.navigate(Screen.SpeakingJourney.route)
                    } else {
                        navController.navigate(Screen.Practice.route)
                    }
                },
                onNavigateToPracticeAI = {
                    navController.navigate(Screen.PracticeWithAI.route)
                },
                onNavigateToLearningPaths = {
                    if (speakingOnly) {
                        navController.navigate(Screen.SpeakingJourney.route)
                    } else {
                        navController.navigate(Screen.LearningPathsRoute.route)
                    }
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements.route)
                },
                onNavigateToLeaderboard = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToLearningPath = { pathId ->
                    navController.navigate(Screen.LearningPathDetail.createRoute(pathId))
                }
            )
        }

        composable(Screen.Practice.route) {
            SpeakingPracticeScreen(
                onNavigateToResults = { sessionId ->
                    navController.navigate(Screen.SessionResult.createRoute(sessionId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Speaking-only Journey (beta)
        composable(Screen.SpeakingJourney.route) {
            SpeakingJourneyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConversation = { topicId ->
                    navController.navigate(Screen.TopicConversation.createRoute(topicId))
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

        // Practice with AI standalone
        composable(Screen.PracticeWithAI.route) {
            PracticeWithAIScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { sessionId ->
                    navController.navigate(Screen.SessionResult.createRoute(sessionId))
                }
            )
        }

        // Practice Flow - Evaluation Result Screen
        composable(
            route = Screen.SessionResult.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            EvaluationResultScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPractice = {
                    navController.navigate(Screen.Practice.route) {
                        popUpTo(Screen.Practice.route) { inclusive = true }
                    }
                }
            )
        }

        // Learning Paths
        composable(Screen.LearningPathsRoute.route) {
            LearningPathsScreen(
                onNavigateToPath = { pathId ->
                    navController.navigate(Screen.LearningPathDetail.createRoute(pathId))
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { moduleId, lessonId ->
                    // Navigate directly to lesson from the list
                    navController.navigate(
                        Screen.DetailedLessonDetail.createRoute("", moduleId, lessonId)
                    )
                }
            )
        }

        composable(
            route = Screen.LearningPathDetail.route,
            arguments = listOf(navArgument("pathId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pathId = backStackEntry.arguments?.getString("pathId") ?: ""
            LearningPathDetailScreen(
                pathId = pathId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { moduleId, lessonId ->
                    navController.navigate(
                        Screen.DetailedLessonDetail.createRoute(pathId, moduleId, lessonId)
                    )
                }
            )
        }

        composable(
            route = Screen.DetailedLessonDetail.route,
            arguments = listOf(
                navArgument("pathId") { type = NavType.StringType },
                navArgument("moduleId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pathId = backStackEntry.arguments?.getString("pathId") ?: ""
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""

            LessonDetailScreen(
                pathId = pathId,
                moduleId = moduleId,
                lessonId = lessonId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNextLesson = { nextPathId, nextModuleId, nextLessonId ->
                    navController.navigate(
                        Screen.DetailedLessonDetail.createRoute(nextPathId, nextModuleId, nextLessonId)
                    ) {
                        popUpTo(Screen.DetailedLessonDetail.route) { inclusive = true }
                    }
                }
            )
        }

        // Cultural Scenarios
        composable(Screen.CulturalScenarios.route) {
            CulturalScenariosScreen(
                onNavigateToScenario = { scenarioId ->
                    navController.navigate(Screen.ScenarioDetail.createRoute(scenarioId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ScenarioDetail.route,
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
            ScenarioDetailScreen(
                scenarioId = scenarioId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onComplete = {
                    // Navigate back to scenarios list after completion
                    navController.popBackStack()
                }
            )
        }

        // Analytics & Progress
        composable(Screen.DetailedAnalytics.route) {
            AnalyticsDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Achievements & Gamification
        composable(Screen.Achievements.route) {
            AchievementsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Profile & Settings
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val scope = rememberCoroutineScope()
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAccountSettings = {
                    navController.navigate(Screen.AccountSettings.route)
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToLanguageSettings = {
                    navController.navigate(Screen.LanguageSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onLogout = {
                    scope.launch {
                        settingsViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                viewModel = settingsViewModel
            )
        }
    }
}
