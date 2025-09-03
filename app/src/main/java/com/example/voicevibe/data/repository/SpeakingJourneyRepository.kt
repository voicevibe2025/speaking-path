package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.SpeakingJourneyApiService
import com.example.voicevibe.data.remote.api.SpeakingTopicDto
import com.example.voicevibe.data.remote.api.SpeakingTopicsResponse
import com.example.voicevibe.data.remote.api.UpdateLastVisitedTopicRequest
import com.example.voicevibe.data.remote.api.UserProfileDto
import com.example.voicevibe.data.remote.api.PhraseSubmissionResultDto
import com.example.voicevibe.data.remote.api.UserPhraseRecordingDto
import com.example.voicevibe.data.remote.api.UserPhraseRecordingsResponseDto
import com.example.voicevibe.data.remote.api.GenerateTtsRequestDto
import com.example.voicevibe.data.remote.api.GenerateTtsResponseDto
import okhttp3.MultipartBody
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

    suspend fun submitPhraseRecording(
        topicId: String,
        phraseIndex: Int,
        audioFile: MultipartBody.Part
    ): Result<PhraseSubmissionResultDto> {
        return try {
            val res = api.submitPhraseRecording(topicId, phraseIndex, audioFile)
            if (res.isSuccessful) {
                Result.success(res.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getUserPhraseRecordings(
        topicId: String,
        phraseIndex: Int? = null
    ): Result<List<UserPhraseRecordingDto>> {
        return try {
            val res = api.getUserPhraseRecordings(topicId, phraseIndex)
            if (res.isSuccessful) {
                val body = res.body()
                Result.success(body?.recordings ?: emptyList())
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun generateTts(text: String, voiceName: String? = null): Result<GenerateTtsResponseDto> {
        return try {
            val res = api.generateTts(GenerateTtsRequestDto(text = text, voiceName = voiceName))
            if (res.isSuccessful) {
                val body = res.body() ?: return Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
