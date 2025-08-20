package com.example.voicevibe.domain.model

import java.util.Date

/**
 * Data class representing a learning path
 */
data class LearningPath(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val category: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Int,
    val estimatedDuration: Int, // in minutes
    val imageUrl: String? = null,
    val isActive: Boolean = false,
    val lastAccessedAt: Date? = null,
    val createdAt: Date,
    val updatedAt: Date
) {
    enum class Difficulty {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }
}

/**
 * Data class for user progress
 */
data class UserProgress(
    val userId: String,
    val level: Int,
    val experiencePoints: Int,
    val nextLevelPoints: Int,
    val totalPracticeTime: Int, // in minutes
    val totalSessions: Int,
    val averageAccuracy: Float,
    val strongAreas: List<String>,
    val improvementAreas: List<String>,
    val lastPracticeDate: Date?
)

/**
 * Data class for gamification stats
 */
data class GamificationStats(
    val userId: String,
    val totalPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val badges: List<Badge>,
    val recentBadges: List<String>,
    val rank: Int,
    val percentile: Float,
    val achievements: List<Achievement>
)

/**
 * Data class for badges
 */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val earnedAt: Date,
    val rarity: BadgeRarity
) {
    enum class BadgeRarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY
    }
}

/**
 * Data class for achievements
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val rewardPoints: Int,
    val isCompleted: Boolean,
    val completedAt: Date?
)
