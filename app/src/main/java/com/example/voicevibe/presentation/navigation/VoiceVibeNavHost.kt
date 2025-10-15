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
import com.example.voicevibe.presentation.screens.main.social.SocialFeedScreen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyScreen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.LearnTopicWithViviScreen
import com.example.voicevibe.presentation.screens.practice.ai.PracticeWithAIScreen
import com.example.voicevibe.presentation.screens.practice.live.LivePracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.TopicPracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.TopicPracticeChatScreen
import com.example.voicevibe.presentation.screens.speakingjourney.VocabularyLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ListeningPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.GrammarPracticeScreen
import com.example.voicevibe.presentation.screens.gamification.AchievementScreen
import com.example.voicevibe.presentation.screens.profile.MyReportsScreen
import com.example.voicevibe.presentation.screens.group.GroupSelectionScreen
import com.example.voicevibe.presentation.screens.group.MyGroupScreen

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
                },
                onNavigateToGroupSelection = {
                    navController.navigate(Screen.GroupSelection.route) {
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
                },
                onNavigateToGroupSelection = {
                    navController.navigate(Screen.GroupSelection.route) {
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
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.TermsOfService.route)
                },
                onNavigateToGroupSelection = {
                    navController.navigate(Screen.GroupSelection.route) {
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
                onNavigateToLivePractice = { navController.navigate(Screen.LivePractice.route) },
                onNavigateToLearningPaths = {
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToSocialFeed = { navController.navigate(Screen.SocialFeed.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLearningPath = { _ ->
                    if (speakingOnly) navController.navigate(Screen.SpeakingJourney.route)
                    else navController.navigate(Screen.LearningPaths.route)
                },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToUserSearch = { navController.navigate(Screen.UserSearch.route) },
                onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                onNavigateToLearnWithVivi = { topicId ->
                    navController.navigate(Screen.LearnTopicWithVivi.createRoute(topicId))
                },
                onNavigateToMyGroup = { navController.navigate(Screen.MyGroup.route) },
                onNavigateToSpeakingJourney = { navController.navigate(Screen.SpeakingJourney.route) },
                onNavigateToRoute = { route -> navController.navigate(route) },
                onNavigateToTopicMaster = { topicId ->
                    navController.navigate(Screen.TopicMaster.createRoute(topicId))
                },
                onNavigateToConversationPractice = { topicId ->
                    navController.navigate(Screen.ConversationPractice.createRoute(topicId))
                },
                onNavigateToVocabularyLesson = { topicId ->
                    navController.navigate(Screen.VocabularyLesson.createRoute(topicId))
                },
                onNavigateToListeningPractice = { topicId ->
                    navController.navigate(Screen.ListeningPractice.createRoute(topicId))
                },
                onNavigateToGrammarPractice = { topicId ->
                    navController.navigate(Screen.GrammarPractice.createRoute(topicId))
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
                onNavigateToLivePractice = { navController.navigate(Screen.LivePractice.route) },
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
                },
                onNavigateToSpeakingJourney = { navController.navigate(Screen.SpeakingJourney.route) },
                onNavigateToRoute = { route -> navController.navigate(route) },
                onNavigateToTopicMaster = { topicId ->
                    navController.navigate(Screen.TopicMaster.createRoute(topicId))
                },
                onNavigateToConversationPractice = { topicId ->
                    navController.navigate(Screen.ConversationPractice.createRoute(topicId))
                },
                onNavigateToVocabularyLesson = { topicId ->
                    navController.navigate(Screen.VocabularyLesson.createRoute(topicId))
                },
                onNavigateToListeningPractice = { topicId ->
                    navController.navigate(Screen.ListeningPractice.createRoute(topicId))
                },
                onNavigateToGrammarPractice = { topicId ->
                    navController.navigate(Screen.GrammarPractice.createRoute(topicId))
                }
            )
        }

        composable(route = Screen.Achievements.route) {
            AchievementScreen(onNavigateBack = { navController.popBackStack() })
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
                onNavigateToLivePractice = { navController.navigate(Screen.LivePractice.route) },
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
                },
                onNavigateToSpeakingJourney = { navController.navigate(Screen.SpeakingJourney.route) },
                onNavigateToRoute = { route -> navController.navigate(route) },
                onNavigateToTopicMaster = { topicId ->
                    navController.navigate(Screen.TopicMaster.createRoute(topicId))
                },
                onNavigateToConversationPractice = { topicId ->
                    navController.navigate(Screen.ConversationPractice.createRoute(topicId))
                },
                onNavigateToVocabularyLesson = { topicId ->
                    navController.navigate(Screen.VocabularyLesson.createRoute(topicId))
                },
                onNavigateToListeningPractice = { topicId ->
                    navController.navigate(Screen.ListeningPractice.createRoute(topicId))
                },
                onNavigateToGrammarPractice = { topicId ->
                    navController.navigate(Screen.GrammarPractice.createRoute(topicId))
                }
            )
        }

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
                },
                onNavigateToLearnWithVivi = { topicId ->
                    navController.navigate(Screen.LearnTopicWithVivi.createRoute(topicId))
                },
                onNavigateToSpeakingLesson = { topicId ->
                    navController.navigate(Screen.SpeakingLesson.createRoute(topicId))
                },
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }

        composable(
            route = Screen.SpeakingLesson.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            SpeakingLessonScreen(
                topicId = topicId,
                onNavigateBack = {
                    // Always navigate back to SpeakingJourney screen
                    navController.popBackStack(Screen.SpeakingJourney.route, inclusive = false)
                },
                onNavigateToTopicMaster = { tid ->
                    navController.navigate(Screen.TopicMaster.createRoute(tid))
                },
                onNavigateToLearnWithVivi = { tid ->
                    navController.navigate(Screen.LearnTopicWithVivi.createRoute(tid))
                },
                onNavigateToConversationPractice = { tid ->
                    navController.navigate(Screen.ConversationPractice.createRoute(tid))
                },
                onNavigateToVocabularyLesson = { tid ->
                    navController.navigate(Screen.VocabularyLesson.createRoute(tid))
                }
            )
        }

        composable(
            route = Screen.TopicConversation.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            ConversationLessonScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLearnWithVivi = { navController.navigate(Screen.LearnTopicWithVivi.createRoute(topicId)) }
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

        composable(
            route = Screen.LearnTopicWithVivi.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            LearnTopicWithViviScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Practice with AI standalone
        composable(route = Screen.PracticeWithAI.route) {
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

        composable(route = Screen.LivePractice.route) {
            LivePracticeScreen(
                onNavigateBack = { navController.popBackStack() }
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
        // Topic Practice list
        composable(route = Screen.TopicPractice.route) {
            TopicPracticeScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenTopicChat = { topicId ->
                    navController.navigate(Screen.TopicPracticeChat.createRoute(topicId))
                }
            )
        }

        // My Reports
        composable(route = Screen.MyReports.route) {
            MyReportsScreen(onNavigateBack = { navController.popBackStack() })
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

        // Group Selection Screen
        composable(route = Screen.GroupSelection.route) {
            GroupSelectionScreen(
                onGroupSelected = {
                    // After joining group, navigate to home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.GroupSelection.route) { inclusive = true }
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // My Group Screen
        composable(route = Screen.MyGroup.route) {
            MyGroupScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Listening Practice Screen
        composable(
            route = Screen.ListeningPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) {
            val topicId = it.arguments?.getString("topicId") ?: return@composable
            ListeningPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Grammar Practice Screen
        composable(
            route = Screen.GrammarPractice.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) {
            val topicId = it.arguments?.getString("topicId") ?: return@composable
            GrammarPracticeScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
