package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.LearningPathApiService
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing learning paths
 */
@Singleton
class LearningPathRepository @Inject constructor(
    private val apiService: LearningPathApiService
) {

    /**
     * Get user's learning paths
     */
    fun getUserLearningPaths(): Flow<Resource<List<LearningPath>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserLearningPaths()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load learning paths: empty body"))
            } else {
                emit(Resource.Error("Failed to load learning paths"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get a specific learning path
     */
    fun getLearningPath(pathId: String): Flow<Resource<LearningPath>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getLearningPath(pathId)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load learning path: empty body"))
            } else {
                emit(Resource.Error("Failed to load learning path"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get all available learning paths
     */
    fun getAllLearningPaths(): Flow<Resource<List<LearningPath>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getAllLearningPaths()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error("Failed to load learning paths: empty body"))
            } else {
                emit(Resource.Error("Failed to load learning paths"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Start a learning path
     */
    suspend fun startLearningPath(pathId: String): Resource<LearningPath> {
        return try {
            val response = apiService.startLearningPath(pathId)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to start learning path: empty body")
            } else {
                Resource.Error("Failed to start learning path")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Update learning path progress
     */
    suspend fun updateProgress(
        pathId: String,
        lessonId: String,
        completed: Boolean
    ): Resource<LearningPath> {
        return try {
            val request = mapOf(
                "lessonId" to lessonId,
                "completed" to completed
            )
            val response = apiService.updateProgress(pathId, request)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error("Failed to update progress: empty body")
            } else {
                Resource.Error("Failed to update progress")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}
