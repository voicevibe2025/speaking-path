package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/**
 * Achievement model representing user achievements/badges
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val iconUrl: String? = null,
    val iconEmoji: String? = null,
    val points: Int,
    val rarity: AchievementRarity,
    val isUnlocked: Boolean = false,
    val unlockedAt: LocalDateTime? = null,
    val progress: Float = 0f, // 0-100
    val maxProgress: Int = 100,
    val currentProgress: Int = 0,
    val requirements: String,
    val reward: AchievementReward? = null
)

/**
 * Achievement categories
 */
enum class AchievementCategory {
    STREAK,
    PRACTICE,
    ACCURACY,
    VOCABULARY,
    PRONUNCIATION,
    CULTURAL,
    SOCIAL,
    MILESTONE,
    SPECIAL
}

/**
 * Achievement rarity levels
 */
enum class AchievementRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * Achievement reward
 */
data class AchievementReward(
    val type: RewardType,
    val value: Int,
    val description: String
)

enum class RewardType {
    XP,
    COINS,
    STREAK_FREEZE,
    DOUBLE_XP,
    BADGE,
    TITLE
}

/**
 * User achievement statistics
 */
data class AchievementStats(
    val totalUnlocked: Int,
    val totalAvailable: Int,
    val totalPoints: Int,
    val categoryCounts: Map<AchievementCategory, Int>,
    val recentUnlocks: List<Achievement>,
    val nextToUnlock: List<Achievement>
)
