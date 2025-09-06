package com.example.voicevibe.presentation.screens.practice.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.BuildConfig
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationTurn
import com.example.voicevibe.presentation.screens.speakingjourney.Topic
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicPracticeChatViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicChatUiState())
    val uiState = _uiState.asStateFlow()

    // Lazily created per-topic
    private var generativeModel: GenerativeModel? = null
    private var chat: com.google.ai.client.generativeai.Chat? = null
    private var currentTopicId: String? = null

    // Function calling: award_xp(points, topicId, reason?)
    private val awardXpDeclaration = FunctionDeclaration(
        name = "award_xp",
        description = "Award XP points to the current user when they have mastered the selected topic.",
        parameters = emptyList<Schema<*>>(),
        requiredParameters = emptyList()
    )

    fun startForTopic(topic: Topic) {
        if (currentTopicId == topic.id && chat != null) return // already started
        currentTopicId = topic.id

        val systemPrompt = buildSystemPrompt(topic)
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.6f },
            systemInstruction = content("system") { text(systemPrompt) },
            tools = listOf(
                Tool(functionDeclarations = listOf(awardXpDeclaration))
            )
        )
        chat = generativeModel!!.startChat()

        // Auto-start the conversation
        viewModelScope.launch {
            sendInternal("Start with a friendly greeting and ask a simple question to begin.")
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val msg = ChatMessage(text = userMessage, isFromUser = true)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg, isLoading = true)

        viewModelScope.launch {
            sendInternal(userMessage)
        }
    }

    private suspend fun sendInternal(message: String) {
        val c = chat ?: return
        try {
            var response = c.sendMessage(message)
            // Handle function calling loop
            while (response.functionCalls.isNotEmpty()) {
                val parts = response.functionCalls.map { call ->
                    when (call.name) {
                        "award_xp" -> {
                            // Read args as JSONObject when available
                            val args = call.args
                            val (points, topicId, reason) = if (args is org.json.JSONObject) {
                                val tid = args.optString("topicId", currentTopicId ?: "")
                                Triple(args.optInt("points", 0), tid, args.optString("reason", "topic_practice:$tid"))
                            } else {
                                val map = args as? Map<*, *>
                                val tid = (map?.get("topicId") as? String) ?: (currentTopicId ?: "")
                                Triple((map?.get("points") as? Number)?.toInt() ?: 0, tid, (map?.get("reason") as? String) ?: "topic_practice:$tid")
                            }
                            val result = gamificationRepository.addExperience(points = points, source = reason)
                            val success = result is com.example.voicevibe.domain.model.Resource.Success
                            val payload = org.json.JSONObject().apply {
                                put("success", success)
                                put("awarded", points)
                                put("topicId", topicId)
                            }
                            FunctionResponsePart(call.name, payload)
                        }
                        else -> {
                            FunctionResponsePart(call.name, org.json.JSONObject().apply {
                                put("error", "Unknown function: ${call.name}")
                            })
                        }
                    }
                }
                response = c.sendMessage(
                    content(role = "function") { parts.forEach { part(it) } }
                )
            }

            val text = response.text ?: ""
            if (text.isNotBlank()) {
                val modelMessage = ChatMessage(text = text, isFromUser = false)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + modelMessage,
                    isLoading = false,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = t.message ?: "Error")
        }
    }

    private fun buildSystemPrompt(topic: Topic): String {
        val phrases = topic.material.joinToString("\n") { "- $it" }
        val vocab = topic.vocabulary.joinToString(", ")
        val conversation = topic.conversation.joinToString("\n") { turn: ConversationTurn -> "${turn.speaker}: ${turn.text}" }
        return """
            You are Vivi, an English speaking practice AI. This is a Topic Practice session.
            STRICTLY follow these rules:
            - Only discuss the selected topic: "${topic.title}". Politely refuse and steer back if the user asks anything unrelated.
            - Use and reinforce the provided materials exactly: phrases, vocabulary, and conversation example.
            - Speak naturally, short turns, and ask questions to keep the dialogue going.
            - When you conclude the user has mastered the topic, call the function award_xp with a reasonable points value (e.g., 50 or 100), include the provided topicId, and a reason like 'topic mastery'. Do not explain the tool itself.

            Topic details:
            Title: ${topic.title}
            Description: ${topic.description}
            Phrases:
            $phrases

            Vocabulary: $vocab

            Conversation example:
            $conversation
        """.trimIndent()
    }
}

data class TopicChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
