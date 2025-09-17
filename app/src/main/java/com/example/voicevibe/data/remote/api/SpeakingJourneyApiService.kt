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

    @Multipart
    @POST("speaking/topics/{topicId}/conversation/submit")
    suspend fun submitConversationTurn(
        @Path("topicId") topicId: String,
        @Part("turnIndex") turnIndex: Int,
        @Part audio: MultipartBody.Part,
        @Part("role") role: String? = null
    ): Response<ConversationSubmissionResultDto>

    @GET("speaking/topics/{topicId}/recordings")
    suspend fun getUserPhraseRecordings(
        @Path("topicId") topicId: String,
        @Query("phraseIndex") phraseIndex: Int? = null
    ): Response<UserPhraseRecordingsResponseDto>

    @POST("speaking/tts/generate")
    suspend fun generateTts(
        @Body request: GenerateTtsRequestDto
    ): Response<GenerateTtsResponseDto>

    @POST("speaking/topics/{topicId}/fluency/submit")
    suspend fun submitFluencyPrompt(
        @Path("topicId") topicId: String,
        @Body body: SubmitFluencyPromptRequestDto
    ): Response<SubmitFluencyPromptResponseDto>

    // --- Vocabulary Practice ---
    @POST("speaking/topics/{topicId}/vocabulary/start")
    suspend fun startVocabularyPractice(
        @Path("topicId") topicId: String
    ): Response<StartVocabularyPracticeResponseDto>

    @POST("speaking/topics/{topicId}/vocabulary/answer")
    suspend fun submitVocabularyAnswer(
        @Path("topicId") topicId: String,
        @Body body: SubmitVocabularyAnswerRequestDto
    ): Response<SubmitVocabularyAnswerResponseDto>

    @POST("speaking/topics/{topicId}/vocabulary/complete")
    suspend fun completeVocabularyPractice(
        @Path("topicId") topicId: String,
        @Body body: CompleteVocabularyPracticeRequestDto
    ): Response<CompleteVocabularyPracticeResponseDto>
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

data class PracticeScoresDto(
    val pronunciation: Int,
    val fluency: Int,
    val vocabulary: Int,
    val average: Float,
    val meetsRequirement: Boolean,
    // Added maxima for correct percentage computation client-side
    val maxPronunciation: Int,
    val maxFluency: Int,
    val maxVocabulary: Int
)

data class SpeakingTopicDto(
    val id: String,
    val title: String,
    val description: String = "",
    val material: List<String>,
    val vocabulary: List<String> = emptyList(),
    val conversation: List<ConversationTurnDto> = emptyList(),
    val fluencyPracticePrompts: List<String> = emptyList(),
    val fluencyProgress: FluencyProgressDto? = null,
    val phraseProgress: PhraseProgressDto? = null,
    val practiceScores: PracticeScoresDto? = null,
    val conversationScore: Int? = null,
    val conversationCompleted: Boolean? = null,
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

data class ConversationSubmissionResultDto(
    val success: Boolean,
    val accuracy: Float,
    val transcription: String,
    val feedback: String?,
    val nextTurnIndex: Int?,
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

// --- Fluency Progress ---
data class FluencyProgressDto(
    val promptsCount: Int,
    val promptScores: List<Int>,
    val totalScore: Int,
    val nextPromptIndex: Int?,
    val completed: Boolean
)

data class SubmitFluencyPromptRequestDto(
    val promptIndex: Int,
    val score: Int,
    val sessionId: String? = null
)

data class SubmitFluencyPromptResponseDto(
    val success: Boolean,
    val nextPromptIndex: Int?,
    val fluencyTotalScore: Int,
    val fluencyCompleted: Boolean,
    val promptScores: List<Int>,
    val xpAwarded: Int = 0
)

// --- Vocabulary Practice DTOs ---
data class VocabularyQuestionDto(
    val id: String,
    val definition: String,
    val options: List<String>
)

data class StartVocabularyPracticeResponseDto(
    val sessionId: String,
    val totalQuestions: Int,
    val questions: List<VocabularyQuestionDto>
)

data class SubmitVocabularyAnswerRequestDto(
    val sessionId: String,
    val questionId: String,
    val selected: String
)

data class SubmitVocabularyAnswerResponseDto(
    val correct: Boolean,
    val xpAwarded: Int,
    val nextIndex: Int?,
    val completed: Boolean,
    val totalScore: Int
)

data class CompleteVocabularyPracticeRequestDto(
    val sessionId: String
)

data class CompleteVocabularyPracticeResponseDto(
    val success: Boolean,
    val totalQuestions: Int,
    val correctCount: Int,
    val totalScore: Int,
    val xpAwarded: Int,
    val vocabularyTotalScore: Int,
    val topicCompleted: Boolean
)

