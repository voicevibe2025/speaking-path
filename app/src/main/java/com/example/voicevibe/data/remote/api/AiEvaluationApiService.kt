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

interface AiEvaluationApiService {
    @POST("evaluate/transcribe/")
    suspend fun transcribeAudio(
        @Body body: TranscribeRequestDto
    ): Response<TranscribeResponseDto>
}
