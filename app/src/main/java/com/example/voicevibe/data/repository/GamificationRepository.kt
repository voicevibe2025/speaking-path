package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.GamificationApiService
import com.example.voicevibe.data.remote.api.AddExperienceRequest
import com.example.voicevibe.data.remote.api.AddExperienceResponse
import com.example.voicevibe.data.remote.api.UpdateStreakResponse

import com.example.voicevibe.data.remote.api.UserApiService
import com.example.voicevibe.data.remote.api.AchievementEventDto
import com.example.voicevibe.domain.model.GamificationStats
import com.example.voicevibe.domain.model.Badge
import com.example.voicevibe.domain.model.Achievement
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.AchievementStats
import com.example.voicevibe.domain.model.CompetitionStats
import com.example.voicevibe.domain.model.LeaderboardData
import com.example.voicevibe.domain.model.LeaderboardFilter
import com.example.voicevibe.domain.model.LeaderboardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing gamification features
 */
@Singleton
class GamificationRepository @Inject constructor(
    private val apiService: GamificationApiService,
    private val userApiService: UserApiService
) {

    /**
     * Get user's gamification stats
     */
    fun getUserStats(): Flow<Resource<GamificationStats>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserStats()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load stats: empty body"))
            } else {
                emit(Resource.Error("Failed to load stats"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get persistent achievement events feed for the current user
     */
    suspend fun getAchievementEvents(limit: Int = 50): Resource<List<AchievementEventDto>> {
        return try {
            val response = apiService.getAchievementEvents(limit)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load achievement events: empty body")
            } else {
                Resource.Error("Failed to load achievement events")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Award experience points to current user
     */
    suspend fun addExperience(points: Int, source: String): Resource<AddExperienceResponse> {
        return try {
            val response = apiService.addExperience(AddExperienceRequest(points = points, source = source))
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to add experience: empty body")
            } else {
                Resource.Error("Failed to add experience")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get user's badges
     */
    fun getUserBadges(): Flow<Resource<List<Badge>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserBadges()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load badges: empty body"))
            } else {
                emit(Resource.Error("Failed to load badges"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get user's achievements
     */
    suspend fun getUserAchievements(): Resource<List<Achievement>> {
        return try {
            val response = apiService.getUserAchievements()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load achievements: empty body")
            } else {
                Resource.Error("Failed to load achievements")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get leaderboard data
     */
    suspend fun getLeaderboard(
        type: LeaderboardType,
        filter: LeaderboardFilter,
        countryCode: String? = null,
        refresh: Boolean = false,
    ): Resource<LeaderboardData> {
        return try {
            val response = when (type) {
                LeaderboardType.DAILY -> apiService.getDailyLeaderboard(refresh)
                LeaderboardType.WEEKLY -> apiService.getWeeklyLeaderboard(refresh)
                LeaderboardType.MONTHLY -> apiService.getMonthlyLeaderboard(refresh)
                LeaderboardType.ALL_TIME -> apiService.getAllTimeLeaderboard(refresh)
                LeaderboardType.FRIENDS -> apiService.getFriendsLeaderboard()
                LeaderboardType.LINGO_LEAGUE -> apiService.getLingoLeague(category = filter.name, limit = 50)
                else -> return Resource.Error("Unsupported leaderboard type: $type")
            }

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load leaderboard: empty body")
            } else {
                Resource.Error("Failed to load leaderboard")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Claim daily reward
     */
    suspend fun claimDailyReward(): Resource<DailyReward> {
        return try {
            val response = apiService.claimDailyReward()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to claim reward: empty body")
            } else {
                Resource.Error("Failed to claim reward")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Update streak (counts today's activity). Safe to call multiple times per day.
     */
    suspend fun updateStreak(): Resource<UpdateStreakResponse> {
        return try {
            val response = apiService.updateStreak()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to update streak: empty body")
            } else {
                Resource.Error("Failed to update streak")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get achievement stats
     */
    suspend fun getAchievementStats(): Resource<AchievementStats> {
        return try {
            val response = apiService.getAchievementStats()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load achievement stats: empty body")
            } else {
                Resource.Error("Failed to load achievement stats")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Claim achievement reward
     */
    suspend fun claimAchievementReward(achievementId: String): Resource<Unit> {
        return try {
            val response = apiService.claimAchievementReward(achievementId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to claim achievement reward")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get competition stats
     */
    suspend fun getCompetitionStats(): Resource<CompetitionStats> {
        return try {
            val response = apiService.getCompetitionStats()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load competition stats: empty body")
            } else {
                Resource.Error("Failed to load competition stats")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Follow a user
     */
    suspend fun followUser(userId: String): Resource<Unit> {
        return try {
            val response = userApiService.followUser(userId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to follow user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}

/**
 * Data class for daily reward
 */
data class DailyReward(
    val points: Int,
    val streakBonus: Int,
    val totalPoints: Int,
    val newStreak: Int,
    val message: String
)
