package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.GamificationApiService
import com.example.voicevibe.domain.model.GamificationStats
import com.example.voicevibe.domain.model.Badge
import com.example.voicevibe.domain.model.Achievement
import com.example.voicevibe.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing gamification features
 */
@Singleton
class GamificationRepository @Inject constructor(
    private val apiService: GamificationApiService
) {

    /**
     * Get user's gamification stats
     */
    fun getUserStats(): Flow<Resource<GamificationStats>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserStats()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()))
            } else {
                emit(Resource.Error("Failed to load stats"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
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
                emit(Resource.Success(response.body()))
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
    fun getUserAchievements(): Flow<Resource<List<Achievement>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserAchievements()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()))
            } else {
                emit(Resource.Error("Failed to load achievements"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get leaderboard data
     */
    fun getLeaderboard(timeFrame: String = "weekly"): Flow<Resource<List<LeaderboardEntry>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getLeaderboard(timeFrame)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()))
            } else {
                emit(Resource.Error("Failed to load leaderboard"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Claim daily reward
     */
    suspend fun claimDailyReward(): Resource<DailyReward> {
        return try {
            val response = apiService.claimDailyReward()
            if (response.isSuccessful) {
                Resource.Success(response.body())
            } else {
                Resource.Error("Failed to claim reward")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Update streak
     */
    suspend fun updateStreak(): Resource<Int> {
        return try {
            val response = apiService.updateStreak()
            if (response.isSuccessful) {
                Resource.Success(response.body())
            } else {
                Resource.Error("Failed to update streak")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}

/**
 * Data class for leaderboard entry
 */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val points: Int,
    val level: Int,
    val badges: Int,
    val isCurrentUser: Boolean = false
)

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
