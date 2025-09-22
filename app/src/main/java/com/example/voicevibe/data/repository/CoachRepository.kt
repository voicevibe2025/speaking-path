package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.CoachAnalysisDto
import com.example.voicevibe.data.remote.api.CoachApiService
import javax.inject.Inject

class CoachRepository @Inject constructor(
    private val api: CoachApiService
) {
    suspend fun getAnalysis(): Result<CoachAnalysisDto> {
        return try {
            val res = api.getCoachAnalysis()
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

    suspend fun refreshAnalysis(): Result<CoachAnalysisDto> {
        return try {
            val res = api.refreshCoachAnalysis()
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
