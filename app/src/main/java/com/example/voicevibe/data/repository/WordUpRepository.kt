package com.example.voicevibe.data.repository

import android.util.Log
import com.example.voicevibe.data.api.WordUpApiService
import com.example.voicevibe.data.mapper.toDomain
import com.example.voicevibe.data.model.EvaluateExampleRequest
import com.example.voicevibe.data.model.EvaluatePronunciationRequest
import com.example.voicevibe.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for WordUp vocabulary feature
 */
@Singleton
class WordUpRepository @Inject constructor(
    private val api: WordUpApiService
) {
    private val tag = "WordUpRepository"

    suspend fun getRandomWord(): Result<WordWithProgress> {
        return try {
            val response = api.getRandomWord()
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Log.e(tag, "Error getting random word", e)
            Result.failure(e)
        }
    }

    suspend fun evaluateExample(
        wordId: Int,
        exampleSentence: String? = null,
        audioBase64: String? = null
    ): Result<EvaluationResult> {
        return try {
            val request = EvaluateExampleRequest(
                wordId = wordId,
                exampleSentence = exampleSentence,
                audioBase64 = audioBase64
            )
            val response = api.evaluateExample(request)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Log.e(tag, "Error evaluating example", e)
            Result.failure(e)
        }
    }

    suspend fun evaluatePronunciation(
        wordId: Int,
        audioBase64: String
    ): Result<PronunciationResult> {
        return try {
            val request = EvaluatePronunciationRequest(
                wordId = wordId,
                audioBase64 = audioBase64
            )
            val response = api.evaluatePronunciation(request)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Log.e(tag, "Error evaluating pronunciation", e)
            Result.failure(e)
        }
    }

    suspend fun getMasteredWords(): Result<List<WordProgress>> {
        return try {
            val response = api.getMasteredWords()
            Result.success(response.masteredWords.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(tag, "Error getting mastered words", e)
            Result.failure(e)
        }
    }

    suspend fun getStats(): Result<WordUpStats> {
        return try {
            val response = api.getStats()
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Log.e(tag, "Error getting stats", e)
            Result.failure(e)
        }
    }

    suspend fun getWordPronunciation(word: String): Result<ByteArray> {
        return try {
            val response = api.getWordPronunciation(word)
            Result.success(response.bytes())
        } catch (e: Exception) {
            Log.e(tag, "Error getting word pronunciation", e)
            Result.failure(e)
        }
    }
}
