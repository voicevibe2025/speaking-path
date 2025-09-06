package com.example.voicevibe.presentation.screens.speakingjourney

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.SpeakingPracticeRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import com.example.voicevibe.domain.model.SpeakingEvaluation
import com.example.voicevibe.domain.model.SpeakingSession
import com.example.voicevibe.domain.model.Resource
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

@HiltViewModel
class FluencyPracticeViewModel @Inject constructor(
    private val practiceRepo: SpeakingPracticeRepository,
    private val journeyRepo: SpeakingJourneyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FluencyUiState())
    val uiState: StateFlow<FluencyUiState> = _uiState

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentTopicId: String? = null
    private var currentPromptId: String? = null // backend prompt used for submission
    private var currentPromptIndex: Int = 0 // topic-specific fluency prompt index
    private var currentPrompts: List<String> = emptyList()

    fun initializeForTopic(context: Context, topic: Topic) {
        if (currentTopicId == topic.id) return // already initialized
        currentTopicId = topic.id

        // Prefer backend-provided prompts for this topic; set active index from fluencyProgress
        currentPrompts = topic.fluencyPracticePrompts
        currentPromptIndex = topic.fluencyProgress?.nextPromptIndex ?: 0
        if (currentPromptIndex !in currentPrompts.indices) currentPromptIndex = 0

        val promptText = currentPrompts.getOrNull(currentPromptIndex)?.trim()
            ?.takeIf { it.isNotBlank() } ?: buildPromptFromTopic(topic)
        val hints = if (currentPrompts.isNotEmpty()) {
            currentPrompts.mapIndexed { idx, p ->
                when {
                    idx < currentPromptIndex -> "✓ ${p}"
                    idx == currentPromptIndex -> "• ${p} (current)"
                    else -> "🔒 ${p}"
                }
            }.filterIndexed { idx, _ -> idx != currentPromptIndex }.take(3)
        } else topic.material.take(3).map { "• $it" }
        _uiState.update { it.copy(prompt = promptText, hints = hints, error = null, allPrompts = currentPrompts, currentPromptIndex = currentPromptIndex) }

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
        val base = "Speak for 60–90 seconds about: \"${topic.title}\"."
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
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            mediaRecorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(file.absolutePath)
            mr.prepare(); mr.start()
            _uiState.update { it.copy(recordingState = RecordingState.RECORDING, audioFilePath = file.absolutePath, recordingDuration = 0, error = null) }
            tickTimer()
        } catch (t: Throwable) {
            Log.e("FluencyVM", "Failed to start recording", t)
            _uiState.update { it.copy(error = "Unable to start recording: ${t.message ?: "unknown"}", recordingState = RecordingState.IDLE) }
            releaseRecorder()
        }
    }

    fun stopRecording() {
        try { mediaRecorder?.stop() } catch (_: Throwable) {}
        try { mediaRecorder?.reset() } catch (_: Throwable) {}
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
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
                        persistAttempt(context, currentTopicId!!, attempt)
                        val updated = _uiState.value.pastAttempts + attempt

                        // Submit fluency score to SpeakingJourney to unlock next prompt
                        val topicId = currentTopicId
                        val scoreToSubmit = attempt.overallScore.toInt().coerceAtLeast(0)
                        if (!topicId.isNullOrBlank()) {
                            try {
                                val res2 = journeyRepo.submitFluencyPromptScore(
                                    topicId = topicId,
                                    promptIndex = currentPromptIndex,
                                    score = scoreToSubmit,
                                    sessionId = sessionId
                                )
                                res2.onSuccess { body ->
                                    // Update local active prompt based on backend response
                                    currentPromptIndex = body.nextPromptIndex ?: currentPromptIndex + 1
                                    if (currentPromptIndex >= currentPrompts.size) currentPromptIndex = currentPrompts.lastIndex.coerceAtLeast(0)
                                    val newPrompt = currentPrompts.getOrNull(currentPromptIndex) ?: _uiState.value.prompt
                                    val newHints = currentPrompts.mapIndexed { idx, p ->
                                        when {
                                            idx < currentPromptIndex -> "✓ ${p}"
                                            idx == currentPromptIndex -> "• ${p} (current)"
                                            else -> "🔒 ${p}"
                                        }
                                    }.filterIndexed { idx, _ -> idx != currentPromptIndex }.take(3)
                                    val isCompleted = body.fluencyCompleted
                                    val summaryScores = body.promptScores
                                    val totalScore = body.fluencyTotalScore
                                    val xpDelta = body.xpAwarded
                                    // Compute total XP across all prompts when completed: 50 per prompt >=80 plus 100 bonus
                                    val totalXpWhenCompleted = if (isCompleted) (summaryScores.count { it >= 80 } * 50) + 100 else 0
                                    _uiState.update { st ->
                                        st.copy(
                                            prompt = newPrompt,
                                            hints = newHints,
                                            currentPromptIndex = currentPromptIndex,
                                            allPrompts = currentPrompts,
                                            showCongrats = isCompleted,
                                            completionPromptScores = if (isCompleted) summaryScores else st.completionPromptScores,
                                            totalFluencyScore = if (isCompleted) totalScore else st.totalFluencyScore,
                                            completionXpGained = if (isCompleted) totalXpWhenCompleted else st.completionXpGained,
                                            lastAwardedXp = xpDelta
                                        )
                                    }
                                }.onFailure { e ->
                                    _uiState.update { it.copy(error = "Failed to save fluency score: ${'$'}{e.message}") }
                                }
                            } catch (t: Throwable) {
                                _uiState.update { it.copy(error = "Failed to save fluency score: ${'$'}{t.message}") }
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                evaluation = eval,
                                session = sess,
                                latestAnalysis = analysis,
                                pastAttempts = updated.sortedByDescending { a -> a.createdAt },
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

    fun resetRecording() {
        _uiState.update { it.copy(recordingState = RecordingState.IDLE, audioFilePath = null, recordingDuration = 0) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun tickTimer() {
        viewModelScope.launch {
            while (_uiState.value.recordingState == RecordingState.RECORDING) {
                kotlinx.coroutines.delay(1000)
                _uiState.update { it.copy(recordingDuration = it.recordingDuration + 1) }
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
    }

    private fun attemptsFile(context: Context, topicId: String): File {
        val dir = File(context.filesDir, "voicevibe/fluency_attempts").apply { mkdirs() }
        return File(dir, "${topicId}.json")
    }

    private fun loadPastAttempts(context: Context, topicId: String) {
        try {
            val file = attemptsFile(context, topicId)
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

    private fun persistAttempt(context: Context, topicId: String, attempt: FluencyAttempt) {
        try {
            val file = attemptsFile(context, topicId)
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
    val lastAwardedXp: Int = 0
)
 

enum class RecordingState { IDLE, RECORDING, PAUSED, STOPPED }
