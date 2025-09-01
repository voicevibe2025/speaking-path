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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speaking Journey", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
            // Voice info
            if (!preferredVoiceId.isNullOrBlank()) {
                Text(
                    text = "TTS voice: $preferredVoiceId",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "TTS voice: Default",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Topics chips (horizontal)
            val topicScrollState = rememberScrollState()
            
            // Auto-scroll to selected topic
            LaunchedEffect(ui.selectedTopicIdx) {
                if (ui.topics.isNotEmpty() && ui.selectedTopicIdx in ui.topics.indices) {
                    // Calculate approximate position - each chip is roughly 180dp + 8dp spacing
                    val chipWidth = 188 // 180dp + 8dp spacing
                    val targetPosition = ui.selectedTopicIdx * chipWidth
                    topicScrollState.animateScrollTo(targetPosition)
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(topicScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.topics.forEachIndexed { index, topic ->
                    val selected = index == ui.selectedTopicIdx
                    AssistChip(
                        onClick = { if (topic.unlocked) viewModel.selectTopic(index) },
                        enabled = topic.unlocked,
                        label = { Text(topic.title) },
                        leadingIcon = {
                            when {
                                topic.completed -> Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                !topic.unlocked -> Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Stage tabs
            val tabIndex = if (ui.stage == Stage.MATERIAL) 0 else 1
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { viewModel.setStage(Stage.MATERIAL) },
                    text = { Text("Material") }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { viewModel.setStage(Stage.PRACTICE) },
                    text = { Text("Practice with AI") }
                )
            }

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

            // Content
            val currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
            when (ui.stage) {
                Stage.MATERIAL -> MaterialStage(
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
                Stage.PRACTICE -> PracticeStageInline()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Complete button
            val current = ui.topics.getOrNull(ui.selectedTopicIdx)
            Button(
                enabled = current != null && !current.completed && !ui.isLoading,
                onClick = { if (current != null) viewModel.markCurrentTopicComplete() }
            ) {
                Text(if (current?.completed == true) "Completed" else "Mark topic complete")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
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
        // Description section
        if (description.isNotBlank()) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

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
    Text(
        text = "Learn Phrases",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    
    // Progress indicator
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Progress: ${phraseProgress.completedPhrases.size} of ${phraseProgress.totalPhrases} completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = phraseProgress.completedPhrases.size.toFloat() / phraseProgress.totalPhrases.coerceAtLeast(1),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    // Show last submission result
    submissionResult?.let { result ->
        RecordingResultCard(result = result, onDismiss = onDismissResult)
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Navigation pills for completed phrases
    if (phraseProgress.completedPhrases.isNotEmpty()) {
        Text(
            text = "Completed Phrases (tap to review):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
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
    Button(
        onClick = {
            when (recordingState) {
                PhraseRecordingState.IDLE -> onStartRecording()
                PhraseRecordingState.RECORDING -> onStopRecording()
                PhraseRecordingState.PROCESSING -> Unit
            }
        },
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = if (result.success) "✅ Great job!" else "❌ Try again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            if (result.accuracy > 0) {
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PracticeStageInline(
    practiceViewModel: SpeakingPracticeViewModel = hiltViewModel()
) {
    val uiState by practiceViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val askedOnce = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                        text = "Enable microphone to record your practice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val permStatus = audioPermissionState.status
                    val deniedPermanently = askedOnce.value && (permStatus is PermissionStatus.Denied) && !permStatus.shouldShowRationale
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
            return@Column
        }

        // Prompt
        uiState.currentPrompt?.let { prompt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Practice Prompt", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = prompt.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Recording status
        if (uiState.recordingState != RecordingState.IDLE) {
            Text(
                text = "Duration: ${uiState.recordingDuration}s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (uiState.recordingState) {
                RecordingState.IDLE -> {
                    Button(onClick = {
                        val f = File(context.cacheDir, "journey_rec_${System.currentTimeMillis()}.m4a")
                        practiceViewModel.startRecording(f)
                    }) { Text("Start recording") }
                }
                RecordingState.RECORDING -> {
                    OutlinedButton(onClick = practiceViewModel::pauseRecording) { Text("Pause") }
                    Button(onClick = practiceViewModel::stopRecording) { Text("Stop") }
                }
                RecordingState.PAUSED -> {
                    Button(onClick = practiceViewModel::resumeRecording) { Text("Resume") }
                    Button(onClick = practiceViewModel::stopRecording) { Text("Stop") }
                }
                RecordingState.STOPPED -> {
                    // Post-stop actions shown below
                }
            }
        }

        if (uiState.recordingState == RecordingState.STOPPED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = practiceViewModel::retryRecording, modifier = Modifier.weight(1f)) {
                    Text("Retry")
                }
                Button(
                    onClick = practiceViewModel::submitRecording,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit")
                    }
                }
            }
        }

        // Error
        uiState.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Basic feedback summary
        uiState.submissionResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("AI Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Score: ${String.format(java.util.Locale.US, "%.1f", result.score)}")
                    Text(result.feedback)
                }
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
