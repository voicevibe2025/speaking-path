package com.example.voicevibe.presentation.screens.practice.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.ai.LiveSessionManager
import com.example.voicevibe.data.repository.AiEvaluationRepository
import com.example.voicevibe.domain.model.LiveChatState
import com.example.voicevibe.domain.model.LiveMessage
import com.example.voicevibe.domain.model.LiveToken
import com.example.voicevibe.domain.model.Resource
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LivePracticeViewModel @Inject constructor(
    private val aiEvaluationRepository: AiEvaluationRepository,
    private val sessionManager: LiveSessionManager,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveChatState(isConnecting = true))
    val uiState: StateFlow<LiveChatState> = _uiState.asStateFlow()

    init {
        connectToLiveSession()
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        _uiState.update {
            it.copy(messages = it.messages + LiveMessage(message, isFromUser = true))
        }
        sessionManager.sendUserMessage(message)
    }

    fun retryConnection() {
        connectToLiveSession()
    }

    private fun connectToLiveSession() {
        _uiState.update {
            it.copy(
                isConnecting = true,
                isConnected = false,
                error = null
            )
        }
        sessionManager.close()
        viewModelScope.launch {
            when (val res = withContext(Dispatchers.IO) { aiEvaluationRepository.requestLiveToken() }) {
                is Resource.Success -> {
                    val token = res.data
                    if (token != null) {
                        openSession(token)
                    } else {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                error = "Live token response was empty"
                            )
                        }
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            error = res.message ?: "Unable to request live token"
                        )
                    }
                }

                is Resource.Loading -> {
                    // Ignored; repository already performs network call
                }
            }
        }
    }

    private fun openSession(token: LiveToken) {
        sessionManager.connect(token.token, token.model, object : LiveSessionManager.Listener {
            override fun onOpen() {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            error = null
                        )
                    }
                }
            }

            override fun onMessage(text: String) {
                val messages = extractModelTexts(text)
                if (messages.isEmpty()) return
                viewModelScope.launch {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + messages.map { LiveMessage(it, isFromUser = false) }
                        )
                    }
                }
            }

            override fun onFailure(t: Throwable) {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            error = t.message ?: "Live session connection failed"
                        )
                    }
                }
            }

            override fun onClosed() {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false
                        )
                    }
                }
            }
        })
    }

    private fun extractModelTexts(raw: String): List<String> {
        return try {
            val root = gson.fromJson(raw, JsonObject::class.java)
            val texts = mutableListOf<String>()

            val serverContent = root.getAsJsonArray("serverContent")
            serverContent?.forEach { entry ->
                texts += extractTextsFromContent(entry)
            }

            val candidates = root.getAsJsonArray("candidates")
            candidates?.forEach { entry ->
                texts += extractTextsFromContent(entry)
            }

            if (texts.isEmpty()) {
                root.getAsJsonObject("error")?.get("message")?.asString?.let { errorMessage ->
                    _uiState.update {
                        it.copy(error = errorMessage)
                    }
                }
            }

            texts.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractTextsFromContent(element: JsonElement): List<String> {
        val collected = mutableListOf<String>()
        if (!element.isJsonObject) return collected
        val obj = element.asJsonObject

        val content = if (obj.has("content") && obj.get("content").isJsonObject) {
            obj.getAsJsonObject("content")
        } else obj

        val parts = content.getAsJsonArray("parts")
        parts?.forEach { partElement ->
            if (partElement.isJsonObject) {
                val partObj = partElement.asJsonObject
                val text = partObj.get("text")?.takeIf { it.isJsonPrimitive }?.asString
                if (!text.isNullOrBlank()) {
                    collected.add(text)
                }
            }
        }

        return collected
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.close()
    }
}
