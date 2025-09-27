@file:OptIn(ExperimentalAnimationApi::class)
package com.example.voicevibe.presentation.screens.practice.live

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicevibe.domain.model.LiveMessage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalAnimationApi::class)
@Composable
fun LivePracticeScreen(
    onNavigateBack: () -> Unit,
    viewModel: LivePracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by rememberSaveable { mutableStateOf("") }
    val canSend = uiState.isConnected && inputText.isNotBlank()
    val listState = rememberLazyListState()
    val recordPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    var isVoiceMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size, uiState.showTypingIndicator, isVoiceMode) {
        if (!isVoiceMode) {
            // In reverseLayout, index 0 is bottom-most
            listState.animateScrollToItem(0)
        }
    }


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ModernTopBar(
                isConnected = uiState.isConnected,
                isConnecting = uiState.isConnecting,
                isRecording = uiState.isRecording,
                isVoiceMode = isVoiceMode,
                onNavigateBack = onNavigateBack,
                onToggleMode = { isVoiceMode = !isVoiceMode }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = isVoiceMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                },
                label = "mode_transition",
                modifier = Modifier.fillMaxSize()
            ) { voiceMode ->
                if (voiceMode) {
                    VoiceModeScreen(
                        isConnected = uiState.isConnected,
                        isRecording = uiState.isRecording,
                        isBotSpeaking = uiState.isAiSpeaking,
                        onToggleRecording = {
                            if (!recordPermission.status.isGranted) {
                                recordPermission.launchPermissionRequest()
                            } else {
                                viewModel.toggleRecording()
                            }
                        },
                        onSwitchToText = { isVoiceMode = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .imePadding()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            state = listState,
                            reverseLayout = true,
                            verticalArrangement = Arrangement.Bottom  // Add this line to align content to bottom
                        ) {
                            // Add spacing between items
                            if (uiState.showTypingIndicator) {
                                item(key = "typing_indicator") { 
                                    TypingIndicator()
                                    Spacer(modifier = Modifier.height(8.dp))  // Add spacing
                                }
                            }

                            itemsIndexed(
                                items = uiState.messages.reversed(),
                                key = { index, message -> "message_${message.hashCode()}_$index" }
                            ) { index, message ->
                                ModernMessageBubble(
                                    message = message,
                                    isLastMessage = index == 0
                                )
                                if (index < uiState.messages.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))  // Add spacing between messages
                                }
                            }
                        }

                        ModernInputArea(
                            inputText = inputText,
                            onInputChange = { inputText = it },
                            isConnected = uiState.isConnected,
                            isRecording = uiState.isRecording,
                            canSend = canSend,
                            onSend = {
                                val text = inputText.trim()
                                if (text.isNotEmpty()) {
                                    viewModel.sendMessage(text)
                                    inputText = ""
                                }
                            },
                            onToggleRecording = {
                                if (!recordPermission.status.isGranted) {
                                    recordPermission.launchPermissionRequest()
                                } else {
                                    isVoiceMode = true
                                    viewModel.toggleRecording()
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            ) {
                ModernErrorCard(
                    message = uiState.error ?: "",
                    onRetry = viewModel::retryConnection,
                    onDismiss = { /* Handle dismiss */ }
                )
            }
        }
    }
}

@Composable
private fun VoiceModeScreen(
    isConnected: Boolean,
    isRecording: Boolean,
    isBotSpeaking: Boolean,
    onToggleRecording: () -> Unit,
    onSwitchToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(48.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Voice Assistant Visual
            AnimatedContent(
                targetState = isBotSpeaking,
                transitionSpec = {
                    scaleIn(animationSpec = tween(300)) + fadeIn() with
                            scaleOut(animationSpec = tween(300)) + fadeOut()
                },
                label = "voice_visual"
            ) { speaking ->
                if (speaking) {
                    // Bot Speaking Indicator
                    BotSpeakingIndicator()
                } else {
                    // Mic Button
                    VoiceMicButton(
                        isRecording = isRecording,
                        isConnected = isConnected,
                        onClick = onToggleRecording
                    )
                }
            }

            // Status Text
            AnimatedContent(
                targetState = when {
                    !isConnected -> "Connecting..."
                    isBotSpeaking -> "AI is speaking..."
                    isRecording -> "Listening..."
                    else -> "Tap to speak"
                },
                transitionSpec = {
                    fadeIn() with fadeOut()
                },
                label = "status_text"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Voice Mode Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 32.dp)
            ) {
                // Switch to Text Button
                OutlinedButton(
                    onClick = onSwitchToText,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch to Text")
                }

                // End Call Button (if recording)
                AnimatedVisibility(visible = isRecording && isConnected) {
                    FilledTonalButton(
                        onClick = onToggleRecording,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "End",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Ambient animation background
        if (isRecording && isConnected || isBotSpeaking) {
            AmbientWaveAnimation(
                isActive = true,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.1f)
            )
        }
    }
}

@Composable
private fun VoiceMicButton(
    isRecording: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer ring animations
        if (isRecording && isConnected) {
            repeat(3) { index ->
                val delay = index * 400
                val ringScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseOut),
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(delay)
                    ),
                    label = "ring_scale_$index"
                )
                val ringAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseOut),
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(delay)
                    ),
                    label = "ring_alpha_$index"
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale)
                        .alpha(ringAlpha)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }

        // Main button
        FilledIconButton(
            onClick = onClick,
            enabled = isConnected,
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .shadow(
                    elevation = if (isConnected && isRecording) 16.dp else 8.dp,
                    shape = CircleShape
                ),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isConnected && isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = if (isConnected && isRecording) Icons.Filled.Mic else Icons.Outlined.Mic,
                contentDescription = if (isConnected && isRecording) "Stop recording" else "Start recording",
                modifier = Modifier.size(64.dp),
                tint = if (isConnected && isRecording) Color.White else MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun BotSpeakingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // Animated speaking waves
        repeat(4) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "wave_$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f + (index * 0.1f),
                targetValue = 1.2f + (index * 0.1f),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000 + (index * 200),
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_scale_$index"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f - (index * 0.05f),
                targetValue = 0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000 + (index * 200),
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_alpha_$index"
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .alpha(alpha),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {}
        }

        // Center icon
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Sound bars animation
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            repeat(5) { index ->
                val infiniteTransition = rememberInfiniteTransition(label = "bar_$index")
                val height by infiniteTransition.animateFloat(
                    initialValue = 10f,
                    targetValue = 30f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(300, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 60)
                    ),
                    label = "bar_height_$index"
                )

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun AmbientWaveAnimation(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "ambient")
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambient_offset"
        )

        Canvas(modifier = modifier) {
            // Draw animated gradient waves
            // Implementation would go here
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTopBar(
    isConnected: Boolean,
    isConnecting: Boolean,
    isRecording: Boolean,
    isVoiceMode: Boolean,
    onNavigateBack: () -> Unit,
    onToggleMode: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isVoiceMode) "Voice Mode" else "Live Practice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        isActive = isConnected,
                        isConnecting = isConnecting,
                        label = if (isConnected) "Connected" else "Offline"
                    )

                    AnimatedVisibility(visible = isRecording && isConnected) {
                        RecordingIndicator()
                    }

                    if (isVoiceMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)

                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "VOICE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Mode toggle button
            IconButton(onClick = onToggleMode) {
                Icon(
                    imageVector = if (isVoiceMode) Icons.Outlined.Chat else Icons.Outlined.Mic,
                    contentDescription = if (isVoiceMode) "Switch to chat" else "Switch to voice"
                )
            }

            // Beta badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "BETA",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    isActive: Boolean,
    isConnecting: Boolean,
    label: String
) {
    val animatedColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFF4CAF50)
            isConnecting -> MaterialTheme.colorScheme.primary
            else -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(300),
        label = "status_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )

        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = alpha))
        )
        Text(
            text = "Recording",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Red
        )
    }
}

@Composable
private fun ModernMessageBubble(
    message: LiveMessage,
    isLastMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
        ) {
            // AI Avatar
            if (!message.isFromUser) {
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message bubble
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 20.dp
                ),
                color = if (message.isFromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shadowElevation = if (isLastMessage) 2.dp else 1.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // User indicator
            if (message.isFromUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 40.dp, top = 8.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing_$index")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                ),
                label = "typing_offset_$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { translationY = offsetY }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    isConnected: Boolean,
    isRecording: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input field
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            "Type a message...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    enabled = isConnected,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }

            // Action buttons
            AnimatedContent(
                targetState = inputText.isNotEmpty(),
                transitionSpec = {
                    scaleIn() + fadeIn() with scaleOut() + fadeOut()
                },
                label = "input_actions"
            ) { hasText ->
                if (hasText) {
                    // Send button
                    FilledIconButton(
                        onClick = onSend,
                        enabled = canSend,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    // Voice button
                    FilledIconButton(
                        onClick = onToggleRecording,
                        enabled = isConnected,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) {
                                Color.Red
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .then(
                                if (isRecording) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = Color.Red.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                } else Modifier
                            )
                    ) {
                        AnimatedContent(
                            targetState = isRecording,
                            transitionSpec = {
                                scaleIn() + fadeIn() with scaleOut() + fadeOut()
                            },
                            label = "mic_icon"
                        ) { recording ->
                            Icon(
                                imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (recording) "Stop recording" else "Start recording",
                                tint = if (recording) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernEmptyState(
    isConnecting: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_scale"
            )

            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isConnecting) "Connecting..." else "Ready to Practice",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (isConnecting) {
                        "Setting up your live practice session"
                    } else {
                        "Start speaking or type a message to begin"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Quick start suggestions
            if (!isConnecting) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "Try saying:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    listOf(
                        "Help me practice my presentation",
                        "Let's do an interview roleplay",
                        "I need to practice small talk"
                    ).forEach { suggestion ->
                        AssistChip(
                            onClick = { /* Handle suggestion */ },
                            label = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.TipsAndUpdates,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(suggestion)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection Issue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }

            TextButton(onClick = onRetry) {
                Text("Retry")
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}