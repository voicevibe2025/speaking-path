package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/**
 * Data class representing a learning path
 */
data class LearningPath(
    val id: String,
    val title: String,
    val description: String,
    val category: PathCategory,
    val difficulty: DifficultyLevel,
    val thumbnailUrl: String?,
    val duration: Int, // in hours
    val modules: List<LearningModule>,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Float, // 0.0 to 1.0
    val isEnrolled: Boolean,
    val enrolledCount: Int,
    val rating: Float,
    val ratingCount: Int,
    val instructor: PathInstructor?,
    val tags: List<String>,
    val estimatedCompletionDays: Int,
    val prerequisites: List<String>,
    val skillsToGain: List<String>,
    val certificateAvailable: Boolean,
    val isPremium: Boolean,
    val isRecommended: Boolean,
    val lastAccessedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val nextLesson: LessonInfo?
)

/**
 * Data class for a learning module
 */
data class LearningModule(
    val id: String,
    val pathId: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val lessons: List<Lesson>,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Float,
    val duration: Int, // in minutes
    val isLocked: Boolean,
    val unlockRequirements: String?,
    val moduleType: ModuleType
)

/**
 * Data class for a lesson
 */
data class Lesson(
    val id: String,
    val moduleId: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val type: LessonType,
    val duration: Int, // in minutes
    val content: LessonContent,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime?,
    val score: Int?, // percentage score if applicable
    val attempts: Int,
    val isLocked: Boolean,
    val unlockRequirements: String?,
    val xpReward: Int,
    val resources: List<LessonResource>,
    val practicePrompts: List<PracticePrompt>?
)

/**
 * Data class for lesson content
 */
data class LessonContent(
    val videoUrl: String?,
    val audioUrl: String?,
    val textContent: String?,
    val interactiveElements: List<InteractiveElement>?,
    val quizQuestions: List<QuizQuestion>?
)

/**
 * Data class for interactive elements
 */
data class InteractiveElement(
    val id: String,
    val type: InteractiveType,
    val content: String,
    val correctResponse: String?,
    val hints: List<String>
)

/**
 * Data class for quiz questions
 */
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val points: Int
)

/**
 * Data class for practice prompts
 */
data class PracticePrompt(
    val id: String,
    val text: String,
    val difficulty: DifficultyLevel,
    val targetWords: List<String>,
    val tips: List<String>,
    val exampleResponse: String?
)

/**
 * Data class for lesson resources
 */
data class LessonResource(
    val id: String,
    val title: String,
    val type: ResourceType,
    val url: String,
    val description: String?
)

/**
 * Data class for path instructors
 */
data class PathInstructor(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val title: String,
    val bio: String?,
    val rating: Float,
    val studentsCount: Int
)

/**
 * Data class for lesson info
 */
data class LessonInfo(
    val lessonId: String,
    val moduleId: String,
    val title: String,
    val moduleTitle: String
)

/**
 * Data class for user path progress
 */
data class UserPathProgress(
    val pathId: String,
    val userId: String,
    val enrolledAt: LocalDateTime,
    val lastAccessedAt: LocalDateTime,
    val completedModules: List<String>,
    val completedLessons: List<String>,
    val currentStreak: Int,
    val totalTimeSpent: Int, // in minutes
    val averageScore: Float,
    val notes: List<PathNote>,
    val bookmarkedLessons: List<String>
)

/**
 * Data class for path notes
 */
data class PathNote(
    val id: String,
    val lessonId: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Data class for path recommendations
 */
data class PathRecommendation(
    val path: LearningPath,
    val reason: String,
    val matchScore: Float,
    val basedOn: RecommendationType
)

/**
 * Data class for learning streaks
 */
data class LearningStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActivityDate: LocalDateTime,
    val streakHistory: List<StreakDay>
)

/**
 * Data class for streak days
 */
data class StreakDay(
    val date: LocalDateTime,
    val lessonsCompleted: Int,
    val minutesLearned: Int,
    val xpEarned: Int
)

// Enums
/**
 * Enum for path categories
 */
enum class PathCategory {
    BEGINNER_BASICS,
    PRONUNCIATION,
    GRAMMAR,
    VOCABULARY,
    CONVERSATION,
    BUSINESS_ENGLISH,
    ACADEMIC_ENGLISH,
    CULTURAL_FLUENCY,
    ACCENT_TRAINING,
    PUBLIC_SPEAKING,
    INTERVIEW_PREP,
    TOEFL_PREP,
    IELTS_PREP
}

/**
 * Enum for difficulty levels
 */
enum class DifficultyLevel {
    BEGINNER,
    ELEMENTARY,
    INTERMEDIATE,
    UPPER_INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Enum for module types
 */
enum class ModuleType {
    THEORY,
    PRACTICE,
    ASSESSMENT,
    PROJECT,
    REVIEW
}

/**
 * Enum for lesson types
 */
enum class LessonType {
    VIDEO,
    AUDIO,
    READING,
    SPEAKING_PRACTICE,
    QUIZ,
    INTERACTIVE,
    LIVE_SESSION,
    ASSIGNMENT
}

/**
 * Enum for interactive types
 */
enum class InteractiveType {
    FILL_IN_BLANK,
    DRAG_DROP,
    MATCHING,
    PRONUNCIATION,
    ROLE_PLAY
}

/**
 * Enum for resource types
 */
enum class ResourceType {
    PDF,
    VIDEO,
    AUDIO,
    DOCUMENT,
    EXTERNAL_LINK,
    WORKSHEET
}

/**
 * Enum for recommendation types
 */
enum class RecommendationType {
    SKILL_BASED,
    GOAL_BASED,
    PERFORMANCE_BASED,
    POPULAR,
    AI_SUGGESTED
}

// Filter and Sort options
/**
 * Data class for path filters
 */
data class PathFilters(
    val categories: List<PathCategory> = emptyList(),
    val difficulties: List<DifficultyLevel> = emptyList(),
    val duration: DurationFilter? = null,
    val isPremium: Boolean? = null,
    val isEnrolled: Boolean? = null,
    val hasCertificate: Boolean? = null
)

/**
 * Data class for duration filters
 */
data class DurationFilter(
    val min: Int,
    val max: Int
)

/**
 * Enum for path sort options
 */
enum class PathSortOption {
    RECOMMENDED,
    POPULARITY,
    RATING,
    NEWEST,
    DURATION_SHORT,
    DURATION_LONG,
    DIFFICULTY_LOW,
    DIFFICULTY_HIGH
}
