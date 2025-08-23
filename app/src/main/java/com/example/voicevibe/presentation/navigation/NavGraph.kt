package com.example.voicevibe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.voicevibe.presentation.screens.auth.*
import com.example.voicevibe.presentation.screens.main.home.HomeScreen
import com.example.voicevibe.presentation.screens.practice.PracticeScreen
import com.example.voicevibe.presentation.screens.practice.SessionPracticeScreen
import com.example.voicevibe.presentation.screens.practice.SessionResultScreen
import com.example.voicevibe.presentation.screens.learning.LearningPathsScreen
import com.example.voicevibe.presentation.screens.learning.LearningPathDetailScreen
import com.example.voicevibe.presentation.screens.learning.LessonDetailScreen
import com.example.voicevibe.presentation.screens.achievements.AchievementsScreen
import com.example.voicevibe.presentation.screens.achievements.LeaderboardScreen
import com.example.voicevibe.presentation.screens.profile.ProfileScreen
import com.example.voicevibe.presentation.screens.profile.SettingsScreen
import com.example.voicevibe.presentation.screens.scenarios.CulturalScenariosScreen
import com.example.voicevibe.presentation.screens.scenarios.ScenarioDetailScreen
import com.example.voicevibe.presentation.screens.analytics.AnalyticsDashboardScreen

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
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToLogin = {
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
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Main Screens
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPractice = {
                    navController.navigate(Screen.Practice.route)
                },
                onNavigateToLearningPaths = {
                    navController.navigate(Screen.LearningPathsRoute.route)
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements.route)
                },
                onNavigateToCulturalScenarios = {
                    navController.navigate(Screen.CulturalScenarios.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.DetailedAnalytics.route)
                }
            )
        }

        composable(Screen.Practice.route) {
            PracticeScreen(
                onNavigateToSession = { sessionId ->
                    navController.navigate(Screen.SessionPractice.createRoute(sessionId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Practice Flow
        composable(
            route = Screen.SessionPractice.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            SessionPracticeScreen(
                sessionId = sessionId,
                onNavigateToResult = {
                    navController.navigate(Screen.SessionResult.createRoute(sessionId)) {
                        popUpTo(Screen.Practice.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.SessionResult.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            SessionResultScreen(
                sessionId = sessionId,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToPracticeAgain = {
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
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToLeaderboard = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
