@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.voicevibe.presentation.screens.speakingjourney

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.R
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(topic) {
        if (topic != null) {
            viewModel.initializeForTopic(context, topic)
        }
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1a1a2e),
            Color(0xFF16213e),
            Color(0xFF0f3460)
        )
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Fluency Practice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp),
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
                    CircularProgressIndicator(
                        color = Color(0xFF64B5F6),
                        strokeWidth = 3.dp
                    )
                }
                return@Box
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(500))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        // Topic Title Card with gradient
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF667eea),
                                                Color(0xFF764ba2)
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = topic.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Enhanced Prompt Card
                        ModernPromptCard(
                            prompt = ui.prompt,
                            hints = ui.hints,
                            isCompleted = topic.fluencyProgress?.completed == true
                        )
                    }

                    item {
                        // Modern Recording Area
                        ModernRecordingArea(
                            recordingState = ui.recordingState,
                            duration = ui.recordingDuration,
                            isSubmitting = ui.isSubmitting,
                            submissionMessages = ui.submissionMessages,
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
                    }

                    // Past Attempts Section
                    if (ui.pastAttempts.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Previous Attempts",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        items(ui.pastAttempts) { attempt ->
                            ModernPastAttemptItem(
                                attempt = attempt,
                                onPlay = { viewModel.playAttempt(it) }
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // Modern Error Snackbar
            ui.error?.let { error ->
                LaunchedEffect(error) {
                    snackbarHostState.showSnackbar(error)
                    viewModel.clearError()
                }
            }

            // Modern Results Dialog
            if (ui.showResults) {
                ModernResultsDialog(
                    ui = ui,
                    onDismiss = { viewModel.dismissResults() },
                    onPlayRecording = {
                        val candidate = ui.session?.audioUrl
                        val playPath = when {
                            candidate?.startsWith("http://") == true || 
                            candidate?.startsWith("https://") == true -> candidate
                            !ui.audioFilePath.isNullOrBlank() -> ui.audioFilePath
                            else -> candidate
                        }
                        if (!playPath.isNullOrBlank()) viewModel.playRecording(playPath)
                    }
                )
            }

            // Congratulations Overlay when all prompts are completed
            if (ui.showCongrats) {
                CongratsOverlay(
                    ui = ui,
                    onContinue = {
                        viewModel.dismissCongrats()
                        // Refresh topics so TopicMaster shows updated fluency score immediately
                        journeyVM.reloadTopics()
                        onNavigateBack()
                    }
                )
            }

            // Start Practice Overlay on first entry
            if (ui.showStartOverlay) {
                StartPracticeOverlay(
                    topicTitle = topic.title,
                    prompt = ui.prompt,
                    onBegin = { viewModel.dismissStartOverlay() }
                )
            }
        }
    }
}

@Composable
private fun ModernPromptCard(prompt: String, hints: List<String>, isCompleted: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2d3a)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Your Challenge",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ) {
                        Text(
                            "Completed",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                prompt,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 28.sp,
                color = Color.White,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
            
            if (hints.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        hints.forEach { hint ->
                            Text(
                                hint,
                                fontSize = 14.sp,
                                color = Color(0xFFB0BEC5),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernRecordingArea(
    recordingState: RecordingState,
    duration: Int,
    isSubmitting: Boolean,
    submissionMessages: List<String>,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onSubmit: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2d3a)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer Display
            AnimatedVisibility(visible = recordingState != RecordingState.IDLE) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = when {
                                recordingState == RecordingState.RECORDING && duration <= 5 -> Color(0xFFFF3B3B)
                                recordingState == RecordingState.RECORDING && duration <= 10 -> Color(0xFFFF9800)
                                recordingState == RecordingState.RECORDING -> Color(0xFFFF6B6B)
                                else -> Color(0xFF64B5F6)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = String.format("%02d:%02d", duration / 60, duration % 60),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                recordingState == RecordingState.RECORDING && duration <= 5 -> Color(0xFFFF3B3B)
                                recordingState == RecordingState.RECORDING && duration <= 10 -> Color(0xFFFF9800)
                                recordingState == RecordingState.RECORDING -> Color(0xFFFF6B6B)
                                else -> Color.White
                            },
                            letterSpacing = 2.sp
                        )
                    }
                    
                    if (recordingState == RecordingState.RECORDING) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (duration <= 5) "Time almost up!" else "Recording in progress...",
                            fontSize = 14.sp,
                            color = when {
                                duration <= 5 -> Color(0xFFFF3B3B)
                                duration <= 10 -> Color(0xFFFF9800)
                                else -> Color(0xFFFF6B6B)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Recording Controls
            when (recordingState) {
                RecordingState.IDLE -> {
                    Surface(
                        onClick = onStart,
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Color(0xFF4CAF50),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Start Recording",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tap to start recording",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
                
                RecordingState.RECORDING -> {
                    Surface(
                        onClick = onStop,
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseAnimation),
                        shape = CircleShape,
                        color = Color(0xFFFF6B6B),
                        shadowElevation = 12.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tap to stop early, or wait for auto-submit",
                        fontSize = 14.sp,
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Center
                    )
                }
                
                RecordingState.STOPPED, RecordingState.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp, 
                                Color(0xFF64B5F6)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF64B5F6)
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Retry",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Button(
                            onClick = onSubmit,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Submit",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Progressive submission messages
                    if (isSubmitting && submissionMessages.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF37474F).copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            submissionMessages.forEachIndexed { idx, msg ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFF64B5F6),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = msg,
                                        fontSize = 14.sp,
                                        color = Color(0xFFE0E0E0),
                                        fontWeight = if (idx == submissionMessages.lastIndex) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernPastAttemptItem(
    attempt: FluencyAttempt,
    onPlay: (FluencyAttempt) -> Unit
) {
    val scoreColor = when {
        attempt.overallScore >= 80 -> Color(0xFF4CAF50)
        attempt.overallScore >= 60 -> Color(0xFFFFEB3B)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2d3a)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = scoreColor.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${attempt.overallScore.toInt()}",
                                color = scoreColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (index < (attempt.overallScore / 20).toInt())
                                        scoreColor else Color(0xFF37474F)
                                )
                            }
                        }
                        Text(
                            attempt.createdAt,
                            fontSize = 12.sp,
                            color = Color(0xFF78909C)
                        )
                    }
                }

                attempt.transcript?.takeIf { it.isNotBlank() }?.let { transcript ->
                    Text(
                        transcript,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5),
                        lineHeight = 20.sp
                    )
                }
            }

            IconButton(
                onClick = { onPlay(attempt) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF64B5F6).copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play Recording",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernResultsDialog(
    ui: FluencyUiState,
    onDismiss: () -> Unit,
    onPlayRecording: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2a2d3a),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Fluency Analysis",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            val score = ui.evaluation?.overallScore ?: 0f
            val scoreInt = score.toInt()
            val scoreColor = when {
                score >= 80f -> Color(0xFF4CAF50)
                score >= 60f -> Color(0xFFFFC107)
                else -> Color(0xFFFF7043)
            }
            val showAdvancedSections = false // Keep code but hide from display for now

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Simple Analysis
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF37474F).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Simple Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF64B5F6),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))

                            val analysis = ui.latestAnalysis
                            if (analysis != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row {
                                        Text("⏱ Pauses: ", fontSize = 14.sp, color = Color(0xFF90A4AE))
                                        Text(
                                            if (analysis.pauses.isNotEmpty())
                                                analysis.pauses.joinToString(", ") { "${it}s" }
                                            else "None detected",
                                            fontSize = 14.sp,
                                            color = Color(0xFFE0E0E0)
                                        )
                                    }
                                    Row {
                                        Text("🔄 Stutters: ", fontSize = 14.sp, color = Color(0xFF90A4AE))
                                        Text(
                                            "${analysis.stutterCount}",
                                            fontSize = 14.sp,
                                            color = Color(0xFFE0E0E0)
                                        )
                                    }
                                    Row {
                                        Text("🗣 Mispronunciations: ", fontSize = 14.sp, color = Color(0xFF90A4AE))
                                        Text(
                                            if (analysis.mispronunciations.isNotEmpty())
                                                analysis.mispronunciations.joinToString(", ")
                                            else "None detected",
                                            fontSize = 14.sp,
                                            color = Color(0xFFE0E0E0)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Analysis not available",
                                    fontSize = 14.sp,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }
                    }
                }

                // Advanced sections (preserved but hidden for now)
                if (showAdvancedSections) {
                    // Transcript Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF37474F).copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "📝 Transcript",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF64B5F6),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                val transcriptText = ui.session?.transcription?.let { t ->
                                    if (t.isNotBlank()) t else null
                                } ?: "No transcript available"
                                Text(
                                    transcriptText,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE0E0E0),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // AI Feedback Section
                    if (ui.evaluation?.feedback != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "🤖 AI Feedback",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        ui.evaluation.feedback,
                                        fontSize = 14.sp,
                                        color = Color(0xFFE0E0E0),
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // Suggestions Section
                    val suggestions = ui.evaluation?.suggestions.orEmpty()
                    if (suggestions.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEB3B).copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "💡 Suggestions",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFFFEB3B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    suggestions.forEach { suggestion ->
                                        Row(Modifier.padding(vertical = 2.dp)) {
                                            Text("• ", fontSize = 14.sp, color = Color(0xFFFFEB3B))
                                            Text(
                                                suggestion,
                                                fontSize = 14.sp,
                                                color = Color(0xFFE0E0E0),
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Play Recording Button
                item {
                    OutlinedButton(
                        onClick = onPlayRecording,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Color(0xFF64B5F6)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF64B5F6)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Play Your Recording",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF64B5F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun StartPracticeOverlay(
    topicTitle: String,
    prompt: String,
    onBegin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Fluency Practice",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Topic: $topicTitle",
                    color = Color(0xFFB0BEC5),
                    fontWeight = FontWeight.Medium
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F).copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("How it works", color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold)
                        Text("1) You have 30 seconds to speak (countdown timer).", color = Color(0xFFE0E0E0), fontSize = 14.sp)
                        Text("2) Keep it smooth: avoid long pauses and stutters.", color = Color(0xFFE0E0E0), fontSize = 14.sp)
                        Text("3) Recording auto-submits when time is up!", color = Color(0xFFE0E0E0), fontSize = 14.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF28323A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Your Challenge", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(prompt, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }

                Button(
                    onClick = onBegin,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Begin", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CongratsOverlay(
    ui: FluencyUiState,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    // Play celebratory sound once when this overlay appears
    DisposableEffect(Unit) {
        val mp = try { android.media.MediaPlayer.create(context, R.raw.win) } catch (_: Throwable) { null }
        try { mp?.start() } catch (_: Throwable) {}
        mp?.setOnCompletionListener { player ->
            try { player.release() } catch (_: Throwable) {}
        }
        onDispose {
            try { mp?.release() } catch (_: Throwable) {}
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Congratulations!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "You've completed the fluency practice!",
                    color = Color(0xFFB0BEC5)
                )

                // Scores summary
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F).copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your Score", color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold)
                        val score = ui.totalFluencyScore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fluency Practice",
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "$score",
                                color = if (score >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // XP summary
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                        Text(
                            text = "+${ui.completionXpGained} XP",
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
                ) {
                    Text("Continue Journey", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}