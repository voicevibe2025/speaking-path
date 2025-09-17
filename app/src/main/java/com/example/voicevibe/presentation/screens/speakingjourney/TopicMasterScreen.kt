package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.voicevibe.R
import com.example.voicevibe.presentation.components.*
import android.media.MediaPlayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

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
    val practiceScores = topic?.practiceScores

    // Celebration state: show confetti and play win sound when unlock condition is met
    val context = LocalContext.current
    var showCelebration by remember { mutableStateOf(false) }
    var celebrationTriggered by rememberSaveable(topicId) { mutableStateOf(false) }
    LaunchedEffect(practiceScores?.meetsRequirement) {
        val meets = practiceScores?.meetsRequirement == true
        if (meets && !celebrationTriggered) {
            showCelebration = true
            try {
                val mp = MediaPlayer.create(context, R.raw.win)
                mp?.setOnCompletionListener { player ->
                    try { player.release() } catch (_: Throwable) {}
                }
                mp?.start()
            } catch (_: Throwable) {}
            delay(2200)
            showCelebration = false
            celebrationTriggered = true
        } else if (!meets) {
            // Allow re-trigger if user drops below threshold and then meets it again later
            celebrationTriggered = false
        }
    }

    // Ensure we refresh topics when returning to this screen (e.g., after practices)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadTopics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    onNavigationIconClick = onNavigateBack
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

                        // Progress Summary Section (moved to top)
                        ProgressSummarySection(topicId = topicId)

                        Spacer(modifier = Modifier.height(24.dp))

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

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Celebration overlay on top of everything
        if (showCelebration) {
            ConfettiOverlay()
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
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }
    val practiceScores = topic?.practiceScores
    
    val practiceItems = listOf(
        // Conversation Practice at the top
        PracticeItem(
            title = "Conversation Practice",
            description = "Take turns in real dialogues",
            icon = Icons.Default.School,
            gradient = listOf(Color(0xFFFF006E), Color(0xFF8338EC)),
            score = topic?.conversationScore ?: 0,
            maxScore = 100,
            onClick = { onNavigateToConversation(topicId) }
        ),
        PracticeItem(
            title = "Pronunciation",
            description = "Perfect your accent",
            icon = Icons.Default.Mic,
            gradient = listOf(Color(0xFFFF006E), Color(0xFFFF4081)),
            score = practiceScores?.pronunciation ?: 0,
            maxScore = practiceScores?.maxPronunciation ?: 100,
            onClick = { onNavigateToPronunciationPractice(topicId) }
        ),
        PracticeItem(
            title = "Fluency",
            description = "Speak naturally",
            icon = Icons.Default.RecordVoiceOver,
            gradient = listOf(Color(0xFF00D9FF), Color(0xFF00B4D8)),
            score = practiceScores?.fluency ?: 0,
            maxScore = practiceScores?.maxFluency ?: 100,
            onClick = { onNavigateToFluencyPractice(topicId) }
        ),
        PracticeItem(
            title = "Vocabulary",
            description = "Expand your words",
            icon = Icons.Default.Translate,
            gradient = listOf(Color(0xFFFFBE0B), Color(0xFFFB8500)),
            score = practiceScores?.vocabulary ?: 0,
            maxScore = practiceScores?.maxVocabulary ?: 100,
            onClick = { onNavigateToVocabularyPractice(topicId) }
        ),
        PracticeItem(
            title = "Listening",
            description = "Improve comprehension",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            gradient = listOf(Color(0xFF8338EC), Color(0xFF6C63FF)),
            score = 0, // Not implemented yet
            maxScore = 100,
            onClick = { onNavigateToListeningPractice(topicId) }
        ),
        PracticeItem(
            title = "Grammar",
            description = "Master the rules",
            icon = Icons.Default.Spellcheck,
            gradient = listOf(Color(0xFF06FFA5), Color(0xFF00C896)),
            score = 0, // Not implemented yet
            maxScore = 100,
            onClick = { onNavigateToGrammarPractice(topicId) }
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
fun ProgressSummarySection(topicId: String) {
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }
    val practiceScores = topic?.practiceScores
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Topic Mastery Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (practiceScores != null) {
                // Show current scores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreIndicator(
                        title = "Pronunciation",
                        score = practiceScores.pronunciation,
                        color = Color(0xFFFF006E)
                    )
                    ScoreIndicator(
                        title = "Fluency", 
                        score = practiceScores.fluency,
                        color = Color(0xFF00D9FF)
                    )
                    ScoreIndicator(
                        title = "Vocabulary",
                        score = practiceScores.vocabulary,
                        color = Color(0xFFFFBE0B)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Average score and progress
                val hasAllScores = practiceScores.pronunciation > 0 && 
                                   practiceScores.fluency > 0 && 
                                   practiceScores.vocabulary > 0
                
                if (hasAllScores) {
                    Text(
                        text = "Overall Progress: ${practiceScores.average}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (practiceScores.average / 100f).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (practiceScores.meetsRequirement) {
                                        Color(0xFF06FFA5)
                                    } else {
                                        Color(0xFFFFBE0B)
                                    }
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (practiceScores.meetsRequirement) {
                            "🎉 Congratulations! You've unlocked the next topic!"
                        } else {
                            "Unlock rule: reach at least 75% in each practice"
                        },
                        fontSize = 14.sp,
                        color = if (practiceScores.meetsRequirement) {
                            Color(0xFF06FFA5)
                        } else {
                            Color.White.copy(alpha = 0.7f)
                        },
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Complete all 3 practices to see your progress",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Start practicing to track your progress",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ScoreIndicator(
    title: String,
    score: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            border = BorderStroke(2.dp, color.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (score > 0) "$score" else "—",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (score > 0) color else Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            
                            // Score display
                            if (item.score > 0) {
                                Card(
                                    modifier = Modifier
                                        .size(40.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${item.score}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "—",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress bar indicating current score percentage of max score for this practice
                        val max = if (item.maxScore > 0) item.maxScore else 100
                        val progress = (item.score.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            if (progress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Brush.horizontalGradient(colors = item.gradient))
                                )
                            }
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
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
    val score: Int,
    val maxScore: Int,
    val onClick: () -> Unit
)

@Composable
private fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    durationMillis: Int = 2200,
    particleCount: Int = 60
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val centerX = widthPx / 2f
        val centerY = heightPx / 2f

        // Precompute particle specs so they stay stable during the animation
        val colors = remember {
            listOf(
                Color(0xFF00D9FF), // Cyan
                Color(0xFF8338EC), // Indigo
                Color(0xFFFF006E), // Fuchsia
                Color(0xFFFFBE0B), // Yellow
                Color(0xFF06FFA5)  // Mint
            )
        }
        val random = remember { kotlin.random.Random(1234) }
        data class ParticleSpec(
            val initialVelocityX: Float,
            val initialVelocityY: Float,
            val sizePx: Float,
            val color: Color,
            val rotationSpeed: Float,
            val delayFrac: Float,
            val isCircle: Boolean,
            val airResistance: Float
        )
        val specs = remember(particleCount, widthPx, heightPx) {
            List(particleCount) {
                // Create explosion effect - particles shoot out in all directions from center
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val speed = 200f + random.nextFloat() * 400f // Explosion speed
                ParticleSpec(
                    initialVelocityX = kotlin.math.cos(angle) * speed,
                    initialVelocityY = kotlin.math.sin(angle) * speed - 200f, // Bias upward for explosion
                    sizePx = (with(density) { (6 + random.nextInt(10)).dp.toPx() }),
                    color = colors[random.nextInt(colors.size)],
                    rotationSpeed = (random.nextFloat() - 0.5f) * 720f, // -360 to +360 degrees per second
                    delayFrac = random.nextFloat() * 0.3f, // Smaller delay for tighter explosion
                    isCircle = random.nextBoolean(),
                    airResistance = 0.98f + random.nextFloat() * 0.015f // 0.98 to 0.995
                )
            }
        }

        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = progress.value
            val gravity = 800f // Pixels per second squared
            val timeScale = durationMillis / 1000f // Convert to seconds
            
            specs.forEach { spec ->
                val localT = ((p * 1.2f) - spec.delayFrac).coerceIn(0f, 1f)
                if (localT > 0f) {
                    val time = localT * timeScale
                    
                    // Physics simulation: explosion + gravity + air resistance
                    var velX = spec.initialVelocityX
                    var velY = spec.initialVelocityY
                    var x = centerX
                    var y = centerY
                    
                    // Simulate movement with small time steps for accuracy
                    val steps = (time * 60f).toInt() // 60 FPS simulation
                    val dt = if (steps > 0) time / steps else 0f
                    
                    repeat(steps) {
                        // Apply air resistance
                        velX *= spec.airResistance
                        velY *= spec.airResistance
                        
                        // Apply gravity
                        velY += gravity * dt
                        
                        // Update position
                        x += velX * dt
                        y += velY * dt
                    }
                    
                    // Fade out over time, more aggressive fade near the end
                    val alpha = when {
                        localT < 0.7f -> 1f
                        else -> ((1f - localT) / 0.3f).coerceIn(0f, 1f)
                    }
                    
                    // Only draw if particle is visible and within reasonable bounds
                    if (alpha > 0f && x > -spec.sizePx && x < widthPx + spec.sizePx && 
                        y > -spec.sizePx && y < heightPx + spec.sizePx * 2f) {
                        
                        val rotation = spec.rotationSpeed * time
                        
                        if (spec.isCircle) {
                            drawCircle(
                                color = spec.color.copy(alpha = alpha),
                                radius = spec.sizePx / 2f,
                                center = Offset(x, y)
                            )
                        } else {
                            withTransform({ 
                                rotate(degrees = rotation, pivot = Offset(x, y)) 
                            }) {
                                drawRect(
                                    color = spec.color.copy(alpha = alpha),
                                    topLeft = Offset(x - spec.sizePx / 2f, y - spec.sizePx / 2f),
                                    size = Size(spec.sizePx, spec.sizePx)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
