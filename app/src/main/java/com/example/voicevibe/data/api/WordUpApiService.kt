package com.example.voicevibe.data.api

import com.example.voicevibe.data.model.*
import retrofit2.http.*

/**
 * API service for WordUp vocabulary feature
 */
interface WordUpApiService {

    @GET("wordup/random-word/")
    suspend fun getRandomWord(): RandomWordResponse

    @POST("wordup/evaluate/")
    suspend fun evaluateExample(
        @Body request: EvaluateExampleRequest
    ): EvaluationResultDto

    @GET("wordup/mastered-words/")
    suspend fun getMasteredWords(): MasteredWordsResponse

    @GET("wordup/stats/")
    suspend fun getStats(): WordUpStatsDto
}
