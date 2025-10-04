package com.example.voicevibe.presentation.screens.speakingjourney

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.audio.AudioPlayer
import com.example.voicevibe.audio.AudioRecorder
import com.example.voicevibe.data.ai.LiveSessionManager
import com.example.voicevibe.data.repository.AiEvaluationRepository
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
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
class LearnTopicWithViviViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val aiEvaluationRepository: AiEvaluationRepository,
    private val sessionManager: LiveSessionManager,
    private val userRepository: UserRepository,
    private val speakingJourneyRepository: SpeakingJourneyRepository,
    private val gamificationRepository: GamificationRepository,
    private val gson: Gson
) : ViewModel() {

    private val topicId: String = checkNotNull(savedStateHandle.get<String>("topicId"))
    
    private val _uiState = MutableStateFlow(LearnWithViviState())
    val uiState: StateFlow<LearnWithViviState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Audio utilities
    private val recorder = AudioRecorder(viewModelScope)
    private val player = AudioPlayer()

    private var currentUser: UserProfile? = null
    private var topicData: Topic? = null
    private var userContextApplied: Boolean = false
    private var pendingModelText: StringBuilder? = null
    private var currentPhraseIndex: Int = 0
    private var phrasesCompleted: MutableSet<Int> = mutableSetOf()

    companion object {
        private const val NATIVE_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-09-2025"
        private const val DEFAULT_LIVE_TEXT_MODEL = "gemini-live-2.5-flash-preview"
        private const val MODEL_SPEAKING_SILENCE_MS = 1200L
        private const val TAG = "LearnTopicWithVivi"
        private const val XP_PER_PHRASE = 10
        private const val COMPLETION_BONUS_XP = 50
    }

    init {
        viewModelScope.launch {
            loadTopicData()
            loadCurrentUser()
            connectToLiveSession()
        }
    }

    private suspend fun loadTopicData() {
        _uiState.update { it.copy(isLoadingTopic = true) }
        try {
            val result = speakingJourneyRepository.getTopics()
            result.fold(
                onSuccess = { response ->
                    val topicDto = response.topics.firstOrNull { it.id == topicId }
                    topicData = topicDto?.let { mapDtoToTopic(it) }
                    _uiState.update {
                        it.copy(
                            isLoadingTopic = false,
                            topic = topicData,
                            totalPhrases = topicData?.conversation?.size ?: 0
                        )
                    }
                    if (topicData != null && !userContextApplied) {
                        // Reconnect with topic data
                        connectToLiveSession()
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingTopic = false,
                            error = error.message ?: "Failed to load topic"
                        )
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingTopic = false,
                    error = e.message ?: "Failed to load topic"
                )
            }
        }
    }

    private suspend fun loadCurrentUser() {
        try {
            userRepository.getCurrentUser().collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val u = res.data
                        if (u != null) {
                            currentUser = u
                            if (!userContextApplied && topicData != null) {
                                userContextApplied = true
                                connectToLiveSession()
                            }
                        }
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) { /* ignore */ }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        pendingModelText = null
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
            connectToLiveSession()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            try {
                loadTopicData()
                connectToLiveSession()
                delay(1000)
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    private fun connectToLiveSession() {
        if (topicData == null) return // Wait for topic data

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
            val wantAudio = true // Always use audio for this feature
            val responseModalities = listOf("AUDIO")
            val systemInstruction = buildSystemInstruction()
            
            // Register function declarations
            val functionDeclarations = buildFunctionDeclarations()
            
            when (val res = withContext(Dispatchers.IO) {
                aiEvaluationRepository.requestLiveToken(
                    model = NATIVE_AUDIO_MODEL,
                    responseModalities = responseModalities,
                    systemInstruction = systemInstruction,
                    functionDeclarations = functionDeclarations,
                    lockAdditionalFields = listOf("system_instruction", "tools")
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
                    // Ignored
                }
            }
        }
    }

    private fun openSession(token: LiveToken) {
        val responseModalities = listOf("AUDIO")
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
                // Handle audio from server
                handleAudioFromServer(text)
                
                // Handle function calls
                handleFunctionCalls(text)

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

    fun toggleRecording() {
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

    private fun handleFunctionCalls(text: String) {
        try {
            val root = gson.fromJson(text, JsonObject::class.java)
            
            // Check for function calls in serverContent
            val sc = root.get("serverContent")
            if (sc != null) {
                val items = if (sc.isJsonArray) sc.asJsonArray.toList() else listOf(sc)
                items.forEach { item ->
                    if (!item.isJsonObject) return@forEach
                    val obj = item.asJsonObject
                    
                    // Look for function calls
                    val functionCall = obj.getAsJsonObject("functionCall")
                    if (functionCall != null) {
                        val functionName = functionCall.get("name")?.asString
                        val args = functionCall.getAsJsonObject("args")
                        
                        when (functionName) {
                            "play_phrase_audio" -> {
                                val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                                handlePlayPhraseAudio(phraseIndex)
                            }
                            "mark_phrase_completed" -> {
                                val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                                handleMarkPhraseCompleted(phraseIndex)
                            }
                            "award_completion_xp" -> {
                                handleAwardCompletionXp()
                            }
                        }
                        
                        // Send function response back
                        sendFunctionResponse(functionName ?: "unknown", "success")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling function calls: ${e.message}")
        }
    }

    private fun handlePlayPhraseAudio(phraseIndex: Int) {
        Log.d(TAG, "Playing audio for phrase $phraseIndex")
        val conversation = topicData?.conversation
        if (conversation != null && phraseIndex < conversation.size) {
            currentPhraseIndex = phraseIndex
            _uiState.update { it.copy(currentPhraseIndex = phraseIndex) }
            // Audio will be played via TTS - you can implement this similar to ConversationLesson
            // For now, just log it
        }
    }

    private fun handleMarkPhraseCompleted(phraseIndex: Int) {
        Log.d(TAG, "Marking phrase $phraseIndex as completed")
        phrasesCompleted.add(phraseIndex)
        currentPhraseIndex = phraseIndex + 1
        
        viewModelScope.launch {
            // Award XP for completing a phrase
            val result = gamificationRepository.addExperience(
                points = XP_PER_PHRASE,
                source = "learn_topic_phrase_${topicId}_$phraseIndex"
            )
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Awarded $XP_PER_PHRASE XP for phrase completion")
                }
                else -> {
                    Log.e(TAG, "Failed to award XP for phrase completion")
                }
            }
        }
        
        _uiState.update { 
            it.copy(
                phrasesCompleted = phrasesCompleted.size,
                currentPhraseIndex = currentPhraseIndex
            ) 
        }
    }

    private fun handleAwardCompletionXp() {
        Log.d(TAG, "Awarding completion bonus XP")
        viewModelScope.launch {
            val result = gamificationRepository.addExperience(
                points = COMPLETION_BONUS_XP,
                source = "learn_topic_completed_$topicId"
            )
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Awarded $COMPLETION_BONUS_XP bonus XP for topic completion")
                    _uiState.update { it.copy(topicCompleted = true) }
                }
                else -> {
                    Log.e(TAG, "Failed to award completion bonus XP")
                }
            }
        }
    }

    private fun sendFunctionResponse(functionName: String, response: String) {
        val functionResponse = mapOf(
            "functionResponse" to mapOf(
                "name" to functionName,
                "response" to mapOf("result" to response)
            )
        )
        val jsonResponse = gson.toJson(functionResponse)
        sessionManager.sendRawMessage(jsonResponse)
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

            if (root.has("setupComplete")) {
                // No text here
            }

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
            obj.has("parts") && obj.get("parts").isJsonArray -> {
                collectFromContentObject(obj)
            }
        }

        return collected
    }

    private fun handleAudioFromServer(raw: String) {
        try {
            val root = gson.fromJson(raw, JsonObject::class.java)

            val topData = root.get("data")
            if (topData != null && topData.isJsonPrimitive) {
                val base64 = topData.asString
                if (!base64.isNullOrEmpty()) {
                    onModelAudioDetected()
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    player.playPcm(bytes, 24_000)
                }
            }

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
                            player.playPcm(bytes, 24_000)
                        }
                    }
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

    private var modelSpeaking: Boolean = false
    private var modelSpeakingClearJob: Job? = null
    
    private fun onModelAudioDetected() {
        modelSpeaking = true
        if (uiState.value.isRecording) {
            stopRecording()
        }
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
        viewModelScope.launch {
            recorder.stop()
        }
    }

    private fun buildSystemInstruction(): String {
        val topic = topicData ?: return ""
        val user = currentUser
        val userName = user?.let { preferredUserName(it) } ?: "there"
        
        val conversation = topic.conversation
        val phrasesText = conversation.mapIndexed { index, turn ->
            "Phrase ${index + 1}: \"${turn.text}\" (Speaker: ${turn.speaker})"
        }.joinToString("\n")

        return """
            ## CHARACTER IDENTITY
            - Name: Vivi
            - Role: English learning guide for VozVibe app
            - Age: 18 years old
            - Location: Batam, Indonesia
            - Gender: Female
            - Language: Indonesian and English
            
            ## YOUR MISSION
            You are helping the user learn the topic: "${topic.title}"
            Description: ${topic.description}
            
            ## LEARNING APPROACH
            - Guide the user through the conversation PHRASE BY PHRASE
            - Start with phrase 0 and move sequentially
            - For each phrase, explain its meaning, usage, and pronunciation
            - Always provide the Indonesian equivalent and cultural context
            - Be encouraging and patient
            - Use simple, friendly language
            
            ## CONVERSATION PHRASES TO TEACH
            $phrasesText
            
            ## TEACHING FLOW
            1. Welcome the user warmly and introduce the topic
            2. Start teaching phrase 0
            3. For each phrase:
               - Explain what it means in English
               - Give the Indonesian translation or equivalent
               - Explain when and how to use it
               - Provide cultural context if relevant
               - Ask the user to try saying it or demonstrate understanding
            4. When the user shows they understand a phrase, call mark_phrase_completed
            5. Move to the next phrase
            6. After all phrases are learned, congratulate them and call award_completion_xp
            
            ## INDONESIAN CULTURAL CONTEXT
            - When explaining phrases, relate them to Indonesian culture and Batam context
            - Example: "Nice to meet you" → "In Indonesian, we say 'Senang bertemu Anda' but in Batam, people often say 'Senang ketemu kamu' in casual situations"
            - Use examples from daily life in Batam, Indonesia
            
            ## PERSONALITY
            - Be super friendly, warm, encouraging
            - Use emojis occasionally 🙂
            - Keep responses natural and conversational
            - Celebrate small wins with the user
            
            ## FUNCTION CALLING
            You have access to these functions:
            - play_phrase_audio(phraseIndex): Play audio for a specific phrase (use when introducing a new phrase)
            - mark_phrase_completed(phraseIndex): Mark a phrase as completed when user demonstrates understanding
            - award_completion_xp(): Award bonus XP when all phrases are completed
            
            ## USER CONTEXT
            ${user?.let { 
                """
                - User ID: ${it.id}
                - Username: ${it.username}
                - Display Name: ${it.displayName}
                - Level: ${it.level}
                - XP: ${it.xp}
                - Preferred name: $userName
                """.trimIndent()
            } ?: ""}
            
            ## IMPORTANT RULES
            - ALWAYS address user as "$userName"
            - Work through phrases in order (0, 1, 2, ...)
            - Don't skip phrases
            - Don't move to next phrase until current is mastered
            - Provide Indonesian context for EVERY phrase
            - Keep explanations brief but clear
            - Use function calls appropriately
            
            START by greeting $userName and introducing the topic "${topic.title}" in a warm, encouraging way!
        """.trimIndent()
    }

    private fun buildFunctionDeclarations(): String {
        return """
        {
            "functionDeclarations": [
                {
                    "name": "play_phrase_audio",
                    "description": "Plays the audio for a specific phrase in the conversation. Use this when introducing a new phrase to help the user hear the correct pronunciation.",
                    "parameters": {
                        "type": "OBJECT",
                        "properties": {
                            "phraseIndex": {
                                "type": "INTEGER",
                                "description": "The index of the phrase to play (0-based)"
                            }
                        },
                        "required": ["phraseIndex"]
                    }
                },
                {
                    "name": "mark_phrase_completed",
                    "description": "Marks a phrase as completed when the user demonstrates understanding. This awards XP to the user and moves them to the next phrase.",
                    "parameters": {
                        "type": "OBJECT",
                        "properties": {
                            "phraseIndex": {
                                "type": "INTEGER",
                                "description": "The index of the completed phrase (0-based)"
                            }
                        },
                        "required": ["phraseIndex"]
                    }
                },
                {
                    "name": "award_completion_xp",
                    "description": "Awards bonus XP when the user has completed learning all phrases in the topic. Call this only after all phrases are mastered.",
                    "parameters": {
                        "type": "OBJECT",
                        "properties": {},
                        "required": []
                    }
                }
            ]
        }
        """.trimIndent()
    }

    private fun preferredUserName(u: UserProfile): String {
        val base = (u.displayName.ifBlank { u.username }).trim()
        val first = base.substringBefore(' ').ifBlank { base }
        return first
    }

    private fun mapDtoToTopic(dto: com.example.voicevibe.data.remote.api.SpeakingTopicDto): Topic {
        return Topic(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            material = dto.material,
            vocabulary = dto.vocabulary,
            conversation = dto.conversation.map { ConversationTurn(it.speaker, it.text) },
            fluencyPracticePrompts = dto.fluencyPracticePrompts,
            fluencyProgress = dto.fluencyProgress?.let { fp ->
                FluencyProgress(
                    promptsCount = fp.promptsCount,
                    promptScores = fp.promptScores,
                    totalScore = fp.totalScore,
                    nextPromptIndex = fp.nextPromptIndex,
                    completed = fp.completed
                )
            },
            phraseProgress = dto.phraseProgress?.let { pp ->
                PhraseProgress(
                    currentPhraseIndex = pp.currentPhraseIndex,
                    completedPhrases = pp.completedPhrases,
                    totalPhrases = pp.totalPhrases,
                    isAllPhrasesCompleted = pp.isAllPhrasesCompleted
                )
            },
            practiceScores = dto.practiceScores?.let { ps ->
                PracticeScores(
                    pronunciation = ps.pronunciation,
                    fluency = ps.fluency,
                    vocabulary = ps.vocabulary,
                    listening = ps.listening,
                    average = ps.average,
                    meetsRequirement = ps.meetsRequirement,
                    maxPronunciation = ps.maxPronunciation,
                    maxFluency = ps.maxFluency,
                    maxVocabulary = ps.maxVocabulary,
                    maxListening = ps.maxListening
                )
            },
            conversationScore = dto.conversationScore,
            conversationCompleted = dto.conversationCompleted,
            unlocked = dto.unlocked,
            completed = dto.completed
        )
    }
}

data class LearnWithViviState(
    val isConnecting: Boolean = true,
    val isConnected: Boolean = false,
    val isLoadingTopic: Boolean = false,
    val messages: List<LiveMessage> = emptyList(),
    val showTypingIndicator: Boolean = false,
    val isRecording: Boolean = false,
    val isAiSpeaking: Boolean = false,
    val error: String? = null,
    val topic: Topic? = null,
    val currentPhraseIndex: Int = 0,
    val phrasesCompleted: Int = 0,
    val totalPhrases: Int = 0,
    val topicCompleted: Boolean = false
)
