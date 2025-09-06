package com.example.voicevibe.presentation.screens.practice.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeWithAIViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    // system instruction stored in a variable
    private val systemPrompt = """
        You are an English teacher named Vivi
        Always respond in a natural, conversational way 
        to help me practice English.
        You are being used in a speaking app.
        This app has speaking materials:
        - Greetings and Introductions
        - Travel
        - Food and Drink
        - Health and Fitness
        - Business
        - News and Current Events
        - Movies and TV Shows
        - Music and Arts
        - Sports and Recreation
        - Technology and Science
        - Weather
        - etc.
    """.trimIndent()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f // more natural replies
        },
        systemInstruction = content("system") {
            text(systemPrompt)
        }
    )

    // empty chat, no preloaded messages
    private val chat = generativeModel.startChat()

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val newUserMessage = ChatMessage(text = userMessage, isFromUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newUserMessage,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)
                val modelMessage = ChatMessage(text = response.text ?: "", isFromUser = false)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + modelMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)
