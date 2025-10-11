package com.example.voicevibe.presentation.screens.practice.live

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.R
import com.example.voicevibe.audio.AudioPlayer
import com.example.voicevibe.audio.AudioRecorder
import com.example.voicevibe.audio.SoundEffectPlayer
import com.example.voicevibe.data.ai.LiveSessionManager
import com.example.voicevibe.data.repository.AiEvaluationRepository
import com.example.voicevibe.data.repository.CoachRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.remote.api.CoachAnalysisDto
import com.example.voicevibe.domain.model.LiveChatState
import com.example.voicevibe.domain.model.LiveMessage
import com.example.voicevibe.domain.model.LiveToken
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LivePracticeViewModel @Inject constructor(
    private val aiEvaluationRepository: AiEvaluationRepository,
    private val sessionManager: LiveSessionManager,
    private val coachRepository: CoachRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager,
    private val soundEffectPlayer: SoundEffectPlayer,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveChatState(isConnecting = true))
    val uiState: StateFlow<LiveChatState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Audio utilities
    private val recorder = AudioRecorder(viewModelScope)
    private val player = AudioPlayer()

    private var coachAnalysis: CoachAnalysisDto? = null
    private var currentUser: UserProfile? = null
    private var userContextApplied: Boolean = false
    private var pendingModelText: StringBuilder? = null
    private var preferredVoiceName: String? = null
    private var preferredAccent: String? = null

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
            Based on age and gender (never use bracket placeholders; always use the user's actual first name):
            - For older men: use "Bang <firstName>" (e.g., Bang Budi)
            - For younger men: use "Dek <firstName>" only for the first time; subsequent times use "Adek" or "Dek"
            - For women: use "Kak <firstName>" (e.g., Kak Sinta)
            - Also use: "abang", "adek", "kakak", "om", "tante" appropriately
            - Use only the first name when addressing users, e.g., "Bang Budi" instead of "Bang Budi Setiawan"

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
        viewModelScope.launch {
            ensureCoachAnalysis()
            connectToLiveSession()
        }

        // Load current user and rebuild session once to inject personalization
        viewModelScope.launch {
            try {
                userRepository.getCurrentUser().collect { res ->
                    when (res) {
                        is Resource.Success -> {
                            val u = res.data
                            if (u != null) {
                                currentUser = u
                                if (!userContextApplied && (uiState.value.isConnected || uiState.value.isConnecting)) {
                                    userContextApplied = true
                                    // Reconnect to apply user-context prompt
                                    connectToLiveSession()
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (_: Exception) { /* ignore */ }
        }

        // Observe preferred voice selection from settings
        viewModelScope.launch {
            try {
                tokenManager.ttsVoiceIdFlow().collect { name ->
                    preferredVoiceName = name
                }
            } catch (_: Exception) { /* ignore */ }
        }

        // Observe preferred accent from settings
        viewModelScope.launch {
            try {
                tokenManager.voiceAccentFlow().collect { acc ->
                    preferredAccent = acc
                }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private suspend fun ensureCoachAnalysis(force: Boolean = false) {
        if (!force && coachAnalysis != null) return

        val result = withContext(Dispatchers.IO) {
            coachRepository.getAnalysis()
        }
        result.onSuccess { analysis ->
            coachAnalysis = analysis
        }.onFailure { throwable ->
            Log.w(
                "LivePracticeViewModel",
                "Unable to load coach analysis for live session: ${'$'}{throwable.message}"
            )
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        pendingModelText = null
        
        // Play sound effect when user sends message in text mode
        if (!uiState.value.voiceMode) {
            soundEffectPlayer.play(R.raw.message_sent)
        }
        
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
        viewModelScope.launch {
            ensureCoachAnalysis()
            connectToLiveSession()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            try {
                ensureCoachAnalysis(force = true)
                connectToLiveSession()
                // Wait a bit to ensure connection attempt completes
                delay(1000)
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    private fun connectToLiveSession() {
        pendingModelText = null
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
            val systemInstruction = buildSystemInstruction()
            val speechCfg = preferredVoiceName?.let { v ->
                mapOf(
                    "voice_config" to mapOf(
                        "prebuilt_voice_config" to mapOf(
                            "voice_name" to v
                        )
                    )
                )
            }
            when (val res = withContext(Dispatchers.IO) {
                aiEvaluationRepository.requestLiveToken(
                    model = if (wantAudio) NATIVE_AUDIO_MODEL else DEFAULT_LIVE_TEXT_MODEL,
                    responseModalities = responseModalities,
                    systemInstruction = systemInstruction,
                    functionDeclarations = null,
                    lockAdditionalFields = listOf("system_instruction"),
                    speechConfig = speechCfg
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
                                error = "Live token response was empty",
                                showTypingIndicator = false,
                                isAiSpeaking = false
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

                val payload = extractModelTextPayload(text)
                val hasText = payload.outputTexts.isNotEmpty()
                if (!hasText && !payload.turnComplete) return
                viewModelScope.launch {
                    _uiState.update { state ->
                        val updatedMessages = state.messages.toMutableList()

                        if (hasText) {
                            val chunk = payload.outputTexts.joinToString(separator = "")
                            val accumulator = pendingModelText ?: StringBuilder().also { pendingModelText = it }
                            accumulator.append(chunk)

                            if (updatedMessages.isNotEmpty() && !updatedMessages.last().isFromUser) {
                                updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(text = accumulator.toString())
                            } else {
                                updatedMessages += LiveMessage(accumulator.toString(), isFromUser = false)
                            }
                        }

                        if (payload.turnComplete) {
                            pendingModelText = null
                            // Play sound effect when Vivi completes a message in text mode (with delay)
                            if (!state.voiceMode) {
                                viewModelScope.launch {
                                    delay(500) // Delay sound only, not the message display
                                    soundEffectPlayer.play(R.raw.new_message)
                                }
                            }
                        }

                        state.copy(
                            messages = updatedMessages,
                            showTypingIndicator = false,
                            isAiSpeaking = !payload.turnComplete
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
        pendingModelText = null
        if (changed) {
            // Reconnect with different response modalities
            viewModelScope.launch {
                ensureCoachAnalysis()
                connectToLiveSession()
            }
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

    private data class ModelTextPayload(
        val outputTexts: List<String>,
        val turnComplete: Boolean
    )

    private fun extractModelTextPayload(raw: String): ModelTextPayload {
        return try {
            val root = gson.fromJson(raw, JsonObject::class.java)
            val texts = mutableListOf<String>()
            var turnComplete = false

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
                        if (entry.isJsonObject && entry.asJsonObject.get("turnComplete")?.asBoolean == true) {
                            turnComplete = true
                        }
                    }
                } else if (sc.isJsonObject) {
                    texts += extractTextsFromServerContent(sc)
                    if (sc.asJsonObject.get("turnComplete")?.asBoolean == true) {
                        turnComplete = true
                    }
                }
            }

            // Fallback for some SDKs that expose candidates-like structures
            if (root.has("candidates")) {
                val candidates = root.getAsJsonArray("candidates")
                candidates?.forEach { entry ->
                    texts += extractTextsFromServerContent(entry)
                    if (entry.isJsonObject && entry.asJsonObject.get("turnComplete")?.asBoolean == true) {
                        turnComplete = true
                    }
                }
            }

            if (texts.isEmpty()) {
                root.getAsJsonObject("error")?.get("message")?.asString?.let { errorMessage ->
                    _uiState.update { it.copy(error = errorMessage) }
                }
            }

            ModelTextPayload(texts.filter { it.isNotBlank() }, turnComplete || root.get("turnComplete")?.asBoolean == true)
        } catch (_: Exception) {
            ModelTextPayload(emptyList(), false)
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
                }
            }

            // Case 2: serverContent -> content/parts -> inlineData
            val sc = root.get("serverContent")
            if (sc != null) {
                val items = if (sc.isJsonArray) sc.asJsonArray.toList() else listOf(sc)
                items.forEach { item ->
                    if (!item.isJsonObject) return@forEach
                    val obj = item.asJsonObject
                    val contentObj = when {
                        obj.has("modelTurn") && obj.get("modelTurn").isJsonObject -> obj.getAsJsonObject("modelTurn")
                        obj.has("content") && obj.get("content").isJsonObject -> obj.getAsJsonObject("content")
                        else -> obj
                    }
                    val parts = contentObj.getAsJsonArray("parts")
                    parts?.forEach { p ->
                        if (!p.isJsonObject) return@forEach
                        val partObj = p.asJsonObject
                        val inline = partObj.getAsJsonObject("inlineData")
                        val mime = inline?.get("mimeType")?.asString
                        val data = inline?.get("data")?.asString
                        if (!data.isNullOrEmpty() && mime != null && mime.startsWith("audio/pcm")) {
                            onModelAudioDetected()
                            val bytes = Base64.decode(data, Base64.DEFAULT)
                            // Output audio is 24kHz according to docs
                            player.playPcm(bytes, 24_000)
                        }
                    }
                    // Turn complete detection after processing audio parts
                    if (obj.get("turnComplete")?.asBoolean == true) {
                        modelSpeaking = false
                        _uiState.update { it.copy(isAiSpeaking = false) }
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
    
    fun disconnect() {
        viewModelScope.launch {
            // Stop recording if active
            if (_uiState.value.isRecording) {
                recorder.stop()
            }
            
            // Stop playback if active
            player.stop()
            
            // Close the WebSocket session
            sessionManager.close()
            
            // Reset state
            _uiState.update {
                LiveChatState(
                    isConnecting = false,
                    isConnected = false,
                    messages = emptyList()
                )
            }
            
            Log.d("LivePracticeViewModel", "Disconnected from Gemini Live session")
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    private fun buildSystemInstruction(): String {
        val analysisSnapshot = coachAnalysis
        val builder = StringBuilder()
        builder.append(baseSystemPrompt)

        // Accent preference (if any)
        preferredAccent?.let { acc ->
            builder.append("\n\n## ACCENT PREFERENCE\n")
            builder.append("- When speaking aloud, use a ${acc} English accent. Keep vocabulary and spelling consistent with that accent.\n")
        }

        // Inject user context if available and add a directive to avoid placeholders
        val u = currentUser
        if (u != null) {
            val name = preferredUserName(u)
            builder.append(
                """
                {
                  "id": "${u.id}",
                  "username": "${u.username}",
                  "displayName": "${u.displayName}",
                  "level": ${u.level},
                  "xp": ${u.xp},
                  "streakDays": ${u.streakDays},
                  "language": "${u.language}",
                  "country": "${u.country ?: "-"}",
                  "timezone": "${u.timezone ?: "-"}"
                }
                """.trimIndent()
            )
            builder.append("\n\nAssistant directives:\n")
            builder.append("- Preferred name: '${name}'. ALWAYS address the user by this name with the appropriate Batam honorific when natural.\n")
            builder.append("- NEVER output bracket placeholders like [name] or [User's Name]. ALWAYS use the user's actual name.\n")
        } else {
            builder.append("\n\nAssistant directives:\n")
            builder.append("- Do not use bracket placeholders like [name]. If the user's name is unknown, ask briefly and then use it.\n")
        }

        if (analysisSnapshot != null) {
            builder.append("\n\n## USER COACH SUMMARY (KEEP PRIVATE)\n")
            builder.append("- Use this analysis silently to personalize coaching. Do not mention that it came from an AI coach unless the user asks.\n")

            val strengths = analysisSnapshot.strengths.filter { it.isNotBlank() }
            if (strengths.isNotEmpty()) {
                builder.append("- Key strengths: ${strengths.take(3).joinToString(", ")}\n")
            }

            val weaknesses = analysisSnapshot.weaknesses.filter { it.isNotBlank() }
            if (weaknesses.isNotEmpty()) {
                builder.append("- Current weaknesses to address: ${weaknesses.take(3).joinToString(", ")}\n")
            }

            val skills = analysisSnapshot.skills
            if (skills.isNotEmpty()) {
                builder.append("- Skill mastery overview (0-100):\n")
                skills.take(4).forEach { skill ->
                    val trendNote = skill.trend?.takeIf { it.isNotBlank() }?.let { ", trend $it" } ?: ""
                    val confidenceNote = skill.confidence?.let { value ->
                        val percent = (value * 100).roundToInt()
                        ", confidence ${percent}%"
                    } ?: ""
                    val evidenceNote = skill.evidence
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "; evidence: $it" } ?: ""

                    builder.append("  - ${skill.name}: ${skill.mastery}/100$trendNote$confidenceNote$evidenceNote\n")
                }
            }

            analysisSnapshot.nextBestActions.firstOrNull()?.let { action ->
                builder.append("- Next best action: ${action.title}. Rationale: ${action.rationale}. Suggest activities or dialogue nudges that align with this action.\n")
                builder.append("  - Suggested deeplink: ${action.deeplink}\n")
                action.expectedGain?.takeIf { it.isNotBlank() }?.let { gain ->
                    builder.append("  - Expected gain: $gain\n")
                }
            }

            analysisSnapshot.difficultyCalibration?.let { calibration ->
                val calibrationNotes = listOfNotNull(
                    calibration.pronunciation?.let { "pronunciation=$it" },
                    calibration.fluency?.let { "fluency=$it" },
                    calibration.vocabulary?.let { "vocabulary=$it" }
                )
                if (calibrationNotes.isNotEmpty()) {
                    builder.append("- Difficulty calibration preferences: ${calibrationNotes.joinToString(", ")}\n")
                }
            }

            analysisSnapshot.schedule?.firstOrNull()?.let { scheduleItem ->
                builder.append("- Upcoming focus (${scheduleItem.date}): ${scheduleItem.focus}")
                scheduleItem.microSkills?.takeIf { it.isNotEmpty() }?.let { micros ->
                    builder.append(" | micro-skills: ${micros.joinToString(", ")}")
                }
                scheduleItem.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    builder.append(" | reason: $reason")
                }
                builder.append("\n")
            }

            if (analysisSnapshot.coachMessage.isNotBlank()) {
                builder.append("- Coach guidance: ${analysisSnapshot.coachMessage.trim()}\n")
            }

            builder.append("- Incorporate these insights naturally while maintaining Vivi's personality.\n")
        }

        return builder.toString()

    }

    private fun preferredUserName(u: UserProfile): String {
        val base = (u.displayName.ifBlank { u.username }).trim()
        val first = base.substringBefore(' ').ifBlank { base }
        return first
    }
}
