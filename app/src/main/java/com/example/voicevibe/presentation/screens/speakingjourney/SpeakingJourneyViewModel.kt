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
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SpeakingJourneyViewModel @Inject constructor(
    private val repo: SpeakingJourneyRepository
) : ViewModel() {
    private val _uiState = mutableStateOf(
        SpeakingJourneyUiState(topics = emptyList())
    )
    val uiState: State<SpeakingJourneyUiState> get() = _uiState

    init { reloadTopics(showWelcomeOnLoad = true) }

    fun reloadTopics(showWelcomeOnLoad: Boolean = false) {
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
                            conversation = dto.conversation.map { ConversationTurn(it.speaker, it.text) },
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
                            conversation = emptyList(),
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
            val phraseIndex = currentTopic.phraseProgress?.currentPhraseIndex ?: 0
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

        val phraseIndex = currentTopic.phraseProgress?.currentPhraseIndex ?: 0
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
                        val newEntry = PhraseTranscriptEntry(
                            index = phraseIndex,
                            text = dto.transcription,
                            audioPath = file.absolutePath,
                            accuracy = dto.accuracy,
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
                                topicCompleted = dto.topicCompleted
                            ),
                            currentTopicTranscripts = updatedTranscripts.sortedBy { it.index }
                        )
                        saveTranscriptEntryToDisk(context, currentTopic.id, newEntry)
                        if (dto.success) reloadTopics()
                    },
                    onFailure = { e ->
                        Log.e("SpeakingJourney", "Submit phrase failed", e)
                        _uiState.value = _uiState.value.copy(
                            phraseRecordingState = PhraseRecordingState.IDLE,
                            phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "Failed to process recording. ${e.message ?: ""}", null, false)
                        )
                    }
                )
            } catch (t: Throwable) {
                Log.e("SpeakingJourney", "Error submitting phrase", t)
                _uiState.value = _uiState.value.copy(
                    phraseRecordingState = PhraseRecordingState.IDLE,
                    phraseSubmissionResult = PhraseSubmissionResultUi(false, 0f, "", "An error occurred. ${t.message ?: ""}", null, false)
                )
            }
        }
    }

    fun dismissPhraseResult() { _uiState.value = _uiState.value.copy(phraseSubmissionResult = null) }

    fun playUserRecording(path: String) {
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        try {
            val mp = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    try { it.release() } catch (_: Throwable) {}
                    if (mediaPlayer === it) mediaPlayer = null
                }
                prepare()
                start()
            }
            mediaPlayer = mp
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to play recording", t)
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
        try {
            val s = _uiState.value
            val topic = s.topics.getOrNull(s.selectedTopicIdx) ?: run {
                _uiState.value = s.copy(currentTopicTranscripts = emptyList())
                return
            }
            val dir = File(context.filesDir, "voicevibe/transcripts")
            val file = File(dir, "${topic.id}.json")
            if (!file.exists()) {
                _uiState.value = s.copy(currentTopicTranscripts = emptyList())
                return
            }
            val root = JSONObject(file.readText())
            val entries = root.optJSONObject("entries") ?: JSONObject()
            val list = mutableListOf<PhraseTranscriptEntry>()
            val keys = entries.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = entries.getJSONObject(k)
                val idx = o.optInt("index")
                val text = o.optString("text")
                val path = o.optString("audioPath")
                val acc = o.optDouble("accuracy", 0.0).toFloat()
                val ts = o.optLong("timestamp", 0L)
                if (File(path).exists()) {
                    list.add(PhraseTranscriptEntry(idx, text, path, acc, ts))
                }
            }
            _uiState.value = s.copy(currentTopicTranscripts = list.sortedBy { it.index })
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to load transcripts", t)
        }
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
                put("timestamp", entry.timestamp)
            }
            entries.put(entry.index.toString(), obj)
            file.writeText(root.toString())
        } catch (t: Throwable) {
            Log.e("SpeakingJourney", "Failed to persist transcript", t)
        }
    }
}
