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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    init {
        viewModelScope.launch {
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
                    _uiState.value = _uiState.value.copy(topics = mapped, isLoading = false)
                },
                onFailure = { e ->
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
                        error = e.message ?: "Failed to load topics"
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
            if (res.isSuccess) {
                val updated = s.topics.toMutableList()
                updated[s.selectedTopicIdx] = current.copy(completed = true)
                if (s.selectedTopicIdx + 1 < updated.size) {
                    val next = updated[s.selectedTopicIdx + 1]
                    if (!next.unlocked) updated[s.selectedTopicIdx + 1] = next.copy(unlocked = true)
                }
                _uiState.value = s.copy(topics = updated)
            }
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
    DisposableEffect(tts) {
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
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Content
            val currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
            when (ui.stage) {
                Stage.MATERIAL -> MaterialStage(
                    lines = currentTopic?.material ?: emptyList(),
                    onSpeak = ::speak
                )
                Stage.PRACTICE -> PracticeStagePlaceholder()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Complete button
            val current = ui.topics.getOrNull(ui.selectedTopicIdx)
            Button(
                enabled = current != null && !current.completed,
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

@Composable
private fun PracticeStagePlaceholder() {
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
                text = "Practice with AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coming soon: speak and get instant feedback from AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* TODO: integrate practice session flow */ }, enabled = false) {
                Text("Start AI Practice")
            }
        }
    }
}
