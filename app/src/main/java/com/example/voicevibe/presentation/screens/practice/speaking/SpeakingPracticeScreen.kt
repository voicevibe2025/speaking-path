package com.example.voicevibe.presentation.screens.practice.speaking

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

/**
 * Speaking Practice screen composable
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SpeakingPracticeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    viewModel: SpeakingPracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Audio recording permission
    val audioPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SpeakingPracticeEvent.NavigateToResults -> {
                    onNavigateToResults(event.sessionId)
                }
                is SpeakingPracticeEvent.ShowPermissionRequest -> {
                    audioPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speaking Practice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::skipPrompt) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !audioPermissionState.status.isGranted -> {
                    PermissionScreen(
                        onRequestPermission = {
                            audioPermissionState.launchPermissionRequest()
                        }
                    )
                }
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    PracticeContent(
                        uiState = uiState,
                        onStartRecording = {
                            val audioFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
                            viewModel.startRecording(audioFile)
                        },
                        onStopRecording = viewModel::stopRecording,
                        onPauseRecording = viewModel::pauseRecording,
                        onResumeRecording = viewModel::resumeRecording,
                        onRetry = viewModel::retryRecording,
                        onSubmit = viewModel::submitRecording
                    )
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { /* Dismiss */ }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Microphone Permission Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VoiceVibe needs access to your microphone to record your speaking practice.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun PracticeContent(
    uiState: SpeakingPracticeUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onRetry: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prompt Card
        uiState.currentPrompt?.let { prompt ->
            PromptCard(prompt = prompt)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Waveform Visualizer
        WaveformVisualizer(
            amplitudes = uiState.amplitudes,
            isRecording = uiState.recordingState == RecordingState.RECORDING,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recording Duration
        if (uiState.recordingState != RecordingState.IDLE) {
            RecordingTimer(duration = uiState.recordingDuration)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recording Controls
        RecordingControls(
            recordingState = uiState.recordingState,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onPauseRecording = onPauseRecording,
            onResumeRecording = onResumeRecording
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        when (uiState.recordingState) {
            RecordingState.STOPPED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }

                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun PromptCard(
    prompt: com.example.voicevibe.domain.model.PracticePrompt
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Practice Prompt",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = prompt.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp
            )

            if (prompt.hints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                prompt.hints.forEach { hint ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hint,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    val primaryWaveColor = MaterialTheme.colorScheme.primary
    val placeholderWaveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2

            // Draw waveform or placeholder
            if (amplitudes.isNotEmpty()) {
                val path = Path()
                val stepX = width / amplitudes.size

                amplitudes.forEachIndexed { index, amplitude ->
                    val x = index * stepX
                    val y = centerY + (amplitude * height / 2 * if (isRecording) animatedAlpha else 1f)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = primaryWaveColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            } else {
                // Draw placeholder line
                drawLine(
                    color = placeholderWaveColor,
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun RecordingTimer(
    duration: Int
) {
    val minutes = duration / 60
    val seconds = duration % 60

    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RecordingControls(
    recordingState: RecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (recordingState) {
            RecordingState.IDLE -> {
                // Start button
                RecordButton(
                    onClick = onStartRecording,
                    isRecording = false
                )
            }
            RecordingState.RECORDING -> {
                // Pause button
                IconButton(
                    onClick = onPauseRecording,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Stop button
                RecordButton(
                    onClick = onStopRecording,
                    isRecording = true
                )
            }
            RecordingState.PAUSED -> {
                // Resume button
                IconButton(
                    onClick = onResumeRecording,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Stop button
                RecordButton(
                    onClick = onStopRecording,
                    isRecording = false
                )
            }
            RecordingState.STOPPED -> {
                // No controls shown
            }
        }
    }
}

@Composable
private fun RecordButton(
    onClick: () -> Unit,
    isRecording: Boolean
) {
    val animatedSize by animateDpAsState(
        targetValue = if (isRecording) 72.dp else 80.dp,
        animationSpec = tween(300)
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(animatedSize)
            .clip(CircleShape)
            .background(
                if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            )
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop" else "Record",
            tint = Color.White,
            modifier = Modifier.size(if (isRecording) 32.dp else 40.dp)
        )
    }
}
