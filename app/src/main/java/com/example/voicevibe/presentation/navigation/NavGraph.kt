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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.FormatQuote
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
import com.example.voicevibe.presentation.screens.gamification.AchievementScreen
import com.example.voicevibe.presentation.screens.gamification.LeaderboardScreen
import com.example.voicevibe.presentation.screens.profile.ProfileScreen
import com.example.voicevibe.presentation.screens.profile.SettingsScreen
 import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
 import com.example.voicevibe.presentation.screens.profile.UserProfileScreen
import com.example.voicevibe.presentation.screens.profile.AccountSettingsScreen
import com.example.voicevibe.presentation.screens.profile.NotificationSettingsScreen
import com.example.voicevibe.presentation.screens.profile.LanguageSettingsScreen
import com.example.voicevibe.presentation.screens.profile.AboutScreen
import com.example.voicevibe.presentation.screens.main.social.SocialFeedScreen
import com.example.voicevibe.presentation.screens.scenarios.CulturalScenariosScreen
import com.example.voicevibe.presentation.screens.scenarios.ScenarioDetailScreen
import com.example.voicevibe.presentation.screens.analytics.AnalyticsDashboardScreen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.TopicMasterScreen
import com.example.voicevibe.presentation.screens.speakingjourney.PronunciationPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.PlaceholderPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationPracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.PracticeWithAIScreen
import com.example.voicevibe.presentation.screens.practice.ai.TopicPracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.TopicPracticeChatScreen
import com.example.voicevibe.presentation.navigation.Screen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyViewModel
import com.example.voicevibe.presentation.screens.speakingjourney.VocabularyLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.FluencyPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.VocabularyPracticeScreen

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

        // Topic Practice list
        composable(route = Screen.TopicPractice.route) {
            TopicPracticeScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenTopicChat = { topicId ->
                    navController.navigate(Screen.TopicPracticeChat.createRoute(topicId))
                }
            )
        }

        // Topic Practice chat
        composable(
            route = Screen.TopicPracticeChat.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TopicPracticeChatScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
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
            val sjVM: SpeakingJourneyViewModel = hiltViewModel()
            val speakingOnly = settingsVM.speakingOnlyEnabled.value
            HomeScreen(
                onNavigateToPractice = {
                    if (speakingOnly) {
                        val sjState = sjVM.uiState.value
                        val topicId = sjState.topics.getOrNull(sjState.selectedTopicIdx)?.id
                            ?: sjState.userProfile?.lastVisitedTopicId
                            ?: sjState.topics.firstOrNull { it.unlocked }?.id
                            ?: sjState.topics.firstOrNull()?.id
                        if (!topicId.isNullOrBlank()) {
                            navController.navigate(Screen.TopicMaster.createRoute(topicId))
                        } else {
                            // Fallback if topics haven't loaded yet
                            navController.navigate(Screen.SpeakingJourney.route)
                        }
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
                onNavigateToSocialFeed = { navController.navigate(Screen.SocialFeed.route) },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToLearningPath = { pathId ->
                    navController.navigate(Screen.LearningPathDetail.createRoute(pathId))
                }
            )
        }

        // Social feed dedicated screen
        composable(Screen.SocialFeed.route) {
            SocialFeedScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
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
            route = Screen.TopicMaster.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TopicMasterScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPronunciationPractice = { navController.navigate(Screen.PronunciationPractice.createRoute(topicId)) },
                onNavigateToFluencyPractice = { navController.navigate(Screen.FluencyPractice.createRoute(topicId)) },
                onNavigateToVocabularyPractice = { navController.navigate(Screen.VocabularyPractice.createRoute(topicId)) },
                onNavigateToListeningPractice = { navController.navigate(Screen.ListeningPractice.createRoute(topicId)) },
                onNavigateToGrammarPractice = { navController.navigate(Screen.GrammarPractice.createRoute(topicId)) },
                onNavigateToConversation = { navController.navigate(Screen.ConversationPractice.createRoute(topicId)) }
            )
        }

        composable(
            route = Screen.PronunciationPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            PronunciationPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FluencyPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            FluencyPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VocabularyPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            VocabularyPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ListeningPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            com.example.voicevibe.presentation.screens.speakingjourney.ListeningPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GrammarPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            PlaceholderPracticeScreen(
                topicId = topicId,
                practiceType = "Grammar",
                icon = Icons.Default.FormatQuote,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TopicConversation.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            ConversationLessonScreen(
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
        composable(Screen.PracticeWithAI.route) {
            PracticeWithAIScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { sessionId ->
                    navController.navigate(Screen.SessionResult.createRoute(sessionId))
                },
                onNavigateToTopicPractice = {
                    navController.navigate(Screen.TopicPractice.route)
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
            AchievementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Profile & Settings
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            UserProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = { navController.navigate(Screen.Settings.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAchievements = { _: String ->
                    navController.navigate(Screen.Achievements.route)
                },
                onNavigateToFollowers = { _: String -> },
                onNavigateToFollowing = { _: String -> }
            )
        }
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

        // Settings sub-screens
        composable(Screen.AccountSettings.route) {
            AccountSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.LanguageSettings.route) {
            LanguageSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
