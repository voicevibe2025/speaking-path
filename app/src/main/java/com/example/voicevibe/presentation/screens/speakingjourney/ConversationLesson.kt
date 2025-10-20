package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationLessonScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }
 
    // Conversation source
    val conversation = topic?.conversation ?: emptyList()

 
    // State handled above

    // Playback state
    var currentIndex by remember(conversation) { mutableStateOf(0) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlayingAll by remember { mutableStateOf(false) }

    // Voice selections
    val maleVoiceName = "Puck"
    val femaleVoiceName = "Zephyr"

    // Animation states
    val infiniteTransition = rememberInfiniteTransition()
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Modern color scheme
    val primaryGradient = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFF093FB)
    )
    val secondaryGradient = listOf(
        Color(0xFF4FACFE),
        Color(0xFF00F2FE),
        Color(0xFF43E97B)
    )
    val darkBackground = Color(0xFF0D0D1A)
    val glassSurface = Color(0x1AFFFFFF)
    // Timed overlay removed for ConversationPractice

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
            if (!embedded) {
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
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = topic?.title?.let { "$it: Conversation Lesson" } ?: "Conversation Lesson",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 20.sp,
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
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                topic == null && ui.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }
                topic == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Conversation not available",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                conversation.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No conversation example for this topic",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    val current = conversation.getOrNull(currentIndex)
                    val playing = currentlyPlayingId == current?.text
                    val compact = embedded
                    
                    // Auto-scroll to current phrase
                    val listState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()
                    
                    LaunchedEffect(currentIndex) {
                        if (currentIndex in conversation.indices) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(currentIndex)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (compact) 16.dp else 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress indicator
                        ConversationProgress(
                            currentIndex = currentIndex,
                            totalSteps = conversation.size,
                            modifier = Modifier.padding(top = if (compact) 8.dp else 16.dp),
                            compact = compact
                        )

                        Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))

                        // All conversation phrases list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
                            contentPadding = PaddingValues(vertical = if (compact) 8.dp else 16.dp)
                        ) {
                            itemsIndexed(conversation) { index, turn ->
                                val isCurrent = index == currentIndex
                                val isSpeakerA = turn.speaker.equals("A", ignoreCase = true)
                                ConversationPhraseItem(
                                    text = turn.text,
                                    isCurrent = isCurrent,
                                    isSpeakerA = isSpeakerA,
                                    isPlaying = playing && isCurrent,
                                    onClick = { 
                                        playTurn(index)
                                    },
                                    compact = compact
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))

                        // Modern control panel
                        ModernControlPanel(
                            currentIndex = currentIndex,
                            conversationSize = conversation.size,
                            isPlaying = playing || isPlayingAll,
                            onPrevious = {
                                val newIdx = (currentIndex - 1).coerceAtLeast(0)
                                playTurn(newIdx)
                            },
                            onPlay = { playTurn(currentIndex) },
                            onPlayAll = { playAllFrom(currentIndex) },
                            onStop = {
                                resetPlaybackFlags()
                                viewModel.stopPlayback()
                            },
                            onNext = {
                                val newIdx = (currentIndex + 1).coerceAtMost(conversation.lastIndex)
                                playTurn(newIdx)
                            },
                            modifier = Modifier.padding(bottom = if (compact) 8.dp else 12.dp),
                            compact = compact
                        )

                        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationProgress(
    currentIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val barHeight = if (compact) 3.dp else 4.dp
            for (i in 0 until totalSteps) {
                val isActive = i <= currentIndex
                val width by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.3f,
                    animationSpec = spring(dampingRatio = 0.7f)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) {
                                Color.White.copy(alpha = 0.6f) // White with low opacity
                            } else {
                                Color.White.copy(alpha = 0.15f) // Very transparent white
                            }
                        )
                        .graphicsLayer { scaleX = width }
                )
            }
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
        Text(
            text = "Step ${currentIndex + 1} of $totalSteps",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ConversationPhraseItem(
    text: String,
    isCurrent: Boolean,
    isSpeakerA: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f)
    )
    
    // Speaker-specific colors: purple for A, orange for B
    val textColor = when {
        isCurrent && isSpeakerA -> Color(0xFFB565D8) // Purple/Magenta for Speaker A
        isCurrent && !isSpeakerA -> Color(0xFFFF9800) // Orange for Speaker B
        else -> Color(0xFF4FC3F7) // Cyan for inactive phrases
    }
    
    val fontSize = if (isCurrent) {
        if (compact) 20.sp else 24.sp // Larger for current
    } else {
        if (compact) 14.sp else 16.sp // Smaller for others
    }
    
    val fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 12.dp)
    ) {
        // Show sound wave animation for currently playing phrase
        if (isPlaying && isCurrent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column {
                    SoundWaveAnimation(
                        modifier = Modifier
                            .width(80.dp)
                            .height(if (compact) 16.dp else 20.dp)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
        
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Start,
            lineHeight = if (isCurrent) {
                if (compact) 28.sp else 32.sp
            } else {
                if (compact) 20.sp else 24.sp
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = if (isCurrent) {
                        if (compact) 8.dp else 12.dp
                    } else {
                        if (compact) 4.dp else 6.dp
                    }
                )
        )
    }
}

@Composable
fun ModernSpeakersSection(
    isSpeakerA: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val pulseAnimation = rememberInfiniteTransition()
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarSize = if (compact) 56.dp else 80.dp
        val glowSize = if (compact) 76.dp else 100.dp
        val nameSize = if (compact) 12.sp else 14.sp
        // Speaker A
        SpeakerAvatar(
            imageRes = R.drawable.ic_male_head,
            label = "Alex",
            isActive = isSpeakerA,
            isPlaying = isPlaying && isSpeakerA,
            pulseScale = if (isPlaying && isSpeakerA) pulseScale else 1f,
            glowColor = Color(0xFF667EEA),
            avatarSize = avatarSize,
            glowSize = glowSize,
            nameFontSize = nameSize
        )

        // VS indicator
        Text(
            text = "×",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraLight
        )

        // Speaker B
        SpeakerAvatar(
            imageRes = R.drawable.ic_female_head,
            label = "Sarah",
            isActive = !isSpeakerA,
            isPlaying = isPlaying && !isSpeakerA,
            pulseScale = if (isPlaying && !isSpeakerA) pulseScale else 1f,
            glowColor = Color(0xFF4FACFE),
            avatarSize = avatarSize,
            glowSize = glowSize,
            nameFontSize = nameSize
        )
    }
}

@Composable
fun SpeakerAvatar(
    imageRes: Int,
    label: String,
    isActive: Boolean,
    isPlaying: Boolean,
    pulseScale: Float,
    glowColor: Color,
    avatarSize: Dp = 80.dp,
    glowSize: Dp = 100.dp,
    nameFontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { 
            scaleX = scale
            scaleY = scale
        }
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(glowSize)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = 0.3f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            // Avatar
            Surface(
                modifier = Modifier.size(avatarSize),
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
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = nameFontSize
        )
    }
}

@Composable
fun ModernSpeechBubble(
    text: String,
    isSpeakerA: Boolean,
    isPlaying: Boolean,
    primaryGradient: List<Color>,
    secondaryGradient: List<Color>,
    compact: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.7f)
    )
    val waveHeight = if (compact) 20.dp else 30.dp
    val waveGap = if (compact) 8.dp else 10.dp

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
                    .padding(if (compact) 12.dp else 24.dp)
            ) {
                Column {
                    if (isPlaying) {
                        SoundWaveAnimation(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(waveHeight)
                                .padding(bottom = waveGap)
                        )
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = if (compact) 24.sp else 28.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isPlaying) {
                        // Add symmetric space below equal to the wave area above, so text stays vertically centered.
                        Spacer(modifier = Modifier.height(waveHeight + waveGap))
                    }
                }
            }
        }
    }
}

@Composable
fun SoundWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
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
            )
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
                            colors = listOf(
                                Color(0xFF667EEA),
                                Color(0xFFF093FB)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernControlPanel(
    currentIndex: Int,
    conversationSize: Int,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onPlayAll: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // Transparent container - no card background
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 10.dp else 16.dp, horizontal = if (compact) 16.dp else 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
            // Previous button
            ModernControlButton(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                size = if (compact) 40.dp else 48.dp
            ) {
                Text(
                    text = "‹",
                    color = Color.White,
                    fontSize = if (compact) 32.sp else 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Play button: single tap toggles stop vs Play All from start
            Box(
                modifier = Modifier.clickable(
                    onClick = { if (isPlaying) onStop() else onPlayAll() }
                )
            ) {
                ModernPlayButton(
                    isPlaying = isPlaying,
                    size = if (compact) 56.dp else 72.dp,
                    iconSize = if (compact) 28.dp else 36.dp
                )
            }

            // Next button
            ModernControlButton(
                onClick = onNext,
                enabled = currentIndex < conversationSize - 1,
                size = if (compact) 40.dp else 48.dp
            ) {
                Text(
                    text = "›",
                    color = Color.White,
                    fontSize = if (compact) 32.sp else 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
}

@Composable
fun ModernControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun ModernPlayButton(
    isPlaying: Boolean,
    size: Dp = 72.dp,
    iconSize: Dp = 36.dp
) {
    // Glassmorphism effect
    Box(
        modifier = Modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.Forum,
            contentDescription = if (isPlaying) "Stop" else "Start Conversation",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

fun DrawScope.drawAnimatedGradient(offset: Float) {
    // Create an animated linear gradient that shifts
    val colors = listOf(
        Color(0xFF0D0D1A),
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460),
        Color(0xFF1A1A2E),
        Color(0xFF0D0D1A)
    )
    
    val gradientBrush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f + offset * 2, 0f),
        end = Offset(size.width + offset * 2, size.height)
    )
    
    drawRect(gradientBrush)
    
    // Dark overlay for better readability
    drawRect(
        color = Color.Black.copy(alpha = 0.4f),
        size = size
    )
}