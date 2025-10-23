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
    object SelectEnglishLevel : Screen("select_english_level")

    // Main screens
    object Home : Screen("home")
    object Practice : Screen("practice")
    object LearningPaths : Screen("learning_paths")
    object Achievements : Screen("achievements")
    object Profile : Screen("profile")
    // Social feed dedicated screen
    object SocialFeed : Screen("social_feed?postId={postId}&commentId={commentId}") {
        fun createRoute(postId: Int? = null, commentId: Int? = null): String {
            val base = "social_feed"
            val query = buildList {
                if (postId != null) add("postId=$postId")
                if (commentId != null) add("commentId=$commentId")
            }.joinToString("&")
            return if (query.isNotEmpty()) "$base?$query" else base
        }
    }
    object Notifications : Screen("notifications")
    // User search screen
    object UserSearch : Screen("user_search")
    object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object FollowersFollowing : Screen("followers_following?userId={userId}&tab={tab}") {
        fun createRoute(userId: String? = null, tab: Int = 0): String {
            val base = "followers_following"
            val params = buildList {
                if (userId != null) add("userId=$userId")
                add("tab=$tab")
            }.joinToString("&")
            return "$base?$params"
        }
    }
    // Messaging
    object Messages : Screen("messages")
    object Conversation : Screen("conversation?conversationId={conversationId}&userId={userId}") {
        fun createRouteWithConversation(conversationId: Int) = "conversation?conversationId=$conversationId"
        fun createRouteWithUser(userId: String) = "conversation?userId=$userId"
    }
    // Speaking-only flow (beta)
    object SpeakingJourney : Screen("speaking_journey")
    object SpeakingLesson : Screen("speaking_lesson/{topicId}") {
        fun createRoute(topicId: String) = "speaking_lesson/$topicId"
    }
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
    object ConversationPractice : Screen("conversation_practice/{topicId}") {
        fun createRoute(topicId: String) = "conversation_practice/$topicId"
    }
    object VocabularyLesson : Screen("vocabulary_lesson/{topicId}") {
        fun createRoute(topicId: String) = "vocabulary_lesson/$topicId"
    }
    object TopicVocabulary : Screen("topic_vocabulary/{topicId}") {
        fun createRoute(topicId: String) = "topic_vocabulary/$topicId"
    }
    object LearnTopicWithVivi : Screen("learn_topic_with_vivi/{topicId}") {
        fun createRoute(topicId: String) = "learn_topic_with_vivi/$topicId"
    }
    // Practice with AI standalone screen
    object PracticeWithAI : Screen("practice_with_ai")
    object LivePractice : Screen("live_practice")

    // Topic Practice (AI-guided chat limited to Speaking Journey topics)
    object TopicPractice : Screen("topic_practice")
    object TopicPracticeChat : Screen("topic_practice_chat/{topicId}") {
        fun createRoute(topicId: String) = "topic_practice_chat/$topicId"
    }

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
    object LingoLeague : Screen("lingo_league")
    object TopicSelection : Screen("topic_selection")
    object TopicLeaderboard : Screen("topic_leaderboard/{topicId}") {
        fun createRoute(topicId: String) = "topic_leaderboard/$topicId"
    }
    object BadgesCollection : Screen("badges")
    object DailyChallenge : Screen("daily_challenge")

    // Settings
    object Settings : Screen("settings")
    object AccountSettings : Screen("account_settings")
    object EditProfile : Screen("edit_profile")
    object NotificationSettings : Screen("notification_settings")
    object LanguageSettings : Screen("language_settings")
    object About : Screen("about")
    object QA : Screen("qa")
    object ImageCrop : Screen("image_crop/{imageUri}") {
        fun createRoute(imageUri: String) = "image_crop/${imageUri}"
    }
    
    // Privacy & Safety
    object PrivacySettings : Screen("privacy_settings")
    object BlockedUsers : Screen("blocked_users")
    object MyReports : Screen("my_reports")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService : Screen("terms_of_service")
    object CommunityGuidelines : Screen("community_guidelines")
    
    // Groups (Collectivism Feature)
    object GroupSelection : Screen("group_selection")
    object MyGroup : Screen("my_group")
    object GroupProfile : Screen("group_profile/{groupId}") {
        fun createRoute(groupId: Int) = "group_profile/$groupId"
    }

    // WordUp Vocabulary Feature & Story Time
    object WordUp : Screen("wordup")
    object StoryList : Screen("story_list")
    object StoryTime : Screen("story_time/{storySlug}") {
        fun createRoute(storySlug: String) = "story_time/$storySlug"
    }
    object MasteredWords : Screen("mastered_words")
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
    GROUP(Screen.MyGroup, "Group", "people"),
    ACHIEVEMENTS(Screen.Achievements, "Achievements", "trophy"),
    PROFILE(Screen.Profile, "Profile", "person")
}
