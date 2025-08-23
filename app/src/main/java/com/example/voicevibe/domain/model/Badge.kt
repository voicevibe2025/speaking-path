package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/**
 * Badge model representing user badges/titles
 */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: String,
    val category: BadgeCategory,
    val rarity: BadgeRarity,
    val isUnlocked: Boolean = false,
    val unlockedAt: LocalDateTime? = null,
    val requirements: String? = null,
    val progress: Float = 0f,
    val maxProgress: Int = 100
)

/**
 * Badge categories
 */
enum class BadgeCategory {
    STREAK,
    ACCURACY,
    PRACTICE,
    VOCABULARY,
    PRONUNCIATION,
    CULTURAL,
    SOCIAL,
    MILESTONE,
    SPECIAL
}

/**
 * Badge rarity levels
 */
enum class BadgeRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * Overall gamification statistics for user
 */
data class GamificationStats(
    val userId: String,
    val totalXp: Int,
    val level: Int,
    val currentLevelXp: Int,
    val nextLevelXp: Int,
    val streakDays: Int,
    val longestStreak: Int,
    val totalPracticeTime: Int, // in minutes
    val accuracyRate: Float, // 0.0 to 1.0
    val totalLessonsCompleted: Int,
    val totalAchievements: Int,
    val totalBadges: Int,
    val weeklyXp: Int,
    val monthlyXp: Int,
    val rank: Int,
    val coins: Int,
    val streakFreezes: Int,
    val doubleXpDays: Int,
    val lastActiveDate: LocalDateTime? = null,
    val joinDate: LocalDateTime,
    val league: String? = null,
    val leagueRank: Int? = null
)
