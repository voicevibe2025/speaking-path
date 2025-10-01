@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import android.media.SoundPool
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.media.AudioManager

import com.example.voicevibe.utils.Constants

@Composable
fun ListeningPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val sjUi = sjVM.uiState.value
    val topic = sjUi.topics.firstOrNull { it.id == topicId }

    val viewModel: ListeningPracticeViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(topic?.id) {
        topic?.let { viewModel.start(it) }
    }

    val background = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A1128), Color(0xFF1E2761), Color(0xFF0A1128))
    )

    var showStartOverlay by remember(topic?.id, ui.totalQuestions) { mutableStateOf(true) }
    var isTimeUp by remember { mutableStateOf(false) }
    val totalSeconds = ui.totalQuestions * Constants.PRACTICE_SECONDS_PER_TURN
    var remainingSeconds by remember(totalSeconds) { mutableStateOf(totalSeconds) }
    var isTtsPreparing by remember { mutableStateOf(false) }
    val isTimerRunning = !showStartOverlay && !isTimeUp && totalSeconds > 0 && !ui.isLoading && !ui.showCongrats && ui.showQuestions && !isTtsPreparing

    val context = LocalContext.current
    val soundPool = remember {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
    }
    fun rawId(name: String): Int = context.resources.getIdentifier(name, "raw", context.packageName)
    val sfxCorrect = remember(soundPool) { rawId("correct").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxIncorrect = remember(soundPool) { rawId("incorrect").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxTick = remember(soundPool) { rawId("tick").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxTimeUp = remember(soundPool) { rawId("timeisup").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxWin = remember(soundPool) { rawId("win").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    DisposableEffect(soundPool) { onDispose { runCatching { soundPool.release() } } }

    val toneGen = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { toneGen?.release() } }
    // Stop any ongoing playback when leaving screen
    DisposableEffect(Unit) { onDispose { sjVM.stopPlayback() } }

    // Intro conversation playback
    var introPlaying by remember { mutableStateOf(false) }
    var introPlayed by remember { mutableStateOf(false) }

    fun playConversationAll() {
        val turns = topic?.conversation ?: emptyList()
        if (turns.isEmpty()) { introPlayed = true; return }
        introPlaying = true
        var idx = 0
        fun playNext(i: Int) {
            if (i >= turns.size) {
                introPlaying = false
                introPlayed = true
                // Reveal questions after audio finished
                viewModel.revealQuestions()
                return
            }
            val t = turns[i]
            val voice = if (t.speaker.equals("A", ignoreCase = true)) "Puck" else "Zephyr"
            val topicKey = topic?.title ?: topicId
            sjVM.playConversationAudioOrTts(
                context = context,
                topicKey = topicKey,
                turnIndex = i,
                text = t.text,
                voiceName = voice,
                onStart = {},
                onDone = { playNext(i + 1) },
                onError = { playNext(i + 1) }
            )
        }
        playNext(idx)
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingSeconds > 0 && !showStartOverlay && !isTimeUp && !ui.showCongrats && ui.showQuestions) {
                delay(1000L)
                remainingSeconds -= 1
                if (remainingSeconds > 0) {
                    if (sfxTick != 0) soundPool.play(sfxTick, 1f, 1f, 1, 0, 1f) else runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 60) }
                }
            }
            if (remainingSeconds <= 0 && !showStartOverlay && !isTimeUp && !ui.showCongrats && ui.showQuestions) {
                isTimeUp = true
                if (sfxTimeUp != 0) soundPool.play(sfxTimeUp, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    // Ensure the timer resets fresh when questions first appear (after audio finishes)
    LaunchedEffect(ui.showQuestions) {
        if (ui.showQuestions) {
            remainingSeconds = totalSeconds
        }
    }

    // SFX on answer reveal
    LaunchedEffect(ui.questionIndex, ui.revealedAnswer, ui.answerCorrect) {
        if (ui.revealedAnswer && !ui.isCompletionPending && !ui.showCongrats) {
            val soundId = if (ui.answerCorrect == true) sfxCorrect else sfxIncorrect
            if (soundId != 0) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    // Speak each question when first shown; show loader and pause timer until TTS actually starts
    LaunchedEffect(ui.question, ui.showQuestions) {
        if (ui.showQuestions && ui.question.isNotBlank()) {
            isTtsPreparing = true
            sjVM.speakWithBackendTts(
                ui.question,
                onStart = { isTtsPreparing = false },
                onDone = {},
                onError = { isTtsPreparing = false }
            )
        } else {
            isTtsPreparing = false
        }
    }

    // Play win SFX when the congrats overlay appears
    LaunchedEffect(ui.showCongrats) {
        if (ui.showCongrats) {
            if (sfxWin != 0) soundPool.play(sfxWin, 1f, 1f, 1, 0, 1f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(20.dp))
                        Text("Listening Practice", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(8.dp).size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (!showStartOverlay && !isTimeUp && totalSeconds > 0 && !ui.isLoading && !ui.showCongrats && ui.showQuestions && !isTtsPreparing) {
                        TimerChip(remainingSeconds = remainingSeconds)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(innerPadding)
        ) {
            if (topic == null || ui.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF90CAF9), strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Preparing listening with AI…",
                            color = Color(0xFFE0E0E0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(400))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Topic Header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (!ui.showQuestions) "Listen to the conversation" else "Question ${ui.questionIndex + 1} of ${ui.totalQuestions}",
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Instructions card (visible before questions)
                        if (!ui.showQuestions) {
                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                    Text("INSTRUCTIONS", color = Color(0xFFFFD700), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "1) Listen to the full conversation.\n2) Then answer short multiple-choice questions.\nScoring: +10 XP per correct answer, +20 XP bonus when you finish all.",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }

                        // Question and options
                        if (ui.showQuestions) {
                            if (isTtsPreparing) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
                                ) {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF90CAF9), strokeWidth = 3.dp)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Preparing next question…", color = Color.White)
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                        Text("Listen & Choose", color = Color(0xFFFFD700), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(Modifier.height(10.dp))
                                        Text(ui.question, color = Color.White, fontSize = 20.sp, lineHeight = 28.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ui.options.forEach { option ->
                                        OptionItem(
                                            text = option,
                                            enabled = !ui.isSubmitting && !ui.revealedAnswer,
                                            selected = ui.selectedOption == option,
                                            reveal = ui.revealedAnswer,
                                            answerCorrect = ui.answerCorrect,
                                        ) { viewModel.selectOption(option) }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Score: ${ui.score}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Text("XP +${ui.lastAwardedXp} (Total ${ui.totalXp})", color = Color(0xFF81C784), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Congrats overlay
            if (ui.showCongrats) {
                CongratsOverlayListening(ui = ui, onContinue = {
                    viewModel.dismissCongrats()
                    // Proactively refresh topics so TopicMaster shows updated Listening score
                    sjVM.reloadTopics()
                    onNavigateBack()
                })
            }

            // Start overlay
            if (showStartOverlay && topic != null && !ui.isLoading && ui.totalQuestions > 0) {
                StartOverlayListening(
                    onStart = {
                        remainingSeconds = totalSeconds
                        isTimeUp = false
                        showStartOverlay = false
                        introPlayed = false
                        // Ensure no lingering audio before starting intro
                        sjVM.stopPlayback()
                        // Start intro audio
                        playConversationAll()
                    }
                )
            }

            if (isTimeUp && topic != null && !ui.showCongrats) {
                TimeUpOverlay(
                    onRestart = {
                        // Reset state, reload questions, and return to Start overlay; do NOT auto-play audio while loading
                        sjVM.stopPlayback()
                        isTimeUp = false
                        showStartOverlay = true
                        introPlayed = false
                        isTtsPreparing = false
                        remainingSeconds = totalSeconds
                        viewModel.restart(topic)
                    },
                    onExit = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun TimerChip(remainingSeconds: Int) {
    val timeText = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Listening",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StartOverlayListening(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1128))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFF093FB),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Listening Challenge",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Listen to the conversation first, then answer short questions.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OptionItem(
    text: String,
    enabled: Boolean,
    selected: Boolean,
    reveal: Boolean,
    answerCorrect: Boolean?,
    onClick: () -> Unit
) {
    val defaultColor = Color(0xFF37474F)
    val correctColor = Color(0xFF2E7D32)
    val wrongColor = Color(0xFFC62828)
    val container = when {
        reveal && selected && (answerCorrect == true) -> correctColor
        reveal && selected && (answerCorrect == false) -> wrongColor
        else -> defaultColor
    }
    val borderColor = when {
        reveal && selected && (answerCorrect == true) -> Color(0xFF66BB6A)
        reveal && selected && (answerCorrect == false) -> Color(0xFFEF5350)
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(if (borderColor == Color.Transparent) 0.dp else 2.dp, borderColor)
    ) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = text, color = Color.White, fontSize = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CongratsOverlayListening(ui: ListeningUiState, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Great job!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Score: ${ui.score}", color = Color(0xFFFFD700), fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("XP gained: +${ui.completionXp} (Total ${ui.totalXp})", color = Color(0xFF81C784), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun TimeUpOverlay(onRestart: () -> Unit, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1128))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.HourglassBottom,
                    contentDescription = null,
                    tint = Color(0xFF4FACFE),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Time's up!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onExit, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
                        Text("Back", color = Color.White)
                    }
                    Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))) {
                        Text("Try Again", color = Color.White)
                    }
                }
            }
        }
    }
}
