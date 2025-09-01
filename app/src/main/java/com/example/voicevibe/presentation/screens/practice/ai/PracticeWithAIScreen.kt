package com.example.voicevibe.presentation.screens.practice.ai

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.voicevibe.presentation.screens.practice.speaking.SpeakingPracticeViewModel
import com.example.voicevibe.presentation.screens.practice.speaking.RecordingState
import com.example.voicevibe.presentation.screens.practice.speaking.SpeakingPracticeEvent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PracticeWithAIScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    practiceViewModel: SpeakingPracticeViewModel = hiltViewModel()
) {
    val uiState by practiceViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val askedOnce = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        practiceViewModel.events.collect { event ->
            when (event) {
                is SpeakingPracticeEvent.NavigateToResults -> onNavigateToResults(event.sessionId)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice with AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
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
                        val permStatus = audioPermissionState.status
                        val deniedPermanently = askedOnce.value && (permStatus is PermissionStatus.Denied) && !permStatus.shouldShowRationale
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                askedOnce.value = true
                                audioPermissionState.launchPermissionRequest()
                            }) {
                                Text("Grant permission")
                            }
                            if (deniedPermanently) {
                                OutlinedButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Text("Open settings")
                                }
                            }
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Practice Prompt", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = prompt.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Recording status
            if (uiState.recordingState != RecordingState.IDLE) {
                Text(
                    text = "Duration: ${uiState.recordingDuration}s",
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
                            val f = File(context.cacheDir, "journey_rec_${System.currentTimeMillis()}.m4a")
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
}
