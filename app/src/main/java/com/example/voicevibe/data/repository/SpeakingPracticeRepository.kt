package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.SpeakingPracticeApiService
import com.example.voicevibe.domain.model.*
import com.example.voicevibe.presentation.screens.practice.speaking.SubmissionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing speaking practice data
 */
@Singleton
class SpeakingPracticeRepository @Inject constructor(
    private val apiService: SpeakingPracticeApiService
) {

    /**
     * Get a random practice prompt
     */
    fun getRandomPrompt(): Flow<Resource<PracticePrompt>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getRandomPrompt()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load practice prompt: empty body"))
            } else {
                emit(Resource.Error("Failed to load practice prompt"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get practice prompts by category
     */
    fun getPromptsByCategory(category: String): Flow<Resource<List<PracticePrompt>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getPromptsByCategory(category)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body() ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to load prompts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Submit audio recording for evaluation
     */
    fun submitRecording(
        promptId: String,
        audioFilePath: String
    ): Flow<Resource<SubmissionResult>> = flow {
        emit(Resource.Loading())
        try {
            val audioFile = File(audioFilePath)
            val requestBody = audioFile.asRequestBody("audio/m4a".toMediaType())
            val audioPart = MultipartBody.Part.createFormData(
                "audio",
                audioFile.name,
                requestBody
            )

            val response = apiService.submitRecording(promptId, audioPart)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to submit recording: empty body"))
            } else {
                emit(Resource.Error("Failed to submit recording"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get speaking session by ID
     */
    suspend fun getSession(sessionId: String): Resource<SpeakingSession> {
        return try {
            val response = apiService.getSession(sessionId)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load session: empty body")
            } else {
                Resource.Error("Failed to load session")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get user's speaking sessions
     */
    fun getUserSessions(
        limit: Int = 20,
        offset: Int = 0
    ): Flow<Resource<List<SpeakingSession>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserSessions(limit, offset)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body() ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to load sessions"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get evaluation for a session
     */
    suspend fun getEvaluation(sessionId: String): Resource<SpeakingEvaluation> {
        return try {
            val response = apiService.getEvaluation(sessionId)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to load evaluation: empty body")
            } else {
                Resource.Error("Failed to load evaluation")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Delete a speaking session
     */
    suspend fun deleteSession(sessionId: String): Resource<Boolean> {
        return try {
            val response = apiService.deleteSession(sessionId)
            if (response.isSuccessful) {
                Resource.Success(true)
            } else {
                Resource.Error("Failed to delete session")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get practice statistics
     */
    fun getPracticeStats(): Flow<Resource<PracticeStatistics>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getPracticeStats()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load statistics: empty body"))
            } else {
                emit(Resource.Error("Failed to load statistics"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
}

/**
 * Practice statistics data
 */
data class PracticeStatistics(
    val totalSessions: Int,
    val totalPracticeTime: Int, // in seconds
    val averageScore: Float,
    val improvementRate: Float,
    val strongestArea: String,
    val weakestArea: String,
    val recentSessions: List<SpeakingSession>
)
