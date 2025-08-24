package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.repository.PracticeStatistics
import com.example.voicevibe.domain.model.PracticePrompt
import com.example.voicevibe.domain.model.SpeakingEvaluation
import com.example.voicevibe.domain.model.SpeakingSession
import com.example.voicevibe.presentation.screens.practice.speaking.SubmissionResult
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for speaking practice operations
 */
interface SpeakingPracticeApiService {

    @GET("practice/prompts/random")
    suspend fun getRandomPrompt(): Response<PracticePrompt>

    @GET("practice/prompts/category/{category}")
    suspend fun getPromptsByCategory(
        @Path("category") category: String
    ): Response<List<PracticePrompt>>

    @GET("practice/prompts/{id}")
    suspend fun getPromptById(
        @Path("id") promptId: String
    ): Response<PracticePrompt>

    @GET("practice/prompts")
    suspend fun getAllPrompts(
        @Query("difficulty") difficulty: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<PracticePrompt>>

    @Multipart
    @POST("practice/sessions/submit/{promptId}")
    suspend fun submitRecording(
        @Path("promptId") promptId: String,
        @Part audio: MultipartBody.Part
    ): Response<SubmissionResult>

    @GET("practice/sessions/{id}")
    suspend fun getSession(
        @Path("id") sessionId: String
    ): Response<SpeakingSession>

    @GET("practice/sessions")
    suspend fun getUserSessions(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<SpeakingSession>>

    @GET("practice/sessions/{id}/evaluation")
    suspend fun getEvaluation(
        @Path("id") sessionId: String
    ): Response<SpeakingEvaluation>

    @DELETE("practice/sessions/{id}")
    suspend fun deleteSession(
        @Path("id") sessionId: String
    ): Response<Unit>

    @GET("practice/statistics")
    suspend fun getPracticeStats(): Response<PracticeStatistics>

    @POST("practice/sessions/{id}/retry")
    suspend fun retryEvaluation(
        @Path("id") sessionId: String
    ): Response<SpeakingEvaluation>

    @GET("practice/sessions/history")
    suspend fun getSessionHistory(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<SpeakingSession>>

    @GET("practice/progress")
    suspend fun getPracticeProgress(): Response<PracticeProgress>

    @POST("practice/feedback/{sessionId}")
    suspend fun submitFeedback(
        @Path("sessionId") sessionId: String,
        @Body feedback: UserFeedback
    ): Response<Unit>
}

/**
 * Practice progress data
 */
data class PracticeProgress(
    val totalSessions: Int,
    val currentStreak: Int,
    val weeklyGoalProgress: Float,
    val improvementRate: Float,
    val nextMilestone: String
)

/**
 * User feedback for evaluation
 */
data class UserFeedback(
    val rating: Int,
    val comment: String?,
    val wasHelpful: Boolean
)
