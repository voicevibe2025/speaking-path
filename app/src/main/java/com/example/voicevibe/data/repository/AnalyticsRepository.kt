package com.example.voicevibe.data.repository

import android.os.Build
import com.example.voicevibe.BuildConfig
import com.example.voicevibe.data.remote.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for analytics tracking
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsApi: AnalyticsApiService
) {
    
    /**
     * Start tracking a chat mode session
     */
    suspend fun startChatSession(mode: ChatMode): Result<ChatModeUsage> = withContext(Dispatchers.IO) {
        try {
            val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
            val appVersion = BuildConfig.VERSION_NAME
            
            val request = StartChatSessionRequest(
                mode = mode.value,
                device_info = deviceInfo,
                app_version = appVersion
            )
            
            val response = analyticsApi.startChatSession(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to start chat session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * End a chat mode session
     */
    suspend fun endChatSession(usageId: String): Result<ChatModeUsage> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApi.endChatSession(usageId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to end chat session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Increment message count for active session
     */
    suspend fun incrementMessageCount(usageId: String, count: Int = 1): Result<ChatModeUsage> = withContext(Dispatchers.IO) {
        try {
            val request = IncrementMessagesRequest(count)
            val response = analyticsApi.incrementMessages(usageId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to increment messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get overall chat mode statistics
     */
    suspend fun getChatModeStats(): Result<ChatModeStats> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApi.getChatModeStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to get chat mode stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get per-user chat mode statistics
     */
    suspend fun getUserStats(): Result<List<ChatModeUserStats>> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApi.getUserStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to get user stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get currently active chat sessions
     */
    suspend fun getActiveSessions(): Result<List<ChatModeUsage>> = withContext(Dispatchers.IO) {
        try {
            val response = analyticsApi.getActiveSessions()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to get active sessions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Domain models
enum class ChatMode(val value: String) {
    TEXT("text"),
    VOICE("voice")
}

data class ChatModeUsage(
    val usageId: String,
    val userId: Int,
    val userEmail: String,
    val username: String,
    val displayName: String,
    val mode: ChatMode,
    val sessionId: String,
    val startedAt: String,
    val endedAt: String?,
    val durationSeconds: Int,
    val currentDuration: Int,
    val messageCount: Int,
    val isActive: Boolean,
    val deviceInfo: String?,
    val appVersion: String?
)

data class ChatModeStats(
    val totalSessions: Int,
    val activeSessions: Int,
    val textChatSessions: Int,
    val voiceChatSessions: Int,
    val textChatPercentage: Double,
    val voiceChatPercentage: Double,
    val averageSessionDuration: Double,
    val totalMessages: Int,
    val uniqueUsers: Int,
    val activeUsersNow: Int,
    val textModeStats: ModeStats,
    val voiceModeStats: ModeStats,
    val todaySessions: Int,
    val thisWeekSessions: Int,
    val thisMonthSessions: Int
)

data class ModeStats(
    val totalSessions: Int,
    val avgDuration: Double,
    val avgMessages: Double,
    val activeNow: Int
)

data class ChatModeUserStats(
    val userId: Int,
    val userEmail: String,
    val username: String,
    val displayName: String,
    val totalSessions: Int,
    val textChatCount: Int,
    val voiceChatCount: Int,
    val preferredMode: String,
    val totalDurationSeconds: Int,
    val totalMessages: Int,
    val lastSessionAt: String?,
    val isCurrentlyActive: Boolean
)

// Extension functions to map DTOs to domain models
private fun ChatModeUsageDto.toDomain() = ChatModeUsage(
    usageId = usage_id,
    userId = user_id,
    userEmail = user_email,
    username = username,
    displayName = display_name,
    mode = if (mode == "text") ChatMode.TEXT else ChatMode.VOICE,
    sessionId = session_id,
    startedAt = started_at,
    endedAt = ended_at,
    durationSeconds = duration_seconds,
    currentDuration = current_duration,
    messageCount = message_count,
    isActive = is_active,
    deviceInfo = device_info,
    appVersion = app_version
)

private fun ChatModeStatsDto.toDomain() = ChatModeStats(
    totalSessions = total_sessions,
    activeSessions = active_sessions,
    textChatSessions = text_chat_sessions,
    voiceChatSessions = voice_chat_sessions,
    textChatPercentage = text_chat_percentage,
    voiceChatPercentage = voice_chat_percentage,
    averageSessionDuration = average_session_duration,
    totalMessages = total_messages,
    uniqueUsers = unique_users,
    activeUsersNow = active_users_now,
    textModeStats = text_mode_stats.toDomain(),
    voiceModeStats = voice_mode_stats.toDomain(),
    todaySessions = today_sessions,
    thisWeekSessions = this_week_sessions,
    thisMonthSessions = this_month_sessions
)

private fun ModeStatsDto.toDomain() = ModeStats(
    totalSessions = total_sessions,
    avgDuration = avg_duration,
    avgMessages = avg_messages,
    activeNow = active_now
)

private fun ChatModeUserStatsDto.toDomain() = ChatModeUserStats(
    userId = user_id,
    userEmail = user_email,
    username = username,
    displayName = display_name,
    totalSessions = total_sessions,
    textChatCount = text_chat_count,
    voiceChatCount = voice_chat_count,
    preferredMode = preferred_mode,
    totalDurationSeconds = total_duration_seconds,
    totalMessages = total_messages,
    lastSessionAt = last_session_at,
    isCurrentlyActive = is_currently_active
)
