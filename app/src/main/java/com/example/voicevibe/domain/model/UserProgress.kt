package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/**
 * User progress tracking for learning journey
 */
data class UserProgress(
    val userId: String,
    val overallProgress: OverallProgress,
    val learningPaths: List<LearningPathProgress>,
    val skillProgress: SkillProgress,
    val practiceHistory: PracticeHistory,
    val achievements: List<String>, // Achievement IDs
    val currentGoals: List<LearningGoal>,
    val weakAreas: List<WeakArea>,
    val recommendations: List<String>,
    val lastUpdated: LocalDateTime
)

/**
 * Overall learning progress statistics
 */
data class OverallProgress(
    val totalXp: Int,
    val currentLevel: Int,
    val xpToNextLevel: Int,
    val completionPercentage: Float, // 0.0 to 1.0
    val totalLessonsCompleted: Int,
    val totalPracticeMinutes: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val averageAccuracy: Float,
    val overallScore: Float
)

/**
 * Progress for individual learning paths
 */
data class LearningPathProgress(
    val pathId: String,
    val pathName: String,
    val completedModules: Int,
    val totalModules: Int,
    val completedLessons: Int,
    val totalLessons: Int,
    val progress: Float, // 0.0 to 1.0
    val currentModuleId: String?,
    val currentLessonId: String?,
    val estimatedCompletionDays: Int?,
    val startedAt: LocalDateTime,
    val lastAccessedAt: LocalDateTime?
)

/**
 * Progress in different language skills
 */
data class SkillProgress(
    val pronunciation: SkillLevel,
    val vocabulary: SkillLevel,
    val grammar: SkillLevel,
    val listening: SkillLevel,
    val speaking: SkillLevel,
    val cultural: SkillLevel
)

/**
 * Individual skill level and progress
 */
data class SkillLevel(
    val level: Int,
    val experience: Int,
    val maxExperience: Int,
    val accuracy: Float,
    val practiceCount: Int,
    val improvement: Float, // percentage improvement over time
    val lastPracticedAt: LocalDateTime?
)

/**
 * Practice session history
 */
data class PracticeHistory(
    val totalSessions: Int,
    val totalMinutes: Int,
    val averageSessionLength: Int,
    val weeklyStats: WeeklyStats,
    val monthlyStats: MonthlyStats,
    val recentSessions: List<PracticeSessionSummary>
)

/**
 * Weekly practice statistics
 */
data class WeeklyStats(
    val sessionsThisWeek: Int,
    val minutesThisWeek: Int,
    val xpThisWeek: Int,
    val averageAccuracyThisWeek: Float,
    val daysActiveMet: Int,
    val goalMet: Boolean
)

/**
 * Monthly practice statistics
 */
data class MonthlyStats(
    val sessionsThisMonth: Int,
    val minutesThisMonth: Int,
    val xpThisMonth: Int,
    val averageAccuracyThisMonth: Float,
    val daysActiveThisMonth: Int,
    val improvement: Float
)

/**
 * Summary of a practice session
 */
data class PracticeSessionSummary(
    val sessionId: String,
    val type: PracticeType,
    val duration: Int, // in minutes
    val accuracy: Float,
    val xpEarned: Int,
    val lessonsCompleted: Int,
    val timestamp: LocalDateTime
)

/**
 * Types of practice sessions
 */
enum class PracticeType {
    LESSON,
    PRONUNCIATION,
    VOCABULARY,
    CULTURAL_SCENARIO,
    CHALLENGE,
    REVIEW
}

/**
 * Learning goal tracking
 */
data class LearningGoal(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val unit: String, // "minutes", "lessons", "accuracy", etc.
    val deadline: LocalDateTime?,
    val priority: GoalPriority,
    val isCompleted: Boolean,
    val createdAt: LocalDateTime
)

/**
 * Goal priority levels
 */
enum class GoalPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

/**
 * Areas where user needs improvement
 */
data class WeakArea(
    val skill: String,
    val category: String,
    val severity: Float, // 0.0 to 1.0, higher means more critical
    val accuracy: Float,
    val practiceCount: Int,
    val lastPracticedAt: LocalDateTime?,
    val suggestedActions: List<String>
)
