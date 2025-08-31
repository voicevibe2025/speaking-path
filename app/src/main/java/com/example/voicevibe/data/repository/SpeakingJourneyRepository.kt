package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.SpeakingJourneyApiService
import com.example.voicevibe.data.remote.api.SpeakingTopicDto
import com.example.voicevibe.data.remote.api.SpeakingTopicsResponse
import com.example.voicevibe.data.remote.api.UpdateLastVisitedTopicRequest
import com.example.voicevibe.data.remote.api.UserProfileDto
import javax.inject.Inject

class SpeakingJourneyRepository @Inject constructor(
    private val api: SpeakingJourneyApiService
) {
    suspend fun getTopics(): Result<SpeakingTopicsResponse> {
        return try {
            val res = api.getTopics()
            if (res.isSuccessful) {
                Result.success(res.body() ?: SpeakingTopicsResponse(emptyList(), 
                    UserProfileDto(true, null, null)))
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun completeTopic(topicId: String): Result<Unit> {
        return try {
            val res = api.completeTopic(topicId)
            if (res.isSuccessful && (res.body()?.success == true)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun updateLastVisitedTopic(topicId: String): Result<Unit> {
        return try {
            val res = api.updateLastVisitedTopic(UpdateLastVisitedTopicRequest(topicId))
            if (res.isSuccessful && (res.body()?.success == true)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
