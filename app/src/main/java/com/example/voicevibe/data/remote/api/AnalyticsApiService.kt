package com.example.voicevibe.data.remote.api

import retrofit2.Response
import retrofit2.http.*

/**
 * API service for analytics tracking and chat mode usage
 */
interface AnalyticsApiService {
    
    /**
     * Start a new chat mode session (Text or Voice)
     */
    @POST("api/v1/analytics/chat-mode-usage/")
    suspend fun startChatSession(
        @Body request: StartChatSessionRequest
    ): Response<ChatModeUsageDto>
    
    /**
     * End a chat mode session
     */
    @POST("api/v1/analytics/chat-mode-usage/{usageId}/end_session/")
    suspend fun endChatSession(
        @Path("usageId") usageId: String
    ): Response<ChatModeUsageDto>
    
    /**
     * Increment message count for active session
     */
    @POST("api/v1/analytics/chat-mode-usage/{usageId}/increment_messages/")
    suspend fun incrementMessages(
        @Path("usageId") usageId: String,
        @Body request: IncrementMessagesRequest
    ): Response<ChatModeUsageDto>
    
    /**
     * Get overall chat mode statistics
     */
    @GET("api/v1/analytics/chat-mode-usage/stats/")
    suspend fun getChatModeStats(): Response<ChatModeStatsDto>
    
    /**
     * Get per-user chat mode statistics
     */
    @GET("api/v1/analytics/chat-mode-usage/user_stats/")
    suspend fun getUserStats(): Response<List<ChatModeUserStatsDto>>
    
    /**
     * Get currently active chat sessions
     */
    @GET("api/v1/analytics/chat-mode-usage/active_sessions/")
    suspend fun getActiveSessions(): Response<List<ChatModeUsageDto>>
    
    /**
     * Get user's chat mode usage history
     */
    @GET("api/v1/analytics/chat-mode-usage/")
    suspend fun getUserChatHistory(): Response<List<ChatModeUsageDto>>
}

// Request DTOs
data class StartChatSessionRequest(
    val mode: String,  // "text" or "voice"
    val device_info: String? = null,
    val app_version: String? = null
)

data class IncrementMessagesRequest(
    val count: Int = 1
)

// Response DTOs
data class ChatModeUsageDto(
    val usage_id: String,
    val user: Int,
    val user_id: Int,
    val user_email: String,
    val username: String,
    val display_name: String,
    val mode: String,
    val session_id: String,
    val started_at: String,
    val ended_at: String?,
    val duration_seconds: Int,
    val current_duration: Int,
    val message_count: Int,
    val is_active: Boolean,
    val device_info: String?,
    val app_version: String?,
    val created_at: String,
    val updated_at: String
)

data class ChatModeStatsDto(
    val total_sessions: Int,
    val active_sessions: Int,
    val text_chat_sessions: Int,
    val voice_chat_sessions: Int,
    val text_chat_percentage: Double,
    val voice_chat_percentage: Double,
    val average_session_duration: Double,
    val total_messages: Int,
    val unique_users: Int,
    val active_users_now: Int,
    val text_mode_stats: ModeStatsDto,
    val voice_mode_stats: ModeStatsDto,
    val today_sessions: Int,
    val this_week_sessions: Int,
    val this_month_sessions: Int
)

data class ModeStatsDto(
    val total_sessions: Int,
    val avg_duration: Double,
    val avg_messages: Double,
    val active_now: Int
)

data class ChatModeUserStatsDto(
    val user_id: Int,
    val user_email: String,
    val username: String,
    val display_name: String,
    val total_sessions: Int,
    val text_chat_count: Int,
    val voice_chat_count: Int,
    val preferred_mode: String,
    val total_duration_seconds: Int,
    val total_messages: Int,
    val last_session_at: String?,
    val is_currently_active: Boolean
)
