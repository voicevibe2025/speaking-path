package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.SpeakingJourneyApiService
import com.example.voicevibe.data.remote.api.SubmitFluencyPromptRequestDto
import com.example.voicevibe.data.remote.api.SubmitFluencyPromptResponseDto
import com.example.voicevibe.data.remote.api.SubmitFluencyRecordingResponseDto
import com.example.voicevibe.data.remote.api.SpeakingTopicDto
import com.example.voicevibe.data.remote.api.SpeakingTopicsResponse
import com.example.voicevibe.data.remote.api.UpdateLastVisitedTopicRequest
import com.example.voicevibe.data.remote.api.UserProfileDto
import com.example.voicevibe.data.remote.api.PhraseSubmissionResultDto
import com.example.voicevibe.data.remote.api.UserPhraseRecordingDto
import com.example.voicevibe.data.remote.api.UserPhraseRecordingsResponseDto
import com.example.voicevibe.data.remote.api.GenerateTtsRequestDto
import com.example.voicevibe.data.remote.api.GenerateTtsResponseDto
import com.example.voicevibe.data.remote.api.StartVocabularyPracticeResponseDto
import com.example.voicevibe.data.remote.api.SubmitVocabularyAnswerRequestDto
import com.example.voicevibe.data.remote.api.SubmitVocabularyAnswerResponseDto
import com.example.voicevibe.data.remote.api.CompleteVocabularyPracticeRequestDto
import com.example.voicevibe.data.remote.api.CompleteVocabularyPracticeResponseDto
import com.example.voicevibe.data.remote.api.StartListeningPracticeResponseDto
import com.example.voicevibe.data.remote.api.SubmitListeningAnswerRequestDto
import com.example.voicevibe.data.remote.api.SubmitListeningAnswerResponseDto
import com.example.voicevibe.data.remote.api.CompleteListeningPracticeRequestDto
import com.example.voicevibe.data.remote.api.CompleteListeningPracticeResponseDto
import com.example.voicevibe.data.remote.api.StartGrammarPracticeResponseDto
import com.example.voicevibe.data.remote.api.SubmitGrammarAnswerRequestDto
import com.example.voicevibe.data.remote.api.SubmitGrammarAnswerResponseDto
import com.example.voicevibe.data.remote.api.CompleteGrammarPracticeRequestDto
import com.example.voicevibe.data.remote.api.CompleteGrammarPracticeResponseDto
import com.example.voicevibe.data.remote.api.ConversationSubmissionResultDto
import com.example.voicevibe.data.remote.api.JourneyActivityDto
import com.example.voicevibe.domain.model.ActivityType
import com.example.voicevibe.domain.model.UserActivity
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import javax.inject.Inject

class SpeakingJourneyRepository @Inject constructor(
    private val api: SpeakingJourneyApiService
) {
    suspend fun getTopics(): Result<SpeakingTopicsResponse> {
        return try {
            val res = api.getTopics()
            if (res.isSuccessful) {
                Result.success(res.body() ?: SpeakingTopicsResponse(emptyList(), UserProfileDto(true, null, null)))
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun submitConversationTurn(
        topicId: String,
        turnIndex: Int,
        audioFile: MultipartBody.Part,
        role: String? = null
    ): Result<ConversationSubmissionResultDto> {
        return try {
            val res = api.submitConversationTurn(topicId, turnIndex, audioFile, role)
            if (res.isSuccessful) {
                Result.success(res.body() ?: throw Exception("Empty response"))
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

    suspend fun submitFluencyPromptScore(
        topicId: String,
        promptIndex: Int,
        score: Int,
        sessionId: String? = null
    ): Result<SubmitFluencyPromptResponseDto> {
        return try {
            val response = api.submitFluencyPrompt(
                topicId,
                SubmitFluencyPromptRequestDto(promptIndex = promptIndex, score = score, sessionId = sessionId)
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun submitFluencyRecording(
        topicId: String,
        audioFile: File,
        promptIndex: Int = 0,
        recordingDuration: Float
    ): Result<SubmitFluencyRecordingResponseDto> {
        return try {
            val audioRequestBody = RequestBody.create("audio/m4a".toMediaTypeOrNull(), audioFile)
            val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, audioRequestBody)
            val promptIndexBody = RequestBody.create("text/plain".toMediaTypeOrNull(), promptIndex.toString())
            val durationBody = RequestBody.create("text/plain".toMediaTypeOrNull(), recordingDuration.toString())
            
            val response = api.submitFluencyRecording(
                topicId = topicId,
                audio = audioPart,
                promptIndex = promptIndexBody,
                recordingDuration = durationBody
            )
            
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    // --- Vocabulary Practice ---
    suspend fun startVocabularyPractice(
        topicId: String
    ): Result<StartVocabularyPracticeResponseDto> {
        return try {
            val res = api.startVocabularyPractice(topicId)
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

    suspend fun submitVocabularyAnswer(
        topicId: String,
        sessionId: String,
        questionId: String,
        selected: String
    ): Result<SubmitVocabularyAnswerResponseDto> {
        return try {
            val res = api.submitVocabularyAnswer(
                topicId,
                SubmitVocabularyAnswerRequestDto(sessionId = sessionId, questionId = questionId, selected = selected)
            )
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

    suspend fun completeVocabularyPractice(
        topicId: String,
        sessionId: String
    ): Result<CompleteVocabularyPracticeResponseDto> {
        return try {
            val res = api.completeVocabularyPractice(
                topicId,
                CompleteVocabularyPracticeRequestDto(sessionId = sessionId)
            )
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

    // --- Activities ---
    suspend fun getActivities(limit: Int = 50, userId: String? = null): Result<List<UserActivity>> {
        return try {
            val res = api.getActivities(limit, userId)
            if (res.isSuccessful) {
                val body = res.body() ?: emptyList()
                Result.success(body.map { it.toDomain() })
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun JourneyActivityDto.toDomain(): UserActivity {
        val t = try { ActivityType.valueOf(type) } catch (_: Exception) { ActivityType.PRACTICE_SESSION }
        return UserActivity(
            id = id,
            type = t,
            title = title,
            description = description ?: "",
            timestamp = timestamp,
            xpEarned = xpEarned,
            achievementId = null,
            sessionId = null
        )
    }

    // --- Listening Practice ---
    suspend fun startListeningPractice(
        topicId: String
    ): Result<StartListeningPracticeResponseDto> {
        return try {
            val res = api.startListeningPractice(topicId)
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

    suspend fun submitListeningAnswer(
        topicId: String,
        sessionId: String,
        questionId: String,
        selected: String
    ): Result<SubmitListeningAnswerResponseDto> {
        return try {
            val res = api.submitListeningAnswer(
                topicId,
                SubmitListeningAnswerRequestDto(sessionId = sessionId, questionId = questionId, selected = selected)
            )
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

    suspend fun completeListeningPractice(
        topicId: String,
        sessionId: String
    ): Result<CompleteListeningPracticeResponseDto> {
        return try {
            val res = api.completeListeningPractice(
                topicId,
                CompleteListeningPracticeRequestDto(sessionId = sessionId)
            )
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

    // --- Grammar Practice ---
    suspend fun startGrammarPractice(
        topicId: String
    ): Result<StartGrammarPracticeResponseDto> {
        return try {
            val res = api.startGrammarPractice(topicId)
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

    suspend fun submitGrammarAnswer(
        topicId: String,
        sessionId: String,
        questionId: String,
        selected: String
    ): Result<SubmitGrammarAnswerResponseDto> {
        return try {
            val res = api.submitGrammarAnswer(
                topicId,
                SubmitGrammarAnswerRequestDto(sessionId = sessionId, questionId = questionId, selected = selected)
            )
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

    suspend fun completeGrammarPractice(
        topicId: String,
        sessionId: String
    ): Result<CompleteGrammarPracticeResponseDto> {
        return try {
            val res = api.completeGrammarPractice(
                topicId,
                CompleteGrammarPracticeRequestDto(sessionId = sessionId)
            )
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
