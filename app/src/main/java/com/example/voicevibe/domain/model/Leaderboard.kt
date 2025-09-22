package com.example.voicevibe.domain.model

import java.time.LocalDateTime
import com.example.voicevibe.domain.model.RewardType

/**
 * Leaderboard entry representing a user's position
 */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val score: Int,
    val level: Int,
    val streakDays: Int = 0,
    val country: String? = null,
    val countryCode: String? = null,
    val isCurrentUser: Boolean = false,
    val change: RankChange = RankChange.NONE,
    val achievements: Int = 0,
    val weeklyXp: Int = 0,
    val monthlyXp: Int = 0,
    val badge: UserBadge? = null
)

/**
 * Rank change indicator
 */
enum class RankChange {
    UP,
    DOWN,
    NONE,
    NEW
}

/**
 * User badge/title
 */
data class UserBadge(
    val id: String,
    val name: String,
    val icon: String,
    val color: String
)

/**
 * Leaderboard types
 */
enum class LeaderboardType {
    GLOBAL,
    COUNTRY,
    FRIENDS,
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME,
    LINGO_LEAGUE
}

/**
 * Leaderboard filter
 */
enum class LeaderboardFilter {
    OVERALL_XP,
    DAILY_XP,
    WEEKLY_XP,
    MONTHLY_XP,
    STREAK,
    ACCURACY,
    PRACTICE_TIME,
    ACHIEVEMENTS,
    PRONUNCIATION,
    FLUENCY,
    VOCABULARY,
    TOPICS_COMPLETED
}

/**
 * Leaderboard data
 */
data class LeaderboardData(
    val type: LeaderboardType,
    val filter: LeaderboardFilter,
    val entries: List<LeaderboardEntry>,
    val currentUserEntry: LeaderboardEntry? = null,
    val lastUpdated: LocalDateTime,
    val totalParticipants: Int
)

/**
 * League/Division information
 */
data class League(
    val id: String,
    val name: String,
    val tier: LeagueTier,
    val iconUrl: String? = null,
    val color: String,
    val minScore: Int,
    val maxScore: Int,
    val rewards: List<LeagueReward>
)

enum class LeagueTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND,
    MASTER,
    GRANDMASTER
}

/**
 * League reward
 */
data class LeagueReward(
    val position: Int,
    val type: RewardType,
    val amount: Int,
    val description: String
)

/**
 * User competition stats
 */
data class CompetitionStats(
    val currentLeague: League,
    val currentRank: Int,
    val pointsToNextLeague: Int,
    val daysUntilReset: Int,
    val personalBestRank: Int,
    val totalCompetitionsWon: Int,
    val weeklyProgress: Float,
    val topPerformers: List<LeaderboardEntry>
)
