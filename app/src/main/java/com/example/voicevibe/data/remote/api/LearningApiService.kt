package com.example.voicevibe.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface LearningApiService {

    @GET("learning-paths")
    suspend fun getAllLearningPaths(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<LearningPathsResponse>

    @GET("learning-paths/enrolled")
    suspend fun getEnrolledPaths(
        @Header("Authorization") token: String
    ): Response<List<LearningPathDto>>

    @GET("learning-paths/recommended")
    suspend fun getRecommendedPaths(
        @Header("Authorization") token: String
    ): Response<List<PathRecommendationDto>>

    @GET("learning-paths/{pathId}")
    suspend fun getPathById(
        @Path("pathId") pathId: String
    ): Response<LearningPathDto>

    @POST("learning-paths/{pathId}/enroll")
    suspend fun enrollInPath(
        @Path("pathId") pathId: String,
        @Header("Authorization") token: String
    ): Response<EnrollmentResponse>

    @DELETE("learning-paths/{pathId}/enroll")
    suspend fun unenrollFromPath(
        @Path("pathId") pathId: String,
        @Header("Authorization") token: String
    ): Response<BaseResponse>

    @GET("learning-paths/{pathId}/modules")
    suspend fun getPathModules(
        @Path("pathId") pathId: String
    ): Response<List<LearningModuleDto>>

    @GET("modules/{moduleId}/lessons")
    suspend fun getModuleLessons(
        @Path("moduleId") moduleId: String
    ): Response<List<LessonDto>>

    @GET("lessons/{lessonId}")
    suspend fun getLessonById(
        @Path("lessonId") lessonId: String
    ): Response<LessonDto>

    @POST("lessons/{lessonId}/complete")
    suspend fun markLessonComplete(
        @Path("lessonId") lessonId: String,
        @Header("Authorization") token: String,
        @Body request: CompleteLessonRequest
    ): Response<LessonProgressResponse>

    @POST("lessons/{lessonId}/bookmark")
    suspend fun bookmarkLesson(
        @Path("lessonId") lessonId: String,
        @Header("Authorization") token: String
    ): Response<BaseResponse>

    @DELETE("lessons/{lessonId}/bookmark")
    suspend fun unbookmarkLesson(
        @Path("lessonId") lessonId: String,
        @Header("Authorization") token: String
    ): Response<BaseResponse>

    @GET("learning/streak")
    suspend fun getLearningStreak(
        @Header("Authorization") token: String
    ): Response<LearningStreakDto>

    @GET("learning/continue")
    suspend fun getContinueLesson(
        @Header("Authorization") token: String
    ): Response<ContinueLessonDto>

    @POST("lessons/{lessonId}/notes")
    suspend fun addLessonNote(
        @Path("lessonId") lessonId: String,
        @Header("Authorization") token: String,
        @Body request: AddNoteRequest
    ): Response<LessonNoteDto>

    @GET("lessons/{lessonId}/notes")
    suspend fun getLessonNotes(
        @Path("lessonId") lessonId: String,
        @Header("Authorization") token: String
    ): Response<List<LessonNoteDto>>

    @PUT("notes/{noteId}")
    suspend fun updateNote(
        @Path("noteId") noteId: String,
        @Header("Authorization") token: String,
        @Body request: UpdateNoteRequest
    ): Response<LessonNoteDto>

    @DELETE("notes/{noteId}")
    suspend fun deleteNote(
        @Path("noteId") noteId: String,
        @Header("Authorization") token: String
    ): Response<BaseResponse>

    @POST("lessons/{lessonId}/quiz/submit")
    suspend fun submitQuizAnswers(
        @Path("lessonId") lessonId: String,
        @Header("Authorization") token: String,
        @Body request: QuizSubmissionRequest
    ): Response<QuizResultResponse>

    @GET("learning-paths/search")
    suspend fun searchLearningPaths(
        @Query("query") query: String,
        @Query("category") category: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<LearningPathsResponse>
}

// DTOs and Request/Response models
data class LearningPathsResponse(
    val paths: List<LearningPathDto>,
    val totalCount: Int,
    val page: Int,
    val totalPages: Int
)

data class LearningPathDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val thumbnailUrl: String?,
    val duration: Int,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Float,
    val isEnrolled: Boolean,
    val enrolledCount: Int,
    val rating: Float,
    val ratingCount: Int,
    val instructor: PathInstructorDto?,
    val tags: List<String>,
    val estimatedCompletionDays: Int,
    val prerequisites: List<String>,
    val skillsToGain: List<String>,
    val certificateAvailable: Boolean,
    val isPremium: Boolean,
    val isRecommended: Boolean,
    val lastAccessedAt: String?,
    val completedAt: String?,
    val nextLesson: LessonInfoDto?
)

data class PathInstructorDto(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val title: String,
    val bio: String,
    val rating: Float,
    val studentsCount: Int
)

data class LessonInfoDto(
    val lessonId: String,
    val moduleId: String,
    val title: String,
    val moduleTitle: String
)

data class PathRecommendationDto(
    val path: LearningPathDto,
    val reason: String,
    val matchScore: Float,
    val basedOn: String
)

data class LearningModuleDto(
    val id: String,
    val pathId: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Float,
    val duration: Int,
    val isLocked: Boolean,
    val unlockRequirements: String?,
    val moduleType: String
)

data class LessonDto(
    val id: String,
    val moduleId: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val type: String,
    val duration: Int,
    val content: LessonContentDto,
    val isCompleted: Boolean,
    val completedAt: String?,
    val score: Int?,
    val attempts: Int,
    val isLocked: Boolean,
    val unlockRequirements: String?,
    val xpReward: Int,
    val resources: List<LessonResourceDto>,
    val practicePrompts: List<PracticePromptDto>?
)

data class LessonContentDto(
    val videoUrl: String?,
    val audioUrl: String?,
    val textContent: String?,
    val interactiveElements: List<InteractiveElementDto>?,
    val quizQuestions: List<QuizQuestionDto>?
)

data class InteractiveElementDto(
    val id: String,
    val type: String,
    val content: String,
    val correctResponse: String,
    val hints: List<String>
)

data class QuizQuestionDto(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String?,
    val points: Int
)

data class LessonResourceDto(
    val id: String,
    val title: String,
    val type: String,
    val url: String,
    val description: String?
)

data class PracticePromptDto(
    val id: String,
    val text: String,
    val difficulty: String,
    val targetTime: Int,
    val tips: List<String>
)

data class LearningStreakDto(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActivityDate: String,
    val streakHistory: List<StreakDayDto>
)

data class StreakDayDto(
    val date: String,
    val lessonsCompleted: Int,
    val minutesLearned: Int,
    val xpEarned: Int
)

data class ContinueLessonDto(
    val lessonId: String,
    val moduleId: String,
    val title: String,
    val moduleTitle: String
)

data class LessonNoteDto(
    val id: String,
    val lessonId: String,
    val content: String,
    val timestamp: Long,
    val createdAt: String,
    val updatedAt: String
)

data class EnrollmentResponse(
    val success: Boolean,
    val message: String,
    val enrollmentId: String
)

data class LessonProgressResponse(
    val success: Boolean,
    val message: String,
    val xpEarned: Int,
    val newLevel: Int?,
    val achievementsUnlocked: List<String>?
)

data class QuizResultResponse(
    val score: Int,
    val totalScore: Int,
    val passed: Boolean,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val feedback: String
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)

data class CompleteLessonRequest(
    val score: Int,
    val timeSpent: Int,
    val completed: Boolean
)

data class AddNoteRequest(
    val content: String,
    val timestamp: Long
)

data class UpdateNoteRequest(
    val content: String
)

data class QuizSubmissionRequest(
    val answers: List<QuizAnswer>
)

data class QuizAnswer(
    val questionId: String,
    val selectedAnswer: Int
)
