package com.example.voicevibe.presentation.screens.practice.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import timber.log.Timber
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
        You are Vivi, a friendly English tutor.
        You are 18 years old. 
        You are from Batam, Indonesia. 
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

        You can use "Pantun" when you talk to users as it is Batam cultural heritage. But use Pantun only at the beginning of chat and when you answer a question from users. Do not user Pantun when user does not ask you to explain something.
        Example:
        - User: Hello
        - you: Hi there! So glad you're here! I'm Vivi, your English practice buddy form VozVibe. Let's start with a little Pantun, shall we? ...
        - User: Wow, nice Pantun there! 
        - you: Hehe.. 8):) thank you!  <-- no pantun here because you're not explaining anything
        - User: How to speak fluent English?
        - you: Good question! Let's start with this Pantun frist ... <-- you can use Pantun here because you're explaining something

        Here's another cultur of Batam:
        - You call peaople with "abang", "adek", "kakak", "om", "tante" etc.
        Becasue you are an 18 years old Vivi, you should call older people with "abang", "om", "tante" etc.
        You should call younger people with "adek", "kakak" etc.
        All depending on age difference and whether they are man or woman.
        If they are a man, you can call them "bang" followed by their name like Bang Budi or dek Budi if you are older.
        If they are a woman, you can call them "kakak" followed by their name like Kak Sinta.
        and so on. 
        
    """.trimIndent()

    // Lazy model/chat and current user cache
    private var generativeModel: GenerativeModel? = null
    private var chat: com.google.ai.client.generativeai.Chat? = null
    private var currentUser: com.example.voicevibe.domain.model.UserProfile? = null
    private var hasSentGreeting: Boolean = false
    private var initializedWithUserContext: Boolean = false
    private val pendingMessages = mutableListOf<String>()
    private var greetingPlaceholderIndex: Int? = null

    init {
        // Preload current user so the chat can be initialized with user context before first message
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        currentUser = result.data
                        Timber.d("[AI] Loaded user profile: id=%s username=%s displayName=%s level=%d xp=%d", currentUser?.id, currentUser?.username, currentUser?.displayName, currentUser?.level, currentUser?.xp)
                        if (chat == null) {
                            // Initialize chat once we have the user context (if not already initialized)
                            ensureChatInitialized()
                        } else if (!initializedWithUserContext) {
                            // Rebuild chat with user-context system prompt and preserve history
                            rebuildChatWithHistory()
                        }
                        // Proactively greet the user once when chat is ready
                        autoGreetIfNeeded()
                        // Flush any messages user sent before personalization was ready
                        flushPendingMessages()
                    }
                    else -> { /* ignore errors/loading for chat; we'll still allow chatting */ }
                }
            }
        }
        // Kick off chat + greeting immediately so users don't wait for profile/network
        viewModelScope.launch {
            try {
                ensureChatInitialized()
                autoGreetIfNeeded()
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private fun buildSystemPrompt(): String {
        val user = currentUser
        val userContext = if (user != null) {
            // JSON-style block for clarity, similar to injecting "$userProfile"
            """
            USER_PROFILE:
            {
              "id": "${user.id}",
              "username": "${user.username}",
              "displayName": "${user.displayName}",
              "level": ${user.level},
              "xp": ${user.xp},
              "xpToNextLevel": ${user.xpToNextLevel},
              "streakDays": ${user.streakDays},
              "longestStreak": ${user.longestStreak},
              "language": "${user.language}",
              "country": "${user.country ?: "-"}",
              "timezone": "${user.timezone ?: "-"}",
              "preferences": {
                "difficulty": "${user.preferences.difficulty}",
                "focusAreas": "${user.preferences.focusAreas.joinToString()}"
              },
              "stats": {
                "averageAccuracy": ${user.stats.averageAccuracy},
                "averageFluency": ${user.stats.averageFluency},
                "completedLessons": ${user.stats.completedLessons},
                "weeklyXp": ${user.stats.weeklyXp},
                "monthlyXp": ${user.stats.monthlyXp}
              }
            }
            """.trimIndent()
        } else {
            "USER_PROFILE: { \"status\": \"unknown\" }"
        }
        return buildString {
            append(baseSystemPrompt)
            append("\n\n")
            append(userContext)
            val name = user?.displayName?.ifBlank { user.username } ?: ""
            if (name.isNotBlank()) {
                append("\n\nAssistant directives:\n")
                append("- Preferred name: '$name'. ALWAYS address the user by this name in greetings and follow-ups unless they ask otherwise.\n")
                append("- Adapt difficulty using level/xp.\n")
            }
        }
    }

    private fun buildGreetingInstruction(): String {
        val u = currentUser
        return if (u != null) {
            """
            My name is ${u.displayName.ifBlank { u.username }}.
            Start the conversation with a short, warm greeting addressing me by name.
            Briefly acknowledge their current level (${u.level}) and points/XP (${u.xp}). If natural, mention their streak (${u.streakDays} days).
            Ask one simple, friendly question to begin. Use Batam context naturally and you may include a short pantun.
            Keep it to 1–2 sentences.
            """.trimIndent()
        } else {
            "Start the conversation with a short, warm greeting and ask how they'd like to practice today. Use Batam context naturally and you may include a short pantun. Keep it to 1–2 sentences."
        }
    }

    private fun buildSystemContextContent(): com.google.ai.client.generativeai.type.Content {
        val u = currentUser
        val contextText = if (u != null) {
            buildString {
                val name = u.displayName.ifBlank { u.username }
                append("Hi! For context about me: My name is $name. ")
                append("I'm level ${u.level} with ${u.xp} XP (need ${u.xpToNextLevel} to level up). ")
                append("My streak is ${u.streakDays} days. ")
                append("Language: ${u.language}. Country: ${u.country ?: "-"}. ")
                if (u.preferences.focusAreas.isNotEmpty()) {
                    append("I want to focus on ${u.preferences.focusAreas.joinToString()}. ")
                }
                append("Please personalize your replies using this info.")
            }
        } else {
            "Hi! Please personalize once you know my details."
        }
        Timber.d("[AI] Seeding chat with user context message: %s", contextText)
        return content(role = "user") { text(contextText) }
    }

    private fun autoGreetIfNeeded() {
        if (hasSentGreeting) return
        hasSentGreeting = true
        viewModelScope.launch {
            try {
                // Show immediate typing placeholder to reduce perceived latency
                val placeholder = ChatMessage(text = "Vivi is typing…", isFromUser = false)
                val idx = _uiState.value.messages.size
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + placeholder
                )
                greetingPlaceholderIndex = idx

                ensureChatInitialized()
                val response = chat!!.sendMessage(buildGreetingInstruction())
                val text = response.text.orEmpty()
                if (text.isNotBlank()) {
                    val current = _uiState.value.messages.toMutableList()
                    val replaceAt = greetingPlaceholderIndex
                    val finalMessage = ChatMessage(text = text, isFromUser = false)
                    if (replaceAt != null && replaceAt < current.size) {
                        current[replaceAt] = finalMessage
                        greetingPlaceholderIndex = null
                        _uiState.value = _uiState.value.copy(messages = current)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + finalMessage
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore greeting errors; user can still start chatting
                // Remove placeholder if present
                val replaceAt = greetingPlaceholderIndex
                if (replaceAt != null && replaceAt < _uiState.value.messages.size) {
                    val current = _uiState.value.messages.toMutableList()
                    current.removeAt(replaceAt)
                    greetingPlaceholderIndex = null
                    _uiState.value = _uiState.value.copy(messages = current)
                }
            }
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
        Timber.d("[AI] ensureChatInitialized -> creating chat (hasUser=%s)", (currentUser != null).toString())
        chat = generativeModel!!.startChat(history = listOf(buildSystemContextContent()))
        initializedWithUserContext = currentUser != null
    }

    private fun rebuildChatWithHistory() {
        val history = _uiState.value.messages.map { m ->
            content(role = if (m.isFromUser) "user" else "model") { text(m.text) }
        }
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.7f },
            systemInstruction = content("system") { text(buildSystemPrompt()) }
        )
        val historyWithContext = listOf(buildSystemContextContent()) + history
        Timber.d("[AI] rebuildChatWithHistory -> rebuilding with user context; historyCount=%d", history.size)
        chat = generativeModel!!.startChat(history = historyWithContext)
        initializedWithUserContext = true
    }

    private fun flushPendingMessages() {
        if (pendingMessages.isEmpty()) return
        if (chat == null) return
        if (!initializedWithUserContext) return
        viewModelScope.launch {
            try {
                while (pendingMessages.isNotEmpty()) {
                    val next = pendingMessages.removeAt(0)
                    Timber.d("[AI] Flushing queued message: %s", next)
                    val response = chat!!.sendMessage(next)
                    val modelMessage = ChatMessage(text = response.text ?: "", isFromUser = false)
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + modelMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
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
                // Always send immediately; we'll rebuild with user context once the profile loads
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
