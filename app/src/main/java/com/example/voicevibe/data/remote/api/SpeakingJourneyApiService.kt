package com.example.voicevibe.data.remote.api

import retrofit2.Response
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
}

data class SpeakingTopicsResponse(
    val topics: List<SpeakingTopicDto>
)

data class SpeakingTopicDto(
    val id: String,
    val title: String,
    val material: List<String>,
    val unlocked: Boolean,
    val completed: Boolean
)

data class CompleteTopicResponse(
    val success: Boolean,
    val message: String,
    val completedTopicId: String,
    val unlockedTopicId: String?
)
