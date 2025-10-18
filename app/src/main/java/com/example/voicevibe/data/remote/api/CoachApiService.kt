package com.example.voicevibe.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface CoachApiService {
    @GET("speaking/coach/analysis")
    suspend fun getCoachAnalysis(): Response<CoachAnalysisDto>

    @POST("speaking/coach/analysis/refresh")
    suspend fun refreshCoachAnalysis(): Response<CoachAnalysisDto>
}

// --- DTOs ---

data class CoachSkillDto(
    val id: String,
    val name: String,
    val mastery: Int,
    val confidence: Float? = null,
    val trend: String? = null,
    val evidence: List<String>? = null
)

data class NextBestActionDto(
    val id: String,
    val title: String,
    val rationale: String,
    val deeplink: String,
    val expectedGain: String? = null
)

data class DifficultyCalibrationDto(
    val pronunciation: String? = null, // easier | baseline | harder
    val fluency: String? = null,       // slower | baseline | faster
    val vocabulary: String? = null,    // fewer_terms | baseline | more_terms
    val listening: String? = null,     // easier | baseline | harder
    val grammar: String? = null        // easier | baseline | harder
)

data class CoachScheduleItemDto(
    val date: String, // YYYY-MM-DD
    val focus: String,
    val microSkills: List<String>? = null,
    val reason: String? = null
)

data class CoachAnalysisDto(
    val currentVersion: Int,
    val generatedAt: String,
    val skills: List<CoachSkillDto>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val nextBestActions: List<NextBestActionDto>,
    val difficultyCalibration: DifficultyCalibrationDto? = null,
    val schedule: List<CoachScheduleItemDto>? = null,
    val coachMessage: String,
    val cacheForHours: Int = 12,
    // Cache metadata from backend
    val _cache_stale: Boolean? = null,
    val _is_ai_generated: Boolean? = null,
    val _generated_at: String? = null,
    val _next_refresh_at: String? = null
)
