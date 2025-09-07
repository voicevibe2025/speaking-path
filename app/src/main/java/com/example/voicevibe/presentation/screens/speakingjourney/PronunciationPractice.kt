package com.example.voicevibe.presentation.screens.speakingjourney

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PronunciationPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState

    // Select the topic by id when topics load
    LaunchedEffect(topicId, ui.topics) {
        val idx = ui.topics.indexOfFirst { it.id == topicId }
        if (idx >= 0 && ui.selectedTopicIdx != idx) viewModel.selectTopic(idx)
    }
    // Load user's recordings when topic changes
    LaunchedEffect(ui.selectedTopicIdx, ui.topics) {
        viewModel.loadTranscriptsForCurrentTopic(context)
    }

    val topic = ui.topics.getOrNull(ui.selectedTopicIdx)

    // Permissions
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Local UI state
    var showResultsSheet by remember { mutableStateOf(false) }
    var analysisFor by remember { mutableStateOf<PhraseTranscriptEntry?>(null) }

    // Gradient similar to FluencyPractice
    val background = Brush.verticalGradient(
        listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
    )

    // Resolve phrase index to display
    val totalPhrases = topic?.phraseProgress?.totalPhrases ?: (topic?.material?.size ?: 0)
    val baseIndex = topic?.phraseProgress?.currentPhraseIndex ?: 0
    val displayedIndex = if (totalPhrases > 0) {
        (ui.inspectedPhraseIndex ?: baseIndex).coerceIn(0, totalPhrases - 1)
    } else 0
    val phraseText = topic?.material?.getOrNull(displayedIndex) ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pronunciation Practice", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
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
            if (topic == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF64B5F6), strokeWidth = 3.dp)
                }
                return@Box
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                    fadeIn(animationSpec = tween(500))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Topic title and progress card
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
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Phrase ${displayedIndex + 1} of $totalPhrases", color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }

                    // Phrase card with Mic button
                    PhrasePracticeCard(
                        phrase = phraseText,
                        recordingState = ui.phraseRecordingState,
                        onMicClicked = {
                            when (ui.phraseRecordingState) {
                                PhraseRecordingState.RECORDING -> viewModel.stopPhraseRecording(context)
                                PhraseRecordingState.IDLE -> {
                                    if (audioPermission.status.isGranted) viewModel.startPhraseRecording(context) else audioPermission.launchPermissionRequest()
                                }
                                PhraseRecordingState.PROCESSING -> Unit // ignore taps while processing
                            }
                        }
                    )

                    // Controls row: Prev - Show results - Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val practiced = ui.currentTopicTranscripts.map { it.index }.distinct().sorted()
                        val hasPrev = if (ui.inspectedPhraseIndex != null) practiced.any { it < (ui.inspectedPhraseIndex ?: 0) }
                            else practiced.any { it < baseIndex }
                        IconButton(
                            onClick = { viewModel.inspectPreviousPhrase() },
                            enabled = hasPrev,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.4f))
                        }

                        Button(
                            onClick = { showResultsSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
                        ) { Text("Show result") }

                        val hasNextInReview = ui.inspectedPhraseIndex?.let { inspected -> practiced.any { it > inspected } } ?: false
                        IconButton(
                            onClick = { if (ui.inspectedPhraseIndex != null) viewModel.inspectNextPhrase() },
                            enabled = hasNextInReview,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = if (hasNextInReview) Color.White else Color.White.copy(alpha = 0.4f))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Helper text about rewards
                    Text(
                        text = "Complete a phrase to earn +10 score and +20 XP. Finish all to get +100 XP.",
                        color = Color(0xFFB0BEC5),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }

            // Results bottom sheet: all recordings list
            if (showResultsSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showResultsSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF2a2d3a),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Your recordings (" + ui.currentTopicTranscripts.size + ")",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        val itemsList = ui.currentTopicTranscripts.sortedByDescending { it.timestamp }
                        if (itemsList.isEmpty()) {
                            Text(
                                text = "No recordings yet. Record the current phrase to see results here.",
                                color = Color(0xFFB0BEC5),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(itemsList) { rec ->
                                    RecordingListItem(
                                        entry = rec,
                                        onPlay = { viewModel.playUserRecording(rec.audioPath) },
                                        onShowAnalysis = { analysisFor = rec }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            // Analysis dialog for selected recording
            analysisFor?.let { entry ->
                AnalysisDialog(entry = entry, onDismiss = { analysisFor = null })
            }

            // Topic completion congratulations overlay
            if (ui.showPronunciationCongrats) {
                val phraseCount = topic.material.size
                val totalScore = phraseCount * 10
                val totalXp = phraseCount * 20 + 100
                PronunciationCongratsDialog(
                    topicTitle = topic.title,
                    score = totalScore,
                    xp = totalXp,
                    onContinue = {
                        viewModel.dismissPronunciationCongrats()
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun PhrasePracticeCard(
    phrase: String,
    recordingState: PhraseRecordingState,
    onMicClicked: () -> Unit
) {
    val isRecording = recordingState == PhraseRecordingState.RECORDING
    val isProcessing = recordingState == PhraseRecordingState.PROCESSING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (phrase.isNotBlank()) phrase else "Loading phrase…",
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center
            )

            Surface(
                onClick = onMicClicked,
                modifier = Modifier.size(100.dp).clip(CircleShape),
                shape = CircleShape,
                color = if (isRecording) Color(0xFFFF6B6B) else Color(0xFF4CAF50),
                shadowElevation = if (isRecording) 16.dp else 8.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    when {
                        isProcessing -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        isRecording -> Icon(Icons.Filled.MicOff, contentDescription = "Stop Recording", tint = Color.White, modifier = Modifier.size(40.dp))
                        else -> Icon(Icons.Filled.Mic, contentDescription = "Start Recording", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
            }

            val hint = when {
                isProcessing -> "Processing…"
                isRecording -> "Tap to stop"
                else -> "Tap to record"
            }
            Text(hint, color = Color(0xFFB0BEC5))
        }
    }
}

@Composable
private fun RecordingListItem(
    entry: PhraseTranscriptEntry,
    onPlay: () -> Unit,
    onShowAnalysis: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Phrase ${entry.index + 1}", color = Color.White, fontWeight = FontWeight.SemiBold)
                val date = remember(entry.timestamp) { DateFormat.getDateTimeInstance().format(Date(entry.timestamp)) }
                Text(text = date, color = Color(0xFFB0BEC5), fontSize = 12.sp)
                if (entry.text.isNotBlank()) {
                    Text(text = entry.text, color = Color(0xFFE0E0E0), fontSize = 14.sp, maxLines = 2)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFF64B5F6))
                }
                IconButton(onClick = onShowAnalysis) {
                    Icon(Icons.Filled.Info, contentDescription = "Analysis", tint = Color(0xFFFFD54F))
                }
            }
        }
    }
}

@Composable
private fun AnalysisDialog(entry: PhraseTranscriptEntry, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Pronunciation Analysis", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Phrase ${entry.index + 1}")
                if (entry.text.isNotBlank()) Text("Transcript:\n${entry.text}")
                val pct = (entry.accuracy * 100).toInt().coerceIn(0, 100)
                Text("Accuracy: ${pct}%")
                entry.feedback?.takeIf { it.isNotBlank() }?.let { fb -> Text("Feedback:\n$fb") }
            }
        }
    )
}

@Composable
private fun PronunciationCongratsDialog(
    topicTitle: String,
    score: Int,
    xp: Int,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("Continue Journey") }
        },
        title = { Text("Congratulations!", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You've completed $topicTitle")
                Text("Score $score")
                Text("XP $xp")
            }
        },
        containerColor = Color(0xFF2D2F5B),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.9f)
    )
}
