package com.example.voicevibe.domain.model

data class LiveToken(
    val token: String,
    val expiresAt: String?,
    val sessionId: String?,
    val model: String,
    val responseModalities: List<String>?,
    val lockedFields: List<String>?
)

data class LiveMessage(
    val text: String,
    val isFromUser: Boolean
)

data class LiveChatState(
    val messages: List<LiveMessage> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    val voiceMode: Boolean = false,
    val isRecording: Boolean = false,
    val isAiSpeaking: Boolean = false,
    val showTypingIndicator: Boolean = false
)
