package com.example.voicevibe.presentation.screens.speakingjourney

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.GamificationProfile
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import com.example.voicevibe.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SpeakingJourneyViewModel @Inject constructor(
    private val repo: SpeakingJourneyRepository,
    private val profileRepo: ProfileRepository,
    private val gamificationRepo: GamificationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = mutableStateOf(
        SpeakingJourneyUiState(topics = emptyList())
    )
    val uiState: State<SpeakingJourneyUiState> get() = _uiState

    init {
        reloadTopics(showWelcomeOnLoad = true)
        fetchGamificationProfile()
        // Observe preferred TTS voice selection
        viewModelScope.launch {
            try {
                tokenManager.ttsVoiceIdFlow().collect { preferred ->
                    preferredTtsVoiceName = preferred
                }
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    // --- Conversation Practice Recording ---
    fun setConversationRole(role: String) {
        conversationSelectedRole = role.trim().uppercase(Locale.US).takeIf { it == "A" || it == "B" }
    }

    fun startConversationRecording(context: Context, turnIndex: Int) {
        try {
            val s = _uiState.value
            val currentTopic = s.topics.getOrNull(s.selectedTopicIdx)
            if (currentTopic == null) {
                _uiState.value = _uiState.value.copy(
                    conversationRecordingState = PhraseRecordingState.IDLE,
                    conversationSubmissionResult = ConversationSubmissionResultUi(false, 0f, "", "No topic selected.", null, false)
                )
                return
            }
            conversationRecordingTargetIndex = turnIndex
            val dir = userRecordingsDir(context, currentTopic.id)
            val outFile = File(dir, "conversation_turn_${turnIndex}.m4a")
            conversationAudioFile = outFile
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare(); mr.start()
            _uiState.value = _uiState.value.copy(
                conversationRecordingState = PhraseRecordingState.RECORDING,
                conversationSubmissionResult = null,
                debug = "conv recording start idx=${turnIndex}"
            )
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to start conv recording", t)
            _uiState.value = _uiState.value.copy(
                conversationRecordingState = PhraseRecordingState.IDLE,
                conversationSubmissionResult = ConversationSubmissionResultUi(false, 0f, "", "Unable to start recording. ${t.message ?: ""}", null, false)
            )
            try { mediaRecorder?.release() } catch (_: Throwable) {}
            mediaRecorder = null
        }
    }

    fun stopConversationRecording(context: Context) {
        val s = _uiState.value
        val currentTopic = s.topics.getOrNull(s.selectedTopicIdx)
        try { mediaRecorder?.stop() } catch (_: Throwable) {}
        try { mediaRecorder?.reset() } catch (_: Throwable) {}
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
        _uiState.value = _uiState.value.copy(conversationRecordingState = PhraseRecordingState.PROCESSING)

        val file = conversationAudioFile
        val turnIndex = conversationRecordingTargetIndex
        val role = conversationSelectedRole
        if (currentTopic == null || file == null || !file.exists() || turnIndex == null || role.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                conversationRecordingState = PhraseRecordingState.IDLE,
                conversationSubmissionResult = ConversationSubmissionResultUi(false, 0f, "", "Recording not available.", null, false)
            )
            return
        }

        viewModelScope.launch {
            try {
                val part = MultipartBody.Part.createFormData(
                    name = "audio",
                    filename = file.name,
                    body = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                val result = repo.submitConversationTurn(currentTopic.id, turnIndex, part, role)
                result.fold(
                    onSuccess = { dto ->
                        val safeAccuracy = try { dto.accuracy } catch (_: Throwable) { null } ?: 0f
                        val newMap = _uiState.value.conversationTurnScores.toMutableMap().apply {
                            put(turnIndex, safeAccuracy.toInt())
                        }
                        _uiState.value = _uiState.value.copy(
                            conversationRecordingState = PhraseRecordingState.IDLE,
                            conversationSubmissionResult = ConversationSubmissionResultUi(
                                success = dto.success,
                                accuracy = safeAccuracy,
                                transcription = dto.transcription ?: "",
                                feedback = dto.feedback,
                                nextTurnIndex = dto.nextTurnIndex,
                                topicCompleted = dto.topicCompleted,
                                xpAwarded = dto.xpAwarded
                            ),
                            conversationTurnScores = newMap,
                            debug = "conv submit ok idx=${turnIndex} next=${dto.nextTurnIndex} topicCompleted=${dto.topicCompleted} localTurns=${newMap.size}"
                        )
                        // Count today's activity towards Day Streak
                        markSpeakingActivity()
                        // If the conversation ended globally OR the user has no remaining turns for their selected role,
                        // show congratulations and refresh topics. This ensures the congrats appears even when the
                        // final turn belongs to the other speaker.
                        runCatching {
                            val sNow = _uiState.value
                            val topicNow = sNow.topics.getOrNull(sNow.selectedTopicIdx)
                            val roleUpper = role.trim().uppercase(Locale.US)
                            val conv = topicNow?.conversation ?: emptyList()
                            // Check if there are more user turns after the CURRENT turn
                            val hasMoreUserTurns = conv.drop(turnIndex + 1).any { it.speaker.equals(roleUpper, ignoreCase = true) }
                            
                            // Debug logging
                            Log.d("ConversationCompletion", "Turn $turnIndex completed by $roleUpper")
                            Log.d("ConversationCompletion", "nextTurnIndex: ${dto.nextTurnIndex}")
                            Log.d("ConversationCompletion", "hasMoreUserTurns: $hasMoreUserTurns")
                            Log.d("ConversationCompletion", "Remaining turns: ${conv.drop(turnIndex + 1).map { "${it.speaker}: ${it.text.take(20)}..." }}")
                            
                            if (dto.nextTurnIndex == null || !hasMoreUserTurns) {
                                Log.d("ConversationCompletion", "Showing congratulations - conversation complete for user")
                                _uiState.value = _uiState.value.copy(showConversationCongrats = true)
                                reloadTopics()
                            }
                        }.onFailure { _ ->
                            // If any issue computing remaining turns, fall back to the original condition
                            if (dto.nextTurnIndex == null) {
                                _uiState.value = _uiState.value.copy(showConversationCongrats = true)
                                reloadTopics()
                            }
                        }
                        fetchGamificationProfile()
                    },
                    onFailure = { e ->
                        Log.e("SpeakingJourney", "Submit conversation failed", e)
                        _uiState.value = _uiState.value.copy(
                            conversationRecordingState = PhraseRecordingState.IDLE,
                            conversationSubmissionResult = ConversationSubmissionResultUi(false, 0f, "", "Failed to process recording. ${e.message ?: ""}", null, false),
                            debug = "conv submit fail: ${e.message ?: "unknown"}"
                        )
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error submitting conversation", t)
                _uiState.value = _uiState.value.copy(
                    conversationRecordingState = PhraseRecordingState.IDLE,
                    conversationSubmissionResult = ConversationSubmissionResultUi(false, 0f, "", "An error occurred. ${t.message ?: ""}", null, false),
                    debug = "conv submit error: ${t.message ?: "unknown"}"
                )
            }
        }
    }

    fun dismissConversationResult() { _uiState.value = _uiState.value.copy(conversationSubmissionResult = null) }
    fun dismissConversationCongrats() { _uiState.value = _uiState.value.copy(showConversationCongrats = false) }

    /**
     * Mark that the user performed a Speaking Journey activity today to update Day Streak.
     * Safe to call multiple times per day; backend is idempotent.
     */
    fun markSpeakingActivity() {
        viewModelScope.launch {
            runCatching { gamificationRepo.updateStreak() }
        }
    }

    fun reloadTopics(showWelcomeOnLoad: Boolean = false, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val prevSelectedId = _uiState.value.topics.getOrNull(_uiState.value.selectedTopicIdx)?.id
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repo.getTopics()
            result.fold(
                onSuccess = { response ->
                    val dtos = response.topics
                    val userProfile = UserProfile(
                        firstVisit = response.userProfile.firstVisit,
                        lastVisitedTopicId = response.userProfile.lastVisitedTopicId,
                        lastVisitedTopicTitle = response.userProfile.lastVisitedTopicTitle
                    )
                    val mapped = if (dtos.isNotEmpty()) dtos.map { dto ->
                        Topic(
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
                            phraseProgress = dto.phraseProgress?.let { progress ->
                                PhraseProgress(
                                    currentPhraseIndex = progress.currentPhraseIndex,
                                    completedPhrases = progress.completedPhrases,
                                    totalPhrases = progress.totalPhrases,
                                    isAllPhrasesCompleted = progress.isAllPhrasesCompleted
                                )
                            },
                            practiceScores = dto.practiceScores?.let { scores ->
                                PracticeScores(
                                    pronunciation = scores.pronunciation,
                                    fluency = scores.fluency,
                                    vocabulary = scores.vocabulary,
                                    listening = scores.listening ?: 0,
                                    average = scores.average,
                                    meetsRequirement = scores.meetsRequirement,
                                    maxPronunciation = scores.maxPronunciation,
                                    maxFluency = scores.maxFluency,
                                    maxVocabulary = scores.maxVocabulary,
                                    maxListening = scores.maxListening ?: 100
                                )
                            },
                            conversationScore = dto.conversationScore,
                            conversationCompleted = dto.conversationCompleted,
                            unlocked = dto.unlocked,
                            completed = dto.completed
                        )
                    } else emptyList()
                    // Prefer auto-selecting the next unlocked, incomplete topic when coming back
                    // from practice and the last-visited/previously-selected topic is now completed.
                    val lastVisitedIdx: Int? = if (!userProfile.firstVisit && userProfile.lastVisitedTopicId != null) {
                        mapped.indexOfFirst { it.id == userProfile.lastVisitedTopicId }.takeIf { it >= 0 }
                    } else null
                    val prevSelectedIdx: Int? = prevSelectedId?.let { id ->
                        mapped.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                    }
                    // Base selection: prefer server's lastVisited, then previous selection, then first unlocked
                    var baseIndex: Int = when {
                        mapped.isEmpty() -> 0
                        lastVisitedIdx != null -> lastVisitedIdx
                        prevSelectedIdx != null -> prevSelectedIdx
                        else -> mapped.indexOfFirst { it.unlocked }.let { if (it >= 0) it else 0 }
                    }
                    // If the base topic is completed, try to move to the next unlocked & incomplete topic
                    if (mapped.isNotEmpty()) {
                        val baseTopic = mapped.getOrNull(baseIndex)
                        if (baseTopic?.completed == true) {
                            val nextIdx = ((baseIndex + 1) until mapped.size)
                                .firstOrNull { idx -> mapped[idx].unlocked && !mapped[idx].completed }
                                ?: mapped.indexOfFirst { it.unlocked && !it.completed }
                                    .takeIf { it >= 0 }
                            if (nextIdx != null) {
                                baseIndex = nextIdx
                            }
                        }
                    }
                    val newIndex = baseIndex
                    _uiState.value = _uiState.value.copy(
                        topics = mapped,
                        userProfile = userProfile,
                        selectedTopicIdx = newIndex.coerceIn(0, (mapped.size - 1).coerceAtLeast(0)),
                        showWelcome = showWelcomeOnLoad,
                        isLoading = false
                    )
                    onComplete?.invoke()
                },
                onFailure = { ex ->
                    Log.e("SpeakingJourney", "Failed to load topics", ex)
                    val fallback = listOf(
                        Topic(
                            id = "t1",
                            title = "Self Introduction",
                            description = "Introduce yourself with simple phrases.",
                            material = listOf(
                                "Hello! My name is Alex.",
                                "I am from San Francisco.",
                                "I work as a software developer.",
                                "Nice to meet you!"
                            ),
                            vocabulary = listOf("hello", "name", "from", "work"),
                            conversation = emptyList(),
                            fluencyPracticePrompts = listOf(
                                "Introduce yourself: name, where you're from, what you do, and one hobby.",
                                "Give a short self-introduction for a new class or team meeting.",
                                "Explain one fun fact about yourself."
                            ),
                            fluencyProgress = FluencyProgress(
                                promptsCount = 3,
                                promptScores = emptyList(),
                                totalScore = 0,
                                nextPromptIndex = 0,
                                completed = false
                            ),
                            phraseProgress = PhraseProgress(
                                currentPhraseIndex = 0,
                                completedPhrases = emptyList(),
                                totalPhrases = 4,
                                isAllPhrasesCompleted = false
                            ),
                            practiceScores = null,
                            unlocked = true,
                            completed = false
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        topics = fallback,
                        userProfile = UserProfile(true, null, null),
                        isLoading = false,
                        error = "Unable to load topics. ${ex.message ?: "Check your connection and try again."}"
                    )
                }
            )
        }
    }

    // Explicitly restart review/practice from phrase 0
    fun restartPracticeFromBeginning() {
        _uiState.value = _uiState.value.copy(inspectedPhraseIndex = 0)
    }

    fun selectTopic(index: Int) {
        val s = _uiState.value
        if (index in s.topics.indices && s.topics[index].unlocked) {
            val topic = s.topics[index]
            _uiState.value = s.copy(selectedTopicIdx = index)
            viewModelScope.launch { repo.updateLastVisitedTopic(topic.id) }
        }
    }

    fun selectTopicById(topicId: String) {
        val s = _uiState.value
        val index = s.topics.indexOfFirst { it.id == topicId }
        android.util.Log.d("SpeakingJourneyViewModel", "selectTopicById($topicId): found index $index")
        if (index >= 0) {
            val topic = s.topics[index]
            android.util.Log.d("SpeakingJourneyViewModel", "Topic found: ${topic.title} (unlocked: ${topic.unlocked})")
            if (topic.unlocked) {
                android.util.Log.d("SpeakingJourneyViewModel", "Setting selectedTopicIdx from ${s.selectedTopicIdx} to $index")
                _uiState.value = s.copy(selectedTopicIdx = index)
                viewModelScope.launch { repo.updateLastVisitedTopic(topic.id) }
            } else {
                android.util.Log.d("SpeakingJourneyViewModel", "ERROR: Topic is locked!")
            }
        } else {
            android.util.Log.d("SpeakingJourneyViewModel", "ERROR: Topic ID $topicId not found!")
        }
    }

    fun dismissWelcome() { _uiState.value = _uiState.value.copy(showWelcome = false) }

    fun setStage(stage: Stage) { _uiState.value = _uiState.value.copy(stage = stage) }

    fun markCurrentTopicComplete() {
        val s = _uiState.value
        val current = s.topics.getOrNull(s.selectedTopicIdx) ?: return
        if (current.completed) return
        viewModelScope.launch {
            val res = repo.completeTopic(current.id)
            if (res.isSuccess) { reloadTopics() }
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var phraseAudioFile: File? = null
    private var recordingTargetIndex: Int? = null
    private var currentUserKey: String = "default"
    private var userKeyInitialized: Boolean = false
    // Conversation practice recording state
    private var conversationAudioFile: File? = null
    private var conversationRecordingTargetIndex: Int? = null
    private var conversationSelectedRole: String? = null
    // Preferred TTS voice (nullable => keep existing defaults)
    private var preferredTtsVoiceName: String? = null

    fun startPhraseRecording(context: Context) {
        try {
            val s = _uiState.value
            val currentTopic = s.topics.getOrNull(s.selectedTopicIdx)
            if (currentTopic == null) {
                _uiState.value = _uiState.value.copy(
                    phraseRecordingState = PhraseRecordingState.IDLE,
                    phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "No topic selected.", null, false)
                )
                return
            }
            // Choose target phrase index: prefer inspected; otherwise clamp current progress within total
            val phraseIndex = run {
                val inspected = s.inspectedPhraseIndex
                if (inspected != null) inspected else {
                    val prog = currentTopic.phraseProgress
                    val total = prog?.totalPhrases ?: currentTopic.material.size
                    val raw = prog?.currentPhraseIndex ?: 0
                    if (total > 0) raw.coerceIn(0, total - 1) else 0
                }
            }
            recordingTargetIndex = phraseIndex
            val dir = userRecordingsDir(context, currentTopic.id)
            val outFile = File(dir, "phrase_${phraseIndex}.m4a")
            phraseAudioFile = outFile
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare(); mr.start()
            _uiState.value = _uiState.value.copy(
                phraseRecordingState = PhraseRecordingState.RECORDING,
                phraseSubmissionResult = null,
                debug = "recording start idx=$phraseIndex"
            )
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to start recording", t)
            _uiState.value = _uiState.value.copy(
                phraseRecordingState = PhraseRecordingState.IDLE,
                phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "Unable to start recording. ${t.message ?: ""}", null, false)
            )
            try { mediaRecorder?.release() } catch (_: Throwable) {}
            mediaRecorder = null
        }
    }

    fun stopPhraseRecording(context: Context) {
        val s = _uiState.value
        val currentTopic = s.topics.getOrNull(s.selectedTopicIdx)
        try { mediaRecorder?.stop() } catch (_: Throwable) {}
        try { mediaRecorder?.reset() } catch (_: Throwable) {}
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
        _uiState.value = _uiState.value.copy(phraseRecordingState = PhraseRecordingState.PROCESSING)

        val file = phraseAudioFile
        if (currentTopic == null || file == null || !file.exists()) {
            _uiState.value = _uiState.value.copy(
                phraseRecordingState = PhraseRecordingState.IDLE,
                phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "Recording not available.", null, false)
            )
            return
        }

        val phraseIndex = recordingTargetIndex ?: currentTopic.phraseProgress?.currentPhraseIndex ?: 0
        viewModelScope.launch {
            try {
                val part = MultipartBody.Part.createFormData(
                    name = "audio",
                    filename = file.name,
                    body = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                val result = repo.submitPhraseRecording(currentTopic.id, phraseIndex, part)
                result.fold(
                    onSuccess = { dto ->
                        val audioOutPath = dto.audioUrl?.takeIf { it.isNotBlank() } ?: file.absolutePath
                        val safeText = dto.transcription ?: currentTopic.material.getOrNull(phraseIndex) ?: ""
                        val safeAccuracy = try { dto.accuracy } catch (_: Throwable) { null } ?: 0f
                        val newEntry = PhraseTranscriptEntry(
                            index = phraseIndex,
                            text = safeText,
                            audioPath = audioOutPath,
                            accuracy = safeAccuracy,
                            feedback = dto.feedback, // nullable in our model
                            timestamp = System.currentTimeMillis()
                        )
                        // Persist locally so recordings survive re-entry
                        saveTranscriptEntryToDisk(context, currentTopic.id, newEntry)
                        val updatedTranscripts = _uiState.value.currentTopicTranscripts.filterNot { it.index == phraseIndex } + newEntry
                        _uiState.value = _uiState.value.copy(
                            phraseRecordingState = PhraseRecordingState.IDLE,
                            phraseSubmissionResult = PhraseSubmissionResultUi(
                                success = dto.success,
                                accuracy = try { dto.accuracy } catch (_: Throwable) { null } ?: 0f,
                                transcription = dto.transcription ?: "",
                                feedback = dto.feedback ?: "",
                                nextPhraseIndex = dto.nextPhraseIndex,
                                topicCompleted = dto.topicCompleted,
                                xpAwarded = dto.xpAwarded
                            ),
                            currentTopicTranscripts = updatedTranscripts.sortedBy { it.index },
                            debug = "submit ok idx=$phraseIndex next=${dto.nextPhraseIndex} topicCompleted=${dto.topicCompleted} localRecs=${updatedTranscripts.size}"
                        )
                        // Refresh topics to pull updated server-side practice scores
                        reloadTopics()
                        // Count today's activity towards Day Streak
                        markSpeakingActivity()
                        // Exit review mode so hero follows current progress
                        _uiState.value = _uiState.value.copy(inspectedPhraseIndex = null)
                        // Optimistically advance current phrase so the UI updates immediately
                        dto.nextPhraseIndex?.let { nextIdx ->
                            val sNow = _uiState.value
                            val selIdx = sNow.selectedTopicIdx
                            val curTopic = sNow.topics.getOrNull(selIdx)
                            if (curTopic != null) {
                                val total = curTopic.phraseProgress?.totalPhrases ?: curTopic.material.size
                                val practicedDistinctCount = updatedTranscripts.map { it.index }.distinct().size
                                val completedNow = dto.topicCompleted || (total > 0 && practicedDistinctCount >= total)
                                val newProgress = curTopic.phraseProgress?.copy(
                                    currentPhraseIndex = nextIdx,
                                    isAllPhrasesCompleted = curTopic.phraseProgress?.isAllPhrasesCompleted == true || completedNow
                                ) ?: PhraseProgress(
                                    currentPhraseIndex = nextIdx,
                                    completedPhrases = curTopic.phraseProgress?.completedPhrases ?: emptyList(),
                                    totalPhrases = total,
                                    isAllPhrasesCompleted = completedNow
                                )
                                val newTopics = sNow.topics.toMutableList()
                                newTopics[selIdx] = curTopic.copy(phraseProgress = newProgress)
                                _uiState.value = sNow.copy(topics = newTopics)
                            }
                        }
                        // Fallback: if success but no nextPhraseIndex returned, advance to phraseIndex + 1
                        if (dto.success && !dto.topicCompleted && dto.nextPhraseIndex == null) {
                            val sNow = _uiState.value
                            val selIdx = sNow.selectedTopicIdx
                            val curTopic = sNow.topics.getOrNull(selIdx)
                            if (curTopic != null) {
                                val total = curTopic.phraseProgress?.totalPhrases ?: curTopic.material.size
                                val nextIdx = (phraseIndex + 1).coerceAtMost((total - 1).coerceAtLeast(0))
                                if (nextIdx != phraseIndex) {
                                    val practicedDistinctCount = updatedTranscripts.map { it.index }.distinct().size
                                    val completedNow = total > 0 && practicedDistinctCount >= total
                                    val newProgress = curTopic.phraseProgress?.copy(
                                        currentPhraseIndex = nextIdx,
                                        isAllPhrasesCompleted = curTopic.phraseProgress?.isAllPhrasesCompleted == true || completedNow
                                    ) ?: PhraseProgress(
                                        currentPhraseIndex = nextIdx,
                                        completedPhrases = curTopic.phraseProgress?.completedPhrases ?: emptyList(),
                                        totalPhrases = total,
                                        isAllPhrasesCompleted = completedNow
                                    )
                                    val newTopics = sNow.topics.toMutableList()
                                    newTopics[selIdx] = curTopic.copy(phraseProgress = newProgress)
                                    _uiState.value = sNow.copy(topics = newTopics)
                                }
                            }
                        }
                        recordingTargetIndex = null
                        // Refresh from backend to ensure we display server-side recordings
                        loadTranscriptsForCurrentTopic(context)

                        if (dto.success) {
                            // Consider topic completion if backend flag is true OR all phrases have at least one recording locally
                            val sNow2 = _uiState.value
                            val curTopic2 = sNow2.topics.getOrNull(sNow2.selectedTopicIdx)
                            val totalPhrases2 = curTopic2?.phraseProgress?.totalPhrases ?: curTopic2?.material?.size ?: 0
                            val practicedDistinct = _uiState.value.currentTopicTranscripts.map { it.index }.distinct().size
                            val shouldComplete = dto.topicCompleted || (totalPhrases2 > 0 && practicedDistinct >= totalPhrases2)
                            if (shouldComplete && curTopic2 != null) {
                                // Show completion overlay only once (on the run that completed the topic)
                                _uiState.value = _uiState.value.copy(showPronunciationCongrats = true)
                                try { repo.completeTopic(curTopic2.id) } catch (_: Throwable) {}
                                val previousTopics = _uiState.value.topics
                                reloadTopics(onComplete = {
                                    val newTopics = _uiState.value.topics
                                    val unlockedIndex = newTopics.indices.firstOrNull { idx ->
                                        val prev = previousTopics.getOrNull(idx)
                                        val now = newTopics[idx]
                                        prev != null && !prev.unlocked && now.unlocked
                                    }
                                    if (unlockedIndex != null) {
                                        _uiState.value = _uiState.value.copy(
                                            unlockedTopicInfo = UnlockedTopicInfo(
                                                title = newTopics[unlockedIndex].title,
                                                description = newTopics[unlockedIndex].description,
                                                xpGained = dto.xpAwarded,
                                                topicIndex = unlockedIndex
                                            ),
                                            // Keep phraseSubmissionResult as returned by server; do not local-add bonuses
                                            phraseSubmissionResult = _uiState.value.phraseSubmissionResult
                                        )
                                    }
                                })
                            }
                            fetchGamificationProfile()
                        } else {
                            // Update phrase submission result even on failure to show feedback
                            _uiState.value = _uiState.value.copy(
                                phraseSubmissionResult = _uiState.value.phraseSubmissionResult?.copy(xpAwarded = dto.xpAwarded)
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e("SpeakingJourney", "Submit phrase failed", e)
                        _uiState.value = _uiState.value.copy(
                            phraseRecordingState = PhraseRecordingState.IDLE,
                            phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "Failed to process recording. ${e.message ?: ""}", null, false),
                            debug = "submit fail: ${e.message ?: "unknown"}"
                        )
                        recordingTargetIndex = null
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error submitting phrase", t)
                _uiState.value = _uiState.value.copy(
                    phraseRecordingState = PhraseRecordingState.IDLE,
                    phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "An error occurred. ${t.message ?: ""}", null, false),
                    debug = "submit error: ${t.message ?: "unknown"}"
                )
                recordingTargetIndex = null
            }
        }
    }

    fun dismissPhraseResult() { _uiState.value = _uiState.value.copy(phraseSubmissionResult = null) }

    fun dismissPronunciationCongrats() {
        _uiState.value = _uiState.value.copy(showPronunciationCongrats = false)
    }

    fun dismissUnlockedTopicInfo() {
        val unlockedInfo = _uiState.value.unlockedTopicInfo
        _uiState.value = _uiState.value.copy(unlockedTopicInfo = null)
        if (unlockedInfo != null) {
            selectTopic(unlockedInfo.topicIndex)
        }
    }

    fun loadTranscriptsForCurrentTopic(context: Context) {
        val s = _uiState.value
        val topic = s.topics.getOrNull(s.selectedTopicIdx)
        if (topic == null) {
            _uiState.value = s.copy(currentTopicTranscripts = emptyList())
            return
        }
        // Ensure we have a user key before reading user-scoped storage
        if (!userKeyInitialized) {
            viewModelScope.launch {
                ensureUserKey()
                // Re-run load after key is set
                loadTranscriptsForCurrentTopic(context)
            }
            return
        }
        // Load locally persisted transcripts first for instant UI, then merge server results
        val local = readTranscriptEntriesFromDisk(context, topic.id)
        _uiState.value = _uiState.value.copy(
            currentTopicTranscripts = local,
            debug = "load rec local=${local.size} user=${currentUserKey}"
        )
        viewModelScope.launch {
            try {
                val res = repo.getUserPhraseRecordings(topic.id)
                res.fold(
                    onSuccess = { list ->
                        // Deduplicate by phraseIndex, keeping the most recent by createdAt
                        val latestByPhrase = list
                            .groupBy { it.phraseIndex }
                            .mapValues { (_, recs) ->
                                recs.maxByOrNull { r -> parseCreatedAtToEpoch(r.createdAt) ?: Long.MIN_VALUE }!!
                            }
                        val serverMapped = latestByPhrase.values.map { rec ->
                            val ts = parseCreatedAtToEpoch(rec.createdAt) ?: 0L
                            PhraseTranscriptEntry(
                                index = rec.phraseIndex,
                                text = rec.transcription,
                                audioPath = rec.audioUrl,
                                accuracy = rec.accuracy ?: 0f,
                                feedback = rec.feedback,
                                timestamp = ts
                            )
                        }
                        // Merge with existing local state to avoid wiping the latest local submission
                        val existing = _uiState.value.currentTopicTranscripts
                        val merged = (existing + serverMapped)
                            .groupBy { it.index }
                            .map { (_, entries) -> entries.maxByOrNull { it.timestamp }!! }
                            .sortedByDescending { it.timestamp }
                        _uiState.value = _uiState.value.copy(
                            currentTopicTranscripts = merged,
                            debug = "load rec local=${existing.size} server=${list.size} merged=${merged.size}"
                        )
                    },
                    onFailure = { e ->
                        Log.e("SpeakingJourney", "Failed to fetch recordings", e)
                        // Keep existing transcripts instead of clearing to avoid empty UI states
                        _uiState.value = _uiState.value.copy(
                            currentTopicTranscripts = _uiState.value.currentTopicTranscripts,
                            debug = "load rec fail: ${e.message ?: "unknown"}"
                        )
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error fetching recordings", t)
                // Keep existing transcripts on error
                _uiState.value = _uiState.value.copy(
                    currentTopicTranscripts = _uiState.value.currentTopicTranscripts,
                    debug = "load rec error: ${t.message ?: "unknown"}"
                )
            }
        }
    }

    private fun readTranscriptEntriesFromDisk(context: Context, topicId: String): List<PhraseTranscriptEntry> {
        return try {
            val dir = userTranscriptsDir(context)
            val file = File(dir, "$topicId.json")
            if (!file.exists()) return emptyList()
            val root = JSONObject(file.readText())
            val entries = if (root.has("entries")) root.getJSONObject("entries") else JSONObject()
            val list = mutableListOf<PhraseTranscriptEntry>()
            val keys = entries.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = entries.optJSONObject(key) ?: continue
                val index = obj.optInt("index", key.toIntOrNull() ?: 0)
                val text = obj.optString("text", "")
                val audioPath = obj.optString("audioPath", "")
                val accuracy = obj.optDouble("accuracy", 0.0).toFloat()
                val feedback = obj.optString("feedback", null)
                val timestamp = obj.optLong("timestamp", 0L)
                list.add(
                    PhraseTranscriptEntry(
                        index = index,
                        text = text,
                        audioPath = audioPath,
                        accuracy = accuracy,
                        feedback = feedback,
                        timestamp = timestamp
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to read local transcripts", t)
            emptyList()
        }
    }

    fun playUserRecording(path: String) {
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        try {
            val mp = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    try { it.release() } catch (_: Throwable) {}
                    if (mediaPlayer === it) mediaPlayer = null
                }
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    setOnPreparedListener { it.start() }
                    prepareAsync()
                } else {
                    prepare()
                    start()
                }
            }
            mediaPlayer = mp
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to play recording", t)
        }
    }

    fun playConversationAudioOrTts(
        context: Context,
        topicKey: String,
        turnIndex: Int,
        text: String,
        voiceName: String? = null,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            // Release any existing player before starting a new one
            try { mediaPlayer?.release() } catch (_: Throwable) {}

            // Build asset path: conversation_audios/<topic-slug>/turn_<n>.wav
            val slug = topicKey.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val turnNumber = (turnIndex + 1).coerceAtLeast(1)
            val assetPath = "conversation_audios/$slug/turn_${turnNumber}.wav"

            try {
                val afd = context.assets.openFd(assetPath)
                afd.use { desc ->
                    val mp = MediaPlayer().apply {
                        setDataSource(desc.fileDescriptor, desc.startOffset, desc.length)
                        setOnPreparedListener {
                            it.start()
                            onStart?.invoke()
                        }
                        setOnCompletionListener {
                            try { it.release() } catch (_: Throwable) {}
                            if (mediaPlayer === it) mediaPlayer = null
                            onDone?.invoke()
                        }
                        setOnErrorListener { player, what, extra ->
                            try { player.release() } catch (_: Throwable) {}
                            if (mediaPlayer === player) mediaPlayer = null
                            // If local asset playback fails at runtime, fallback to TTS
                            speakWithBackendTts(
                                text = text,
                                voiceName = voiceName,
                                onStart = onStart,
                                onDone = onDone,
                                onError = onError
                            )
                            true
                        }
                    }
                    mediaPlayer = mp
                    mp.prepareAsync()
                    return
                }
            } catch (_: Throwable) {
                // Asset not found or compressed; try stream -> cache file fallback
                try {
                    val ins = context.assets.open(assetPath)
                    ins.use { input ->
                        val outFile = File(context.cacheDir, "conv_${slug}_turn_${turnNumber}.wav")
                        outFile.outputStream().use { out ->
                            input.copyTo(out)
                        }
                        val mp = MediaPlayer().apply {
                            setDataSource(outFile.absolutePath)
                            setOnPreparedListener {
                                it.start()
                                onStart?.invoke()
                            }
                            setOnCompletionListener {
                                try { it.release() } catch (_: Throwable) {}
                                if (mediaPlayer === it) mediaPlayer = null
                                onDone?.invoke()
                            }
                            setOnErrorListener { player, what, extra ->
                                try { player.release() } catch (_: Throwable) {}
                                if (mediaPlayer === player) mediaPlayer = null
                                // If local playback fails at runtime, fallback to TTS
                                speakWithBackendTts(
                                    text = text,
                                    voiceName = voiceName,
                                    onStart = onStart,
                                    onDone = onDone,
                                    onError = onError
                                )
                                true
                            }
                        }
                        mediaPlayer = mp
                        mp.prepareAsync()
                        return
                    }
                } catch (_: Throwable) {
                    // proceed to res/raw fallback next
                }
            }

            // Try res/raw fallback with common naming patterns
            val candidates = listOf(
                "conv_${slug}_turn_${turnNumber}",
                "${slug}_turn_${turnNumber}",
                "turn_${turnNumber}"
            )
            val resId = candidates
                .map { name -> context.resources.getIdentifier(name, "raw", context.packageName) }
                .firstOrNull { it != 0 } ?: 0
            if (resId != 0) {
                try {
                    try { mediaPlayer?.release() } catch (_: Throwable) {}
                    val mp = MediaPlayer.create(context, resId)
                    if (mp != null) {
                        mediaPlayer = mp
                        onStart?.invoke()
                        mp.setOnCompletionListener {
                            try { it.release() } catch (_: Throwable) {}
                            if (mediaPlayer === it) mediaPlayer = null
                            onDone?.invoke()
                        }
                        mp.setOnErrorListener { player, what, extra ->
                            try { player.release() } catch (_: Throwable) {}
                            if (mediaPlayer === player) mediaPlayer = null
                            speakWithBackendTts(
                                text = text,
                                voiceName = voiceName,
                                onStart = onStart,
                                onDone = onDone,
                                onError = onError
                            )
                            true
                        }
                        mp.start()
                        return
                    }
                } catch (_: Throwable) {
                    // fall through to TTS
                }
            }

            // Finally, TTS fallback
            speakWithBackendTts(
                text = text,
                voiceName = voiceName,
                onStart = onStart,
                onDone = onDone,
                onError = onError
            )
        } catch (t: Throwable) {
            // As a last resort, try TTS and report error
            Log.e("SpeakingJourney", "Local audio playback failed, using TTS", t)
            speakWithBackendTts(
                text = text,
                voiceName = voiceName,
                onStart = onStart,
                onDone = onDone,
                onError = onError
            )
        }
    }

    fun speakWithBackendTts(
        text: String,
        voiceName: String? = null,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val effectiveVoice = preferredTtsVoiceName ?: voiceName
                val result = repo.generateTts(text, effectiveVoice)
                result.fold(
                    onSuccess = { dto ->
                        val url = dto.audioUrl
                        try { mediaPlayer?.release() } catch (_: Throwable) {}
                        try {
                            val mp = MediaPlayer().apply {
                                setDataSource(url)
                                setOnPreparedListener {
                                    it.start()
                                    onStart?.invoke()
                                }
                                setOnCompletionListener {
                                    try { it.release() } catch (_: Throwable) {}
                                    if (mediaPlayer === it) mediaPlayer = null
                                    onDone?.invoke()
                                }
                                setOnErrorListener { player, what, extra ->
                                    try { player.release() } catch (_: Throwable) {}
                                    if (mediaPlayer === player) mediaPlayer = null
                                    onError?.invoke("Playback error ($what/$extra)")
                                    true
                                }
                            }
                            mediaPlayer = mp
                            mp.prepareAsync()
                        } catch (t: Throwable) {
                            Log.e("SpeakingJourney", "Failed to play TTS", t)
                            onError?.invoke("Unable to play audio: ${t.message ?: "unknown"}")
                        }
                    },
                    onFailure = { e ->
                        Log.e("SpeakingJourney", "TTS generation failed", e)
                        onError?.invoke(e.message ?: "TTS generation failed")
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error during TTS request", t)
                onError?.invoke(t.message ?: "Unexpected error")
            }
        }
    }

    fun stopPlayback() {
        try { mediaPlayer?.stop() } catch (_: Throwable) {}
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        mediaPlayer = null
    }

    // --- Review/inspection helpers for practiced phrases ---
    fun setInspectedPhraseIndex(index: Int) {
        val practiced = _uiState.value.currentTopicTranscripts.map { it.index }.distinct().toSet()
        if (index in practiced) {
            _uiState.value = _uiState.value.copy(inspectedPhraseIndex = index)
        }
    }

    /**
     * Begin review mode from the start of the topic.
     * Selects the earliest practiced phrase (smallest index) so users can review from phrase 1
     * even on completed topics. If nothing is practiced yet, falls back to index 0.
     */
    fun beginReviewFromStart() {
        val practiced = _uiState.value.currentTopicTranscripts.map { it.index }.distinct().sorted()
        val startIndex = practiced.firstOrNull() ?: 0
        _uiState.value = _uiState.value.copy(inspectedPhraseIndex = startIndex)
    }

    fun clearInspection() {
        _uiState.value = _uiState.value.copy(inspectedPhraseIndex = null)
    }

    fun inspectPreviousPhrase() {
        val s = _uiState.value
        val practiced = s.currentTopicTranscripts.map { it.index }.distinct().sorted()
        if (practiced.isEmpty()) return

        val inspected = s.inspectedPhraseIndex
        if (inspected != null) {
            val target = practiced.filter { it < inspected }.maxOrNull()
            if (target != null) {
                _uiState.value = s.copy(inspectedPhraseIndex = target)
            }
        } else {
            // Not currently in review: jump to the most recent practiced phrase before the current phrase
            val currentIdx = s.topics.getOrNull(s.selectedTopicIdx)?.phraseProgress?.currentPhraseIndex ?: 0
            val target = practiced.filter { it <= currentIdx - 1 }.maxOrNull()
            if (target != null) {
                _uiState.value = s.copy(inspectedPhraseIndex = target)
            }
        }
    }

    fun inspectNextPhrase() {
        val s = _uiState.value
        val practiced = s.currentTopicTranscripts.map { it.index }.distinct().sorted()
        val inspected = s.inspectedPhraseIndex ?: return
        val target = practiced.firstOrNull { it > inspected }
        if (target != null) {
            _uiState.value = s.copy(inspectedPhraseIndex = target)
        }
    }

    private fun fetchGamificationProfile() {
        viewModelScope.launch {
            try {
                val profile = profileRepo.getProfile()
                val xp = profile.experiencePoints ?: 0
                val level = profile.currentLevel ?: 1
                val streak = profile.streakDays ?: 0
                // Update user-scoped storage key from profile so recordings/transcripts are namespaced per user
                runCatching {
                    val base = (profile.userEmail?.trim()?.lowercase(Locale.US))
                        ?: (profile.userName?.trim()?.lowercase(Locale.US))
                        ?: "default"
                    val sanitized = base.replace(Regex("[^a-z0-9._-]"), "_")
                    currentUserKey = sanitized
                    userKeyInitialized = true
                }
                _uiState.value = _uiState.value.copy(
                    gamificationProfile = GamificationProfile(level, xp, streak)
                )
            } catch (e: Exception) {
                Log.e("SpeakingJourneyViewModel", "Failed to fetch gamification profile", e)
                // Keep existing or default data on failure
            }
        }
    }
 

    override fun onCleared() {
        super.onCleared()
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        mediaPlayer = null
    }

    private fun parseCreatedAtToEpoch(createdAt: String?): Long? {
        if (createdAt.isNullOrBlank()) return null
        // First, try to normalize fractional seconds to milliseconds (3 digits)
        val normalized = try {
            val tIndex = createdAt.indexOf('T')
            val dotIndex = createdAt.indexOf('.', startIndex = if (tIndex >= 0) tIndex else 0)
            if (dotIndex >= 0) {
                // Find end of fraction (before timezone indicator 'Z' or offset)
                val tzStart = createdAt.indexOfAny(charArrayOf('Z', '+', '-'), startIndex = dotIndex)
                val endIndex = if (tzStart >= 0) tzStart else createdAt.length
                val fraction = createdAt.substring(dotIndex + 1, endIndex)
                val milli = when {
                    fraction.length >= 3 -> fraction.substring(0, 3)
                    else -> fraction.padEnd(3, '0')
                }
                val prefix = createdAt.substring(0, dotIndex)
                val suffix = if (tzStart >= 0) createdAt.substring(tzStart) else "Z"
                "$prefix.$milli$suffix"
            } else createdAt
        } catch (_: Throwable) { createdAt }

        // Try multiple ISO8601 patterns
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd HH:mm:ss",
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val d: Date? = sdf.parse(normalized)
                if (d != null) return d.time
            } catch (_: ParseException) {
            } catch (_: IllegalArgumentException) {
            }
        }
        return null
    }

    private fun saveTranscriptEntryToDisk(context: Context, topicId: String, entry: PhraseTranscriptEntry) {
        try {
            val dir = userTranscriptsDir(context)
            val file = File(dir, "$topicId.json")
            val root = if (file.exists()) JSONObject(file.readText()) else JSONObject()
            val entries = if (root.has("entries")) root.getJSONObject("entries") else JSONObject().also { root.put("entries", it) }
            val obj = JSONObject().apply {
                put("index", entry.index)
                put("text", entry.text)
                put("audioPath", entry.audioPath)
                put("accuracy", entry.accuracy.toDouble())
                put("feedback", entry.feedback ?: "")
                put("timestamp", entry.timestamp)
            }
            entries.put(entry.index.toString(), obj)
            file.writeText(root.toString())
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to persist transcript", t)
        }
    }

    private fun userTranscriptsDir(context: Context): File {
        return File(context.filesDir, "voicevibe/users/${currentUserKey}/transcripts").apply { mkdirs() }
    }

    private fun userRecordingsDir(context: Context, topicId: String): File {
        return File(context.filesDir, "voicevibe/users/${currentUserKey}/recordings/${topicId}").apply { mkdirs() }
    }

    private suspend fun ensureUserKey() {
        if (userKeyInitialized && currentUserKey != "default") return
        try {
            val profile = profileRepo.getProfile()
            val base = (profile.userEmail?.trim()?.lowercase(Locale.US))
                ?: (profile.userName?.trim()?.lowercase(Locale.US))
                ?: "default"
            val sanitized = base.replace(Regex("[^a-z0-9._-]"), "_")
            currentUserKey = sanitized
            userKeyInitialized = true
        } catch (_: Throwable) {
            // Keep default, but mark initialized to avoid loops
            userKeyInitialized = true
        }
    }
}
