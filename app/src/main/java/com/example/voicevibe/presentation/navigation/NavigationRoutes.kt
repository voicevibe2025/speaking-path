package com.example.voicevibe.presentation.navigation

/**
 * Defines all navigation routes in the app
 */
sealed class Screen(val route: String) {
    // Authentication
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Main screens
    object Home : Screen("home")
    object Practice : Screen("practice")
    object LearningPaths : Screen("learning_paths")
    object Achievements : Screen("achievements")
    object Profile : Screen("profile")
    // Speaking-only flow (beta)
    object SpeakingJourney : Screen("speaking_journey")
    object TopicMaster : Screen("topic_master/{topicId}") {
        fun createRoute(topicId: String) = "topic_master/$topicId"
    }
    object PronunciationPractice : Screen("pronunciation_practice/{topicId}") {
        fun createRoute(topicId: String) = "pronunciation_practice/$topicId"
    }
    object FluencyPractice : Screen("fluency_practice/{topicId}") {
        fun createRoute(topicId: String) = "fluency_practice/$topicId"
    }
    object VocabularyPractice : Screen("vocabulary_practice/{topicId}") {
        fun createRoute(topicId: String) = "vocabulary_practice/$topicId"
    }
    object ListeningPractice : Screen("listening_practice/{topicId}") {
        fun createRoute(topicId: String) = "listening_practice/$topicId"
    }
    object GrammarPractice : Screen("grammar_practice/{topicId}") {
        fun createRoute(topicId: String) = "grammar_practice/$topicId"
    }
    object TopicConversation : Screen("speaking_conversation/{topicId}") {
        fun createRoute(topicId: String) = "speaking_conversation/$topicId"
    }
    // Practice with AI standalone screen
    object PracticeWithAI : Screen("practice_with_ai")

    // Practice flow
    object SessionPractice : Screen("session_practice/{sessionId}") {
        fun createRoute(sessionId: String) = "session_practice/$sessionId"
    }
    object SessionResult : Screen("session_result/{sessionId}") {
        fun createRoute(sessionId: String) = "session_result/$sessionId"
    }

    // Learning path flow
    object LessonDetail : Screen("lesson/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson/$lessonId"
    }
    object ModuleDetail : Screen("module/{moduleId}") {
        fun createRoute(moduleId: String) = "module/$moduleId"
    }

    // Learning Paths Routes
    object LearningPathsRoute : Screen("learning_paths")
    object LearningPathDetail : Screen("learning_path/{pathId}") {
        fun createRoute(pathId: String) = "learning_path/$pathId"
    }
    object DetailedLessonDetail : Screen("lesson/{pathId}/{moduleId}/{lessonId}") {
        fun createRoute(pathId: String, moduleId: String, lessonId: String) = 
            "lesson/$pathId/$moduleId/$lessonId"
    }

    // Cultural scenarios
    object CulturalScenarios : Screen("cultural_scenarios")
    object ScenarioDetail : Screen("scenario/{scenarioId}") {
        fun createRoute(scenarioId: String) = "scenario/$scenarioId"
    }

    // Analytics & Progress
    object ProgressDashboard : Screen("progress_dashboard")
    object DetailedAnalytics : Screen("detailed_analytics")

    // Gamification
    object Leaderboard : Screen("leaderboard")
    object BadgesCollection : Screen("badges")
    object DailyChallenge : Screen("daily_challenge")

    // Settings
    object Settings : Screen("settings")
    object AccountSettings : Screen("account_settings")
    object NotificationSettings : Screen("notification_settings")
    object LanguageSettings : Screen("language_settings")
    object About : Screen("about")
}

/**
 * Bottom navigation destinations
 */
enum class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: String
) {
    HOME(Screen.Home, "Home", "home"),
    PRACTICE(Screen.Practice, "Practice", "mic"),
    PATHS(Screen.LearningPaths, "Paths", "school"),
    ACHIEVEMENTS(Screen.Achievements, "Achievements", "trophy"),
    PROFILE(Screen.Profile, "Profile", "person")
}
