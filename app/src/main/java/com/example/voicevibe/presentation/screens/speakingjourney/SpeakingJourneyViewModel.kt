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
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class SpeakingJourneyViewModel @Inject constructor(
    private val repo: SpeakingJourneyRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {
    private val _uiState = mutableStateOf(
        SpeakingJourneyUiState(topics = emptyList())
    )
    val uiState: State<SpeakingJourneyUiState> get() = _uiState

    init {
        reloadTopics(showWelcomeOnLoad = true)
        fetchGamificationProfile()
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
                            phraseProgress = dto.phraseProgress?.let { progress ->
                                PhraseProgress(
                                    currentPhraseIndex = progress.currentPhraseIndex,
                                    completedPhrases = progress.completedPhrases,
                                    totalPhrases = progress.totalPhrases,
                                    isAllPhrasesCompleted = progress.isAllPhrasesCompleted
                                )
                            },
                            unlocked = dto.unlocked,
                            completed = dto.completed
                        )
                    } else emptyList()
                    val newIndex = when {
                        mapped.isEmpty() -> 0
                        !userProfile.firstVisit && userProfile.lastVisitedTopicId != null -> {
                            mapped.indexOfFirst { it.id == userProfile.lastVisitedTopicId }
                                .let { if (it >= 0) it else mapped.indexOfFirst { it.unlocked }.let { idx -> if (idx >= 0) idx else 0 } }
                        }
                        prevSelectedId != null -> mapped.indexOfFirst { it.id == prevSelectedId }
                            .let { if (it >= 0) it else mapped.indexOfFirst { it.unlocked }.let { idx -> if (idx >= 0) idx else 0 } }
                        else -> mapped.indexOfFirst { it.unlocked }.let { if (it >= 0) it else 0 }
                    }
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
                            phraseProgress = PhraseProgress(
                                currentPhraseIndex = 0,
                                completedPhrases = emptyList(),
                                totalPhrases = 4,
                                isAllPhrasesCompleted = false
                            ),
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

    fun selectTopic(index: Int) {
        val s = _uiState.value
        if (index in s.topics.indices && s.topics[index].unlocked) {
            val topic = s.topics[index]
            _uiState.value = s.copy(selectedTopicIdx = index)
            viewModelScope.launch { repo.updateLastVisitedTopic(topic.id) }
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
            val dir = File(context.filesDir, "voicevibe/recordings/${currentTopic.id}").apply { mkdirs() }
            val outFile = File(dir, "phrase_${phraseIndex}.m4a")
            phraseAudioFile = outFile
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            mediaRecorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare(); mr.start()
            _uiState.value = _uiState.value.copy(phraseRecordingState = PhraseRecordingState.RECORDING, phraseSubmissionResult = null)
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
                        val newEntry = PhraseTranscriptEntry(
                            index = phraseIndex,
                            text = dto.transcription,
                            audioPath = audioOutPath,
                            accuracy = dto.accuracy,
                            feedback = dto.feedback,
                            timestamp = System.currentTimeMillis()
                        )
                        val updatedTranscripts = _uiState.value.currentTopicTranscripts.filterNot { it.index == phraseIndex } + newEntry
                        _uiState.value = _uiState.value.copy(
                            phraseRecordingState = PhraseRecordingState.IDLE,
                            phraseSubmissionResult = PhraseSubmissionResultUi(
                                success = dto.success,
                                accuracy = dto.accuracy,
                                transcription = dto.transcription,
                                feedback = dto.feedback,
                                nextPhraseIndex = dto.nextPhraseIndex,
                                topicCompleted = dto.topicCompleted,
                                xpAwarded = dto.xpAwarded
                            ),
                            currentTopicTranscripts = updatedTranscripts.sortedBy { it.index }
                        )
                        recordingTargetIndex = null
                        // Refresh from backend to ensure we display server-side recordings
                        loadTranscriptsForCurrentTopic(context)

                        if (dto.success) {
                            // If topic is completed, we need to check for unlock *after* reload
                            if (dto.topicCompleted) {
                                val previousTopics = _uiState.value.topics
                                val currentTopicIndex = _uiState.value.selectedTopicIdx

                                // Reload topics and then check for unlock
                                reloadTopics(onComplete = {
                                    val newTopics = _uiState.value.topics
                                    val nextTopic = newTopics.getOrNull(currentTopicIndex + 1)
                                    val previousNextTopic = previousTopics.getOrNull(currentTopicIndex + 1)

                                    if (nextTopic != null && nextTopic.unlocked && previousNextTopic?.unlocked == false) {
                                        _uiState.value = _uiState.value.copy(
                                            unlockedTopicInfo = UnlockedTopicInfo(
                                                title = nextTopic.title,
                                                description = nextTopic.description,
                                                xpGained = 100,
                                                topicIndex = currentTopicIndex + 1
                                            )
                                        )
                                        // Also update the XP in the other modal if it's showing
                                        _uiState.value = _uiState.value.copy(
                                            phraseSubmissionResult = _uiState.value.phraseSubmissionResult?.copy(
                                                xpAwarded = dto.xpAwarded + 100
                                            )
                                        )
                                    }
                                })
                            } else {
                                reloadTopics() // Just reload without the special check
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
                            phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "Failed to process recording. ${e.message ?: ""}", null, false)
                        )
                        recordingTargetIndex = null
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error submitting phrase", t)
                _uiState.value = _uiState.value.copy(
                    phraseRecordingState = PhraseRecordingState.IDLE,
                    phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "An error occurred. ${t.message ?: ""}", null, false)
                )
                recordingTargetIndex = null
            }
        }
    }

        fun dismissPhraseResult() { _uiState.value = _uiState.value.copy(phraseSubmissionResult = null) }

        fun dismissUnlockedTopicInfo() {
            val unlockedInfo = _uiState.value.unlockedTopicInfo
            _uiState.value = _uiState.value.copy(unlockedTopicInfo = null)
            if (unlockedInfo != null) {
                selectTopic(unlockedInfo.topicIndex)
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

    fun speakWithBackendTts(
        text: String,
        voiceName: String? = null,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val result = repo.generateTts(text, voiceName)
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
                val level = xp / 500 + 1 // Consistent with mock logic
                val streak = profile.streakDays ?: 0
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

    fun loadTranscriptsForCurrentTopic(context: Context) {
        val s = _uiState.value
        val topic = s.topics.getOrNull(s.selectedTopicIdx)
        if (topic == null) {
            _uiState.value = s.copy(currentTopicTranscripts = emptyList())
            return
        }
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
                        val mapped = latestByPhrase.values.map { rec ->
                            val ts = parseCreatedAtToEpoch(rec.createdAt) ?: 0L
                            PhraseTranscriptEntry(
                                index = rec.phraseIndex,
                                text = rec.transcription,
                                audioPath = rec.audioUrl,
                                accuracy = rec.accuracy ?: 0f,
                                feedback = rec.feedback,
                                timestamp = ts
                            )
                        }.sortedByDescending { it.timestamp }
                        _uiState.value = _uiState.value.copy(currentTopicTranscripts = mapped)
                    },
                    onFailure = { e ->
                        Log.e("SpeakingJourney", "Failed to fetch recordings", e)
                        _uiState.value = _uiState.value.copy(currentTopicTranscripts = emptyList())
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error fetching recordings", t)
                _uiState.value = _uiState.value.copy(currentTopicTranscripts = emptyList())
            }
        }
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
            val dir = File(context.filesDir, "voicevibe/transcripts").apply { mkdirs() }
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
}
