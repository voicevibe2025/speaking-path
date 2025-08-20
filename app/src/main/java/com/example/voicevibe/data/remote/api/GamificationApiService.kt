package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.repository.DailyReward
import com.example.voicevibe.data.repository.LeaderboardEntry
import com.example.voicevibe.domain.model.Achievement
import com.example.voicevibe.domain.model.Badge
import com.example.voicevibe.domain.model.GamificationStats
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for gamification features
 */
interface GamificationApiService {

    @GET("api/gamification/stats")
    suspend fun getUserStats(): Response<GamificationStats>

    @GET("api/gamification/badges")
    suspend fun getUserBadges(): Response<List<Badge>>

    @GET("api/gamification/achievements")
    suspend fun getUserAchievements(): Response<List<Achievement>>

    @GET("api/gamification/leaderboard")
    suspend fun getLeaderboard(
        @Query("timeFrame") timeFrame: String = "weekly"
    ): Response<List<LeaderboardEntry>>

    @POST("api/gamification/daily-reward")
    suspend fun claimDailyReward(): Response<DailyReward>

    @POST("api/gamification/streak")
    suspend fun updateStreak(): Response<Int>

    @GET("api/gamification/leaderboard/friends")
    suspend fun getFriendsLeaderboard(): Response<List<LeaderboardEntry>>

    @GET("api/gamification/challenges")
    suspend fun getChallenges(): Response<List<Challenge>>

    @POST("api/gamification/challenges/{id}/join")
    suspend fun joinChallenge(@Path("id") challengeId: String): Response<Challenge>

    @POST("api/gamification/challenges/{id}/complete")
    suspend fun completeChallenge(
        @Path("id") challengeId: String,
        @Body result: Map<String, Any>
    ): Response<ChallengeResult>
}

/**
 * Data class for challenge
 */
data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val startDate: String,
    val endDate: String,
    val targetValue: Int,
    val currentValue: Int,
    val reward: ChallengeReward,
    val participants: Int,
    val isJoined: Boolean,
    val isCompleted: Boolean
) {
    enum class ChallengeType {
        DAILY,
        WEEKLY,
        MONTHLY,
        SPECIAL
    }
}

/**
 * Data class for challenge reward
 */
data class ChallengeReward(
    val points: Int,
    val badgeId: String?,
    val title: String?
)

/**
 * Data class for challenge result
 */
data class ChallengeResult(
    val challengeId: String,
    val completed: Boolean,
    val score: Int,
    val rank: Int,
    val rewardEarned: ChallengeReward,
    val message: String
)
