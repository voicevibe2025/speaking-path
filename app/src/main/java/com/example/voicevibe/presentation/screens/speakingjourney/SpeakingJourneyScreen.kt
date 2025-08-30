package com.example.voicevibe.presentation.screens.speakingjourney

// removed unused import: androidx.compose.foundation.background
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
// removed unused import: androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
// removed unused import: androidx.compose.runtime.DisposableEffect
// removed unused import: androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
// removed unused import: androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// removed unused import: androidx.compose.runtime.rememberCoroutineScope
// removed unused import: androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
import java.util.Locale
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import android.Manifest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.voicevibe.presentation.screens.practice.speaking.SpeakingPracticeViewModel
import com.example.voicevibe.presentation.screens.practice.speaking.RecordingState
import java.io.File

enum class Stage { MATERIAL, PRACTICE }

data class Topic(
    val id: String,
    val title: String,
    val material: List<String>,
    val unlocked: Boolean,
    val completed: Boolean
)

data class SpeakingJourneyUiState(
    val topics: List<Topic>,
    val selectedTopicIdx: Int = 0,
    val stage: Stage = Stage.MATERIAL,
    val isLoading: Boolean = false,
    val error: String? = null
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SpeakingJourneyViewModel @javax.inject.Inject constructor(
    private val repo: SpeakingJourneyRepository
) : androidx.lifecycle.ViewModel() {
    private val _uiState = androidx.compose.runtime.mutableStateOf(
        SpeakingJourneyUiState(topics = emptyList())
    )
    val uiState: androidx.compose.runtime.State<SpeakingJourneyUiState> get() = _uiState

    init { reloadTopics() }

    fun reloadTopics() {
        viewModelScope.launch {
            val prevSelectedId = _uiState.value.topics.getOrNull(_uiState.value.selectedTopicIdx)?.id
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repo.getTopics()
            result.fold(
                onSuccess = { dtos ->
                    val mapped = if (dtos.isNotEmpty()) dtos.map { dto ->
                        Topic(
                            id = dto.id,
                            title = dto.title,
                            material = dto.material,
                            unlocked = dto.unlocked,
                            completed = dto.completed
                        )
                    } else emptyList()
                    val newIndex = when {
                        mapped.isEmpty() -> 0
                        prevSelectedId != null -> mapped.indexOfFirst { it.id == prevSelectedId }
                            .let { if (it >= 0) it else mapped.indexOfFirst { it.unlocked }.let { idx -> if (idx >= 0) idx else 0 } }
                        else -> mapped.indexOfFirst { it.unlocked }.let { if (it >= 0) it else 0 }
                    }
                    _uiState.value = _uiState.value.copy(
                        topics = mapped,
                        selectedTopicIdx = newIndex.coerceIn(0, (mapped.size - 1).coerceAtLeast(0)),
                        isLoading = false
                    )
                },
                onFailure = {
                    // Keep minimal fallback to keep UI usable offline
                    val fallback = listOf(
                        Topic(
                            id = "t1",
                            title = "Self Introduction",
                            material = listOf(
                                "Hello! My name is Alex.",
                                "I am from San Francisco.",
                                "I work as a software developer.",
                                "Nice to meet you!"
                            ),
                            unlocked = true,
                            completed = false
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        topics = fallback,
                        isLoading = false,
                        error = "Unable to load topics. Check your connection or try again."
                    )
                }
            )
        }
    }

    fun selectTopic(index: Int) {
        val s = _uiState.value
        if (index in s.topics.indices && s.topics[index].unlocked) {
            _uiState.value = s.copy(selectedTopicIdx = index)
        }
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
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${'$'}{System.currentTimeMillis()}")
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
                    text = "TTS voice: ${'$'}preferredVoiceId",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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

            // Content
            val currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
            when (ui.stage) {
                Stage.MATERIAL -> MaterialStage(
                    lines = currentTopic?.material ?: emptyList(),
                    onSpeak = ::speak
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

@Composable
private fun MaterialStage(
    lines: List<String>,
    onSpeak: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        lines.forEach { sentence ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onSpeak(sentence) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Play TTS")
                    }
                }
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
                    Button(onClick = { audioPermissionState.launchPermissionRequest() }) {
                        Text("Grant permission")
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Practice Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = prompt.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Recording status
        if (uiState.recordingState != RecordingState.IDLE) {
            Text(
                text = "Duration: ${'$'}{uiState.recordingDuration}s",
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
                        val f = File(context.cacheDir, "journey_rec_${'$'}{System.currentTimeMillis()}.m4a")
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
