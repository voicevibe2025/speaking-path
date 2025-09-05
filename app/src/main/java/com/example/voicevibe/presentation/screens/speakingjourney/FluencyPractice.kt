@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FluencyPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val journeyVM: SpeakingJourneyViewModel = hiltViewModel()
    val sjUi by journeyVM.uiState
    val topic = sjUi.topics.firstOrNull { it.id == topicId }

    val viewModel: FluencyPracticeViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsState()

    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(topic) {
        if (topic != null) {
            viewModel.initializeForTopic(context, topic)
        }
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fluency Practice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                    CircularProgressIndicator()
                }
                return@Box
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Topic Title
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Prompt Card
                    PromptCard(prompt = ui.prompt, hints = ui.hints)

                    Spacer(Modifier.height(16.dp))

                    // Recording area
                    RecordingArea(
                        recordingState = ui.recordingState,
                        duration = ui.recordingDuration,
                        isSubmitting = ui.isSubmitting,
                        onStart = {
                            if (audioPermission.status.isGranted) {
                                viewModel.startRecording(context)
                            } else {
                                audioPermission.launchPermissionRequest()
                            }
                        },
                        onStop = { viewModel.stopRecording() },
                        onRetry = { viewModel.resetRecording() },
                        onSubmit = { viewModel.submitRecording(context) }
                    )

                    Spacer(Modifier.height(12.dp))

                    // Past Attempts
                    if (ui.pastAttempts.isNotEmpty()) {
                        Text(
                            text = "Past Attempts",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        ) {
                            LazyColumn(modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)) {
                                items(ui.pastAttempts) { attempt ->
                                    PastAttemptItem(
                                        attempt = attempt,
                                        onPlay = { viewModel.playAttempt(it) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }

            // Error snackbar
            ui.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) { Text(error) }
            }

            // Results dialog
            if (ui.showResults) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissResults() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissResults() }) { Text("Close") }
                    },
                    title = { Text("Fluency Analysis", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Transcript", style = MaterialTheme.typography.titleMedium)
                            val transcript = ui.session?.transcription?.takeIf { it.isNotBlank() } ?: "No transcript"
                            Text(transcript, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Technical details", style = MaterialTheme.typography.titleMedium)
                            val analysis = ui.latestAnalysis
                            if (analysis != null) {
                                val pausesText = if (analysis.pauses.isNotEmpty()) {
                                    analysis.pauses.joinToString(", ") { "${it}s" }
                                } else "None"
                                val misText = if (analysis.mispronunciations.isNotEmpty()) {
                                    analysis.mispronunciations.joinToString(", ")
                                } else "None"
                                Text("Pauses: $pausesText", fontSize = 14.sp)
                                Text("Stutters: ${analysis.stutterCount}", fontSize = 14.sp)
                                Text("Mispronunciations: $misText", fontSize = 14.sp)
                            } else {
                                Text("Not available", fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("AI Feedback", style = MaterialTheme.typography.titleMedium)
                            Text(ui.evaluation?.feedback ?: "No feedback", fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val candidate = ui.session?.audioUrl
                                    val playPath = when {
                                        candidate?.startsWith("http://") == true || candidate?.startsWith("https://") == true -> candidate
                                        !ui.audioFilePath.isNullOrBlank() -> ui.audioFilePath
                                        else -> candidate
                                    }
                                    if (!playPath.isNullOrBlank()) viewModel.playRecording(playPath)
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.size(4.dp))
                                    Text("Play Recording")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PromptCard(prompt: String, hints: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Practice Prompt", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(prompt, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            if (hints.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                hints.forEach { h ->
                    Text(h, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun RecordingArea(
    recordingState: RecordingState,
    duration: Int,
    isSubmitting: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onSubmit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Timer
            if (recordingState != RecordingState.IDLE) {
                val minutes = duration / 60
                val seconds = duration % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                when (recordingState) {
                    RecordingState.IDLE -> {
                        IconButton(
                            onClick = onStart,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    RecordingState.RECORDING -> {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    RecordingState.PAUSED, RecordingState.STOPPED -> {
                        // Not using paused currently
                    }
                }
            }

            if (recordingState == RecordingState.STOPPED) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Retry")
                    }
                    Button(onClick = onSubmit, modifier = Modifier.weight(1f), enabled = !isSubmitting) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastAttemptItem(
    attempt: FluencyAttempt,
    onPlay: (FluencyAttempt) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Score: ${"%.1f".format(attempt.overallScore)}", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { onPlay(attempt) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Play")
                }
            }
            attempt.transcript?.takeIf { it.isNotBlank() }?.let { t ->
                Spacer(Modifier.height(4.dp))
                Text(t, maxLines = 2, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(attempt.createdAt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
