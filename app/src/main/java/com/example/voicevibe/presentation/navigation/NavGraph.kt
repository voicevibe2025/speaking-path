package com.example.voicevibe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.voicevibe.presentation.screens.gamification.LingoLeagueScreen
import com.example.voicevibe.presentation.screens.gamification.TopicLeaderboardScreen
import com.example.voicevibe.presentation.screens.gamification.TopicSelectionScreen
import com.example.voicevibe.presentation.screens.profile.ProfileScreen
import com.example.voicevibe.presentation.screens.profile.SettingsScreen
 import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
 import com.example.voicevibe.presentation.screens.profile.UserProfileScreen
import com.example.voicevibe.presentation.screens.profile.AccountSettingsScreen
import com.example.voicevibe.presentation.screens.profile.EditProfileScreen
import com.example.voicevibe.presentation.screens.profile.AboutScreen
import com.example.voicevibe.presentation.screens.profile.QAScreen
import com.example.voicevibe.presentation.screens.profile.PrivacySettingsScreen
import com.example.voicevibe.presentation.screens.profile.BlockedUsersScreen
import com.example.voicevibe.presentation.screens.profile.PrivacyPolicyScreen
import com.example.voicevibe.presentation.screens.profile.TermsOfServiceScreen
import com.example.voicevibe.presentation.screens.profile.CommunityGuidelinesScreen
import com.example.voicevibe.presentation.screens.profile.MyReportsScreen
import com.example.voicevibe.presentation.screens.profile.ImageCropScreen
import com.example.voicevibe.presentation.screens.main.social.SocialFeedScreen
import com.example.voicevibe.presentation.screens.main.search.UserSearchResultsScreen
import com.example.voicevibe.presentation.screens.scenarios.CulturalScenariosScreen
import com.example.voicevibe.presentation.screens.scenarios.ScenarioDetailScreen
import com.example.voicevibe.presentation.screens.analytics.AnalyticsDashboardScreen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyScreen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.TopicMasterScreen
import com.example.voicevibe.presentation.screens.speakingjourney.PronunciationPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.PlaceholderPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationPracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.PracticeWithAIScreen
import com.example.voicevibe.presentation.screens.practice.live.LivePracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.TopicPracticeScreen
import com.example.voicevibe.presentation.screens.practice.ai.TopicPracticeChatScreen
import com.example.voicevibe.presentation.navigation.Screen
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyViewModel
import com.example.voicevibe.presentation.screens.speakingjourney.VocabularyLessonScreen
import com.example.voicevibe.presentation.screens.speakingjourney.TopicVocabularyScreen
import com.example.voicevibe.presentation.screens.speakingjourney.LearnTopicWithViviScreen
import com.example.voicevibe.presentation.screens.speakingjourney.FluencyPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.VocabularyPracticeScreen
import com.example.voicevibe.presentation.screens.speakingjourney.GrammarPracticeScreen
import com.example.voicevibe.presentation.screens.group.GroupSelectionScreen
import com.example.voicevibe.presentation.screens.group.MyGroupScreen
import com.example.voicevibe.presentation.screens.group.GroupProfileScreen

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
                },
                onNavigateToGroupSelection = {
                    navController.navigate(Screen.GroupSelection.route) {
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
                },
                onNavigateToGroupSelection = {
                    navController.navigate(Screen.GroupSelection.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
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
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.TermsOfService.route)
                },
                onNavigateToGroupSelection = {
                    navController.navigate(Screen.GroupSelection.route) {
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
                onNavigateToLivePractice = {
                    navController.navigate(Screen.LivePractice.route)
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
                onNavigateToLingoLeague = {
                    navController.navigate(Screen.LingoLeague.route)
                },
                onNavigateToSocialFeed = { navController.navigate(Screen.SocialFeed.createRoute()) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToUserSearch = { navController.navigate(Screen.UserSearch.route) },
                onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToLearningPath = { pathId ->
                    navController.navigate(Screen.LearningPathDetail.createRoute(pathId))
                },
                onNavigateToLearnWithVivi = { topicId ->
                    navController.navigate(Screen.LearnTopicWithVivi.createRoute(topicId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToMyGroup = {
                    navController.navigate(Screen.MyGroup.route)
                },
                onNavigateToSpeakingJourney = {
                    navController.navigate(Screen.SpeakingJourney.route)
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
                onNavigateToListeningPractice = { topicId ->
                    navController.navigate(Screen.ListeningPractice.createRoute(topicId))
                },
                onNavigateToGrammarPractice = { topicId ->
                    navController.navigate(Screen.GrammarPractice.createRoute(topicId))
                },
                onNavigateToWordUp = {
                    navController.navigate(Screen.WordUp.route)
                },
                onNavigateToTopicLeaderboard = { topicId ->
                    navController.navigate(Screen.TopicLeaderboard.createRoute(topicId))
                },
                onNavigateToTopicSelection = {
                    navController.navigate(Screen.TopicSelection.route)
                }
            )
        }

        // Social feed dedicated screen (supports optional postId/commentId)
        composable(
            route = Screen.SocialFeed.route,
            arguments = listOf(
                navArgument("postId") { nullable = true },
                navArgument("commentId") { nullable = true }
            )
        ) { backStackEntry ->
            val postIdStr = backStackEntry.arguments?.getString("postId")
            val commentIdStr = backStackEntry.arguments?.getString("commentId")
            val postId = postIdStr?.toIntOrNull()
            val commentId = commentIdStr?.toIntOrNull()
            SocialFeedScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                postId = postId,
                commentId = commentId,
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
                onNavigateToPronunciationPractice = { topicId ->
                    navController.navigate(Screen.PronunciationPractice.createRoute(topicId))
                },
                onNavigateToFluencyPractice = { topicId ->
                    navController.navigate(Screen.FluencyPractice.createRoute(topicId))
                },
                onNavigateToVocabularyPractice = { topicId ->
                    navController.navigate(Screen.VocabularyPractice.createRoute(topicId))
                },
                onNavigateToListeningPractice = { topicId ->
                    navController.navigate(Screen.ListeningPractice.createRoute(topicId))
                },
                onNavigateToGrammarPractice = { topicId ->
                    navController.navigate(Screen.GrammarPractice.createRoute(topicId))
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
                onNavigateToTopicVocabulary = { topicId ->
                    navController.navigate(Screen.TopicVocabulary.createRoute(topicId))
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
                    // Prefer popping back to SpeakingJourney if it exists; otherwise navigate to it
                    val popped = navController.popBackStack(Screen.SpeakingJourney.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.SpeakingJourney.route) {
                            // Clear anything above Home so stack becomes Home -> SpeakingJourney
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
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
                },
                onNavigateToSpeakingLesson = { tid ->
                    // Navigate to the selected topic's SpeakingLesson. We allow stacking for simplicity.
                    navController.navigate(Screen.SpeakingLesson.createRoute(tid))
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
                onNavigateBack = {
                    // If threshold met, go to SpeakingJourney; pop if present else navigate and clear TopicMaster
                    val popped = navController.popBackStack(Screen.SpeakingJourney.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.SpeakingJourney.route) {
                            // Remove TopicMaster (and any others) so back from journey returns to Home
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                },
                onNavigateToLesson = {
                    // If a SpeakingLesson for this topic exists in back stack, pop to it; else navigate to it
                    val lessonRoute = Screen.SpeakingLesson.createRoute(topicId)
                    val poppedToLesson = navController.popBackStack(lessonRoute, inclusive = false)
                    if (!poppedToLesson) {
                        navController.navigate(lessonRoute)
                    }
                },
                onNavigateToPronunciationPractice = { navController.navigate(Screen.PronunciationPractice.createRoute(topicId)) },
                onNavigateToFluencyPractice = { navController.navigate(Screen.FluencyPractice.createRoute(topicId)) },
                onNavigateToVocabularyPractice = { navController.navigate(Screen.VocabularyPractice.createRoute(topicId)) },
                onNavigateToListeningPractice = { navController.navigate(Screen.ListeningPractice.createRoute(topicId)) },
                onNavigateToGrammarPractice = { navController.navigate(Screen.GrammarPractice.createRoute(topicId)) },
                onNavigateToConversation = { navController.navigate(Screen.ConversationPractice.createRoute(topicId)) },
                onNavigateToSpeakingJourney = {
                    // Navigate to SpeakingJourney screen
                    val popped = navController.popBackStack(Screen.SpeakingJourney.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.SpeakingJourney.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                }
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
            GrammarPracticeScreen(
                topicId = topicId,
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

        composable(
            route = Screen.TopicVocabulary.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TopicVocabularyScreen(
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

        composable(Screen.LivePractice.route) {
            LivePracticeScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToGroupProfile = { groupId ->
                    navController.navigate(Screen.GroupProfile.createRoute(groupId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.LingoLeague.route) {
            LingoLeagueScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TopicSelection.route) {
            TopicSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onTopicSelected = { topicId ->
                    navController.navigate(Screen.TopicLeaderboard.createRoute(topicId))
                }
            )
        }

        composable(
            route = Screen.TopicLeaderboard.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TopicLeaderboardScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
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
                onNavigateToFollowers = { userId: String ->
                    navController.navigate(Screen.FollowersFollowing.createRoute(userId = userId, tab = 0))
                },
                onNavigateToFollowing = { userId: String ->
                    navController.navigate(Screen.FollowersFollowing.createRoute(userId = userId, tab = 1))
                },
                onNavigateToMessage = { userId ->
                    navController.navigate(Screen.Conversation.createRouteWithUser(userId))
                }
            )
        }

        // User Search
        composable(Screen.UserSearch.route) {
            UserSearchResultsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onOpenGroup = { groupId ->
                    navController.navigate(Screen.GroupProfile.createRoute(groupId))
                },
                onOpenMaterial = { topicId ->
                    navController.navigate(Screen.TopicMaster.createRoute(topicId))
                }
            )
        }

        // Messaging
        composable(Screen.Messages.route) {
            com.example.voicevibe.presentation.screens.messaging.ConversationsListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConversation = { conversationId, otherUserId, otherUserName ->
                    navController.navigate(Screen.Conversation.createRouteWithConversation(conversationId))
                }
            )
        }

        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("conversationId") { 
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("userId") { 
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            com.example.voicevibe.presentation.screens.messaging.ConversationScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToFollowers = {
                    navController.navigate(Screen.FollowersFollowing.createRoute(userId = null, tab = 0))
                },
                onNavigateToFollowing = {
                    navController.navigate(Screen.FollowersFollowing.createRoute(userId = null, tab = 1))
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
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToQA = {
                    navController.navigate(Screen.QA.route)
                },
                onNavigateToPrivacySettings = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(Screen.BlockedUsers.route)
                },
                onNavigateToMyReports = {
                    navController.navigate(Screen.MyReports.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onNavigateToTermsOfService = {
                    navController.navigate(Screen.TermsOfService.route)
                },
                onNavigateToCommunityGuidelines = {
                    navController.navigate(Screen.CommunityGuidelines.route)
                },
                onNavigateToImageCrop = { encodedUri ->
                    navController.navigate(Screen.ImageCrop.createRoute(encodedUri))
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
            AccountSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.QA.route) {
            QAScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // Privacy & Safety screens
        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.BlockedUsers.route) {
            BlockedUsersScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.CommunityGuidelines.route) {
            CommunityGuidelinesScreen(onNavigateBack = { navController.popBackStack() })
        }

        // My Reports
        composable(Screen.MyReports.route) {
            MyReportsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Image Crop Screen
        composable(
            route = Screen.ImageCrop.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            ImageCropScreen(
                imageUri = imageUri,
                onNavigateBack = { navController.popBackStack() },
                onCropComplete = { navController.popBackStack() },
                viewModel = settingsViewModel
            )
        }

        // Notifications list
        composable(Screen.Notifications.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val homeViewModel: com.example.voicevibe.presentation.screens.main.home.HomeViewModel = hiltViewModel(parentEntry)

            com.example.voicevibe.presentation.screens.main.social.NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenNotification = { postId, commentId ->
                    navController.navigate(Screen.SocialFeed.createRoute(postId = postId, commentId = commentId))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId.toString()))
                },
                viewModel = homeViewModel
            )
        }

        // Followers/Following screen
        composable(
            route = Screen.FollowersFollowing.route,
            arguments = listOf(
                navArgument("userId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("tab") { 
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0

            com.example.voicevibe.presentation.screens.profile.FollowersFollowingScreen(
                userId = userId,
                initialTab = tab,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { targetUserId ->
                    navController.navigate(Screen.UserProfile.createRoute(targetUserId))
                }
            )
        }

        // Group Selection Screen
        composable(Screen.GroupSelection.route) {
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
        composable(Screen.MyGroup.route) {
            MyGroupScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId.toString()))
                }
            )
        }

        // Group Profile Screen
        composable(
            route = Screen.GroupProfile.route,
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            GroupProfileScreen(
                groupId = groupId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateToGroupChat = {
                    navController.navigate(Screen.MyGroup.route)
                }
            )
        }

        // WordUp Vocabulary Feature
        composable(route = Screen.WordUp.route) {
            com.example.voicevibe.presentation.screens.wordup.WordUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMasteredWords = { navController.navigate(Screen.MasteredWords.route) }
            )
        }

        composable(route = Screen.MasteredWords.route) {
            com.example.voicevibe.presentation.screens.wordup.MasteredWordsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
