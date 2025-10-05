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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    private var mediaPlayer: android.media.MediaPlayer? = null

    private var currentUser: UserProfile? = null
    private var topicData: Topic? = null
    private var userContextApplied: Boolean = false
    private var pendingModelText: StringBuilder? = null
    private var currentPhraseIndex: Int = 0
    private var phrasesCompleted: MutableSet<Int> = mutableSetOf()
    private var appContext: android.content.Context? = null

    companion object {
        // Choose your priority:
        // STABLE_LIVE_MODEL = Reliable functions but robotic voice
        // NATIVE_AUDIO_MODEL = Natural voice but sometimes misses function calls
        private const val STABLE_LIVE_MODEL = "gemini-live-2.5-flash-preview"
        private const val NATIVE_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-09-2025"
        
        // Current choice: Natural voice (with extra safeguards in prompts)
        private const val ACTIVE_MODEL = NATIVE_AUDIO_MODEL
        
        private const val MODEL_SPEAKING_SILENCE_MS = 1200L
        private const val TAG = "LearnTopicWithVivi"
        private const val XP_PER_PHRASE = 10
        private const val COMPLETION_BONUS_XP = 50
    }
    
    // Voice presets for Vivi
    enum class ViviVoice(val displayName: String, val voiceName: String) {
        PUCK("Puck (Energetic & Fun)", "Puck"),
        CHARON("Charon (Warm & Natural)", "Charon"),
        KORE("Kore (Clear & Professional)", "Kore"),
        FENRIR("Fenrir (Deep & Dramatic)", "Fenrir"),
        AOEDE("Aoede (Smooth & Friendly)", "Aoede")
    }
    
    private fun buildVoiceConfig(voice: ViviVoice = ViviVoice.PUCK): Map<String, Any> {
        // Structure: speech_config -> voice_config -> prebuilt_voice_config -> voice_name
        return mapOf(
            "voice_config" to mapOf(
                "prebuilt_voice_config" to mapOf(
                    "voice_name" to voice.voiceName
                )
            )
        )
    }

    init {
        viewModelScope.launch {
            // Show loading state
            _uiState.update { it.copy(isLoadingTopic = true) }
            Log.d(TAG, "Loading topic data and user profile...")
            
            // Load all required data first
            val topicSuccess = loadTopicData()
            val userSuccess = loadCurrentUserOnce()
            
            // Hide loading state
            _uiState.update { it.copy(isLoadingTopic = false) }
            
            // Only connect if both loaded successfully
            if (topicSuccess && userSuccess && topicData != null && currentUser != null) {
                userContextApplied = true
                Log.d(TAG, "All data loaded successfully, connecting to live session")
                connectToLiveSession()
            } else {
                Log.e(TAG, "Failed to load required data: topicSuccess=$topicSuccess, userSuccess=$userSuccess, topicData=${topicData != null}, currentUser=${currentUser != null}")
                _uiState.update { 
                    it.copy(error = "Failed to load lesson data. Please try again.")
                }
            }
        }
    }

    private suspend fun loadTopicData(): Boolean {
        return try {
            val result = speakingJourneyRepository.getTopics()
            result.fold(
                onSuccess = { response ->
                    val topicDto = response.topics.firstOrNull { it.id == topicId }
                    topicData = topicDto?.let { mapDtoToTopic(it) }
                    _uiState.update {
                        it.copy(
                            topic = topicData,
                            totalPhrases = topicData?.conversation?.size ?: 0
                        )
                    }
                    Log.d(TAG, "Topic data loaded successfully: ${topicData?.title}")
                    topicData != null // Return true if data was loaded
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load topic data: ${error.message}")
                    false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading topic data: ${e.message}")
            false
        }
    }

    private suspend fun loadCurrentUserOnce(): Boolean {
        return try {
            var resultFound = false
            var success = false
            
            // Manually collect and exit on first non-Loading emission
            withContext(Dispatchers.IO) {
                val job = launch {
                    userRepository.getCurrentUser().collect { res ->
                        if (resultFound) return@collect // Already got result, ignore further emissions
                        
                        when (res) {
                            is Resource.Success -> {
                                resultFound = true
                                val u = res.data
                                if (u != null) {
                                    currentUser = u
                                    Log.d(TAG, "User profile loaded: ${u.displayName}")
                                    success = true
                                } else {
                                    Log.w(TAG, "User profile data is null")
                                    success = false
                                }
                            }
                            is Resource.Error -> {
                                if (!resultFound) { // Only log error if we haven't got success yet
                                    resultFound = true
                                    Log.e(TAG, "Failed to load user profile: ${res.message}")
                                    success = false
                                }
                            }
                            is Resource.Loading -> {
                                Log.d(TAG, "Loading user profile...")
                            }
                        }
                    }
                }
                
                // Wait for result or timeout after 10 seconds
                var waited = 0
                while (!resultFound && waited < 100) {
                    delay(100)
                    waited++
                }
                
                job.cancel() // Stop collecting
            }
            
            success
        } catch (e: Exception) { 
            Log.e(TAG, "Exception loading user profile: ${e.message}")
            false
        }
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
    
    fun requestShowPhrase() {
        // Send a predefined message to ask the AI to show the current phrase
        val message = "Please show me the phrase you are explaining right now. Call show_phrase_card() for the current phrase."
        sendMessage(message)
        Log.d(TAG, "User requested to show phrase via button")
    }
    
    fun requestRolePlay() {
        // Send a predefined message to initiate role play mode
        val topicTitle = topicData?.title ?: "this topic"
        val message = "Let's do a role play! I want to practice the conversation from $topicTitle using the exact phrases. Please ask me which role I want to take: Speaker A or Speaker B. After I choose, you will take the opposite role and we'll practice the conversation. When it's your turn, only say your character's phrases from the conversation - don't explain anything, just act as that speaker."
        sendMessage(message)
        Log.d(TAG, "User requested role play via button")
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
            
            // Configure Vivi's voice (Aoede = smooth & natural)
            val speechConfig = buildVoiceConfig(ViviVoice.AOEDE)
            Log.d(TAG, "Using model: $ACTIVE_MODEL with voice config: $speechConfig")
            
            when (val res = withContext(Dispatchers.IO) {
                aiEvaluationRepository.requestLiveToken(
                    model = ACTIVE_MODEL,
                    responseModalities = responseModalities,
                    systemInstruction = systemInstruction,
                    functionDeclarations = functionDeclarations,
                    lockAdditionalFields = listOf("system_instruction", "tools"),
                    speechConfig = speechConfig
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
                            showTypingIndicator = true  // Show typing indicator for initial response
                        )
                    }
                    
                    // Send initial greeting to trigger Vivi's welcome message
                    delay(500)  // Brief delay to ensure session is fully ready
                    sessionManager.sendUserMessage("Hello")
                    Log.d(TAG, "Sent initial greeting to Vivi")
                }
            }

            override fun onMessage(text: String) {
                // Log raw message for debugging
                Log.d(TAG, "Raw message received: ${text.take(500)}...")
                
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
            Log.d(TAG, "Checking for function calls in message...")
            
            // Check for toolCall in root
            val toolCall = root.getAsJsonObject("toolCall")
            if (toolCall != null) {
                Log.d(TAG, "Found toolCall in root: $toolCall")
                processFunctionCalls(toolCall)
                return
            }
            
            // Check for function calls in serverContent
            val sc = root.get("serverContent")
            if (sc != null) {
                Log.d(TAG, "Found serverContent, checking for function calls...")
                val items = if (sc.isJsonArray) sc.asJsonArray.toList() else listOf(sc)
                items.forEach { item ->
                    if (!item.isJsonObject) return@forEach
                    val obj = item.asJsonObject
                    
                    // Check for toolCall at this level
                    val itemToolCall = obj.getAsJsonObject("toolCall")
                    if (itemToolCall != null) {
                        Log.d(TAG, "Found toolCall in serverContent item: $itemToolCall")
                        processFunctionCalls(itemToolCall)
                        return@forEach
                    }
                    
                    // Check for modelTurn with parts
                    val modelTurn = obj.getAsJsonObject("modelTurn")
                    if (modelTurn != null) {
                        val parts = modelTurn.getAsJsonArray("parts")
                        if (parts != null) {
                            Log.d(TAG, "Found modelTurn.parts, checking for functionCall...")
                            processParts(parts)
                        }
                    }
                    
                    // Look for function calls directly in parts
                    val parts = obj.getAsJsonArray("parts")
                    if (parts != null) {
                        Log.d(TAG, "Found parts array, checking for functionCall...")
                        processParts(parts)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling function calls: ${e.message}", e)
        }
    }
    
    private fun processParts(parts: com.google.gson.JsonArray) {
        parts.forEach { partElement ->
            if (!partElement.isJsonObject) return@forEach
            val partObj = partElement.asJsonObject
            val functionCall = partObj.getAsJsonObject("functionCall")
            
            if (functionCall != null) {
                val functionName = functionCall.get("name")?.asString
                val callId = functionCall.get("id")?.asString ?: "unknown"
                val args = functionCall.getAsJsonObject("args")
                
                Log.d(TAG, "✅ Function call received: $functionName (id=$callId, args=$args)")
                
                when (functionName) {
                    "show_phrase_card" -> {
                        val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                        handleShowPhraseCard(phraseIndex)
                    }
                    "mark_phrase_completed" -> {
                        val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                        handleMarkPhraseCompleted(phraseIndex)
                    }
                    "award_completion_xp" -> {
                        handleAwardCompletionXp()
                    }
                    "suggest_video_clip" -> {
                        val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                        val searchQuery = args?.get("searchQuery")?.asString ?: ""
                        val description = args?.get("description")?.asString ?: ""
                        handleSuggestVideoClip(phraseIndex, searchQuery, description)
                    }
                }
                
                // Send function response back with proper format
                sendFunctionResponse(callId, functionName ?: "unknown", "success")
            }
        }
    }
    
    private fun processFunctionCalls(toolCall: JsonObject) {
        val functionCalls = toolCall.getAsJsonArray("functionCalls")
        if (functionCalls != null) {
            functionCalls.forEach { callElement ->
                if (!callElement.isJsonObject) return@forEach
                val call = callElement.asJsonObject
                val functionName = call.get("name")?.asString
                val callId = call.get("id")?.asString ?: "unknown"
                val args = call.getAsJsonObject("args")
                
                Log.d(TAG, "✅ Function call from toolCall: $functionName (id=$callId, args=$args)")
                
                when (functionName) {
                    "show_phrase_card" -> {
                        val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                        handleShowPhraseCard(phraseIndex)
                    }
                    "mark_phrase_completed" -> {
                        val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                        handleMarkPhraseCompleted(phraseIndex)
                    }
                    "award_completion_xp" -> {
                        handleAwardCompletionXp()
                    }
                    "suggest_video_clip" -> {
                        val phraseIndex = args?.get("phraseIndex")?.asInt ?: currentPhraseIndex
                        val searchQuery = args?.get("searchQuery")?.asString ?: ""
                        val description = args?.get("description")?.asString ?: ""
                        handleSuggestVideoClip(phraseIndex, searchQuery, description)
                    }
                }
                
                sendFunctionResponse(callId, functionName ?: "unknown", "success")
            }
        }
    }

    private fun handleShowPhraseCard(phraseIndex: Int) {
        Log.d(TAG, "Showing phrase card for phrase $phraseIndex")
        val conversation = topicData?.conversation
        
        if (conversation != null && phraseIndex < conversation.size) {
            currentPhraseIndex = phraseIndex
            val turn = conversation[phraseIndex]
            
            // Add a clickable phrase card to the messages
            _uiState.update { state ->
                val newCard = PhraseCard(
                    phraseIndex = phraseIndex,
                    text = turn.text,
                    speaker = turn.speaker
                )
                state.copy(
                    currentPhraseIndex = phraseIndex,
                    phraseCards = state.phraseCards + newCard
                )
            }
            
            Log.d(TAG, "Phrase card added for phrase $phraseIndex: ${turn.text}")
        } else {
            Log.e(TAG, "Cannot show phrase card: conversation=$conversation, phraseIndex=$phraseIndex")
        }
    }
    
    fun playPhraseAudio(phraseIndex: Int) {
        Log.d(TAG, "Playing audio for phrase $phraseIndex")
        val conversation = topicData?.conversation
        val ctx = appContext
        
        if (conversation != null && phraseIndex < conversation.size && ctx != null) {
            val turn = conversation[phraseIndex]
            val topicTitle = topicData?.title ?: topicId
            
            // Play audio from assets like ConversationLesson does
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Release any existing player
                    mediaPlayer?.release()
                    
                    // Build asset path
                    val slug = topicTitle.trim().lowercase(java.util.Locale.US)
                        .replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    val turnNumber = (phraseIndex + 1).coerceAtLeast(1)
                    val assetPath = "conversation_audios/$slug/turn_${turnNumber}.wav"
                    
                    val afd = ctx.assets.openFd(assetPath)
                    afd.use { desc ->
                        val mp = android.media.MediaPlayer().apply {
                            setDataSource(desc.fileDescriptor, desc.startOffset, desc.length)
                            setOnPreparedListener {
                                it.start()
                                Log.d(TAG, "Audio playback started for phrase $phraseIndex")
                            }
                            setOnCompletionListener {
                                try { it.release() } catch (_: Throwable) {}
                                if (mediaPlayer === it) mediaPlayer = null
                                Log.d(TAG, "Audio playback completed for phrase $phraseIndex")
                            }
                            setOnErrorListener { pl, what, extra ->
                                try { pl.release() } catch (_: Throwable) {}
                                if (mediaPlayer === pl) mediaPlayer = null
                                Log.e(TAG, "Audio playback error: what=$what, extra=$extra")
                                true
                            }
                        }
                        mediaPlayer = mp
                        mp.prepareAsync()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play audio for phrase $phraseIndex: ${e.message}", e)
                }
            }
        } else {
            Log.e(TAG, "Cannot play audio: conversation=$conversation, phraseIndex=$phraseIndex, context=$ctx")
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
        
        // Automatically show the next phrase card if available
        val nextPhraseIndex = currentPhraseIndex // Already incremented above
        val totalPhrases = topicData?.conversation?.size ?: 0
        if (nextPhraseIndex < totalPhrases) {
            Log.d(TAG, "Auto-showing next phrase card: $nextPhraseIndex")
            handleShowPhraseCard(nextPhraseIndex)
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

    private fun handleSuggestVideoClip(phraseIndex: Int, searchQuery: String, description: String) {
        Log.d(TAG, "Suggesting video clip for phrase $phraseIndex: query='$searchQuery', description='$description'")
        
        val suggestionId = "video_${phraseIndex}_${System.currentTimeMillis()}"
        val suggestion = VideoSuggestion(
            id = suggestionId,
            phraseIndex = phraseIndex,
            searchQuery = searchQuery,
            description = description
        )
        
        _uiState.update { state ->
            state.copy(
                videoSuggestions = state.videoSuggestions + suggestion
            )
        }
        
        Log.d(TAG, "Video suggestion added: $suggestion")
    }
    
    fun acceptVideoSuggestion(suggestionId: String) {
        val suggestion = uiState.value.videoSuggestions.find { it.id == suggestionId }
        if (suggestion != null) {
            Log.d(TAG, "User accepted video suggestion: ${suggestion.searchQuery}")
            
            // Create YouTube search URL (embedded player with search results)
            val encodedQuery = java.net.URLEncoder.encode(suggestion.searchQuery, "UTF-8")
            val youtubeSearchUrl = "https://www.youtube.com/results?search_query=$encodedQuery"
            
            _uiState.update { state ->
                state.copy(
                    activeVideoUrl = youtubeSearchUrl,
                    videoSuggestions = state.videoSuggestions.map {
                        if (it.id == suggestionId) it.copy(dismissed = true) else it
                    }
                )
            }
        }
    }
    
    fun dismissVideoSuggestion(suggestionId: String) {
        Log.d(TAG, "User dismissed video suggestion: $suggestionId")
        _uiState.update { state ->
            state.copy(
                videoSuggestions = state.videoSuggestions.map {
                    if (it.id == suggestionId) it.copy(dismissed = true) else it
                }
            )
        }
    }
    
    fun closeVideo() {
        _uiState.update { it.copy(activeVideoUrl = null) }
    }

    private fun sendFunctionResponse(callId: String, functionName: String, response: String) {
        // Build a detailed response that includes the current state of shown phrases
        val shownPhraseIndices = uiState.value.phraseCards.map { it.phraseIndex }.toSet()
        val totalPhrases = topicData?.conversation?.size ?: 0
        
        // Create a status message showing which phrases have been displayed
        val statusMessage = buildString {
            append(response)
            append("\n\n")
            append("📊 Current Status:\n")
            append("• Phrases with cards shown: ")
            if (shownPhraseIndices.isEmpty()) {
                append("NONE ❌")
            } else {
                append(shownPhraseIndices.sorted().joinToString(", ") { "#$it ✅" })
            }
            append("\n")
            append("• Current phrase index: ${uiState.value.currentPhraseIndex}\n")
            append("• Total phrases: $totalPhrases\n")
            append("\n")
            
            // Special reminder after marking a phrase complete
            if (functionName == "mark_phrase_completed") {
                val nextPhraseIndex = uiState.value.currentPhraseIndex
                if (nextPhraseIndex < totalPhrases) {
                    append("🎯 NEXT STEP: Now show the next phrase!\n")
                    append("   👉 Call show_phrase_card($nextPhraseIndex) immediately!\n")
                    append("   Don't explain it yet - show the card FIRST!\n")
                    append("\n")
                } else {
                    append("🎉 All phrases completed! Time to award completion XP!\n")
                    append("   👉 Call award_completion_xp() now!\n")
                    append("\n")
                }
            }
            
            // Add a reminder if teaching a new phrase
            val nextPhraseToTeach = uiState.value.currentPhraseIndex
            if (nextPhraseToTeach < totalPhrases && nextPhraseToTeach !in shownPhraseIndices) {
                append("⚠️ REMINDER: Phrase #$nextPhraseToTeach has NOT been shown yet!\n")
                append("   You MUST call show_phrase_card($nextPhraseToTeach) before explaining it.\n")
            }
        }
        
        // According to Gemini Live API docs, function responses must include the call id
        // Use INTERRUPT scheduling to ensure Vivi knows the function completed before continuing
        val functionResponse = mapOf(
            "toolResponse" to mapOf(
                "functionResponses" to listOf(
                    mapOf(
                        "id" to callId,
                        "name" to functionName,
                        "response" to mapOf(
                            "result" to statusMessage,
                            "scheduling" to "INTERRUPT"
                        )
                    )
                )
            )
        )
        val jsonResponse = gson.toJson(functionResponse)
        Log.d(TAG, "Sending function response with status: $jsonResponse")
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
                    
                    // Skip thinking/thought parts - these are internal reasoning, not for display
                    val isThought = partObj.get("thought")?.asBoolean == true
                    if (isThought) {
                        Log.d(TAG, "Skipping thought part (internal reasoning)")
                        return@forEach
                    }
                    
                    val text = partObj.get("text")?.takeIf { it.isJsonPrimitive }?.asString
                    if (!text.isNullOrBlank()) {
                        Log.d(TAG, "Collected text part: ${text.take(50)}...")
                        collected.add(text)
                    }
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
    
    fun clearCompletionState() {
        _uiState.update { it.copy(topicCompleted = false) }
    }
    
    fun setContext(context: android.content.Context) {
        appContext = context.applicationContext
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel clearing - stopping all audio and closing session")
        
        // Update UI state to reflect shutdown
        _uiState.update { 
            it.copy(
                isAiSpeaking = false, 
                showTypingIndicator = false,
                isConnected = false,
                isConnecting = false
            ) 
        }
        
        // Close session gracefully first
        try {
            sessionManager.close()
            Log.d(TAG, "Session manager closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session: ${e.message}")
        }
        
        // Release audio resources (player.release() handles stopping gracefully)
        try {
            player.release()
            Log.d(TAG, "Audio player released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player: ${e.message}")
        }
        
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d(TAG, "Media player released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player: ${e.message}")
        }
        
        // Stop recorder
        viewModelScope.launch {
            try {
                recorder.stop()
                Log.d(TAG, "Recorder stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder: ${e.message}")
            }
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
            - Language: English but can switch to Indonesian when needed
            
            ## YOUR MISSION
            You are helping the user learn the topic: "${topic.title}"
            Description: ${topic.description}
            
            ## LEARNING APPROACH
            - Guide the user through the conversation PHRASE BY PHRASE
            - Start with phrase 0 and move sequentially
            - For each phrase, explain its meaning, usage, and pronunciation
            - Always provide the Indonesian cultural context
            - Be encouraging, patient, and humorous
            - Use short, simple, friendly language
            
            ## CONVERSATION PHRASES TO TEACH
            $phrasesText
            
            ## TEACHING FLOW
            1. Welcome the user warmly and introduce the topic
            2. Start teaching phrase 0
            3. For EACH phrase you teach, follow this EXACT sequence:
               a. Say "The first phrase is:" or "Next phrase is:"
               b. **IMMEDIATELY** call show_phrase_card(phraseIndex) - DO NOT SKIP THIS!
               c. After calling show_phrase_card, check the function response - it will tell you if the card is now shown ✅
               d. Only after confirming the card is shown, explain what it means in English
               e. Explain when and how to use it
               f. Provide Indonesian cultural context if relevant
               g. Ask the user to repeat the phrase. If the user repeats the phrase correctly, **IMMEDIATELY** call mark_phrase_completed(phraseIndex)
            4. Move to the next phrase
            5. After ALL phrases are learned, congratulate them and **IMMEDIATELY** call award_completion_xp()
            
            ## CRITICAL: FUNCTION CALLING RULES ⚠️⚠️⚠️
            YOU ARE REQUIRED TO USE THE FOLLOWING FUNCTIONS. CALLING THEM IS NOT OPTIONAL.
            
            1. **show_phrase_card(phraseIndex)** - Call this for the FIRST phrase (phrase 0)
               - Example: When starting the lesson, call show_phrase_card(0) FIRST before explaining
               - For subsequent phrases (1, 2, 3...), they will be shown AUTOMATICALLY when you mark the previous phrase complete
               - You only need to call this manually for phrase 0 at the start
               - After each function call, you'll receive a status report showing which phrases have cards (✅) and which don't (❌)
            
            2. **mark_phrase_completed(phraseIndex)** - Call when user successfully repeats/understands
               - Example: After user repeats phrase 0 well, call mark_phrase_completed(0)
               - This tracks their progress, awards XP, AND automatically shows the next phrase card
               - So after mark_phrase_completed(0), phrase card 1 will appear automatically! You can start explaining it right away
            
            3. **award_completion_xp()** - Call ONCE after ALL phrases are done
               - Only call this when the entire lesson is finished
               - This gives the user their completion bonus XP
            
            ⚠️ THE APP WILL BREAK IF YOU FORGET THESE FUNCTION CALLS ⚠️
            ⚠️ ALWAYS CHECK THE FUNCTION RESPONSE STATUS BEFORE PROCEEDING ⚠️
            
            ## USER INTERACTIONS
            - The user can click phrase cards to hear audio pronunciation
            - The user has a "Show me the phrase" button - if they click it, they're asking you to show the current phrase card
            - When you receive a request like "Please show me the phrase", immediately call show_phrase_card() with the current phrase index
            
            ## ROLE PLAY MODE
            - The user has a "Let's do a role play" button to practice the conversation
            - When user requests role play:
              1. Ask them: "Which role would you like to take? Speaker A or Speaker B?"
              2. Wait for their choice
              3. You will take the OPPOSITE role (if they choose A, you are B; if they choose B, you are A)
              4. In role play mode, ONLY say your character's exact phrases from the conversation
              5. Do NOT explain, teach, or add commentary - just act naturally as that speaker
              6. Wait for the user to say their lines before saying yours
              7. Keep the conversation flowing naturally like a real dialogue
              8. After the conversation ends, ask if they want to practice again or switch roles
            - Example: If they choose Speaker A, you respond with Speaker B's lines only
            - Stay in character until the role play is complete or user ends it
            
            ## INDONESIAN CULTURAL CONTEXT
            - When explaining phrases, relate them to Indonesian culture and Batam context
            - Example: "Nice to meet you" → "In Indonesian, we say 'Senang bertemu Anda' but in Batam, people often say 'Senang ketemu kamu' in casual situations"
            - Use examples from daily life in Batam, Indonesia
            
            ## PERSONALITY
            - Be super friendly, warm, encouraging, and humorous
            - Use emojis occasionally 🙂
            - Keep responses natural and conversational
            - Celebrate small wins with the user
            
            ## FUNCTION CALLING
            You have access to these functions:
            - show_phrase_card(phraseIndex): Display a clickable phrase card (use when introducing a new phrase)
            - mark_phrase_completed(phraseIndex): Mark a phrase as completed when user demonstrates understanding
            - award_completion_xp(): Award bonus XP when all phrases are completed
            - suggest_video_clip(phraseIndex, searchQuery, description): Offer the user a YouTube video to help visualize the phrase context
            
            ## USING VIDEO SUGGESTIONS
            - After explaining a phrase, you MAY offer a video to help visualize it
            - Call suggest_video_clip(phraseIndex, searchQuery, description) where:
              * searchQuery: YouTube search terms (e.g., "how to order coffee in english")
              * description: Brief explanation of what the video will show (e.g., "See how people greet each other in real situations")
            - The user will be asked if they want to watch it
            - Don't offer videos for EVERY phrase, only when it would genuinely help (2-3 per lesson max)
            - Good for: greetings, ordering food, asking directions, cultural situations
            - Not needed for: simple vocabulary, grammar points
            
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

            ## FEEDBACK
            - Bagus
            - Mantap
            - Gokil
            - Keren 
            - Nice
            - Very good
            - Great job
            - Amazing
            - Super
            - Wow, very nice!
            - Impressive   
            - Incredible

            ## ADDRESSING USERS (BATAM CUSTOMS)
            Based on age and gender:
            - For older men: "Bang [name]" (e.g., Bang Budi)
            - For younger men: "Dek [name]" e.g. (Dek Budi) only for the first time, subsequent times use "Adek" or "Dek" only
            - For women: "Kak [name]" (e.g., Kak Sinta)
            - Also use: "abang", "adek", "kakak", "om", "tante" appropriately
            - Use only first name when addressing users e.g. "Bang Budi" instead of "Bang Budi Setiawan"

            ## IMPORTANT RULES
            - Use English to explain, and only use Indonesian when you think users have difficulty understanding English
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
                    "name": "show_phrase_card",
                    "description": "Displays a clickable phrase card in the chat. The user can click on it to hear the audio pronunciation. Use this when introducing each new phrase so the user can listen to it as many times as they want. IMPORTANT: This function returns a status report showing which phrases have cards displayed (marked with ✅). Always call this BEFORE explaining a phrase.",
                    "parameters": {
                        "type": "OBJECT",
                        "properties": {
                            "phraseIndex": {
                                "type": "INTEGER",
                                "description": "The index of the phrase to display (0-based)"
                            }
                        },
                        "required": ["phraseIndex"]
                    }
                },
                {
                    "name": "mark_phrase_completed",
                    "description": "Marks a phrase as completed when the user demonstrates understanding. This awards XP to the user and moves them to the next phrase. The function returns a status report showing your progress and reminding you if the next phrase needs to be shown.",
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
                },
                {
                    "name": "suggest_video_clip",
                    "description": "Suggests a YouTube video clip to help the user visualize the phrase in real context. The user will be asked if they want to watch it. Use this selectively (2-3 times per lesson) when a video would genuinely help understanding, such as for greetings, ordering food, asking directions, or cultural situations.",
                    "parameters": {
                        "type": "OBJECT",
                        "properties": {
                            "phraseIndex": {
                                "type": "INTEGER",
                                "description": "The index of the phrase this video relates to (0-based)"
                            },
                            "searchQuery": {
                                "type": "STRING",
                                "description": "YouTube search query to find relevant videos (e.g., 'how to greet someone in english', 'ordering coffee in english conversation')"
                            },
                            "description": {
                                "type": "STRING",
                                "description": "Brief, friendly explanation of what the video will show (e.g., 'See how people use this greeting in real conversations')"
                            }
                        },
                        "required": ["phraseIndex", "searchQuery", "description"]
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
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val isAiSpeaking: Boolean = false,
    val messages: List<LiveMessage> = emptyList(),
    val showTypingIndicator: Boolean = false,
    val error: String? = null,
    val isLoadingTopic: Boolean = false,
    val topic: Topic? = null,
    val currentPhraseIndex: Int = 0,
    val phrasesCompleted: Int = 0,
    val totalPhrases: Int = 0,
    val topicCompleted: Boolean = false,
    val phraseCards: List<PhraseCard> = emptyList(),
    val videoSuggestions: List<VideoSuggestion> = emptyList(),
    val activeVideoUrl: String? = null
)

data class PhraseCard(
    val phraseIndex: Int,
    val text: String,
    val speaker: String
)

data class VideoSuggestion(
    val id: String,
    val phraseIndex: Int,
    val searchQuery: String,
    val description: String,
    val dismissed: Boolean = false
)
