package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicMasterScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPronunciationPractice: (String) -> Unit,
    onNavigateToFluencyPractice: (String) -> Unit,
    onNavigateToVocabularyPractice: (String) -> Unit,
    onNavigateToListeningPractice: (String) -> Unit,
    onNavigateToGrammarPractice: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit
) {
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }
    
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(topic) {
        delay(100)
        showContent = true
    }
    
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedBackground(animatedOffset)
        
        // Floating particles
        FloatingParticles()
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                ModernTopBar(
                    title = topic?.title ?: "Master Topic",
                    onNavigateBack = onNavigateBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(600)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Hero Section
                        HeroSection()
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Practice Cards Grid
                        PracticeCardsSection(
                            topicId = topicId,
                            onNavigateToPronunciationPractice = onNavigateToPronunciationPractice,
                            onNavigateToFluencyPractice = onNavigateToFluencyPractice,
                            onNavigateToVocabularyPractice = onNavigateToVocabularyPractice,
                            onNavigateToListeningPractice = onNavigateToListeningPractice,
                            onNavigateToGrammarPractice = onNavigateToGrammarPractice,
                            onNavigateToConversation = onNavigateToConversation
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedBackground(animatedOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Create animated gradient mesh
        val colors = listOf(
            Color(0xFF6C63FF),
            Color(0xFF00D9FF),
            Color(0xFFFF006E),
            Color(0xFFFFBE0B),
            Color(0xFF8338EC)
        )
        
        // Draw multiple gradient circles
        colors.forEachIndexed { index, color ->
            val angle = animatedOffset + (index * 72f)
            val radius = width * 0.6f
            val x = width / 2 + cos(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.3f
            val y = height / 2 + sin(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.3f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = radius
                ),
                center = Offset(x, y),
                radius = radius
            )
        }
        
        // Dark overlay for better contrast
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.7f),
                    Color.Black.copy(alpha = 0.6f)
                )
            )
        )
    }
}

@Composable
fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(15) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = -0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (15000..25000).random(),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "particle$index"
            )
            
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -0.1f,
                targetValue = 0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (5000..8000).random(),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "particleX$index"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(
                        x = (offsetX * 100).dp,
                        y = with(LocalDensity.current) { (offsetY * 1200).dp }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((index * 73) % 350).dp,
                            y = ((index * 47) % 200).dp
                        )
                        .size((2..6).random().dp)
                        .clip(CircleShape)
                        .background(
                            Color.White.copy(alpha = Random.nextFloat() * 0.4f + 0.3f)
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6C63FF).copy(alpha = 0.3f),
                                Color(0xFF00D9FF).copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun HeroSection() {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    ),
                    start = Offset(shimmerOffset * 1000f, 0f),
                    end = Offset(shimmerOffset * 1000f + 500f, 0f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6C63FF).copy(alpha = 0.5f),
                        Color(0xFF00D9FF).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated icon
            val pulseAnimation by shimmerTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseAnimation)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6C63FF),
                                Color(0xFF00D9FF)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Master Your Speaking Skills",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose your practice mode and level up",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PracticeCardsSection(
    topicId: String,
    onNavigateToPronunciationPractice: (String) -> Unit,
    onNavigateToFluencyPractice: (String) -> Unit,
    onNavigateToVocabularyPractice: (String) -> Unit,
    onNavigateToListeningPractice: (String) -> Unit,
    onNavigateToGrammarPractice: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit
) {
    val practiceItems = listOf(
        PracticeItem(
            title = "Pronunciation",
            description = "Perfect your accent",
            icon = Icons.Default.Mic,
            gradient = listOf(Color(0xFFFF006E), Color(0xFFFF4081)),
            onClick = { onNavigateToPronunciationPractice(topicId) }
        ),
        PracticeItem(
            title = "Fluency",
            description = "Speak naturally",
            icon = Icons.Default.RecordVoiceOver,
            gradient = listOf(Color(0xFF00D9FF), Color(0xFF00B4D8)),
            onClick = { onNavigateToFluencyPractice(topicId) }
        ),
        PracticeItem(
            title = "Vocabulary",
            description = "Expand your words",
            icon = Icons.Default.Translate,
            gradient = listOf(Color(0xFFFFBE0B), Color(0xFFFB8500)),
            onClick = { onNavigateToVocabularyPractice(topicId) }
        ),
        PracticeItem(
            title = "Listening",
            description = "Improve comprehension",
            icon = Icons.Default.VolumeUp,
            gradient = listOf(Color(0xFF8338EC), Color(0xFF6C63FF)),
            onClick = { onNavigateToListeningPractice(topicId) }
        ),
        PracticeItem(
            title = "Grammar",
            description = "Master the rules",
            icon = Icons.Default.Spellcheck,
            gradient = listOf(Color(0xFF06FFA5), Color(0xFF00C896)),
            onClick = { onNavigateToGrammarPractice(topicId) }
        ),
        PracticeItem(
            title = "Conversation",
            description = "Real dialogues",
            icon = Icons.Default.School,
            gradient = listOf(Color(0xFFFF006E), Color(0xFF8338EC)),
            onClick = { onNavigateToConversation(topicId) }
        )
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        practiceItems.forEachIndexed { index, item ->
            ModernPracticeCard(
                item = item,
                animationDelay = index * 100
            )
        }
    }
}

@Composable
fun ModernPracticeCard(
    item: PracticeItem,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInHorizontally(
            initialOffsetX = { 100 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Card(
            onClick = {
                isPressed = true
                item.onClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .scale(scale)
                .drawBehind {
                    // Glow effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                item.gradient[0].copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.5f
                    )
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        item.gradient[0].copy(alpha = 0.5f),
                        item.gradient[1].copy(alpha = 0.5f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = item.gradient
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = item.description,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class PracticeItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit
)