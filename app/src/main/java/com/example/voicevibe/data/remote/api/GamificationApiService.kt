package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.repository.DailyReward
import com.example.voicevibe.domain.model.Achievement
import com.example.voicevibe.domain.model.AchievementStats
import com.example.voicevibe.domain.model.Badge
import com.example.voicevibe.domain.model.CompetitionStats
import com.example.voicevibe.domain.model.GamificationStats
import com.example.voicevibe.domain.model.LeaderboardData
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for gamification features
 */
interface GamificationApiService {

    @GET("gamification/stats")
    suspend fun getUserStats(): Response<GamificationStats>

    @GET("gamification/badges")
    suspend fun getUserBadges(): Response<List<Badge>>

    @GET("gamification/achievements")
    suspend fun getUserAchievements(): Response<List<Achievement>>

    @GET("gamification/leaderboards/weekly")
    suspend fun getWeeklyLeaderboard(@Query("refresh") refresh: Boolean = false): Response<LeaderboardData>

    @GET("gamification/leaderboards/daily")
    suspend fun getDailyLeaderboard(@Query("refresh") refresh: Boolean = false): Response<LeaderboardData>

    @GET("gamification/leaderboards/monthly")
    suspend fun getMonthlyLeaderboard(@Query("refresh") refresh: Boolean = false): Response<LeaderboardData>

    @GET("gamification/leaderboards/all_time")
    suspend fun getAllTimeLeaderboard(@Query("refresh") refresh: Boolean = false): Response<LeaderboardData>

    @GET("gamification/leaderboards/friends")
    suspend fun getFriendsLeaderboard(): Response<LeaderboardData>

    @GET("gamification/achievements/stats")
    suspend fun getAchievementStats(): Response<AchievementStats>

    @POST("gamification/achievements/{id}/claim")
    suspend fun claimAchievementReward(
        @Path("id") achievementId: String
    ): Response<Unit>

    @GET("gamification/competition/stats")
    suspend fun getCompetitionStats(): Response<CompetitionStats>

    @POST("gamification/daily-reward")
    suspend fun claimDailyReward(): Response<DailyReward>

    @POST("gamification/streak")
    suspend fun updateStreak(): Response<Int>

    // --- XP Awards ---
    @POST("gamification/user-levels/add_experience/")
    suspend fun addExperience(
        @Body body: AddExperienceRequest
    ): Response<AddExperienceResponse>

    // Challenge endpoints (kept for future use)
    @GET("gamification/challenges")
    suspend fun getChallenges(): Response<List<Challenge>>

    @POST("gamification/challenges/{id}/join")
    suspend fun joinChallenge(@Path("id") challengeId: String): Response<Challenge>

    @POST("gamification/challenges/{id}/complete")
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

// ---- XP Award DTOs ----
data class AddExperienceRequest(
    val points: Int,
    val source: String
)

data class AddExperienceResponse(
    val success: Boolean,
    val points_added: Int? = null,
    val current_level: Int? = null,
    val experience_points: Int? = null,
    val wayang_character: String? = null,
    val leveled_up: Boolean? = null
)
