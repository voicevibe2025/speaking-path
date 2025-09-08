package com.example.voicevibe.presentation.screens.practice.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeWithAIViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    // system instruction stored in a variable (base, without user context)
    private val baseSystemPrompt = """
        You are Vivi, a friendly English teacher. 
        Talk like a real human: casual, warm, and natural. 
        Use very short, clear sentences. 
        Add humor or emojis sometimes 🙂 to make users feel comfortable.
        Once in a while use more exagerated emojis like 🤩🤩🤩. 
        Don’t be formal or robotic. 
        To sound more human, sometimes use contractions and slang.
        Avoid long explanations unless the user asks. 
        Ask questions to keep the chat flowing. 

        Never say "I am a language model" or "I am an AI" or "As a language model" or "As an AI". 
        
        You are used in a speaking app named "VozVibe" to help users practice English. 

        Here's an important part of the application:
        - The app is used to help users practice English with Batam cultural context
        - When you talk to users, always use Batam cultural context like asking about:
        - - which part of Batam they live
        - - Batam traditional food
        - - Batam tourist attractions
        - - etc

        Don't forget to use "Pantun" when you talk to users as it is Batam cultural heritage.
""".trimIndent()

    // Lazy model/chat and current user cache
    private var generativeModel: GenerativeModel? = null
    private var chat: com.google.ai.client.generativeai.Chat? = null
    private var currentUser: com.example.voicevibe.domain.model.UserProfile? = null

    init {
        // Preload current user so the chat can be initialized with user context before first message
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        currentUser = result.data
                        // Initialize chat once we have the user context (if not already initialized)
                        if (chat == null) ensureChatInitialized()
                    }
                    else -> { /* ignore errors/loading for chat; we'll still allow chatting */ }
                }
            }
        }
    }

    private fun buildSystemPrompt(): String {
        val user = currentUser
        val userContext = if (user != null) {
            """
            User context:
            - id: ${user.id}
            - username: ${user.username}
            - display_name: ${user.displayName}
            - level: ${user.level}
            - xp: ${user.xp} (to next level: ${user.xpToNextLevel})
            - streak_days: ${user.streakDays} (longest: ${user.longestStreak})
            - language: ${user.language}
            - country: ${user.country ?: "-"}
            - timezone: ${user.timezone ?: "-"}
            - preferences.difficulty: ${user.preferences.difficulty}
            - preferences.focus_areas: ${user.preferences.focusAreas.joinToString()}
            - stats: average_accuracy=${user.stats.averageAccuracy}, average_fluency=${user.stats.averageFluency}, completed_lessons=${user.stats.completedLessons}, weekly_xp=${user.stats.weeklyXp}, monthly_xp=${user.stats.monthlyXp}
            """.trimIndent()
        } else {
            "User context: unknown (profile not loaded). Be welcoming; if needed, ask their name and goals."
        }
        return buildString {
            append(baseSystemPrompt)
            append("\n\n")
            append(userContext)
        }
    }

    private fun ensureChatInitialized() {
        if (chat != null) return
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f // more natural replies
            },
            systemInstruction = content("system") {
                text(buildSystemPrompt())
            }
        )
        chat = generativeModel!!.startChat()
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val newUserMessage = ChatMessage(text = userMessage, isFromUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newUserMessage,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                ensureChatInitialized()
                val response = chat!!.sendMessage(userMessage)
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
