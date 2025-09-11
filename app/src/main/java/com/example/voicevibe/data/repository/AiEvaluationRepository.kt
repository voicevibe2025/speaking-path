package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.AiEvaluationApiService
import com.example.voicevibe.data.remote.api.TranscribeRequestDto
import com.example.voicevibe.domain.model.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiEvaluationRepository @Inject constructor(
    private val api: AiEvaluationApiService
) {
    suspend fun transcribeBase64(audioBase64: String, language: String = "en"): Resource<String> {
        return try {
            val res = api.transcribeAudio(TranscribeRequestDto(audio_data = audioBase64, language = language))
            if (res.isSuccessful) {
                val body = res.body()
                val text = body?.transcription?.text
                if (body?.success == true && !text.isNullOrBlank()) {
                    Resource.Success(text)
                } else {
                    Resource.Error("Transcription failed: empty response")
                }
            } else {
                Resource.Error("Transcription request failed: ${res.code()} ${res.message()}")
            }
        } catch (t: Throwable) {
            Resource.Error("Transcription error: ${t.message ?: "unknown"}")
        }
    }
}
