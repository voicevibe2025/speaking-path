package com.example.voicevibe.presentation.screens.speakingjourney

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.SpeakingPracticeRepository
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.SpeakingEvaluation
import com.example.voicevibe.domain.model.SpeakingSession
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.utils.TranscriptUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class FluencyPracticeViewModel @Inject constructor(
    private val practiceRepo: SpeakingPracticeRepository,
    private val journeyRepo: SpeakingJourneyRepository,
    private val gamificationRepo: GamificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FluencyUiState())
    val uiState: StateFlow<FluencyUiState> = _uiState

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentTopicId: String? = null
    private var currentPromptId: String? = null // backend prompt used for submission
    private var currentPromptIndex: Int = 0 // topic-specific fluency prompt index
    private var currentPrompts: List<String> = emptyList()

    private var currentUserId: String? = null
    
    // Countdown timer configuration
    private val MAX_RECORDING_DURATION = 30 // seconds
    private var autoSubmitContext: Context? = null

    init {
        // Cache current user ID for user-scoped persistence of attempts
        viewModelScope.launch {
            userRepository.getCurrentUser().collectLatest { res ->
                if (res is Resource.Success) {
                    currentUserId = res.data?.id
                }
            }
        }
    }

    fun initializeForTopic(context: Context, topic: Topic) {
        if (currentTopicId == topic.id) return // already initialized
        currentTopicId = topic.id

        // For single prompt, just get the one prompt from backend or generate one
        currentPrompts = topic.fluencyPracticePrompts
        currentPromptIndex = 0 // Always 0 for single prompt

        val promptText = if (currentPrompts.isNotEmpty()) {
            currentPrompts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: buildPromptFromTopic(topic)
        } else {
            buildPromptFromTopic(topic)
        }
        
        // No hints for single prompt fluency practice
        val hints = emptyList<String>()
        _uiState.update { it.copy(prompt = promptText, hints = hints, error = null, allPrompts = listOf(promptText), currentPromptIndex = 0, showStartOverlay = true) }

        // Load an associated PracticePrompt for submission (best-effort)
        viewModelScope.launch {
            // Try to find a prompt by category = topic.title; otherwise fallback to random
            var selectedPromptId: String? = null
            practiceRepo.getPromptsByCategory(topic.title).collectLatest { res ->
                when (res) {
                    is Resource.Success -> {
                        val list = res.data ?: emptyList()
                        if (list.isNotEmpty()) {
                            selectedPromptId = list.first().id
                        }
                    }
                    is Resource.Error -> {
                        // ignore, we'll fallback below
                    }
                    is Resource.Loading -> {
                        // ignore
                    }
                }
            }
            if (selectedPromptId == null) {
                practiceRepo.getRandomPrompt().collectLatest { res ->
                    if (res is Resource.Success) {
                        selectedPromptId = res.data?.id
                    }
                }
            }
            currentPromptId = selectedPromptId
            // Wait briefly for current user to be known so we can scope attempts per-user
            if (currentUserId == null) {
                repeat(40) { // ~2s max
                    kotlinx.coroutines.delay(50)
                    if (currentUserId != null) return@repeat
                }
            }
            loadPastAttempts(context, topic.id)
        }
    }

    fun playAttempt(attempt: FluencyAttempt) {
        val path = attempt.audioPath
        if (path.startsWith("/")) {
            // Likely an older relative URL saved before backend fix; try to refetch the session for an absolute URL
            viewModelScope.launch {
                when (val ss = practiceRepo.getSession(attempt.sessionId)) {
                    is Resource.Success -> {
                        val session = ss.data
                        if (session != null) {
                            val url = session.audioUrl
                            playRecording(url)
                        } else {
                            _uiState.update { it.copy(error = "Unable to fetch audio for playback") }
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(error = "Unable to fetch audio for playback") }
                    }
                }
            }
        } else {
            playRecording(path)
        }
    }

    private fun buildPromptFromTopic(topic: Topic): String {
        val base = "Speak for about 30 seconds about: \"${topic.title}\"."
        val guidance = if (topic.description.isNotBlank()) {
            " Consider: ${topic.description.take(160)}"
        } else ""
        val materialHint = if (topic.material.isNotEmpty()) {
            val bullets = topic.material.take(3).joinToString("; ")
            " Try to include: $bullets."
        } else ""
        return base + guidance + materialHint
    }

    fun startRecording(context: Context) {
        try {
            val topicId = currentTopicId ?: return
            val dir = File(context.filesDir, "voicevibe/fluency/$topicId").apply { mkdirs() }
            val file = File(dir, "attempt_${System.currentTimeMillis()}.m4a")
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
            mr.setOutputFile(file.absolutePath)
            mr.prepare(); mr.start()
            // Start countdown from MAX_RECORDING_DURATION
            autoSubmitContext = context
            _uiState.update { it.copy(recordingState = RecordingState.RECORDING, audioFilePath = file.absolutePath, recordingDuration = MAX_RECORDING_DURATION, error = null) }
            tickTimer()
        } catch (t: Throwable) {
            Log.e("FluencyVM", "Failed to start recording", t)
            _uiState.update { it.copy(error = "Unable to start recording: ${t.message ?: "unknown"}", recordingState = RecordingState.IDLE) }
            releaseRecorder()
            autoSubmitContext = null
        }
    }

    fun stopRecording() {
        try { mediaRecorder?.stop() } catch (_: Throwable) {}
        try { mediaRecorder?.reset() } catch (_: Throwable) {}
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
        autoSubmitContext = null
        _uiState.update { st -> st.copy(recordingState = RecordingState.STOPPED) }
    }

    fun submitRecording(context: Context) {
        val promptId = currentPromptId ?: run {
            _uiState.update { it.copy(error = "No prompt available yet. Please try again.") }
            return
        }
        val filePath = _uiState.value.audioFilePath ?: run {
            _uiState.update { it.copy(error = "No recording to submit.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            practiceRepo.submitRecording(promptId, filePath).collectLatest { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isSubmitting = true) }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSubmitting = false, error = res.message ?: "Submission failed") }
                    }
                    is Resource.Success -> {
                        val sessionId = res.data?.sessionId
                        if (sessionId.isNullOrBlank()) {
                            _uiState.update { it.copy(isSubmitting = false, error = "Missing session id from server") }
                            return@collectLatest
                        }
                        // Fetch evaluation and session details
                        val eval = when (val ev = practiceRepo.getEvaluation(sessionId)) {
                            is Resource.Success -> ev.data
                            else -> null
                        }
                        val sess = when (val ss = practiceRepo.getSession(sessionId)) {
                            is Resource.Success -> ss.data
                            else -> null
                        }
                        val analysis = buildAnalysisFromEvaluation(eval, sess)
                        val serverUrl = sess?.audioUrl
                        val chosenAudio = if (serverUrl != null && (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))) serverUrl else filePath
                        val attempt = FluencyAttempt(
                            sessionId = sessionId,
                            audioPath = chosenAudio,
                            transcript = sess?.transcription,
                            feedback = eval?.feedback ?: "",
                            overallScore = eval?.overallScore ?: res.data?.score ?: 0f,
                            createdAt = currentTimestampString(),
                            pauses = analysis.pauses,
                            stutterCount = analysis.stutterCount,
                            mispronunciations = analysis.mispronunciations
                        )
                        // Count this speaking activity towards Day Streak (idempotent server-side)
                        runCatching { gamificationRepo.updateStreak() }

                        // Submit fluency recording to Gemini for evaluation
                        val topicId = currentTopicId
                        if (!topicId.isNullOrBlank()) {
                            try {
                                // Use new Gemini-based endpoint
                                val audioFile = File(filePath)
                                // Calculate actual elapsed time (MAX_RECORDING_DURATION - remaining time)
                                val recordingDuration = (MAX_RECORDING_DURATION - _uiState.value.recordingDuration).toFloat()
                                val res2 = journeyRepo.submitFluencyRecording(
                                    topicId = topicId,
                                    audioFile = audioFile,
                                    recordingDuration = recordingDuration
                                )
                                res2.onSuccess { body ->
                                    // Compute objective score locally (more strict and consistent)
                                    val pausesList = analysis.pauses
                                    val stutters = analysis.stutterCount
                                    val mispronCount = analysis.mispronunciations.size
                                    val objectiveScore = computeObjectiveFluencyScore(
                                        transcription = body.transcription,
                                        durationSec = recordingDuration,
                                        pauses = pausesList,
                                        stutterCount = stutters,
                                        mispronCount = mispronCount
                                    )

                                    // Also sync this objective score back to server so TopicMaster reflects it
                                    val serverSessionId = try { body.sessionId } catch (_: Throwable) { null }
                                    val topicIdSafe = topicId
                                    viewModelScope.launch {
                                        runCatching {
                                            journeyRepo.submitFluencyPromptScore(
                                                topicId = topicIdSafe,
                                                promptIndex = 0,
                                                score = objectiveScore,
                                                sessionId = serverSessionId
                                            )
                                        }.onFailure { e ->
                                            Log.w("FluencyVM", "Failed to sync objective score to server: ${e.message}")
                                        }
                                    }

                                    // Update the attempt with objective score and Gemini feedback/transcript
                                    val updatedAttempt = attempt.copy(
                                        overallScore = objectiveScore.toFloat(),
                                        feedback = body.feedback,
                                        transcript = body.transcription
                                    )
                                    persistAttempt(context, currentTopicId!!, updatedAttempt)

                                    // Create updated evaluation with objective score for UI display
                                    val updatedEval = eval?.copy(
                                        overallScore = objectiveScore.toFloat(),
                                        feedback = body.feedback,
                                        suggestions = body.suggestions
                                    )

                                    // Completion based on objective score
                                    val isCompleted = objectiveScore >= 75
                                    val xpGained = if (isCompleted) 50 else 10  // Completion bonus or participation

                                    _uiState.update { st ->
                                        st.copy(
                                            showCongrats = isCompleted,
                                            totalFluencyScore = objectiveScore,
                                            completionXpGained = if (isCompleted) xpGained else st.completionXpGained,
                                            lastAwardedXp = xpGained,
                                            evaluation = updatedEval,
                                            pastAttempts = st.pastAttempts.map { 
                                                if (it.sessionId == updatedAttempt.sessionId) updatedAttempt else it
                                            }
                                        )
                                    }
                                }.onFailure { e ->
                                    // Fallback: compute objective score using available data and save locally
                                    val pausesList = analysis.pauses
                                    val stutters = analysis.stutterCount
                                    val mispronCount = analysis.mispronunciations.size
                                    val objectiveScore = computeObjectiveFluencyScore(
                                        transcription = sess?.transcription,
                                        durationSec = recordingDuration,
                                        pauses = pausesList,
                                        stutterCount = stutters,
                                        mispronCount = mispronCount
                                    )
                                    val updatedAttempt = attempt.copy(overallScore = objectiveScore.toFloat())
                                    persistAttempt(context, currentTopicId!!, updatedAttempt)
                                    _uiState.update { st -> 
                                        st.copy(
                                            error = "Failed to submit to server: ${e.message}",
                                            evaluation = st.evaluation?.copy(overallScore = objectiveScore.toFloat()) ?: eval?.copy(overallScore = objectiveScore.toFloat()),
                                            totalFluencyScore = objectiveScore,
                                            pastAttempts = st.pastAttempts + updatedAttempt
                                        )
                                    }
                                }
                            } catch (t: Throwable) {
                                // Fallback: compute objective score and save locally
                                val pausesList = analysis.pauses
                                val stutters = analysis.stutterCount
                                val mispronCount = analysis.mispronunciations.size
                                val objectiveScore = computeObjectiveFluencyScore(
                                    transcription = sess?.transcription,
                                    durationSec = (MAX_RECORDING_DURATION - _uiState.value.recordingDuration).toFloat(),
                                    pauses = pausesList,
                                    stutterCount = stutters,
                                    mispronCount = mispronCount
                                )
                                val updatedAttempt = attempt.copy(overallScore = objectiveScore.toFloat())
                                persistAttempt(context, currentTopicId!!, updatedAttempt)
                                _uiState.update { st ->
                                    st.copy(
                                        error = "Failed to submit: ${t.message}",
                                        evaluation = st.evaluation?.copy(overallScore = objectiveScore.toFloat()) ?: eval?.copy(overallScore = objectiveScore.toFloat()),
                                        totalFluencyScore = objectiveScore,
                                        pastAttempts = st.pastAttempts + updatedAttempt
                                    )
                                }
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                evaluation = _uiState.value.evaluation ?: eval,
                                session = sess,
                                latestAnalysis = analysis,
                                showResults = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildAnalysisFromEvaluation(eval: SpeakingEvaluation?, sess: SpeakingSession?): FluencyAnalysis {
        val mis = eval?.phoneticErrors?.map { e -> e.word } ?: emptyList()
        // Prefer server-provided values
        val pauses = eval?.pauses ?: run {
            val text = sess?.transcription.orEmpty()
            if (text.isNotBlank()) listOf(0.8f, 1.2f) else emptyList()
        }
        val stutters = eval?.stutters ?: run {
            val text = sess?.transcription.orEmpty()
            if (text.contains("-")) 2 else 0
        }
        return FluencyAnalysis(pauses = pauses, stutterCount = stutters, mispronunciations = mis)
    }

    // Objective fluency scoring derived from measurable signals
    private fun computeObjectiveFluencyScore(
        transcription: String?,
        durationSec: Float,
        pauses: List<Float>?,
        stutterCount: Int,
        mispronCount: Int
    ): Int {
        val dur = if (durationSec > 0f) durationSec else 0f
        val raw = (transcription ?: "").trim()
        val originalWords = if (raw.isNotEmpty()) raw.split(Regex("\\s+")).filter { it.isNotBlank() } else emptyList()
        val collapsed = TranscriptUtils.collapseRepeats(raw, force = true)
        val cleanedWords = if (collapsed.isNotEmpty()) collapsed.split(Regex("\\s+")).filter { it.isNotBlank() } else emptyList()

        val originalCount = originalWords.size
        val cleanedCount = cleanedWords.size
        val repeatsRemoved = max(0, originalCount - cleanedCount)
        val repeatRatio = if (originalCount > 0) repeatsRemoved.toFloat() / originalCount.toFloat() else 0f

        val minutes = if (dur > 0f) dur / 60f else 1f
        val wpm = if (dur > 0f) cleanedCount / minutes else 0f

        // Speech rate score (ideal ~140 wpm, tolerance ±60)
        val target = 140f
        val allowed = 60f
        val rateDiff = abs(wpm - target)
        val rateScore = (100f - (rateDiff / allowed) * 100f).coerceIn(0f, 100f)

        // Time coverage score (full credit near 28-30s)
        val timeScore = (min(1f, dur / 28f) * 100f).coerceIn(0f, 100f)

        // Penalties
        val pList = pauses ?: emptyList()
        val longPausesCount = pList.count { it >= 1.0f }
        val pausePenalty = min(18f, (longPausesCount * 3).toFloat())
        val stutterPenalty = min(16f, (stutterCount * 4).toFloat())
        val repetitionPenalty = min(16f, repeatRatio * 40f)
        val mispronPenalty = min(18f, (mispronCount * 3).toFloat())

        val base = rateScore * 0.6f + timeScore * 0.4f
        val score = (base - (pausePenalty + stutterPenalty + repetitionPenalty + mispronPenalty)).toInt()
        return score.coerceIn(0, 100)
    }

    fun playRecording(path: String) {
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
                    prepare(); start()
                }
            }
            mediaPlayer = mp
        } catch (t: Throwable) {
            Log.e("FluencyVM", "Failed to play recording", t)
            _uiState.update { it.copy(error = "Unable to play: ${t.message}") }
        }
    }

    fun dismissResults() {
        _uiState.update {
            it.copy(
                showResults = false,
                recordingState = RecordingState.IDLE,
                audioFilePath = null,
                recordingDuration = 0
            )
        }
    }

    fun dismissCongrats() {
        _uiState.update { it.copy(showCongrats = false) }
    }

    fun dismissStartOverlay() {
        _uiState.update { it.copy(showStartOverlay = false) }
    }

    fun resetRecording() {
        autoSubmitContext = null
        _uiState.update { it.copy(recordingState = RecordingState.IDLE, audioFilePath = null, recordingDuration = 0) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun tickTimer() {
        viewModelScope.launch {
            while (_uiState.value.recordingState == RecordingState.RECORDING) {
                kotlinx.coroutines.delay(1000)
                val newDuration = _uiState.value.recordingDuration - 1
                
                if (newDuration <= 0) {
                    // Time's up! Auto-stop and auto-submit
                    stopRecording()
                    val ctx = autoSubmitContext
                    if (ctx != null) {
                        submitRecording(ctx)
                    }
                    break
                } else {
                    _uiState.update { it.copy(recordingDuration = newDuration) }
                }
            }
        }
    }

    private fun releaseRecorder() {
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
    }

    override fun onCleared() {
        super.onCleared()
        releaseRecorder()
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        mediaPlayer = null
        autoSubmitContext = null
    }

    private fun attemptsFile(context: Context, topicId: String, userId: String): File {
        val dir = File(context.filesDir, "voicevibe/fluency_attempts/${userId}").apply { mkdirs() }
        return File(dir, "${topicId}.json")
    }

    private suspend fun loadPastAttempts(context: Context, topicId: String) {
        try {
            val uid = currentUserId
            if (uid.isNullOrBlank()) {
                _uiState.update { it.copy(pastAttempts = emptyList()) }
                return
            }
            val file = attemptsFile(context, topicId, uid)
            if (!file.exists()) {
                _uiState.update { it.copy(pastAttempts = emptyList()) }
                return
            }
            val arr = JSONArray(file.readText())
            val list = mutableListOf<FluencyAttempt>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    FluencyAttempt(
                        sessionId = o.optString("sessionId"),
                        audioPath = o.optString("audioPath"),
                        transcript = o.optString("transcript"),
                        feedback = o.optString("feedback"),
                        overallScore = o.optDouble("overallScore", 0.0).toFloat(),
                        createdAt = o.optString("createdAt"),
                        pauses = o.optJSONArray("pauses")?.let { ja ->
                            List(ja.length()) { idx -> ja.getDouble(idx).toFloat() }
                        } ?: emptyList(),
                        stutterCount = o.optInt("stutterCount", 0),
                        mispronunciations = o.optJSONArray("mispronunciations")?.let { ja ->
                            List(ja.length()) { idx -> ja.getString(idx) }
                        } ?: emptyList()
                    )
                )
            }
            _uiState.update { it.copy(pastAttempts = list.sortedByDescending { a -> a.createdAt }) }
        } catch (t: Throwable) {
            Log.e("FluencyVM", "Failed to load past attempts", t)
            _uiState.update { it.copy(pastAttempts = emptyList()) }
        }
    }

    private suspend fun persistAttempt(context: Context, topicId: String, attempt: FluencyAttempt) {
        try {
            val uid = currentUserId ?: return
            val file = attemptsFile(context, topicId, uid)
            val arr = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            val obj = JSONObject().apply {
                put("sessionId", attempt.sessionId)
                put("audioPath", attempt.audioPath)
                put("transcript", attempt.transcript ?: "")
                put("feedback", attempt.feedback)
                put("overallScore", attempt.overallScore.toDouble())
                put("createdAt", attempt.createdAt)
                put("pauses", JSONArray(attempt.pauses))
                put("stutterCount", attempt.stutterCount)
                put("mispronunciations", JSONArray(attempt.mispronunciations))
                put("userId", uid)
            }
            arr.put(obj)
            file.writeText(arr.toString())
        } catch (t: Throwable) {
            Log.e("FluencyVM", "Failed to save attempt", t)
        }
    }

    private fun currentTimestampString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }
}

// --- UI Models ---

data class FluencyAttempt(
    val sessionId: String,
    val audioPath: String,
    val transcript: String?,
    val feedback: String,
    val overallScore: Float,
    val createdAt: String,
    val pauses: List<Float> = emptyList(),
    val stutterCount: Int = 0,
    val mispronunciations: List<String> = emptyList()
)

data class FluencyAnalysis(
    val pauses: List<Float> = emptyList(),
    val stutterCount: Int = 0,
    val mispronunciations: List<String> = emptyList()
)

data class FluencyUiState(
    val isLoading: Boolean = false,
    val prompt: String = "",
    val hints: List<String> = emptyList(),
    val allPrompts: List<String> = emptyList(),
    val currentPromptIndex: Int = 0,
    val recordingState: RecordingState = RecordingState.IDLE,
    val recordingDuration: Int = 0,
    val audioFilePath: String? = null,
    val isSubmitting: Boolean = false,
    val evaluation: SpeakingEvaluation? = null,
    val session: SpeakingSession? = null,
    val showResults: Boolean = false,
    val latestAnalysis: FluencyAnalysis? = null,
    val pastAttempts: List<FluencyAttempt> = emptyList(),
    val error: String? = null,
    // Congrats overlay state
    val showCongrats: Boolean = false,
    val completionPromptScores: List<Int> = emptyList(),
    val totalFluencyScore: Int = 0,
    val completionXpGained: Int = 0,
    val lastAwardedXp: Int = 0,
    // Start overlay shown when first entering practice for the topic
    val showStartOverlay: Boolean = false
)
 

enum class RecordingState { IDLE, RECORDING, PAUSED, STOPPED }
