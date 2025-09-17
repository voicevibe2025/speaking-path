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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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

    // Mode: choose => overlay, review => read-only, practice => speaking (one-way)
    var mode by rememberSaveable { mutableStateOf("choose") } // "choose" | "review" | "practice"
    var roleLocked by rememberSaveable { mutableStateOf(false) }
    var sessionXp by rememberSaveable { mutableStateOf(0) }
    var sessionScoreSum by rememberSaveable { mutableStateOf(0.0) }
    var sessionTurns by rememberSaveable { mutableStateOf(0) }

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

                        // Role selector (only in Practice mode). Lock once chosen.
                        if (mode == "practice") {
                            ModernRoleSelector(
                                selectedRole = selectedRole,
                                onSelect = { role ->
                                    if (!roleLocked) {
                                        selectedRole = role
                                        roleLocked = true
                                    }
                                },
                                modifier = Modifier.padding(bottom = 12.dp),
                                enabled = !roleLocked
                            )
                        }

                        // Modern speech bubble (hidden in Practice mode)
                        if (mode != "practice") {
                            current?.let { turn ->
                                ModernSpeechBubble(
                                    text = turn.text,
                                    isSpeakerA = isSpeakerA,
                                    isPlaying = playing,
                                    primaryGradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFFF093FB)),
                                    secondaryGradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF43E97B))
                                )
                            }
                        }

                        // Controls: Review shows full controls; Practice hides manual play/pause/next/prev
                        if (mode != "practice") {
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
                        }

                        // Bottom area: Practice => recording panel; Review => Start practice button
                        if (mode == "practice") {
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
                        } else {
                            Button(
                                onClick = {
                                    // Reset to beginning and stop any playback before entering practice
                                    resetPlaybackFlags()
                                    viewModel.stopPlayback()
                                    currentIndex = 0
                                    // Reset session aggregates and role state
                                    sessionXp = 0
                                    sessionScoreSum = 0.0
                                    sessionTurns = 0
                                    selectedRole = null
                                    roleLocked = false
                                    mode = "practice"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                            ) {
                                Text("Start practice", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Mode selection overlay on first open
            if (mode == "choose") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111226)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Choose a mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                "Review lets you read and listen. Practice lets you speak your lines.",
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { mode = "review" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FACFE))
                                ) { Text("Review", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                Button(
                                    onClick = { mode = "practice" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43E97B))
                                ) { Text("Practice", color = Color.White, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }

            // Result dialog + aggregate session totals
            ui.conversationSubmissionResult?.let { result ->
                LaunchedEffect(result) {
                    sessionXp += result.xpAwarded ?: 0
                    sessionScoreSum += result.accuracy
                    sessionTurns += 1
                }
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

            // Completion overlay/dialog
            if (ui.showConversationCongrats) {
                if (mode == "practice") {
                    val avg = if (sessionTurns > 0) sessionScoreSum / sessionTurns.toDouble() else 0.0
                    PracticeCongratsOverlay(
                        averageScore = avg,
                        xp = sessionXp,
                        onDismiss = {
                            viewModel.dismissConversationCongrats()
                            // Unlock role switching for a fresh run; reset counters and index
                            roleLocked = false
                            selectedRole = null
                            currentIndex = 0
                            sessionXp = 0
                            sessionScoreSum = 0.0
                            sessionTurns = 0
                        }
                    )
                } else {
                    ConversationCompletionDialog(
                        onDismiss = { viewModel.dismissConversationCongrats() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeMinimalControls(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play button
        Surface(
            onClick = onPlay,
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                ModernPlayButton(isPlaying)
            }
        }

        // Stop button
        Surface(
            onClick = onStop,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
            }
        }
    }
}

// ----- Practice-specific helper composables -----

@Composable
fun ModernRoleSelector(
    selectedRole: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoleChip(
            label = "Speak as A",
            selected = selectedRole?.equals("A", ignoreCase = true) == true,
            onClick = { onSelect("A") },
            leadingColor = Color(0xFF667EEA),
            enabled = enabled
        )
        RoleChip(
            label = "Speak as B",
            selected = selectedRole?.equals("B", ignoreCase = true) == true,
            onClick = { onSelect("B") },
            leadingColor = Color(0xFF4FACFE),
            enabled = enabled
        )
    }
}

@Composable
private fun RoleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingColor: Color,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (!enabled) Color.White.copy(alpha = 0.04f) else if (selected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (!enabled) Color.White.copy(alpha = 0.06f) else if (selected) leadingColor else Color.White.copy(alpha = 0.12f)
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
                    .background(if (!enabled) Color.White.copy(alpha = 0.2f) else leadingColor, CircleShape)
            )
            Text(
                text = label,
                color = if (!enabled) Color.White.copy(alpha = 0.6f) else Color.White,
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

@Composable
private fun PracticeCongratsOverlay(
    averageScore: Double,
    xp: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111226)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Great job! 🎉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    text = "Practice session complete",
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Avg Score", color = Color.White.copy(alpha = 0.7f))
                        Text("${"%.1f".format(averageScore)}%", color = Color(0xFF06FFA5), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("XP", color = Color.White.copy(alpha = 0.7f))
                        Text("+$xp", color = Color(0xFFFFBE0B), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                ) {
                    Text("Finish", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
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
