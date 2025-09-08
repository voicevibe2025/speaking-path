package com.example.voicevibe.data.remote.api

import com.example.voicevibe.domain.model.LearningPath
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for learning paths
 */
interface LearningPathApiService {

    @GET("learning/paths/")
    suspend fun getUserLearningPaths(): Response<List<LearningPath>>

    @GET("learning/paths/{id}/")
    suspend fun getLearningPath(@Path("id") pathId: String): Response<LearningPath>

    @GET("learning/paths/")
    suspend fun getAllLearningPaths(): Response<List<LearningPath>>

    // Backend custom action name is 'activate' on LearningPathViewSet
    @POST("learning/paths/{id}/activate/")
    suspend fun startLearningPath(@Path("id") pathId: String): Response<LearningPath>

    // Note: Progress updates are modeled under modules/activities in the backend.
    // This endpoint is kept for compatibility and may not exist server-side yet.
    @PUT("learning/paths/{id}/progress/")
    suspend fun updateProgress(
        @Path("id") pathId: String,
        @Body request: Map<String, Any>
    ): Response<LearningPath>

    // These lesson endpoints may not exist on the backend. Keep signatures for forward-compat.
    @GET("learning/paths/{id}/lessons/")
    suspend fun getLessons(@Path("id") pathId: String): Response<List<Lesson>>

    @GET("learning/paths/{pathId}/lessons/{lessonId}/")
    suspend fun getLesson(
        @Path("pathId") pathId: String,
        @Path("lessonId") lessonId: String
    ): Response<Lesson>

    @POST("learning/paths/{pathId}/lessons/{lessonId}/complete/")
    suspend fun completeLesson(
        @Path("pathId") pathId: String,
        @Path("lessonId") lessonId: String
    ): Response<LessonProgress>
}

/**
 * Data class for lesson
 */
data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val type: LessonType,
    val duration: Int,
    val points: Int,
    val order: Int,
    val isCompleted: Boolean = false
) {
    enum class LessonType {
        VIDEO,
        READING,
        PRACTICE,
        QUIZ,
        SPEAKING
    }
}

/**
 * Data class for lesson progress
 */
data class LessonProgress(
    val lessonId: String,
    val completed: Boolean,
    val score: Int?,
    val completedAt: String?,
    val pointsEarned: Int
)
