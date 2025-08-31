package com.example.voicevibe.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SpeakingJourneyApiService {
    @GET("speaking/topics")
    suspend fun getTopics(): Response<SpeakingTopicsResponse>

    @POST("speaking/topics/{topicId}/complete")
    suspend fun completeTopic(
        @Path("topicId") topicId: String
    ): Response<CompleteTopicResponse>

    @POST("speaking/topics")
    suspend fun updateLastVisitedTopic(
        @Body request: UpdateLastVisitedTopicRequest
    ): Response<UpdateLastVisitedTopicResponse>
}

data class SpeakingTopicsResponse(
    val topics: List<SpeakingTopicDto>,
    val userProfile: UserProfileDto
)

data class ConversationTurnDto(
    val speaker: String,
    val text: String
)

data class UserProfileDto(
    val firstVisit: Boolean,
    val lastVisitedTopicId: String?,
    val lastVisitedTopicTitle: String?
)

data class SpeakingTopicDto(
    val id: String,
    val title: String,
    val description: String = "",
    val material: List<String>,
    val conversation: List<ConversationTurnDto> = emptyList(),
    val unlocked: Boolean,
    val completed: Boolean
)

data class CompleteTopicResponse(
    val success: Boolean,
    val message: String,
    val completedTopicId: String,
    val unlockedTopicId: String?
)

data class UpdateLastVisitedTopicRequest(
    val lastVisitedTopicId: String
)

data class UpdateLastVisitedTopicResponse(
    val success: Boolean
)
