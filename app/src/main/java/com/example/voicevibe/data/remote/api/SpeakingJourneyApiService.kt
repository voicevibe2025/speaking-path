package com.example.voicevibe.data.remote.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @Multipart
    @POST("speaking/topics/{topicId}/phrases/submit")
    suspend fun submitPhraseRecording(
        @Path("topicId") topicId: String,
        @Part("phraseIndex") phraseIndex: Int,
        @Part audio: MultipartBody.Part
    ): Response<PhraseSubmissionResultDto>

    @GET("speaking/topics/{topicId}/recordings")
    suspend fun getUserPhraseRecordings(
        @Path("topicId") topicId: String,
        @Query("phraseIndex") phraseIndex: Int? = null
    ): Response<UserPhraseRecordingsResponseDto>

    @POST("speaking/tts/generate")
    suspend fun generateTts(
        @Body request: GenerateTtsRequestDto
    ): Response<GenerateTtsResponseDto>
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

data class PhraseProgressDto(
    val currentPhraseIndex: Int,
    val completedPhrases: List<Int>,
    val totalPhrases: Int,
    val isAllPhrasesCompleted: Boolean
)

data class SpeakingTopicDto(
    val id: String,
    val title: String,
    val description: String = "",
    val material: List<String>,
    val vocabulary: List<String> = emptyList(),
    val conversation: List<ConversationTurnDto> = emptyList(),
    val phraseProgress: PhraseProgressDto? = null,
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

data class PhraseSubmissionResultDto(
    val success: Boolean,
    val accuracy: Float,
    val transcription: String,
    val feedback: String,
    val nextPhraseIndex: Int?,
    val topicCompleted: Boolean,
    val xpAwarded: Int = 0,
    val recordingId: String? = null,
    val audioUrl: String? = null
)

data class UserPhraseRecordingDto(
    val id: String,
    val phraseIndex: Int,
    val audioUrl: String,
    val transcription: String,
    val accuracy: Float?,
    val feedback: String,
    val createdAt: String
)

data class UserPhraseRecordingsResponseDto(
    val recordings: List<UserPhraseRecordingDto>
)

data class GenerateTtsRequestDto(
    val text: String,
    val voiceName: String? = null
)

data class GenerateTtsResponseDto(
    val audioUrl: String,
    val sampleRate: Int,
    val voiceName: String?
)
