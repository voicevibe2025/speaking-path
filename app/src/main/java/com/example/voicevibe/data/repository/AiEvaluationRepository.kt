package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.AiEvaluationApiService
import com.example.voicevibe.data.remote.api.LiveTokenRequestDto
import com.example.voicevibe.data.remote.api.TranscribeRequestDto
import com.example.voicevibe.domain.model.LiveToken
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

    suspend fun requestLiveToken(
        model: String? = null,
        responseModalities: List<String>? = null,
        systemInstruction: String? = null,
        functionDeclarations: String? = null,
        lockAdditionalFields: List<String>? = null,
        speechConfig: Map<String, Any>? = null
    ): Resource<LiveToken> {
        return try {
            val response = api.createLiveToken(
                LiveTokenRequestDto(
                    model = model,
                    response_modalities = responseModalities,
                    system_instruction = systemInstruction,
                    lock_additional_fields = lockAdditionalFields,
                    function_declarations = functionDeclarations,
                    speech_config = speechConfig
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.token.isNotBlank()) {
                    Resource.Success(
                        LiveToken(
                            token = body.token,
                            expiresAt = body.expiresAt,
                            sessionId = body.sessionId,
                            model = body.model,
                            responseModalities = body.responseModalities,
                            lockedFields = body.lockedFields
                        )
                    )
                } else {
                    Resource.Error("Live token response was empty")
                }
            } else {
                Resource.Error("Live token request failed: ${response.code()} ${response.message()}")
            }
        } catch (t: Throwable) {
            Resource.Error("Live token error: ${t.message ?: "unknown"}")
        }
    }
}
