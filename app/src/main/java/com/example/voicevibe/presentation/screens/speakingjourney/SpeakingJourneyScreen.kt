package com.example.voicevibe.presentation.screens.speakingjourney

import android.speech.tts.TextToSpeech
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
import java.util.Locale
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import android.Manifest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.voicevibe.presentation.screens.practice.speaking.SpeakingPracticeViewModel
import com.example.voicevibe.presentation.screens.practice.speaking.RecordingState
import java.io.File
import android.os.Build
import android.media.MediaRecorder
import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

enum class Stage { MATERIAL, PRACTICE }

data class ConversationTurn(
    val speaker: String,
    val text: String
)

data class PhraseProgress(
    val currentPhraseIndex: Int,
    val completedPhrases: List<Int>,
    val totalPhrases: Int,
    val isAllPhrasesCompleted: Boolean
)

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val material: List<String>,
    val conversation: List<ConversationTurn>,
    val phraseProgress: PhraseProgress?,
    val unlocked: Boolean,
    val completed: Boolean
)

data class UserProfile(
    val firstVisit: Boolean,
    val lastVisitedTopicId: String?,
    val lastVisitedTopicTitle: String?
)

data class SpeakingJourneyUiState(
    val topics: List<Topic>,
    val userProfile: UserProfile? = null,
    val selectedTopicIdx: Int = 0,
    val stage: Stage = Stage.MATERIAL,
    val showWelcome: Boolean = false,
    val phraseRecordingState: PhraseRecordingState = PhraseRecordingState.IDLE,
    val phraseSubmissionResult: PhraseSubmissionResultUi? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class PhraseRecordingState { IDLE, RECORDING, PROCESSING }

data class PhraseSubmissionResultUi(
    val success: Boolean,
    val accuracy: Float,
    val transcription: String,
    val feedback: String,
    val nextPhraseIndex: Int?,
    val topicCompleted: Boolean
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SpeakingJourneyViewModel @javax.inject.Inject constructor(
    private val repo: SpeakingJourneyRepository
) : androidx.lifecycle.ViewModel() {
    private val _uiState = androidx.compose.runtime.mutableStateOf(
        SpeakingJourneyUiState(topics = emptyList())
    )
    val uiState: androidx.compose.runtime.State<SpeakingJourneyUiState> get() = _uiState

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
                        // For returning users, try to select their last visited topic
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
                    // Keep minimal fallback to keep UI usable offline
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
            // Update last visited topic
            viewModelScope.launch {
                repo.updateLastVisitedTopic(topic.id)
            }
        }
    }

    fun dismissWelcome() {
        _uiState.value = _uiState.value.copy(showWelcome = false)
    }

    fun setStage(stage: Stage) {
        _uiState.value = _uiState.value.copy(stage = stage)
    }

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
    private var phraseAudioFile: File? = null

    fun startPhraseRecording(context: Context) {
        try {
            val outFile = File(context.cacheDir, "phrase_rec_${System.currentTimeMillis()}.m4a")
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

    fun stopPhraseRecording() {
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
                        _uiState.value = _uiState.value.copy(
                            phraseRecordingState = PhraseRecordingState.IDLE,
                            phraseSubmissionResult = PhraseSubmissionResultUi(
                                success = dto.success,
                                accuracy = dto.accuracy,
                                transcription = dto.transcription,
                                feedback = dto.feedback,
                                nextPhraseIndex = dto.nextPhraseIndex,
                                topicCompleted = dto.topicCompleted
                            )
                        )
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

    override fun onCleared() {
        super.onCleared()
        try { mediaRecorder?.release() } catch (_: Throwable) {}
        mediaRecorder = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakingJourneyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Init TextToSpeech (Android TTS as a fallback; can be replaced by ElevenLabs streaming)
    val tts = remember(context) {
        var ref: TextToSpeech? = null
        val created = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Default to US English; can be adjusted per user locale
                try {
                    ref?.language = Locale.US
                } catch (_: Throwable) { /* ignore */ }
            }
        }
        ref = created
        created
    }
    androidx.compose.runtime.DisposableEffect(tts) {
        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Throwable) { /* ignore */ }
        }
    }

    fun speak(text: String) {
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.currentTimeMillis()}")
        } catch (_: Throwable) { /* ignore for now */ }
    }

    // Observe preferred voice id from Settings (to be used when ElevenLabs is wired)
    val settingsVM: SettingsViewModel = hiltViewModel()
    val preferredVoiceId = settingsVM.ttsVoiceId.value

    // ViewModel state
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text("Speaking Quest", fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            }
        ) { innerPadding: PaddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val headerTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
                GamifiedHeader(currentTopic = headerTopic, phraseProgress = headerTopic?.phraseProgress)
                Spacer(modifier = Modifier.height(4.dp))

                // Gamified topic selector
                TopicQuestRow(
                    topics = ui.topics,
                    selectedIndex = ui.selectedTopicIdx,
                    onSelect = { idx -> if (ui.topics.getOrNull(idx)?.unlocked == true) viewModel.selectTopic(idx) }
                )

                // Stage tabs removed: Practice with AI moved to its own screen.

                if (ui.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
                ui.error?.let { err ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { viewModel.reloadTopics() }) { Text("Retry") }
                    }
                }

                // Show welcome screen if needed
                if (ui.showWelcome) {
                    WelcomeScreen(
                        userProfile = ui.userProfile,
                        currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx),
                        onDismiss = { viewModel.dismissWelcome() }
                    )
                    return@Column
                }

                // Content (Material only)
                val currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
                MaterialStage(
                    description = currentTopic?.description.orEmpty(),
                    material = currentTopic?.material ?: emptyList(),
                    phraseProgress = currentTopic?.phraseProgress,
                    conversation = currentTopic?.conversation ?: emptyList(),
                    onSpeak = ::speak,
                    recordingState = ui.phraseRecordingState,
                    submissionResult = ui.phraseSubmissionResult,
                    onStartRecording = { viewModel.startPhraseRecording(context) },
                    onStopRecording = viewModel::stopPhraseRecording,
                    onDismissResult = viewModel::dismissPhraseResult
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Complete button
                val current = ui.topics.getOrNull(ui.selectedTopicIdx)
            }
        }
    }
}

@Composable
private fun GamifiedHeader(
    currentTopic: Topic?,
    phraseProgress: PhraseProgress?
) {
    val completed = phraseProgress?.completedPhrases?.size ?: 0
    val total = phraseProgress?.totalPhrases ?: (currentTopic?.material?.size ?: 0)
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Your Speaking Quest",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                val title = currentTopic?.title ?: "Choose a lesson to begin"
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "$completed/$total phrases",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun MaterialStage(
    description: String,
    material: List<String>,
    phraseProgress: PhraseProgress?,
    conversation: List<ConversationTurn>,
    onSpeak: (String) -> Unit,
    recordingState: PhraseRecordingState,
    submissionResult: PhraseSubmissionResultUi?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDismissResult: () -> Unit
) {
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val askedOnce = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Topic description removed intentionally to keep focus on core actions.
        // Interactive Phrase Learning section
        if (material.isNotEmpty() && phraseProgress != null) {
            val permStatus = audioPermissionState.status
            val deniedPermanently = askedOnce.value && (permStatus is PermissionStatus.Denied) && !permStatus.shouldShowRationale
            // Local review state: when set, the Current Phrase card shows the reviewed phrase
            val reviewPhraseIndex = remember(phraseProgress) { mutableStateOf<Int?>(null) }

            if (!audioPermissionState.status.isGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Microphone permission required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable microphone to record your pronunciation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                askedOnce.value = true
                                audioPermissionState.launchPermissionRequest()
                            }) {
                                Text("Grant permission")
                            }
                            if (deniedPermanently) {
                                OutlinedButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Text("Open settings")
                                }
                            }
                        }
                    }
                }
            } else {
                InteractivePhraseSection(
                    material = material,
                    phraseProgress = phraseProgress,
                    onSpeak = onSpeak,
                    onPhraseSelected = { idx ->
                        reviewPhraseIndex.value = idx
                        material.getOrNull(idx)?.let { onSpeak(it) }
                    },
                    recordingState = recordingState,
                    submissionResult = submissionResult,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onDismissResult = onDismissResult,
                    reviewPhraseIndex = reviewPhraseIndex.value,
                    onClearReview = { reviewPhraseIndex.value = null }
                )
            }
        }

        // Conversation section
        if (conversation.isNotEmpty()) {
            val combined = conversation.joinToString(separator = "\n") { "${it.speaker}: ${it.text}" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversation Example",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onSpeak(combined) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Play conversation")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = combined,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractivePhraseSection(
    material: List<String>,
    phraseProgress: PhraseProgress,
    onSpeak: (String) -> Unit,
    onPhraseSelected: (Int) -> Unit,
    recordingState: PhraseRecordingState,
    submissionResult: PhraseSubmissionResultUi?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDismissResult: () -> Unit,
    reviewPhraseIndex: Int?,
    onClearReview: () -> Unit
) {
    // Show last submission result
    submissionResult?.let { result ->
        RecordingResultCard(result = result, onDismiss = onDismissResult)
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Navigation pills for completed phrases
    if (phraseProgress.completedPhrases.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(phraseProgress.completedPhrases.sorted()) { phraseIndex ->
                AssistChip(
                    onClick = { onPhraseSelected(phraseIndex) },
                    label = { Text("${phraseIndex + 1}") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
    
    // Current phrase learning (supports review mode)
    val baseIndex = phraseProgress.currentPhraseIndex.coerceIn(0, material.size - 1)
    val effectiveIndex = reviewPhraseIndex?.coerceIn(0, material.size - 1) ?: baseIndex
    val isReviewMode = reviewPhraseIndex != null
    val currentPhrase = material.getOrNull(effectiveIndex)
    
    if (currentPhrase != null) {
        Text(
            text = if (isReviewMode) "Review Phrase ${effectiveIndex + 1} of ${material.size}:" else "Current Phrase ${effectiveIndex + 1} of ${material.size}:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        // Current phrase card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentPhrase,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = { onSpeak(currentPhrase) }) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Listen to phrase",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isReviewMode) {
                    // Review mode hint + exit
                    Text(
                        text = "You are reviewing a completed phrase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onClearReview, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to current phrase")
                    }
                } else {
                    // Instructions
                    Text(
                        text = "📝 Record yourself saying this phrase aloud",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Record button
                    RecordingButton(
                        recordingState = recordingState,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording
                    )
                }
            }
        }
    } else if (phraseProgress.isAllPhrasesCompleted) {
        // All phrases completed
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎉 All phrases completed!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Great job! You can review completed phrases above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecordingButton(
    recordingState: PhraseRecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val enabled = recordingState != PhraseRecordingState.PROCESSING
    val scaleFactor = if (recordingState == PhraseRecordingState.RECORDING) {
        val infinite = rememberInfiniteTransition()
        val p by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse)
        )
        p
    } else 1f
    Button(
        onClick = {
            when (recordingState) {
                PhraseRecordingState.IDLE -> onStartRecording()
                PhraseRecordingState.RECORDING -> onStopRecording()
                PhraseRecordingState.PROCESSING -> Unit
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleFactor),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (recordingState) {
                PhraseRecordingState.RECORDING -> MaterialTheme.colorScheme.error
                PhraseRecordingState.PROCESSING -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        when (recordingState) {
            PhraseRecordingState.PROCESSING -> {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            PhraseRecordingState.RECORDING -> {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            else -> {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            when (recordingState) {
                PhraseRecordingState.RECORDING -> "Stop Recording"
                PhraseRecordingState.PROCESSING -> "Processing..."
                else -> "Record Pronunciation"
            }
        )
    }
}

@Composable
private fun RecordingResultCard(
    result: PhraseSubmissionResultUi,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                val onColor = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (result.success) Icons.Outlined.EmojiEvents else Icons.Default.Mic,
                        contentDescription = null,
                        tint = onColor
                    )
                    Text(
                        text = if (result.success) "✅ Great job!" else "❌ Try again",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onColor
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = onColor
                    )
                }
            }
            if (result.accuracy > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (result.accuracy / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Accuracy: ${"%.1f".format(Locale.US, result.accuracy)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (result.transcription.isNotBlank()) {
                Text(
                    text = "You said: \"${result.transcription}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (result.feedback.isNotBlank()) {
                Text(
                    text = result.feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    userProfile: UserProfile?,
    currentTopic: Topic?,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 4 seconds, but user can tap to skip
    LaunchedEffect(Unit) {
        delay(4000)
        onDismiss()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clickable { onDismiss() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userProfile?.firstVisit == true) {
            // First-time user welcome
            Text(
                text = "Welcome to Speaking Journey",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            currentTopic?.let { topic ->
                Text(
                    text = "Lesson ${topic.title.substringBefore(':').takeIf { it.contains("Lesson") } ?: "1"}: ${topic.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "In this lesson, you will learn ${topic.description.lowercase()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Returning user welcome
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!userProfile?.lastVisitedTopicTitle.isNullOrBlank()) {
                Text(
                    text = "Last time you were learning about:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userProfile?.lastVisitedTopicTitle ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Want to continue?",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "Ready to continue your journey?",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Tap anywhere to continue",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TopicQuestRow(
    topics: List<Topic>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    if (topics.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        items(topics.size) { index ->
            val topic = topics[index]
            val selected = index == selectedIndex
            val isLocked = !topic.unlocked
            val isCompleted = topic.completed
            val scale by animateFloatAsState(targetValue = if (selected) 1.05f else 1f, animationSpec = tween(250))

            val gradient = when {
                isCompleted -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                isLocked -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface))
                else -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))
            }

            Card(
                modifier = Modifier
                    .width(180.dp)
                    .height(84.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = topic.unlocked) { onSelect(index) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient)
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            } else if (isLocked) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                        }
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 2
                        )
                        val phrases = topic.phraseProgress?.totalPhrases ?: topic.material.size
                        val completed = topic.phraseProgress?.completedPhrases?.size ?: 0
                        if (phrases > 0) {
                            LinearProgressIndicator(
                                progress = completed.toFloat() / phrases,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
