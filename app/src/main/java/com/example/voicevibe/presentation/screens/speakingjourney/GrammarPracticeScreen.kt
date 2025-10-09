@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.utils.Constants
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import android.media.SoundPool
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.media.AudioManager

@Composable
fun GrammarPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val sjUi = sjVM.uiState.value
    val topic = sjUi.topics.firstOrNull { it.id == topicId }

    val viewModel: GrammarPracticeViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(topic?.id) {
        topic?.let { viewModel.start(it) }
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )

    // Timed practice state
    var showStartOverlay by remember(topic?.id, ui.totalQuestions) { mutableStateOf(true) }
    var isTimeUp by remember { mutableStateOf(false) }
    val totalSeconds = ui.totalQuestions * Constants.PRACTICE_SECONDS_PER_TURN
    var remainingSeconds by remember(totalSeconds) { mutableStateOf(totalSeconds) }

    val isTimerRunning = !showStartOverlay && !isTimeUp && totalSeconds > 0 && !ui.isLoading && !ui.showCongrats

    // --- Sound Effects using SoundPool ---
    val context = LocalContext.current
    val soundPool = remember {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
    }
    // Helper to get raw id by name; returns 0 if not found (safe when assets aren't added yet)
    fun rawId(name: String): Int = context.resources.getIdentifier(name, "raw", context.packageName)
    // Load sounds if available
    val sfxCorrect = remember(soundPool) { rawId("correct").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxIncorrect = remember(soundPool) { rawId("incorrect").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxWelcome = remember(soundPool) {
        val rid = rawId("welcome_to_grammar").takeIf { it != 0 } ?: rawId("welcom_to_vocab")
        if (rid != 0) soundPool.load(context, rid, 1) else 0
    }
    val sfxTimeUp = remember(soundPool) { rawId("timeisup").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    val sfxWin = remember(soundPool) { rawId("win").let { if (it != 0) soundPool.load(context, it, 1) else 0 } }
    DisposableEffect(soundPool) {
        onDispose { runCatching { soundPool.release() } }
    }
    // Fallback tone generator for tick if tick.wav is not provided yet
    val toneGen = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { toneGen?.release() } }
    // Play welcome when the start overlay first appears (once per session)
    var playedWelcome by remember(topic?.id) { mutableStateOf(false) }
    LaunchedEffect(sfxWelcome, showStartOverlay, ui.isLoading, ui.totalQuestions) {
        // Ensure we play as soon as the sample finishes loading
        soundPool.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status == 0 && sampleId == sfxWelcome && showStartOverlay && !playedWelcome && !ui.isLoading && ui.totalQuestions > 0) {
                val sid = sp.play(sampleId, 1f, 1f, 1, 0, 1f)
                if (sid != 0) playedWelcome = true
            }
        }
        if (showStartOverlay && !playedWelcome && !ui.isLoading && ui.totalQuestions > 0 && sfxWelcome != 0) {
            // Try immediate play, and if not ready, retry a few times briefly
            var sid = soundPool.play(sfxWelcome, 1f, 1f, 1, 0, 1f)
            if (sid == 0) {
                repeat(4) {
                    kotlinx.coroutines.delay(150)
                    if (!showStartOverlay || playedWelcome) return@repeat
                    sid = soundPool.play(sfxWelcome, 1f, 1f, 1, 0, 1f)
                    if (sid != 0) return@repeat
                }
            }
            if (sid != 0) playedWelcome = true
        }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingSeconds > 0 && !showStartOverlay && !isTimeUp && !ui.showCongrats) {
                delay(1000L)
                remainingSeconds -= 1
                if (remainingSeconds > 0) {
                }
            }
            if (remainingSeconds <= 0 && !showStartOverlay && !isTimeUp && !ui.showCongrats) {
                isTimeUp = true
                // Final SFX to signal time up
                if (sfxTimeUp != 0) soundPool.play(sfxTimeUp, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    // Play sound feedback when an answer is revealed
    LaunchedEffect(ui.questionIndex, ui.revealedAnswer, ui.answerCorrect) {
        if (ui.revealedAnswer) {
            val soundId = if (ui.answerCorrect == true) sfxCorrect else sfxIncorrect
            if (soundId != 0) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    // Play win sound when user completes before time is up (congrats shown)
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
                        Icon(Icons.Default.Spellcheck, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(20.dp))
                        Text("Grammar Practice", fontWeight = FontWeight.Bold)
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
                    if (!showStartOverlay && !isTimeUp && totalSeconds > 0 && !ui.isLoading && !ui.showCongrats) {
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
                            text = "Preparing questions with AI…",
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
                        // Topic Title Card
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
                                            colors = listOf(Color(0xFF06FFA5), Color(0xFF00C896))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Question ${ui.questionIndex + 1} of ${ui.totalQuestions}", color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        // Grammar sentence card
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                Text("Fill in the Blank", color = Color(0xFFFFD700), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(ui.sentence, color = Color.White, fontSize = 20.sp, lineHeight = 28.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Options
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

                        // Score/Xp footer
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

            // Error toast-like dialog
            ui.error?.let { msg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    confirmButton = {
                        Button(onClick = { viewModel.clearError() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))) {
                            Text("OK")
                        }
                    },
                    title = { Text("Error", color = Color.White) },
                    text = { Text(msg, color = Color(0xFFE0E0E0)) },
                    containerColor = Color(0xFF2a2d3a),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Congrats overlay
            if (ui.showCongrats) {
                CongratsOverlay(ui = ui, onContinue = {
                    viewModel.dismissCongrats()
                    onNavigateBack()
                })
            }

            // Timed practice overlays
            if (showStartOverlay && topic != null && !ui.isLoading && ui.totalQuestions > 0) {
                StartOverlay(
                    totalSeconds = totalSeconds,
                    perQuestionSeconds = Constants.PRACTICE_SECONDS_PER_TURN,
                    totalQuestions = ui.totalQuestions,
                    onStart = {
                        remainingSeconds = totalSeconds
                        isTimeUp = false
                        showStartOverlay = false
                    }
                )
            }

            if (isTimeUp && topic != null && !ui.showCongrats) {
                TimeUpOverlay(
                    onRestart = {
                        remainingSeconds = totalSeconds
                        isTimeUp = false
                        showStartOverlay = false
                        // restart the practice from backend
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
                imageVector = Icons.Default.Timer,
                contentDescription = "Time left",
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
private fun StartOverlay(
    totalSeconds: Int,
    perQuestionSeconds: Int,
    totalQuestions: Int,
    onStart: () -> Unit
) {
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
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFF06FFA5),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Timed Grammar",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You have $perQuestionSeconds seconds per question to finish all $totalQuestions questions. Be quick and focused!",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06FFA5))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start", color = Color.White)
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
                    imageVector = Icons.Default.HourglassBottom,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onExit, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
                        Text("Back", color = Color.White)
                    }
                    Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06FFA5))) {
                        Text("Try Again", color = Color.White)
                    }
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
    val correctColor = Color(0xFF2E7D32) // green
    val wrongColor = Color(0xFFC62828)   // red
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
private fun CongratsOverlay(ui: GrammarUiState, onContinue: () -> Unit) {
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
