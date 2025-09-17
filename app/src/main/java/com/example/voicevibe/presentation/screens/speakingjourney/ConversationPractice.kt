package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.example.voicevibe.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }

    // Conversation source and role selection state
    val conversation = topic?.conversation ?: emptyList()
    var selectedRole by remember(conversation) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedRole) {
        selectedRole?.let { viewModel.setConversationRole(it) }
    }

    // Playback state
    var currentIndex by remember(conversation) { mutableStateOf(0) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlayingAll by remember { mutableStateOf(false) }

    // Voice selections
    val maleVoiceName = "Puck"
    val femaleVoiceName = "Zephyr"

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "convPracticeBG")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "convPracticeOffset"
    )

    fun resetPlaybackFlags() {
        isPlayingAll = false
        currentlyPlayingId = null
    }

    fun playTurn(index: Int) {
        if (conversation.isEmpty()) return
        val i = index.coerceIn(0, conversation.lastIndex)
        currentIndex = i
        val turn = conversation[i]
        val id = turn.text
        val voice = if (turn.speaker.equals("A", ignoreCase = true)) maleVoiceName else femaleVoiceName
        isPlayingAll = false
        viewModel.markSpeakingActivity()
        viewModel.speakWithBackendTts(
            text = turn.text,
            voiceName = voice,
            onStart = { currentlyPlayingId = id },
            onDone = { currentlyPlayingId = null },
            onError = { _ -> resetPlaybackFlags() }
        )
    }

    fun playAllFrom(start: Int = 0) {
        if (conversation.isEmpty()) return
        if (isPlayingAll || currentlyPlayingId != null) return
        val startIdx = start.coerceIn(0, conversation.lastIndex)
        isPlayingAll = true
        currentIndex = startIdx

        fun playNext(i: Int) {
            if (!isPlayingAll) return
            if (i >= conversation.size) {
                resetPlaybackFlags()
                return
            }
            val t = conversation[i]
            val id = t.text
            currentIndex = i
            val voice = if (t.speaker.equals("A", ignoreCase = true)) maleVoiceName else femaleVoiceName
            viewModel.markSpeakingActivity()
            viewModel.speakWithBackendTts(
                text = t.text,
                voiceName = voice,
                onStart = { currentlyPlayingId = id },
                onDone = { playNext(i + 1) },
                onError = { _ -> resetPlaybackFlags() }
            )
        }
        playNext(startIdx)
    }

    DisposableEffect(Unit) {
        onDispose {
            resetPlaybackFlags()
            viewModel.stopPlayback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = topic?.title ?: "Conversation Practice",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        resetPlaybackFlags()
                        viewModel.stopPlayback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawAnimatedGradient(gradientOffset) }
                .padding(innerPadding)
        ) {
            when {
                topic == null && ui.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    }
                }
                topic == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Conversation not available", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                conversation.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No conversation example for this topic", color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    val current = conversation.getOrNull(currentIndex)
                    val isSpeakerA = current?.speaker.equals("A", ignoreCase = true)
                    val playing = currentlyPlayingId == current?.text || isPlayingAll

                    // Auto-play the other turn (non-user role) using TTS
                    LaunchedEffect(currentIndex, selectedRole, ui.conversationRecordingState) {
                        val role = selectedRole
                        val turn = conversation.getOrNull(currentIndex)
                        if (role != null && turn != null) {
                            val isUserTurn = turn.speaker.equals(role, ignoreCase = true)
                            val canAutoPlayOther = !isUserTurn && currentlyPlayingId == null && !isPlayingAll &&
                                ui.conversationRecordingState != PhraseRecordingState.RECORDING &&
                                ui.conversationRecordingState != PhraseRecordingState.PROCESSING
                            if (canAutoPlayOther) {
                                val id = turn.text
                                val voice = if (turn.speaker.equals("A", ignoreCase = true)) maleVoiceName else femaleVoiceName
                                viewModel.speakWithBackendTts(
                                    text = turn.text,
                                    voiceName = voice,
                                    onStart = { currentlyPlayingId = id },
                                    onDone = {
                                        currentlyPlayingId = null
                                        val next = (currentIndex + 1).coerceAtMost(conversation.lastIndex)
                                        if (next != currentIndex) currentIndex = next
                                    },
                                    onError = { _ -> resetPlaybackFlags() }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress indicator
                        ConversationProgress(
                            currentIndex = currentIndex,
                            totalSteps = conversation.size,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        // Speakers section with modern avatars
                        ModernSpeakersSection(
                            isSpeakerA = isSpeakerA,
                            isPlaying = playing,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )

                        // Role selector (choose A or B to speak as)
                        ModernRoleSelector(
                            selectedRole = selectedRole,
                            onSelect = { role -> selectedRole = role },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Modern speech bubble
                        current?.let { turn ->
                            ModernSpeechBubble(
                                text = turn.text,
                                isSpeakerA = isSpeakerA,
                                isPlaying = playing,
                                primaryGradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFFF093FB)),
                                secondaryGradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF43E97B))
                            )
                        }

                        // Modern control panel
                        ModernControlPanel(
                            currentIndex = currentIndex,
                            conversationSize = conversation.size,
                            isPlaying = playing,
                            onPrevious = {
                                val newIdx = (currentIndex - 1).coerceAtLeast(0)
                                playTurn(newIdx)
                            },
                            onPlay = { playTurn(currentIndex) },
                            onPlayAll = { playAllFrom(0) },
                            onStop = {
                                resetPlaybackFlags()
                                viewModel.stopPlayback()
                            },
                            onNext = {
                                val newIdx = (currentIndex + 1).coerceAtMost(conversation.lastIndex)
                                playTurn(newIdx)
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Recording panel
                        val canRecord = selectedRole != null && (
                            (isSpeakerA && selectedRole!!.equals("A", ignoreCase = true)) ||
                                (!isSpeakerA && selectedRole!!.equals("B", ignoreCase = true))
                            )
                        ModernRecordPanel(
                            recordingState = ui.conversationRecordingState,
                            enabled = canRecord,
                            onStart = { viewModel.startConversationRecording(context, currentIndex) },
                            onStop = { viewModel.stopConversationRecording(context) },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }
            }

            // Result dialog
            ui.conversationSubmissionResult?.let { result ->
                ConversationResultDialog(
                    result = result,
                    onDismiss = { viewModel.dismissConversationResult() },
                    onGoNext = {
                        val next = result.nextTurnIndex
                        if (next != null) {
                            // Move to next turn
                            currentIndex = next
                        }
                        viewModel.dismissConversationResult()
                    }
                )
            }

            // Completion dialog
            if (ui.showConversationCongrats) {
                ConversationCompletionDialog(
                    onDismiss = { viewModel.dismissConversationCongrats() }
                )
            }
        }
    }
}

// ----- Practice-specific helper composables -----

@Composable
fun ModernRoleSelector(
    selectedRole: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoleChip(
            label = "Speak as A (Alex)",
            selected = selectedRole?.equals("A", ignoreCase = true) == true,
            onClick = { onSelect("A") },
            leadingColor = Color(0xFF667EEA)
        )
        RoleChip(
            label = "Speak as B (Sarah)",
            selected = selectedRole?.equals("B", ignoreCase = true) == true,
            onClick = { onSelect("B") },
            leadingColor = Color(0xFF4FACFE)
        )
    }
}

@Composable
private fun RoleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingColor: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (selected) leadingColor else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(leadingColor, CircleShape)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernRecordPanel(
    recordingState: PhraseRecordingState,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = recordingState == PhraseRecordingState.RECORDING
    val isProcessing = recordingState == PhraseRecordingState.PROCESSING
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Record your line",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                val hint = if (!enabled) "Select the matching role for this line" else if (isProcessing) "Please wait..." else "Tap to ${if (isRecording) "stop" else "start"}"
                Text(
                    text = hint,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                onClick = {
                    if (!enabled || isProcessing) return@Surface
                    if (isRecording) onStop() else onStart()
                },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = if (!enabled || isProcessing) Color.White.copy(alpha = 0.05f) else Color(0xFFE53935)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (isRecording) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationResultDialog(
    result: ConversationSubmissionResultUi,
    onDismiss: () -> Unit,
    onGoNext: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                TextButton(onClick = onGoNext) { Text(if (result.nextTurnIndex != null) "Next" else "OK") }
            }
        },
        title = { Text("Submission Result") },
        text = {
            Column {
                Text("Accuracy: ${"%.1f".format(result.accuracy)}%")
                if ((result.xpAwarded ?: 0) > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("+${result.xpAwarded} XP", color = Color(0xFF06FFA5), fontWeight = FontWeight.SemiBold)
                }
                if (result.feedback?.isNotBlank() == true) {
                    Spacer(Modifier.height(6.dp))
                    Text(result.feedback!!)
                }
                if (result.transcription.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Heard: \"${result.transcription}\"")
                }
            }
        }
    )
}

@Composable
fun ConversationCompletionDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Awesome!") }
        },
        title = { Text("Conversation Complete 🎉") },
        text = {
            Text("Great job! You've completed all turns for this conversation.")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopicConversationScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }

    val conversation = topic?.conversation ?: emptyList()

    var currentIndex by remember(conversation) { mutableStateOf(0) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlayingAll by remember { mutableStateOf(false) }

    val maleVoiceName = "Puck"
    val femaleVoiceName = "Zephyr"

    val infiniteTransition = rememberInfiniteTransition()
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    fun resetPlaybackFlags() {
        isPlayingAll = false
        currentlyPlayingId = null
    }

    fun playTurn(index: Int) {
        if (conversation.isEmpty()) return
        val i = index.coerceIn(0, conversation.lastIndex)
        currentIndex = i
        val turn = conversation[i]
        val id = turn.text
        val voice = if (turn.speaker.equals("A", ignoreCase = true)) maleVoiceName else femaleVoiceName
        isPlayingAll = false
        viewModel.markSpeakingActivity()
        viewModel.speakWithBackendTts(
            text = turn.text,
            voiceName = voice,
            onStart = { currentlyPlayingId = id },
            onDone = { currentlyPlayingId = null },
            onError = { _ -> resetPlaybackFlags() }
        )
    }

    fun playAllFrom(start: Int = 0) {
        if (conversation.isEmpty()) return
        if (isPlayingAll || currentlyPlayingId != null) return
        val startIdx = start.coerceIn(0, conversation.lastIndex)
        isPlayingAll = true
        currentIndex = startIdx

        fun playNext(i: Int) {
            if (!isPlayingAll) return
            if (i >= conversation.size) {
                resetPlaybackFlags()
                return
            }
            val t = conversation[i]
            val id = t.text
            currentIndex = i
            val voice = if (t.speaker.equals("A", ignoreCase = true)) maleVoiceName else femaleVoiceName
            viewModel.markSpeakingActivity()
            viewModel.speakWithBackendTts(
                text = t.text,
                voiceName = voice,
                onStart = { currentlyPlayingId = id },
                onDone = { playNext(i + 1) },
                onError = { _ -> resetPlaybackFlags() }
            )
        }
        playNext(startIdx)
    }

    DisposableEffect(Unit) {
        onDispose {
            resetPlaybackFlags()
            viewModel.stopPlayback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = topic?.title ?: "Conversation",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        resetPlaybackFlags()
                        viewModel.stopPlayback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawAnimatedGradient(gradientOffset) }
                .padding(innerPadding)
        ) {
            when {
                topic == null && ui.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    }
                }
                topic == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Conversation not available", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                conversation.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No conversation example for this topic", color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    val current = conversation.getOrNull(currentIndex)
                    val isSpeakerA = current?.speaker.equals("A", ignoreCase = true)
                    val playing = currentlyPlayingId == current?.text || isPlayingAll

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ConversationProgress(
                            currentIndex = currentIndex,
                            totalSteps = conversation.size,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        ModernSpeakersSection(
                            isSpeakerA = isSpeakerA,
                            isPlaying = playing,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )

                        current?.let { turn ->
                            ModernSpeechBubble(
                                text = turn.text,
                                isSpeakerA = isSpeakerA,
                                isPlaying = playing,
                                primaryGradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFFF093FB)),
                                secondaryGradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF43E97B))
                            )
                        }

                        ModernControlPanel(
                            currentIndex = currentIndex,
                            conversationSize = conversation.size,
                            isPlaying = playing,
                            onPrevious = {
                                val newIdx = (currentIndex - 1).coerceAtLeast(0)
                                playTurn(newIdx)
                            },
                            onPlay = { playTurn(currentIndex) },
                            onPlayAll = { playAllFrom(0) },
                            onStop = {
                                resetPlaybackFlags()
                                viewModel.stopPlayback()
                            },
                            onNext = {
                                val newIdx = (currentIndex + 1).coerceAtMost(conversation.lastIndex)
                                playTurn(newIdx)
                            },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }
            }
        }
    }
}
