package com.example.voicevibe.presentation.screens.practice.live

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.audio.AudioPlayer
import com.example.voicevibe.audio.AudioRecorder
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // Audio utilities
    private val recorder = AudioRecorder(viewModelScope)
    private val player = AudioPlayer()

    companion object {
        private const val NATIVE_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-09-2025"
        private const val DEFAULT_LIVE_TEXT_MODEL = "gemini-live-2.5-flash-preview"
        private const val MODEL_SPEAKING_SILENCE_MS = 1200L
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
    }

    init {
        connectToLiveSession()
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        _uiState.update {
            it.copy(
                messages = it.messages + LiveMessage(message, isFromUser = true),
                showTypingIndicator = true,
                isAiSpeaking = false
            )
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
                error = null,
                showTypingIndicator = false,
                isAiSpeaking = false
            )
        }
        sessionManager.close()
        viewModelScope.launch {
            val wantAudio = uiState.value.voiceMode
            val responseModalities = if (wantAudio) listOf("AUDIO") else listOf("TEXT")
            when (val res = withContext(Dispatchers.IO) {
                aiEvaluationRepository.requestLiveToken(
                    model = if (wantAudio) NATIVE_AUDIO_MODEL else DEFAULT_LIVE_TEXT_MODEL,
                    responseModalities = responseModalities,
                    systemInstruction = baseSystemPrompt,
                    lockAdditionalFields = listOf("system_instruction")
                )
            }) {
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
                            error = res.message ?: "Unable to request live token",
                            showTypingIndicator = false,
                            isAiSpeaking = false
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
        val wantAudio = uiState.value.voiceMode
        val responseModalities = if (wantAudio) listOf("AUDIO") else listOf("TEXT")
        sessionManager.connect(token.token, token.model, responseModalities, object : LiveSessionManager.Listener {
            override fun onOpen() {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            error = null,
                            showTypingIndicator = false
                        )
                    }
                }
            }

            override fun onMessage(text: String) {
                // First, try to play audio if present
                handleAudioFromServer(text)

                val messages = extractModelTexts(text)
                if (messages.isEmpty()) return
                viewModelScope.launch {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + messages.map { LiveMessage(it, isFromUser = false) },
                            showTypingIndicator = false,
                            isAiSpeaking = false
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
                            error = t.message ?: "Live session connection failed",
                            showTypingIndicator = false,
                            isAiSpeaking = false
                        )
                    }
                }
            }

            override fun onClosed() {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            showTypingIndicator = false,
                            isAiSpeaking = false
                        )
                    }
                }
            }
        })
    }

    // Public controls for voice mode and recording
    fun setVoiceMode(enabled: Boolean) {
        val changed = uiState.value.voiceMode != enabled
        if (!enabled && uiState.value.isRecording) {
            stopRecording()
        }
        _uiState.update {
            it.copy(
                voiceMode = enabled,
                showTypingIndicator = false,
                isAiSpeaking = false
            )
        }
        if (changed) {
            // Reconnect with different response modalities
            connectToLiveSession()
        }
    }

    fun toggleVoiceMode() {
        setVoiceMode(!uiState.value.voiceMode)
    }

    fun toggleRecording() {
        if (!uiState.value.voiceMode) {
            // Enable voice mode and reconnect, then start recording
            setVoiceMode(true)
        }
        if (uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (uiState.value.isRecording) return
        _uiState.update { it.copy(isRecording = true, showTypingIndicator = false) }
        recorder.start(sampleRateHz = 16_000) { chunk ->
            // Gate mic audio while model is speaking to prevent feedback loops
            if (!modelSpeaking) {
                sessionManager.sendAudioChunk(chunk, 16_000)
            }
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            try {
                recorder.stop()
            } finally {
                sessionManager.sendAudioStreamEnd()
                _uiState.update { it.copy(isRecording = false, showTypingIndicator = false) }
            }
        }
    }

    private fun extractModelTexts(raw: String): List<String> {
        return try {
            val root = gson.fromJson(raw, JsonObject::class.java)
            val texts = mutableListOf<String>()

            // Handle Live API response types
            if (root.has("setupComplete")) {
                // No text here; just acknowledgement from server.
            }

            // Input or output transcriptions are separate server messages
            root.getAsJsonObject("inputTranscription")?.get("text")?.asString?.let { t ->
                if (t.isNotBlank()) {
                    viewModelScope.launch {
                        _uiState.update {
                            it.copy(
                                messages = it.messages + LiveMessage(t, isFromUser = true),
                                showTypingIndicator = false
                            )
                        }
                    }
                }
            }
            root.getAsJsonObject("outputTranscription")?.get("text")?.asString?.let { t ->
                if (t.isNotBlank()) {
                    _uiState.update { it.copy(isAiSpeaking = true, showTypingIndicator = false) }
                    texts.add(t)
                }
            }

            if (root.has("serverContent")) {
                val sc = root.get("serverContent")
                if (sc.isJsonArray) {
                    sc.asJsonArray.forEach { entry ->
                        texts += extractTextsFromServerContent(entry)
                    }
                } else if (sc.isJsonObject) {
                    texts += extractTextsFromServerContent(sc)
                }
            }

            // Fallback for some SDKs that expose candidates-like structures
            if (root.has("candidates")) {
                val candidates = root.getAsJsonArray("candidates")
                candidates?.forEach { entry ->
                    texts += extractTextsFromServerContent(entry)
                }
            }

            if (texts.isEmpty()) {
                root.getAsJsonObject("error")?.get("message")?.asString?.let { errorMessage ->
                    _uiState.update { it.copy(error = errorMessage) }
                }
            }

            texts.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractTextsFromServerContent(element: JsonElement): List<String> {
        val collected = mutableListOf<String>()
        if (!element.isJsonObject) return collected
        val obj = element.asJsonObject

        // Try typical shape: { serverContent: { content: { parts: [ { text: ... } ] } } }
        fun collectFromContentObject(contentObj: JsonObject) {
            val parts = contentObj.getAsJsonArray("parts")
            parts?.forEach { partElement ->
                if (partElement.isJsonObject) {
                    val partObj = partElement.asJsonObject
                    val text = partObj.get("text")?.takeIf { it.isJsonPrimitive }?.asString
                    if (!text.isNullOrBlank()) collected.add(text)
                }
            }
        }

        when {
            // serverContent.modelTurn: Content
            obj.has("modelTurn") && obj.get("modelTurn").isJsonObject -> {
                collectFromContentObject(obj.getAsJsonObject("modelTurn"))
            }
            obj.has("content") && obj.get("content").isJsonObject -> {
                collectFromContentObject(obj.getAsJsonObject("content"))
            }
            obj.has("contents") && obj.get("contents").isJsonArray -> {
                obj.getAsJsonArray("contents").forEach { co ->
                    if (co.isJsonObject && co.asJsonObject.has("parts")) {
                        collectFromContentObject(co.asJsonObject)
                    }
                }
            }
            // Some responses may directly contain parts
            obj.has("parts") && obj.get("parts").isJsonArray -> {
                collectFromContentObject(obj)
            }
        }

        return collected
    }

    private fun handleAudioFromServer(raw: String) {
        try {
            val root = gson.fromJson(raw, JsonObject::class.java)

            // Case 1: Native audio models may stream base64 under top-level data
            val topData = root.get("data")
            if (topData != null && topData.isJsonPrimitive) {
                val base64 = topData.asString
                if (!base64.isNullOrEmpty()) {
                    onModelAudioDetected()
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    player.playPcm(bytes, 24_000)
                    return
                }
            }

            // Case 2: serverContent -> content/parts -> inlineData
            val sc = root.get("serverContent")
            if (sc != null) {
                val items = if (sc.isJsonArray) sc.asJsonArray.toList() else listOf(sc)
                items.forEach { item ->
                    if (item.isJsonObject) {
                        val obj = item.asJsonObject
                        // Turn complete detection
                        if (obj.get("turnComplete")?.asBoolean == true) {
                            modelSpeaking = false
                            _uiState.update { it.copy(isAiSpeaking = false) }
                        }
                        val contentObj = when {
                            obj.has("modelTurn") && obj.get("modelTurn").isJsonObject -> obj.getAsJsonObject("modelTurn")
                            obj.has("content") && obj.get("content").isJsonObject -> obj.getAsJsonObject("content")
                            else -> obj
                        }
                        val parts = contentObj.getAsJsonArray("parts")
                        parts?.forEach { p ->
                            if (p.isJsonObject) {
                                val partObj = p.asJsonObject
                                val inline = partObj.getAsJsonObject("inlineData")
                                val mime = inline?.get("mimeType")?.asString
                                val data = inline?.get("data")?.asString
                                if (!data.isNullOrEmpty() && mime != null && mime.startsWith("audio/pcm")) {
                                    onModelAudioDetected()
                                    val bytes = Base64.decode(data, Base64.DEFAULT)
                                    // Output audio is 24kHz according to docs
                                    player.playPcm(bytes, 24_000)
                                    return
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    // Duplex/echo control
    private var modelSpeaking: Boolean = false
    private var modelSpeakingClearJob: Job? = null
    private fun onModelAudioDetected() {
        modelSpeaking = true
        // Auto-stop recording if user forgot to tap, to close the turn and avoid echo
        if (uiState.value.isRecording) {
            stopRecording()
        }
        // Debounce: clear after some silence since last audio chunk
        modelSpeakingClearJob?.cancel()
        modelSpeakingClearJob = viewModelScope.launch {
            delay(MODEL_SPEAKING_SILENCE_MS)
            modelSpeaking = false
            _uiState.update { it.copy(isAiSpeaking = false) }
        }
        _uiState.update { it.copy(isAiSpeaking = true, showTypingIndicator = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.close()
        player.release()
    }
}
