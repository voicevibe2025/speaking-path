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
    private var currentTopic: Topic? = null

    // Function calling: award_xp(points, topicId, reason?)
    private val awardXpDeclaration = FunctionDeclaration(
        name = "award_xp",
        description = "Award XP points to the current user when they have mastered the selected topic.",
        parameters = emptyList<Schema<*>>(),
        requiredParameters = emptyList()
    )

    // Function calling: show_practice_menu() -> shows practice mode cards inside chat
    private val showPracticeMenuDeclaration = FunctionDeclaration(
        name = "show_practice_menu",
        description = "Render a card in the chat UI with available practice modes for this topic. Always call this after your first greeting and whenever the user asks to change mode.",
        parameters = emptyList<Schema<*>>(),
        requiredParameters = emptyList()
    )

    // Function calling: show_conversation_example() -> shows inline conversation with TTS + explain actions
    private val showConversationExampleDeclaration = FunctionDeclaration(
        name = "show_conversation_example",
        description = "Render a simple inline interface that lists the conversation example turns for the current topic, with per-turn audio playback and an explain action.",
        parameters = emptyList<Schema<*>>(),
        requiredParameters = emptyList()
    )

    fun startForTopic(topic: Topic) {
        if (currentTopicId == topic.id && chat != null) return // already started
        currentTopicId = topic.id
        currentTopic = topic

        val systemPrompt = buildSystemPrompt(topic)
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.6f },
            systemInstruction = content("system") { text(systemPrompt) },
            tools = listOf(
                Tool(functionDeclarations = listOf(
                    awardXpDeclaration,
                    showPracticeMenuDeclaration,
                    showConversationExampleDeclaration
                ))
            )
        )
        chat = generativeModel!!.startChat()

        // Auto-start the conversation
        viewModelScope.launch {
            // Greet, then instruct the model to call show_practice_menu to render practice options.
            sendInternal("Start with a friendly greeting (1–2 short sentences). Then immediately call the function show_practice_menu to present practice options.")
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val msg = TopicChatItem.UserText(userMessage)
        _uiState.value = _uiState.value.copy(items = _uiState.value.items + msg, isLoading = true)

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
                        "show_practice_menu" -> {
                            // Append a Practice Menu card into chat
                            val newItem = TopicChatItem.PracticeMenu
                            _uiState.value = _uiState.value.copy(items = _uiState.value.items + newItem)
                            val payload = org.json.JSONObject().apply {
                                put("shown", true)
                            }
                            FunctionResponsePart(call.name, payload)
                        }
                        "show_conversation_example" -> {
                            val topic = currentTopic
                            if (topic != null) {
                                val newItem = TopicChatItem.ConversationExample(topic.conversation)
                                _uiState.value = _uiState.value.copy(items = _uiState.value.items + newItem)
                                val payload = org.json.JSONObject().apply {
                                    put("shown", true)
                                    put("turns", topic.conversation.size)
                                }
                                FunctionResponsePart(call.name, payload)
                            } else {
                                val payload = org.json.JSONObject().apply { put("error", "No topic in context") }
                                FunctionResponsePart(call.name, payload)
                            }
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
                val modelMessage = TopicChatItem.AiText(text)
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items + modelMessage,
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
            - Only discuss the selected topic: "${topic.title}". Politely refuse and gently steer back if the user asks anything unrelated.
            - Use and reinforce the provided materials exactly: phrases, vocabulary, and the conversation example.
            - Speak naturally, short turns, and ask questions to keep the dialogue going.
            - Use the following tools when appropriate:
              • show_practice_menu — Call this right after your initial greeting to display practice options (Conversation, Pronunciation, Fluency, Vocabulary, Listening, Grammar). Also call it anytime the user asks to change mode.
              • show_conversation_example — When the user chooses Conversation Practice, call this to render the inline conversation example UI.
              • award_xp — When you conclude the user has mastered the topic, call this with a reasonable points value (e.g., 50 or 100), include the topicId, and a reason like 'topic mastery'. Do not explain the tool itself.
            - After showing the conversation example, you may briefly offer to practice this conversation with you. If the user accepts, acknowledge that the full interactive practice is coming soon.

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
    
    // UI callbacks (member functions)
    fun onSelectPracticeMode(mode: PracticeMode) {
        when (mode) {
            PracticeMode.CONVERSATION -> {
                // Add a user marker and nudge the model to call the function
                val marker = TopicChatItem.UserText("Selected: Conversation Practice")
                _uiState.value = _uiState.value.copy(items = _uiState.value.items + marker, isLoading = true)
                viewModelScope.launch {
                    sendInternal("The user selected Conversation Practice. Please call show_conversation_example to render the inline conversation example in the chat, then ask if they'd like to practice it with you.")
                }
            }
            else -> {
                // Coming soon placeholders
                val label = mode.name.lowercase().replaceFirstChar { it.titlecase() }
                val marker = TopicChatItem.UserText("Selected: $label Practice")
                val reply = TopicChatItem.AiText("$label Practice is coming soon. Stay tuned!")
                _uiState.value = _uiState.value.copy(items = _uiState.value.items + marker + reply)
            }
        }
    }

    fun explainConversationTurn(turnText: String) {
        if (turnText.isBlank()) return
        val userMsg = TopicChatItem.UserText("Explain this line: \"$turnText\" (in the context of this topic's conversation example). Keep it concise with bullet points and give 1–2 variations.")
        _uiState.value = _uiState.value.copy(items = _uiState.value.items + userMsg, isLoading = true)
        viewModelScope.launch {
            sendInternal("Please explain this conversation line in context: '$turnText'. Keep it concise with bullet points and include 1–2 variations and tips for usage.")
        }
    }
}

data class TopicChatUiState(
    val items: List<TopicChatItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Rich chat item model for Topic Practice
sealed class TopicChatItem {
    data class UserText(val text: String) : TopicChatItem()
    data class AiText(val text: String) : TopicChatItem()
    object PracticeMenu : TopicChatItem()
    data class ConversationExample(val turns: List<ConversationTurn>) : TopicChatItem()
}

enum class PracticeMode { CONVERSATION, PRONUNCIATION, FLUENCY, VOCABULARY, LISTENING, GRAMMAR }

// (Top-level UI callbacks removed; now member functions inside the ViewModel)
