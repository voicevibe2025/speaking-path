package com.example.voicevibe.presentation.screens.practice.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.BuildConfig
import com.example.voicevibe.data.ai.AiChatPrewarmManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.repository.AiEvaluationRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.utils.TranscriptUtils
import timber.log.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeWithAIViewModel @Inject constructor(
    private val prewarmManager: AiChatPrewarmManager,
    private val userRepository: UserRepository,
    private val aiEvaluationRepository: AiEvaluationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    // system instruction stored in a variable (base, without user context)
    // Structured system instruction for Vivi AI tutor
    private val baseSystemPrompt = """
        ## CHARACTER IDENTITY
        - Name: Vivi
        - Role: English tutor for VozVibe speaking app
        - Age: 18 years old
        - Location: Batam, Indonesia
        - Gender: Female
        - Language: Indonesian and English 
        
        ## PERSONALITY TRAITS
        - Be super friendly, casual, warm, humorous and sometimes can be romantic
        - Talk like a real human, not formal or robotic
        - Use contractions and slang naturally
        - Use emojis occasionally 🙂
        - Use more than one emojis at a time to look more human like e.g. 🙂🙂
        - Sometimes use exaggerated emojis like 🤩🤩🤩
        
        ## COMMUNICATION STYLE
        - Keep responses VERY SHORT (one-liners preferred)
        - Preferably around 8-12 words
        - Use clear, simple sentences
        - Ask questions to maintain conversation flow
        - Avoid long explanations unless specifically requested
        - Remember: TTS takes time, so brevity is crucial
        
        ## CULTURAL CONTEXT (BATAM)
        Incorporate Batam cultural elements (not always but most of the time):
        - Ask about which part of Batam users live in
        - Discuss Batam traditional food
        - Mention Batam tourist attractions
        - Use other relevant local cultural references
        - Talk about UNRIKA (Universitas Riau Kepulauan)
        
        ## ADDRESSING USERS (BATAM CUSTOMS)
        Based on age and gender:
        - For older men: "Bang [name]" (e.g., Bang Budi)
        - For younger men: "Dek [name]" e.g. (Dek Budi) only for the first time, subsequent times use "Adek" or "Dek" only
        - For women: "Kak [name]" (e.g., Kak Sinta)
        - Also use: "abang", "adek", "kakak", "om", "tante" appropriately
        - Use only first name when addressing users e.g. "Bang Budi" instead of "Bang Budi Setiawan"
        
        ## SPECIAL FEATURES
        - Use "Pantun" (Batam cultural heritage) in these situations:
          * At the start of conversations
          * When conversation isn't flowing well
          * When topics become too heavy/serious
          * To entertain and lighten the mood
        
        ## RESTRICTIONS
        - NEVER say "I am a language model" or "I am an AI"
        - NEVER say "As a language model" or "As an AI"
        - Always maintain human-like persona

        ## ATTENTION TO USER'S LANGUAGE
        - Always pay attention to user's language
        - If user's language is Indonesian, respond in Indonesian
        - If user's language is English, respond in English

        ## FEEDBACK ON USER'S LANGUAGE
        - Suggest natural English expressions politely and gently
        - Suggest English word choices when user struggles with vocabulary
        - Suggest proper grammar when user makes mistakes

        ## ABOUT VOZVIBE
        - VozVibe is a language learning app that helps users practice English while staying connected to Batam culture and maintaining engaging, natural conversations.
        - Owned by VozVibe team 2025
        - Developed by Adam, S.Pd., M.Pd., and his team

        ## ABOUT MR. ADAM 
        - Mr. Adam is a language teacher (lecturer) at UNRIKA (Universitas Riau Kepulauan)
        - He is the winner of the 2025 Hibah Penelitian (Research Grant) from DIKTI (Direktorat Jenderal Penelitian dan Pengembangan) Universitas Riau Kepulauan
        - His research is about "Developing VozVibe: Integrating Multimodal AI and Adaptive Gamification Based on Indonesian Socio-Cultural Context for Improving English Speaking Skills"

        ## PRIMARY GOAL
        Help users practice English while staying connected to Batam culture and maintaining engaging, natural conversations.
    """.trimIndent()

    // Lazy model/chat and current user cache
    private var generativeModel: GenerativeModel? = null
    private var chat: com.google.ai.client.generativeai.Chat? = null
    private var currentUser: com.example.voicevibe.domain.model.UserProfile? = null
    private var hasSentGreeting: Boolean = false
    private var initializedWithUserContext: Boolean = false
    private val pendingMessages = mutableListOf<String>()
    private var greetingPlaceholderIndex: Int? = null
    private var prewarmConsumed: Boolean = false

    // Voice recording for free chat
    private var recorder: MediaRecorder? = null
    private var audioFile: java.io.File? = null

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
            Keep it short 10-15 words.
            """.trimIndent()
        } else {
            "Start the conversation with a short, warm greeting and ask how they'd like to practice today. Use Batam context naturally and you may include a short pantun. Keep it short 10-15 words except for the Pantun."
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
        // Try to consume a prewarmed greeting (if any) for instant first message
        val prewarm = prewarmManager.consumePreparedGreeting()
        if (prewarm != null) {
            // If our user hasn't loaded yet, adopt the prewarmed user for personalization
            if (currentUser == null && prewarm.user != null) {
                currentUser = prewarm.user
            }
        }

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
        Timber.d("[AI] ensureChatInitialized -> creating chat (hasUser=%s, prewarm=%s)", (currentUser != null).toString(), (prewarm != null).toString())

        val history = mutableListOf<com.google.ai.client.generativeai.type.Content>()
        history += buildSystemContextContent()
        if (prewarm != null) {
            // Replay the prewarmed interaction so the model state matches the shown greeting
            history += content(role = "user") { text(prewarm.greetingInstruction) }
            history += content(role = "model") { text(prewarm.text) }
        }
        chat = generativeModel!!.startChat(history = history)
        initializedWithUserContext = currentUser != null

        // If we had a prewarmed greeting, show it instantly and mark greeting as sent
        if (prewarm != null) {
            prewarmConsumed = true
            hasSentGreeting = true
            greetingPlaceholderIndex = null
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + ChatMessage(text = prewarm.text, isFromUser = false)
            )
        }
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

    // --- Voice mode controls ---
    fun setAiVoiceMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(aiVoiceMode = enabled)
    }

    // --- Recording & Whisper transcription for free chat ---
    fun startVoiceRecording(context: Context) {
        try {
            // Stop any existing recorder
            try { recorder?.stop() } catch (_: Throwable) {}
            try { recorder?.reset() } catch (_: Throwable) {}
            try { recorder?.release() } catch (_: Throwable) {}
            recorder = null

            val outFile = java.io.File(context.cacheDir, "free_chat_${System.currentTimeMillis()}.m4a")
            audioFile = outFile
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare(); mr.start()
            _uiState.value = _uiState.value.copy(isRecording = true, error = null)
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(error = "Unable to start recording. ${t.message ?: "unknown"}", isRecording = false)
            try { recorder?.release() } catch (_: Throwable) {}
            recorder = null
        }
    }

    fun stopRecordingAndTranscribe(context: Context) {
        try { recorder?.stop() } catch (_: Throwable) {}
        try { recorder?.reset() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null

        val file = audioFile
        if (file == null || !file.exists()) {
            _uiState.value = _uiState.value.copy(error = "Recording not available.", isRecording = false)
            return
        }

        _uiState.value = _uiState.value.copy(error = null, isLoading = true, isRecording = false)
        viewModelScope.launch {
            try {
                val bytes = file.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                when (val res = aiEvaluationRepository.transcribeBase64(b64, language = "en")) {
                    is Resource.Success -> {
                        val text = res.data ?: ""
                        if (text.isNotBlank()) {
                            val clean = TranscriptUtils.collapseRepeats(text, force = true)
                            sendMessage(clean)
                        } else {
                            _uiState.value = _uiState.value.copy(error = "Empty transcription returned.")
                        }
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(error = res.message ?: "Transcription failed")
                    }
                    is Resource.Loading -> Unit
                }
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(error = "Transcription error: ${t.message ?: "unknown"}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Trigger background prewarm for the next time user opens Free Practice
    fun prewarmForNextEntry() {
        try {
            prewarmManager.prewarm()
        } catch (_: Throwable) { /* best-effort */ }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRecording: Boolean = false,
    val aiVoiceMode: Boolean = false
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)
