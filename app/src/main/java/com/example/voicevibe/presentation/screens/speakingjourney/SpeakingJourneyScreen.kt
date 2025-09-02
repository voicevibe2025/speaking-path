package com.example.voicevibe.presentation.screens.speakingjourney

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.data.repository.GamificationProfile
import com.example.voicevibe.presentation.screens.profile.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.Locale

enum class Stage { MATERIAL, PRACTICE }

data class ConversationTurn(
    val speaker: String,
    val text: String
)

data class PhraseProgress(
    val currentPhraseIndex: Int,
    val completedPhrases: List<Int>,
    val totalPhrases: Int,
    val isAllPhrasesCompleted: Boolean
)

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val material: List<String>,
    val conversation: List<ConversationTurn>,
    val phraseProgress: PhraseProgress?,
    val unlocked: Boolean,
    val completed: Boolean
)

data class UserProfile(
    val firstVisit: Boolean,
    val lastVisitedTopicId: String?,
    val lastVisitedTopicTitle: String?
)

data class SpeakingJourneyUiState(
    val topics: List<Topic>,
    val userProfile: UserProfile? = null,
    val gamificationProfile: GamificationProfile? = null,
    val selectedTopicIdx: Int = 0,
    val stage: Stage = Stage.MATERIAL,
    val showWelcome: Boolean = false,
    val phraseRecordingState: PhraseRecordingState = PhraseRecordingState.IDLE,
    val phraseSubmissionResult: PhraseSubmissionResultUi? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTopicTranscripts: List<PhraseTranscriptEntry> = emptyList()
)

enum class PhraseRecordingState { IDLE, RECORDING, PROCESSING }

data class PhraseSubmissionResultUi(
    val success: Boolean,
    val accuracy: Float,
    val transcription: String,
    val feedback: String,
    val nextPhraseIndex: Int?,
    val topicCompleted: Boolean,
    val xpAwarded: Int = 0
)

data class PhraseTranscriptEntry(
    val index: Int,
    val text: String,
    val audioPath: String,
    val accuracy: Float,
    val timestamp: Long
)

/**
 * Composable for the Speaking Journey screen.
 *
 * This screen is the main entry point for users to explore the speaking journey.
 * It displays a gamified topic selector, shows a welcome screen if needed,
 * and renders the content for the selected topic.
 *
 * @param onNavigateBack Callback for when the user taps the back button
 * @see [SpeakingJourneyViewModel] for the state and actions used by this composable
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SpeakingJourneyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Init TextToSpeech (Android TTS as a fallback; can be replaced by ElevenLabs streaming)
    val tts = remember(context) {
        var ref: TextToSpeech? = null
        val created = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Default to US English; can be adjusted per user locale
                try {
                    ref?.language = Locale.US
                } catch (_: Throwable) { /* ignore */ }
            }
        }
        ref = created
        created
    }
    androidx.compose.runtime.DisposableEffect(tts) {
        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Throwable) { /* ignore */ }
        }
    }

    fun speak(text: String) {
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.currentTimeMillis()}")
        } catch (_: Throwable) { /* ignore for now */ }
    }

    // Observe preferred voice id from Settings (to be used when ElevenLabs is wired)
    val settingsVM: SettingsViewModel = hiltViewModel()
    val preferredVoiceId = settingsVM.ttsVoiceId.value

    // ViewModel state
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    LaunchedEffect(ui.selectedTopicIdx, ui.topics) {
        viewModel.loadTranscriptsForCurrentTopic(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1B3A), // Dark purple-blue
                        Color(0xFF2D2F5B), // Lighter purple-blue
                        Color(0xFF1A1B3A)  // Back to dark for depth
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Animated background particles for engagement
        AnimatedBackgroundParticles()
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MinimalTopBar(
                    onNavigateBack = onNavigateBack,
                    currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
                )
            }
        ) { innerPadding: PaddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Show welcome screen if needed
                if (ui.showWelcome) {
                    ModernWelcomeScreen(
                        userProfile = ui.userProfile,
                        currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx),
                        onDismiss = { viewModel.dismissWelcome() }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Compact topic selector at the top
                        CompactTopicSelector(
                            topics = ui.topics,
                            selectedTopicIdx = ui.selectedTopicIdx,
                            onTopicSelect = { idx -> 
                                if (ui.topics.getOrNull(idx)?.unlocked == true) 
                                    viewModel.selectTopic(idx) 
                            }
                        )
                        
                        // Progress and stats bar
                        val currentTopic = ui.topics.getOrNull(ui.selectedTopicIdx)
                        currentTopic?.let { topic ->
                            val phraseProgress = topic.phraseProgress ?: PhraseProgress(
                                currentPhraseIndex = 0,
                                completedPhrases = emptyList(),
                                totalPhrases = topic.material.size,
                                isAllPhrasesCompleted = false
                            )
                            GamificationStatsBar(ui.gamificationProfile, phraseProgress)
                        }
                        
                        // Loading/Error states
                        if (ui.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ModernLoadingIndicator()
                            }
                        }
                        
                        ui.error?.let { err ->
                            ErrorCard(
                                error = err,
                                onRetry = { viewModel.reloadTopics() }
                            )
                        }
                        
                        // Main Hero Content Area
                        currentTopic?.let { topic ->
                            if (topic.material.isNotEmpty() && topic.phraseProgress != null) {
                                HeroPhraseCard(
                                    material = topic.material,
                                    phraseProgress = topic.phraseProgress,
                                    onSpeak = ::speak,
                                    recordingState = ui.phraseRecordingState,
                                    onStartRecording = { viewModel.startPhraseRecording(context) },
                                    onStopRecording = { viewModel.stopPhraseRecording(context) },
                                    onDismissResult = viewModel::dismissPhraseResult,
                                    transcripts = ui.currentTopicTranscripts,
                                    onPlayTranscript = { path -> viewModel.playUserRecording(path) }
                                )
                            }
                            
                            // Conversation example in a modern card
                            if (topic.conversation.isNotEmpty()) {
                                ModernConversationCard(
                                    conversation = topic.conversation,
                                    onSpeak = ::speak
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // Results and feedback modal
                ui.phraseSubmissionResult?.let { result ->
                    AlertDialog(
                        onDismissRequest = viewModel::dismissPhraseResult,
                        properties = DialogProperties(usePlatformDefaultWidth = false),
                        content = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), // Padding around the card
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedResultCard(
                                    result = result,
                                    onDismiss = viewModel::dismissPhraseResult
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalTopBar(
    onNavigateBack: () -> Unit,
    currentTopic: Topic?
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White
                )
                Text("Speaking Quest", fontWeight = FontWeight.Bold)
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
private fun CompactTopicSelector(
    topics: List<Topic>,
    selectedTopicIdx: Int,
    onTopicSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Choose Your Topic",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(topics) { index, topic ->
                ModernTopicCard(
                    topic = topic,
                    isSelected = index == selectedTopicIdx,
                    onClick = { onTopicSelect(index) },
                    index = index
                )
            }
        }
        
        // Selected topic details
        topics.getOrNull(selectedTopicIdx)?.let { selectedTopic ->
            Spacer(modifier = Modifier.height(12.dp))
            SelectedTopicDetails(selectedTopic)
        }
    }
}

@Composable
private fun ModernTopicCard(
    topic: Topic,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(200)
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(200)
    )
    
    val topicIcons = remember {
        listOf(
            Icons.Default.School,
            Icons.Default.Work,
            Icons.Default.Restaurant,
            Icons.Default.DirectionsBus,
            Icons.Default.ShoppingCart,
            Icons.Default.HealthAndSafety,
            Icons.Default.SportsSoccer,
            Icons.Default.MusicNote
        )
    }
    
    val topicColors = remember {
        listOf(
            Color(0xFF6C63FF),
            Color(0xFFFF6B6B),
            Color(0xFF4ECDC4),
            Color(0xFFFFBE0B),
            Color(0xFFFB5607),
            Color(0xFF3A86FF),
            Color(0xFF8338EC),
            Color(0xFFFF006E)
        )
    }
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(borderWidth, MaterialTheme.colorScheme.primary)
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(
                                topicColors[index % topicColors.size].copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            listOf(
                                topicColors[index % topicColors.size].copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface
                            )
                        }
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topic Icon
                Surface(
                    shape = CircleShape,
                    color = topicColors[index % topicColors.size].copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = topicIcons[index % topicIcons.size],
                            contentDescription = null,
                            tint = topicColors[index % topicColors.size],
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                // Topic Title
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Progress indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val completedCount = topic.phraseProgress?.completedPhrases?.size ?: 0
                    val progress = completedCount.toFloat() / topic.material.size.coerceAtLeast(1)
                    
                    CircularProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = if (progress >= 1f) {
                            Color(0xFF4CAF50)
                        } else {
                            topicColors[index % topicColors.size]
                        }
                    )
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                // Selected indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedTopicDetails(topic: Topic) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = shimmerAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Current Topic",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty badge
                val difficulty = when {
                    topic.material.size < 6 -> "Beginner"
                    topic.material.size < 12 -> "Intermediate"
                    else -> "Advanced"
                }
                val badgeColor = when (difficulty) {
                    "Beginner" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    "Intermediate" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    else -> Color(0xFFF44336).copy(alpha = 0.2f)
                }
                val badgeTextColor = when (difficulty) {
                    "Beginner" -> Color(0xFF2E7D32)
                    "Intermediate" -> Color(0xFFE65100)
                    else -> Color(0xFFC62828)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = difficulty,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
                
                // Phrases count
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${topic.material.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "phrases",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GamificationStatsBar(gamificationProfile: GamificationProfile?, phraseProgress: PhraseProgress) {
    if (gamificationProfile == null) {
        // You might want a loading indicator here
        Row(modifier = Modifier.height(48.dp)) { Spacer(Modifier.fillMaxSize()) }
        return
    }

    val streakDays = gamificationProfile.streak
    val userXP = gamificationProfile.xp
    val userLevel = gamificationProfile.level
    val levelProgress = (userXP % 500) / 500f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row - Level and XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "$userLevel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Level $userLevel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$userXP XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Streak indicator
                StreakIndicator(streakDays)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Level progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next level",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(levelProgress * 500).toInt()}/500 XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = levelProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${phraseProgress.completedPhrases.size}",
                    label = "Completed"
                )
                StatItem(
                    icon = Icons.Default.Star,
                    value = "0", // Placeholder, logic for 'perfect' is not defined
                    label = "Perfect"
                )
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = "--%", // Placeholder, logic for accuracy is not defined
                    label = "Accuracy"
                )
            }
            
            // Achievement badges (if any)
            if (phraseProgress.completedPhrases.size >= 1) {
                Spacer(modifier = Modifier.height(12.dp))
                AchievementBadges(phraseProgress)
            }
        }
    }
}

@Composable
private fun StreakIndicator(streakDays: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Streak",
            tint = Color(0xFFFF6B35).copy(alpha = glowAlpha),
            modifier = Modifier.size(28.dp)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$streakDays",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35)
            )
            Text(
                text = "day streak",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AchievementBadges(phraseProgress: PhraseProgress) {
    val achievements = remember(phraseProgress) {
        buildList {
            if (phraseProgress.completedPhrases.size >= 1) add("First Steps" to Icons.Default.DirectionsWalk)
            if (phraseProgress.completedPhrases.size >= 5) add("Rising Star" to Icons.Default.Star)
            if (phraseProgress.completedPhrases.size >= 10) add("Master" to Icons.Default.EmojiEvents)
        }
    }
    
    if (achievements.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(achievements) { (title, icon) ->
                AchievementBadge(title = title, icon = icon)
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    title: String,
    icon: ImageVector
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = shimmerAlpha),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary
            )
        }
    }
}

@Composable
private fun ModernLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(40.dp),
        strokeWidth = 4.dp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HeroPhraseCard(
    material: List<String>,
    phraseProgress: PhraseProgress,
    onSpeak: (String) -> Unit,
    recordingState: PhraseRecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDismissResult: () -> Unit,
    transcripts: List<PhraseTranscriptEntry>,
    onPlayTranscript: (String) -> Unit
) {
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val baseIndex = phraseProgress.currentPhraseIndex.coerceIn(0, material.size - 1)
    val currentPhrase = material.getOrNull(baseIndex)
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val cardScale by animateFloatAsState(
        targetValue = if (recordingState == PhraseRecordingState.RECORDING) 1.05f else 1f,
        animationSpec = tween(300)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicators with gamification
        ProgressIndicatorRow(
            completedPhrases = phraseProgress.completedPhrases,
            totalPhrases = material.size,
            currentIndex = baseIndex
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main Hero Card
        if (currentPhrase != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(cardScale)
            ) {
                // Glowing background effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .offset(y = 4.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                                    Color.Transparent
                                ),
                                radius = 600f
                            )
                        )
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (recordingState == PhraseRecordingState.RECORDING) 16.dp else 8.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Phrase number badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Phrase ${baseIndex + 1} of ${material.size}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Main phrase text
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentPhrase,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 28.sp,
                                        lineHeight = 36.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Play button
                                AnimatedPlayButton(
                                    onClick = { onSpeak(currentPhrase) },
                                    isPlaying = false
                                )
                            }
                            
                            // Recording Section
                            if (!audioPermissionState.status.isGranted) {
                                PermissionRequestCard(
                                    onRequestPermission = {
                                        audioPermissionState.launchPermissionRequest()
                                    }
                                )
                            } else {
                                AnimatedRecordingButton(
                                    recordingState = recordingState,
                                    onStartRecording = onStartRecording,
                                    onStopRecording = onStopRecording
                                )
                            }
                        }
                        
                        // Recording animation overlay
                        if (recordingState == PhraseRecordingState.RECORDING) {
                            RecordingAnimationOverlay()
                        }
                    }
                }
            }
        } else if (phraseProgress.isAllPhrasesCompleted) {
            CompletionCelebrationCard()
        }
        
        
        // Transcripts section
        if (transcripts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            TranscriptPlaybackSection(
                transcripts = transcripts,
                material = material,
                onPlay = { entry -> onPlayTranscript(entry.audioPath) }
            )
        }
    }
}

@Composable
private fun ProgressIndicatorRow(
    completedPhrases: List<Int>,
    totalPhrases: Int,
    currentIndex: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalPhrases) {
            val isCompleted = completedPhrases.contains(i)
            val isCurrent = i == currentIndex
            
            val scale by animateFloatAsState(
                targetValue = when {
                    isCurrent -> 1.3f
                    isCompleted -> 1f
                    else -> 0.8f
                },
                animationSpec = tween(300)
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun AnimatedPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .scale(if (!isPlaying) pulseScale else 1f),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Play phrase",
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun AnimatedRecordingButton(
    recordingState: PhraseRecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val recordingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val buttonScale = when (recordingState) {
        PhraseRecordingState.RECORDING -> recordingScale
        PhraseRecordingState.PROCESSING -> 0.95f
        else -> 1f
    }
    
    Button(
        onClick = {
            when (recordingState) {
                PhraseRecordingState.IDLE -> onStartRecording()
                PhraseRecordingState.RECORDING -> onStopRecording()
                PhraseRecordingState.PROCESSING -> Unit
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(buttonScale),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (recordingState) {
                PhraseRecordingState.RECORDING -> MaterialTheme.colorScheme.error
                PhraseRecordingState.PROCESSING -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (recordingState == PhraseRecordingState.RECORDING) 8.dp else 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (recordingState) {
                PhraseRecordingState.PROCESSING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PhraseRecordingState.RECORDING -> {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (recordingState) {
                    PhraseRecordingState.RECORDING -> "Stop Recording"
                    PhraseRecordingState.PROCESSING -> "Processing..."
                    else -> "Start Recording"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecordingAnimationOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.error.copy(alpha = alpha)
            )
    )
}

@Composable
private fun ModernConversationCard(
    conversation: List<ConversationTurn>,
    onSpeak: (String) -> Unit
) {
    val combined = conversation.joinToString(separator = "\n") { "${it.speaker}: ${it.text}" }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversation Example",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onSpeak(combined) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Play conversation")
                }
            }
            Text(
                text = combined,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun TranscriptPlaybackSection(
    transcripts: List<PhraseTranscriptEntry>,
    material: List<String>,
    onPlay: (PhraseTranscriptEntry) -> Unit
) {
    var playingIndex by remember { mutableStateOf<Int?>(null) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your Transcript (${transcripts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (transcripts.isEmpty()) {
            Text(
                text = "No recordings yet. Record to see your transcript.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            transcripts.sortedBy { it.index }.forEach { entry ->
                val isWeak = entry.accuracy < 85f
                val isPlaying = entry.index == playingIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable {
                            playingIndex = entry.index
                            onPlay(entry)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isPlaying -> MaterialTheme.colorScheme.primaryContainer
                            isWeak -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    border = if (isPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Phrase ${entry.index + 1}: ${material.getOrNull(entry.index) ?: ""}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = {
                                playingIndex = entry.index
                                onPlay(entry)
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play recording")
                            }
                        }
                        Text(
                            text = "\"${entry.text}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isPlaying -> MaterialTheme.colorScheme.onPrimaryContainer
                                isWeak -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val now = System.currentTimeMillis()
                        val elapsed = (now - entry.timestamp).coerceAtLeast(0L)
                        val timeAgo = when {
                            elapsed < 60_000L -> "${elapsed / 1_000}s ago"
                            elapsed < 3_600_000L -> "${elapsed / 60_000}m ago"
                            elapsed < 86_400_000L -> "${elapsed / 3_600_000}h ago"
                            else -> "${elapsed / 86_400_000}d ago"
                        }
                        Text(
                            text = "Recorded $timeAgo",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isPlaying -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                isWeak -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (entry.accuracy > 0f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = (entry.accuracy / 100f).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Accuracy: ${"%.1f".format(Locale.US, entry.accuracy)}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBackgroundParticles() {
    val particles = remember { List(15) { Particle() } }
    val infiniteTransition = rememberInfiniteTransition()
    var canvasHeight by remember { mutableStateOf(0) }
    
    // Precompute animated values outside draw scope
    val animatedStates = particles.mapIndexed { index, particle ->
        val targetX = remember(particle.x) { particle.x + ((-20..20).random().toFloat()) }
        val animatedY by infiniteTransition.animateFloat(
            initialValue = canvasHeight.toFloat() + 100f,
            targetValue = -100f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = particle.duration,
                    delayMillis = index * 500,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
        val animatedX by infiniteTransition.animateFloat(
            initialValue = particle.x,
            targetValue = targetX,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = particle.maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = index * 200
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
        Triple(animatedX, animatedY, alpha)
    }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasHeight = it.height }
    ) {
        animatedStates.forEachIndexed { index, (animatedX, animatedY, alpha) ->
            val particle = particles[index]
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.radius,
                center = Offset(animatedX, animatedY)
            )
            // Glowing effect
            drawCircle(
                color = particle.color.copy(alpha = alpha * 0.3f),
                radius = particle.radius * 2,
                center = Offset(animatedX, animatedY)
            )
        }
    }
}

data class Particle(
    val x: Float = (0..1000).random().toFloat(),
    val radius: Float = (4..12).random().toFloat(),
    val duration: Int = (15000..25000).random(),
    val color: Color = listOf(
        Color(0xFF6C63FF),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFD700),
        Color(0xFFFB5607),
        Color(0xFF3A86FF),
        Color(0xFF8338EC),
        Color(0xFFFF006E)
    ).random(),
    val maxAlpha: Float = (0.3f..0.7f).random()
)

private fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + kotlin.random.Random.nextFloat() * (endInclusive - start)
}

@Composable
private fun ModernWelcomeScreen(
    userProfile: UserProfile?,
    currentTopic: Topic?,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 4 seconds, but user can tap to skip
    LaunchedEffect(Unit) {
        delay(4000)
        onDismiss()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clickable { onDismiss() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userProfile?.firstVisit == true) {
            // First-time user welcome
            Text(
                text = "Welcome to Speaking Journey",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            currentTopic?.let { topic ->
                Text(
                    text = "Lesson ${topic.title.substringBefore(':').takeIf { it.contains("Lesson") } ?: "1"}: ${topic.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "In this lesson, you will learn ${topic.description.lowercase()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Returning user welcome
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!userProfile?.lastVisitedTopicTitle.isNullOrBlank()) {
                Text(
                    text = "Last time you were learning about:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userProfile?.lastVisitedTopicTitle ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Want to continue?",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "Ready to continue your journey?",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Tap anywhere to continue",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecordingResultCard(
    result: PhraseSubmissionResultUi,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                val onColor = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (result.success) Icons.Outlined.EmojiEvents else Icons.Default.Mic,
                        contentDescription = null,
                        tint = onColor
                    )
                    Text(
                        text = if (result.success) "✅ Great job!" else "❌ Try again",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onColor
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = onColor
                    )
                }
            }
            if (result.accuracy > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (result.accuracy / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Accuracy: ${"%.1f".format(Locale.US, result.accuracy)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (result.transcription.isNotBlank()) {
                Text(
                    text = "You said: \"${result.transcription}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (result.feedback.isNotBlank()) {
                Text(
                    text = result.feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun AnimatedResultCard(
    result: PhraseSubmissionResultUi,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val isSuccess = result.accuracy >= 85
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = if (isSuccess) 0.5f else 0.3f,
        targetValue = if (isSuccess) 1f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            // Success particles background
            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = shimmerAlpha * 0.3f),
                                    Color.Transparent
                                ),
                                radius = 500f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Result Icon with animation
                val iconRotation by infiniteTransition.animateFloat(
                    initialValue = if (isSuccess) -5f else 0f,
                    targetValue = if (isSuccess) 5f else 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "iconRotation"
                )

                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(iconRotation),
                    tint = if (isSuccess) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (result.success) "Phrase Passed!" else "Try Again",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    if (result.success && result.xpAwarded > 0) {
                        Text(
                            text = "+${result.xpAwarded} XP",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107) // Amber color for XP
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Accuracy Score with animation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Accuracy: ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSuccess) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = "${result.accuracy.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                // XP Earned (if success)
                if (isSuccess) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "+50 XP",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                        }
                    }
                }

                // Transcript
                if (result.transcription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "You said:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${result.transcription}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Feedback
                if (result.feedback.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = result.feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (isSuccess) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text(
                        text = if (isSuccess) "Continue" else "Try Again",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionCelebrationCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .scale(scale)
            .rotate(rotation),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primaryContainer,
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                        ),
                        radius = 800f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFFFFD700)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Congratulations!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You've completed all phrases!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Rewards row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RewardItem(icon = Icons.Default.Star, text = "+500 XP", color = Color(0xFFFFD700))
                    RewardItem(icon = Icons.Default.LocalFireDepartment, text = "Streak +1", color = Color(0xFFFF6B35))
                    RewardItem(icon = Icons.Default.CheckCircle, text = "100%", color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun RewardItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PermissionRequestCard(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Microphone Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "To record your pronunciation, we need access to your microphone.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun ModernTranscriptSection(
    transcripts: List<PhraseTranscriptEntry>,
    material: List<String>,
    onPlay: (PhraseTranscriptEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your Transcript (${transcripts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (transcripts.isEmpty()) {
            Text(
                text = "No recordings yet. Record to see your transcript.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            transcripts.sortedBy { it.index }.forEach { entry ->
                val isWeak = entry.accuracy < 85f
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { onPlay(entry) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWeak) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Phrase ${entry.index + 1}: ${material.getOrNull(entry.index) ?: ""}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { onPlay(entry) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play recording")
                            }
                        }
                        Text(
                            text = "\"${entry.text}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isWeak) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val now = System.currentTimeMillis()
                        val elapsed = (now - entry.timestamp).coerceAtLeast(0L)
                        val timeAgo = when {
                            elapsed < 60_000L -> "${elapsed / 1_000}s ago"
                            elapsed < 3_600_000L -> "${elapsed / 60_000}m ago"
                            elapsed < 86_400_000L -> "${elapsed / 3_600_000}h ago"
                            else -> "${elapsed / 86_400_000}d ago"
                        }
                        Text(
                            text = "Recorded $timeAgo",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWeak) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entry.accuracy > 0f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = (entry.accuracy / 100f).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Accuracy: ${"%.1f".format(Locale.US, entry.accuracy)}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
