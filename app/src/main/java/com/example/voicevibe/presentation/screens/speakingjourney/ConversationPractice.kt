package com.example.voicevibe.presentation.screens.speakingjourney

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
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

    // Microphone permission state (fallback if global request was denied)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

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
        val topicKey = topic?.title ?: topicId
        viewModel.playConversationAudioOrTts(
            context = context,
            topicKey = topicKey,
            turnIndex = i,
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
            val topicKey = topic?.title ?: topicId
            viewModel.playConversationAudioOrTts(
                context = context,
                topicKey = topicKey,
                turnIndex = i,
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = topic?.title ?: "Conversation Practice",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 18.sp,
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
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
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
                .drawBehind { drawPracticeAnimatedGradient(gradientOffset) }
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

                    // Auto-play the other turn (non-user role) using local audio first, then TTS fallback
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
                                val topicKey = topic?.title ?: topicId
                                viewModel.playConversationAudioOrTts(
                                    context = context,
                                    topicKey = topicKey,
                                    turnIndex = currentIndex,
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
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress indicator
                        PracticeConversationProgress(
                            currentIndex = currentIndex,
                            totalSteps = conversation.size,
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        // Speakers section with modern avatars
                        PracticeSpeakersSection(
                            isSpeakerA = isSpeakerA,
                            isPlaying = playing,
                            modifier = Modifier.padding(vertical = 16.dp)
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
                                modifier = Modifier.padding(bottom = 16.dp),
                                enabled = !roleLocked
                            )
                        }

                        // Modern speech bubble (hidden in Practice mode)
                        if (mode != "practice") {
                            current?.let { turn ->
                                PracticeSpeechBubble(
                                    text = turn.text,
                                    isSpeakerA = isSpeakerA,
                                    isPlaying = playing,
                                    primaryGradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFFF093FB)),
                                    secondaryGradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF43E97B))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Controls: Review shows full controls; Practice hides manual play/pause/next/prev
                        if (mode != "practice") {
                            PracticeControlPanel(
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
                                modifier = Modifier.padding(bottom = 20.dp)
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
                                onStart = {
                                    if (audioPermission.status.isGranted) {
                                        viewModel.startConversationRecording(context, currentIndex)
                                    } else {
                                        audioPermission.launchPermissionRequest()
                                    }
                                },
                                onStop = { viewModel.stopConversationRecording(context) },
                                modifier = Modifier.padding(bottom = 24.dp)
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
                                    .height(56.dp)
                                    .padding(horizontal = 48.dp)
                                    .padding(bottom = 24.dp)
                                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text(
                                    "Start Practice", 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
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
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B2E)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color(0xFF667EEA),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Choose Your Mode", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 20.sp
                            )
                            Text(
                                "Review mode lets you read and listen.\nPractice mode lets you speak your lines.",
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { mode = "review" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4FACFE)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) { 
                                    Text("Review", color = Color.White, fontWeight = FontWeight.SemiBold) 
                                }
                                Button(
                                    onClick = { mode = "practice" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF43E97B)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) { 
                                    Text("Practice", color = Color.White, fontWeight = FontWeight.SemiBold) 
                                }
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
fun PracticeConversationProgress(
    currentIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0 until totalSteps) {
                val isActive = i <= currentIndex
                val width by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.25f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "practiceProgressWidth"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4FACFE), Color(0xFF43E97B))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))
                                )
                            }
                        )
                ) {
                    // scale effect
                    Box(modifier = Modifier.graphicsLayer { scaleX = width })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Step ${currentIndex + 1} of $totalSteps",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PracticeSpeakersSection(
    isSpeakerA: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseAnimation = rememberInfiniteTransition(label = "practicePulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "practicePulseScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PracticeSpeakerAvatar(
            imageRes = R.drawable.ic_male_head,
            label = "Alex",
            isActive = isSpeakerA,
            isPlaying = isPlaying && isSpeakerA,
            pulseScale = if (isPlaying && isSpeakerA) pulseScale else 1f,
            glowColor = Color(0xFF667EEA)
        )
        Text(
            text = "×",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraLight
        )
        PracticeSpeakerAvatar(
            imageRes = R.drawable.ic_female_head,
            label = "Sarah",
            isActive = !isSpeakerA,
            isPlaying = isPlaying && !isSpeakerA,
            pulseScale = if (isPlaying && !isSpeakerA) pulseScale else 1f,
            glowColor = Color(0xFF4FACFE)
        )
    }
}

@Composable
fun PracticeSpeakerAvatar(
    imageRes: Int,
    label: String,
    isActive: Boolean,
    isPlaying: Boolean,
    pulseScale: Float,
    glowColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "practiceAvatarScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = 0.3f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                border = BorderStroke(
                    width = if (isActive) 3.dp else 1.dp,
                    brush = if (isActive) {
                        Brush.linearGradient(listOf(glowColor, glowColor.copy(alpha = 0.5f)))
                    } else {
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f)))
                    }
                ),
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
fun PracticeSpeechBubble(
    text: String,
    isSpeakerA: Boolean,
    isPlaying: Boolean,
    primaryGradient: List<Color>,
    secondaryGradient: List<Color>
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "practiceBubbleScale"
    )
    val waveHeight = 28.dp
    val waveGap = 8.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.1f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (isSpeakerA) primaryGradient else secondaryGradient
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    if (isPlaying) {
                        PracticeSoundWaveAnimation(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(waveHeight)
                                .padding(bottom = waveGap)
                        )
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isPlaying) {
                        Spacer(modifier = Modifier.height(waveHeight + waveGap))
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeSoundWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "practiceWaves")
    val waves = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    delayMillis = index * 100,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "practiceWave$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waves.forEach { animatedHeight ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(animatedHeight.value)
                    .padding(horizontal = 2.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF667EEA), Color(0xFFF093FB))
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PracticeControlPanel(
    currentIndex: Int,
    conversationSize: Int,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onPlayAll: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PracticeControlButton(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                size = 48.dp
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Surface(onClick = { if (isPlaying) onStop() else onPlayAll() }, shape = CircleShape, color = Color.Transparent) {
                PracticePlayButton(isPlaying = isPlaying)
            }

            PracticeControlButton(
                onClick = onNext,
                enabled = currentIndex < conversationSize - 1,
                size = 48.dp
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PracticeControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size).clip(CircleShape),
        color = if (enabled) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun PracticePlayButton(isPlaying: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = if (isPlaying) {
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        },
        label = "practicePlayRotation"
    )
    Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFFF093FB))
                    ),
                    shape = CircleShape
                )
                .graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

fun DrawScope.drawPracticeAnimatedGradient(offset: Float) {
    val colors = listOf(
        Color(0xFF0C1024),
        Color(0xFF141A33),
        Color(0xFF0C2740),
        Color(0xFF16213E),
        Color(0xFF141A33),
        Color(0xFF0C1024)
    )
    val gradientBrush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f + offset * 2, 0f),
        end = Offset(size.width + offset * 2, size.height)
    )
    drawRect(gradientBrush)
    drawRect(color = Color.Black.copy(alpha = 0.35f), size = size)
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
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play button
        Surface(
            onClick = onPlay,
            shape = CircleShape,
            color = Color(0xFF667EEA).copy(alpha = 0.15f),
            border = BorderStroke(2.dp, Color(0xFF667EEA))
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Stop button
        Surface(
            onClick = onStop,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (enabled) "Select Your Role" else "Role Selected",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoleChip(
                label = "Speaker A",
                selected = selectedRole?.equals("A", ignoreCase = true) == true,
                onClick = { onSelect("A") },
                leadingColor = Color(0xFF667EEA),
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(16.dp))
            RoleChip(
                label = "Speaker B",
                selected = selectedRole?.equals("B", ignoreCase = true) == true,
                onClick = { onSelect("B") },
                leadingColor = Color(0xFF4FACFE),
                enabled = enabled
            )
        }
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
        shape = RoundedCornerShape(24.dp),
        color = when {
            !enabled -> Color.White.copy(alpha = 0.05f)
            selected -> leadingColor.copy(alpha = 0.2f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.1f)
                selected -> leadingColor
                else -> Color.White.copy(alpha = 0.2f)
            }
        ),
        modifier = Modifier
            .shadow(
                elevation = if (selected) 8.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = leadingColor.copy(alpha = 0.3f)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (!enabled) Color.White.copy(alpha = 0.3f) else leadingColor,
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                color = if (!enabled) Color.White.copy(alpha = 0.5f) else Color.White,
                fontSize = 14.sp,
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isRecording -> Color(0xFFE53935)
                enabled -> Color.White.copy(alpha = 0.2f)
                else -> Color.White.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Record Your Line",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        !enabled -> "Select your role first"
                        isProcessing -> "Processing..."
                        isRecording -> "Recording now..."
                        else -> "Tap microphone to start"
                    },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            
            Box(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Surface(
                    onClick = {
                        if (!enabled || isProcessing) return@Surface
                        if (isRecording) onStop() else onStart()
                    },
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = when {
                        !enabled || isProcessing -> Color.White.copy(alpha = 0.05f)
                        isRecording -> Color(0xFFE53935)
                        else -> Color(0xFF667EEA)
                    },
                    border = if (isRecording) {
                        BorderStroke(2.dp, Color(0xFFFF6B6B))
                    } else null
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
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
        containerColor = Color(0xFF1A1B2E),
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.7f))
                }
                TextButton(onClick = onGoNext) {
                    Text(
                        if (result.nextTurnIndex != null) "Next" else "OK",
                        color = Color(0xFF667EEA),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        title = {
            Text(
                "Submission Result",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Accuracy", color = Color.White.copy(alpha = 0.7f))
                    Text(
                        "${"%.1f".format(result.accuracy)}%",
                        color = Color(0xFF06FFA5),
                        fontWeight = FontWeight.Bold
                    )
                }
                if ((result.xpAwarded ?: 0) > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("XP Earned", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            "+${result.xpAwarded}",
                            color = Color(0xFFFFBE0B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (result.feedback?.isNotBlank() == true) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            result.feedback!!,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (result.transcription.isNotBlank()) {
                    Text(
                        "You said: \"${result.transcription}\"",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
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
        containerColor = Color(0xFF1A1B2E),
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    "Awesome!",
                    color = Color(0xFF667EEA),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Conversation Complete",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎉", fontSize = 20.sp)
            }
        },
        text = {
            Text(
                "Great job! You've completed all turns for this conversation.",
                color = Color.White.copy(alpha = 0.9f)
            )
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
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B2E)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 48.sp)
                Text(
                    "Great Job!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Practice session complete",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Average Score",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${"%.1f".format(averageScore)}%",
                                color = Color(0xFF06FFA5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "XP Earned",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "+$xp",
                                color = Color(0xFFFFBE0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667EEA)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "Continue",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
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
        val topicKey = topic?.title ?: topicId
        viewModel.playConversationAudioOrTts(
            context = context,
            topicKey = topicKey,
            turnIndex = i,
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
            val topicKey = topic?.title ?: topicId
            viewModel.playConversationAudioOrTts(
                context = context,
                topicKey = topicKey,
                turnIndex = i,
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = topic?.title ?: "Conversation",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 18.sp,
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
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
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
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ConversationProgress(
                            currentIndex = currentIndex,
                            totalSteps = conversation.size,
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        ModernSpeakersSection(
                            isSpeakerA = isSpeakerA,
                            isPlaying = playing,
                            modifier = Modifier.padding(vertical = 16.dp)
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

                        Spacer(modifier = Modifier.weight(1f))

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
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }
            }
        }
    }
}