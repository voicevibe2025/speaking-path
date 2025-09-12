package com.example.voicevibe.presentation.screens.practice.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeWithAIScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit = { _ -> },
    onNavigateToTopicPractice: () -> Unit = {},
    viewModel: PracticeWithAIViewModel = hiltViewModel()
) {
    var showPracticeSelection by remember { mutableStateOf(true) }
    var showComingSoon by remember { mutableStateOf(false) }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1a1a2e),
            Color(0xFF16213e),
            Color(0xFF0f3460)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Practice with AI",
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
            when {
                showPracticeSelection -> {
                    PracticeSelectionScreen(
                        onTopicPracticeSelected = {
                            onNavigateToTopicPractice()
                        },
                        onFreePracticeSelected = {
                            showPracticeSelection = false
                            showComingSoon = false
                        }
                    )
                }
                showComingSoon -> {
                    ComingSoonScreen()
                }
                else -> {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PracticeSelectionScreen(
    onTopicPracticeSelected: () -> Unit,
    onFreePracticeSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1B3A),
                        Color(0xFF2D2E4F),
                        Color(0xFF1A1B3A)
                    )
                )
            )
    ) {
        // Decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    radius = 300f
                ),
                radius = 300f,
                center = Offset(x = size.width * 0.1f, y = size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B82F6).copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    radius = 250f
                ),
                radius = 250f,
                center = Offset(x = size.width * 0.9f, y = size.height * 0.7f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Choose Your",
                    fontSize = 18.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Practice Mode",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Practice Cards
            PracticeCard(
                title = "Topic Practice",
                description = "Master specific topics with guided exercises",
                icon = "📚",
                gradientColors = listOf(
                    Color(0xFF667EEA),
                    Color(0xFF764BA2)
                ),
                onClick = onTopicPracticeSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            PracticeCard(
                title = "Free Practice",
                description = "Explore and practice at your own pace",
                icon = "🎯",
                gradientColors = listOf(
                    Color(0xFF3B82F6),
                    Color(0xFF06B6D4)
                ),
                onClick = onFreePracticeSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeCard(
    title: String,
    description: String,
    icon: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .height(140.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
                
                // Icon container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 32.sp
                    )
                }
            }

            // Subtle glow effect - removed the problematic toPx() call
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = size.minDimension / 2
                    ),
                    radius = size.minDimension / 2,
                    center = center
                )
            }
        }
    }
}

@Composable
fun ComingSoonScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Coming Soon!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

@Composable
fun ChatScreen(viewModel: PracticeWithAIViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var lastSpokenIndex by remember { mutableStateOf(-1) }

    // Auto-play latest AI message when voice mode is ON
    LaunchedEffect(uiState.messages.size, uiState.aiVoiceMode) {
        if (!uiState.aiVoiceMode) return@LaunchedEffect
        val idx = uiState.messages.lastIndex
        if (idx <= lastSpokenIndex || idx < 0) return@LaunchedEffect
        val msg = uiState.messages[idx]
        if (!msg.isFromUser && msg.text.isNotBlank() && !msg.text.contains("Vivi is typing")) {
            // Count activity towards streak
            sjVM.markSpeakingActivity()
            sjVM.speakWithBackendTts(
                text = msg.text,
                voiceName = "Leda", // Vivi default voice (youthful, higher pitch)
                onStart = { lastSpokenIndex = idx },
                onDone = {},
                onError = { _ -> lastSpokenIndex = idx }
            )
        }
    }

    // Stop playback when leaving the screen
    DisposableEffect(Unit) {
        onDispose { sjVM.stopPlayback() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = true
        ) {
            itemsIndexed(uiState.messages.reversed()) { reversedIndex, reversedMsg ->
                val originalIdx = uiState.messages.lastIndex - reversedIndex
                val isAiMsg = !reversedMsg.isFromUser
                val shouldMask = uiState.aiVoiceMode && isAiMsg && originalIdx > lastSpokenIndex
                val displayed = if (shouldMask) {
                    ChatMessage(text = "Vivi is speaking…", isFromUser = false)
                } else reversedMsg
                ChatMessageItem(displayed)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message", color = Color(0xFF78909C)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color(0xFF37474F),
                    cursorColor = Color(0xFF64B5F6),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            // AI Voice toggle
            IconToggleButton(
                checked = uiState.aiVoiceMode,
                onCheckedChange = { viewModel.setAiVoiceMode(it) }
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = if (uiState.aiVoiceMode) "AI voice on" else "AI voice off",
                    tint = if (uiState.aiVoiceMode) Color(0xFF64B5F6) else Color(0xFFB0BEC5)
                )
            }
            // Mic record button
            IconButton(
                onClick = {
                    if (uiState.isRecording) viewModel.stopRecordingAndTranscribe(context)
                    else viewModel.startVoiceRecording(context)
                },
                enabled = !uiState.isLoading
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = if (uiState.isRecording) "Stop recording" else "Start recording",
                    tint = if (uiState.isRecording) Color(0xFFE57373) else Color.White
                )
            }
            Button(
                onClick = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                enabled = !uiState.isLoading && messageText.isNotBlank(),
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        val shape = RoundedCornerShape(16.dp)
        if (message.isFromUser) {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .shadow(4.dp, shape)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF667eea),
                                    Color(0xFF764ba2)
                                )
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.text,
                        color = Color.White
                    )
                }
            }
        } else {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
