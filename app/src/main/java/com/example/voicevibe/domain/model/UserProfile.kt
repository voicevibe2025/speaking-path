package com.example.voicevibe.domain.model

import java.time.LocalDateTime
import com.example.voicevibe.domain.model.DifficultyLevel

/**
 * User profile information
 */
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val level: Int,
    val xp: Int,
    val totalXp: Int = 0,
    val xpToNextLevel: Int,
    val streakDays: Int,
    val longestStreak: Int,
    val joinedDate: LocalDateTime,
    val lastActiveDate: LocalDateTime,
    val country: String? = null,
    val countryCode: String? = null,
    val language: String,
    val timezone: String? = null,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val isOnline: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false,
    val isBlocked: Boolean = false,
    val stats: UserStats,
    val badges: List<UserBadge>,
    val preferences: UserPreferences,
    val socialLinks: SocialLinks? = null
)

/**
 * User statistics
 */
data class UserStats(
    val totalPracticeSessions: Int,
    val totalPracticeMinutes: Int,
    val averageAccuracy: Float,
    val averageFluency: Float,
    val completedLessons: Int,
    val achievementsUnlocked: Int,
    val followersCount: Int,
    val followingCount: Int,
    val globalRank: Int? = null,
    val weeklyXp: Int,
    val monthlyXp: Int,
    val totalWords: Int,
    val improvementRate: Float
)

/**
 * User preferences
 */
data class UserPreferences(
    val dailyGoalMinutes: Int = 15,
    val practiceRemindersEnabled: Boolean = true,
    val reminderTime: String? = null,
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val autoPlayAudio: Boolean = true,
    val showPronunciationGuide: Boolean = true,
    val difficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE,
    val focusAreas: List<String> = emptyList(),
    val privacy: PrivacySettings
)

/**
 * Privacy settings
 */
data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val showOnlineStatus: Boolean = true,
    val showAchievements: Boolean = true,
    val showStatistics: Boolean = true,
    val allowFriendRequests: Boolean = true,
    val allowMessages: Boolean = true,
    val allowChallenges: Boolean = true
)

enum class ProfileVisibility {
    PUBLIC,
    FRIENDS_ONLY,
    PRIVATE
}

/**
 * Social media links
 */
data class SocialLinks(
    val website: String? = null,
    val twitter: String? = null,
    val linkedin: String? = null,
    val instagram: String? = null,
    val youtube: String? = null
)

/**
 * User activity
 */
data class UserActivity(
    val id: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val timestamp: LocalDateTime,
    val xpEarned: Int? = null,
    val achievementId: String? = null,
    val sessionId: String? = null
)

enum class ActivityType {
    PRACTICE_SESSION,
    LESSON_COMPLETED,
    ACHIEVEMENT_UNLOCKED,
    LEVEL_UP,
    STREAK_MILESTONE,
    CHALLENGE_COMPLETED,
    FRIEND_ADDED,
    BADGE_EARNED
}

/**
 * Friend request
 */
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUsername: String,
    val fromDisplayName: String,
    val fromAvatarUrl: String? = null,
    val toUserId: String,
    val message: String? = null,
    val sentAt: LocalDateTime,
    val status: FriendRequestStatus
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
