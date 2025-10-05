package com.example.voicevibe.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// DTOs for Whisper transcription API

data class TranscribeRequestDto(
    val audio_data: String, // base64 encoded audio data
    val language: String = "en"
)

data class WhisperTranscriptionDto(
    val text: String,
    val language: String? = null,
    val duration: Double? = null
)

data class TranscribeResponseDto(
    val success: Boolean,
    val transcription: WhisperTranscriptionDto?
)

data class LiveTokenRequestDto(
    val model: String? = null,
    val response_modalities: List<String>? = null,
    val system_instruction: String? = null,
    val lock_additional_fields: List<String>? = null,
    val function_declarations: String? = null,
    val speech_config: Map<String, Any>? = null
)

data class LiveTokenResponseDto(
    val token: String,
    val expiresAt: String?,
    val sessionId: String?,
    val model: String,
    val responseModalities: List<String>?,
    val lockedFields: List<String>?
)

interface AiEvaluationApiService {
    @POST("evaluate/transcribe/")
    suspend fun transcribeAudio(
        @Body body: TranscribeRequestDto
    ): Response<TranscribeResponseDto>

    @POST("evaluate/live/token/")
    suspend fun createLiveToken(
        @Body body: LiveTokenRequestDto = LiveTokenRequestDto()
    ): Response<LiveTokenResponseDto>
}
