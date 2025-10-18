package com.example.voicevibe.data.api

import com.example.voicevibe.data.model.*
import okhttp3.ResponseBody
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

    @POST("wordup/evaluate-pronunciation/")
    suspend fun evaluatePronunciation(
        @Body request: EvaluatePronunciationRequest
    ): PronunciationResultDto

    @GET("wordup/mastered-words/")
    suspend fun getMasteredWords(): MasteredWordsResponse

    @GET("wordup/stats/")
    suspend fun getStats(): WordUpStatsDto

    @GET("wordup/tts/")
    @Streaming
    suspend fun getWordPronunciation(
        @Query("text") text: String
    ): ResponseBody
}
