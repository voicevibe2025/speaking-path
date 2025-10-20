package com.example.voicevibe.presentation.screens.speakingjourney

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlin.math.sqrt
import kotlin.random.Random
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.voicevibe.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.example.voicevibe.presentation.components.ModernTopBar
import com.example.voicevibe.ui.theme.BrandCyan
import com.example.voicevibe.ui.theme.BrandIndigo
import com.example.voicevibe.ui.theme.BrandFuchsia
import com.example.voicevibe.data.remote.api.CoachAnalysisDto
import com.example.voicevibe.presentation.screens.speakingjourney.CoachViewModel

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

data class FluencyProgress(
    val promptsCount: Int,
    val promptScores: List<Int>,
    val totalScore: Int,
    val nextPromptIndex: Int?,
    val completed: Boolean
)

data class PracticeScores(
    val pronunciation: Int,
    val fluency: Int,
    val vocabulary: Int,
    val listening: Int? = null,
    val grammar: Int? = null,
    val average: Float,
    val meetsRequirement: Boolean,
    // Maxima for correct percentage calculations on the client
    val maxPronunciation: Int,
    val maxFluency: Int,
    val maxVocabulary: Int,
    val maxListening: Int? = null,
    val maxGrammar: Int? = null
)

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val material: List<String>,
    val vocabulary: List<String>,
    val conversation: List<ConversationTurn>,
    val fluencyPracticePrompts: List<String>,
    val fluencyProgress: FluencyProgress?,
    val phraseProgress: PhraseProgress?,
    val practiceScores: PracticeScores?,
    val conversationScore: Int? = null,
    val conversationCompleted: Boolean? = null,
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
    val phraseRecordingState: PhraseRecordingState = PhraseRecordingState.IDLE,
    val phraseSubmissionResult: PhraseSubmissionResultUi? = null,
    val conversationRecordingState: PhraseRecordingState = PhraseRecordingState.IDLE,
    val conversationSubmissionResult: ConversationSubmissionResultUi? = null,
    val showConversationCongrats: Boolean = false,
    val conversationTurnScores: Map<Int, Int> = emptyMap(),
    val unlockedTopicInfo: UnlockedTopicInfo? = null,
    val showPronunciationCongrats: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTopicTranscripts: List<PhraseTranscriptEntry> = emptyList(),
    val inspectedPhraseIndex: Int? = null,
    val debug: String? = null
)

data class UnlockedTopicInfo(
    val title: String,
    val description: String,
    val xpGained: Int,
    val topicIndex: Int
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

data class ConversationSubmissionResultUi(
    val success: Boolean,
    val accuracy: Float,
    val transcription: String,
    val feedback: String?,
    val nextTurnIndex: Int?,
    val topicCompleted: Boolean,
    val xpAwarded: Int = 0
)

data class PhraseTranscriptEntry(
    val index: Int,
    val text: String,
    val audioPath: String,
    val accuracy: Float,
    val feedback: String?,
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
    onNavigateBack: () -> Unit = {},
    onNavigateToConversation: (String) -> Unit,
    onNavigateToTopicMaster: (String) -> Unit,
    onNavigateToPronunciationPractice: (String) -> Unit = {},
    onNavigateToFluencyPractice: (String) -> Unit = {},
    onNavigateToVocabularyPractice: (String) -> Unit = {},
    onNavigateToListeningPractice: (String) -> Unit = {},
    onNavigateToGrammarPractice: (String) -> Unit = {},
    onNavigateToConversationPractice: (String) -> Unit = {},
    onNavigateToVocabularyLesson: (String) -> Unit = {},
    onNavigateToLearnWithVivi: (String) -> Unit = {},
    onNavigateToSpeakingLesson: (String) -> Unit = {},
    onNavigateToTopicVocabulary: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {}
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
    
    // Bottom navigation state
    var selectedTab by remember { mutableStateOf(1) } // 1 = Learn tab

    // ViewModel state
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadTopics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    LaunchedEffect(ui.selectedTopicIdx, ui.topics) {
        viewModel.loadTranscriptsForCurrentTopic(context)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Ethereal gradient background with network overlay (same as HomeScreen)
        EtherealNetworkBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ModernTopBar(
                    title = "Speaking Journey",
                    onNavigationIconClick = onNavigateBack
                )
            },
            bottomBar = {
                com.example.voicevibe.presentation.screens.main.home.FloatingBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        when (index) {
                            0 -> onNavigateToHome()
                            1 -> { /* Already on Learn */ }
                        }
                    }
                )
            }
        ) { innerPadding: PaddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    
                    // Vertical scrollable list of topic cards
                    if (ui.topics.isNotEmpty()) {
                        VerticalTopicList(
                            topics = ui.topics,
                            selectedTopicIdx = ui.selectedTopicIdx,
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            onTopicClick = { topic ->
                                if (topic.unlocked) {
                                    onNavigateToSpeakingLesson(topic.id)
                                }
                            },
                            onPronunciationClick = { tid -> onNavigateToPronunciationPractice(tid) },
                            onFluencyClick = { tid -> onNavigateToFluencyPractice(tid) },
                            onVocabularyPracticeClick = { tid -> onNavigateToVocabularyPractice(tid) },
                            onGrammarPracticeClick = { tid -> onNavigateToGrammarPractice(tid) },
                            onListeningPracticeClick = { tid -> onNavigateToListeningPractice(tid) },
                            onConversationPracticeClick = { tid -> onNavigateToConversationPractice(tid) },
                            onPhrasesClick = { tid -> onNavigateToLearnWithVivi(tid) },
                            onDialogueClick = { tid -> onNavigateToConversation(tid) },
                            onVocabularyClick = { tid -> onNavigateToTopicVocabulary(tid) }
                        )
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
                                // Compute running total score across phrases (latest attempt per phrase)
                                val totalScore = ui.currentTopicTranscripts
                                    .groupBy { it.index }
                                    .values
                                    .map { entries -> entries.maxByOrNull { it.timestamp }?.accuracy?.toInt() ?: 0 }
                                    .sum()
                                AnimatedResultCard(
                                    result = result,
                                    totalScore = totalScore,
                                    onDismiss = viewModel::dismissPhraseResult
                                )
                            }
                        }
                    )
                }

                // Congratulation screen for unlocking a topic
                ui.unlockedTopicInfo?.let { unlockedTopicInfo ->
                    CongratulationScreen(
                        unlockedTopicInfo = unlockedTopicInfo,
                        onDismiss = viewModel::dismissUnlockedTopicInfo
                    )
                }
            }
        }
    }
}

@Composable
private fun AiCoachCard(
    analysis: CoachAnalysisDto,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            var expanded by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = BrandCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Coach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Details")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = analysis.coachMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                // Cap expanded content height and allow internal scroll to avoid huge card height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (analysis.strengths.isNotEmpty()) {
                                    Text(
                                        text = "Strengths",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        analysis.strengths.take(2).forEach { s: String ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(s.replaceFirstChar { it.titlecase(Locale.ROOT) }) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = Color.White.copy(alpha = 0.06f),
                                                    labelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (analysis.weaknesses.isNotEmpty()) {
                                    Text(
                                        text = "Focus",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandFuchsia
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        analysis.weaknesses.take(2).forEach { w: String ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(w.replaceFirstChar { it.titlecase(Locale.ROOT) }) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = Color.White.copy(alpha = 0.06f),
                                                    labelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            val actionTitle = analysis.nextBestActions.firstOrNull()?.title ?: "Start Recommended Practice"
            Button(
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo, contentColor = Color.White)
            ) {
                Text(actionTitle, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CongratulationScreen(
    unlockedTopicInfo: UnlockedTopicInfo,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CongratulationScreenScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Continue Journey")
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Congratulations",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(80.dp)
                )
                Text(
                    text = "Congratulations!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "You've unlocked: ${unlockedTopicInfo.title}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "+${unlockedTopicInfo.xpGained} XP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "In this topic:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = unlockedTopicInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF2D2F5B),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f)
    )
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
                    imageVector = Icons.Filled.RecordVoiceOver,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Speaking Journey",
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
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
private fun VerticalTopicList(
    topics: List<Topic>,
    selectedTopicIdx: Int,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onTopicClick: (Topic) -> Unit,
    onPronunciationClick: (String) -> Unit,
    onFluencyClick: (String) -> Unit,
    onVocabularyPracticeClick: (String) -> Unit,
    onGrammarPracticeClick: (String) -> Unit,
    onListeningPracticeClick: (String) -> Unit,
    onConversationPracticeClick: (String) -> Unit,
    onPhrasesClick: (String) -> Unit,
    onDialogueClick: (String) -> Unit,
    onVocabularyClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    
    // Ensure the newly selected topic is brought into view (e.g., after unlocking)
    LaunchedEffect(selectedTopicIdx, topics.size) {
        if (selectedTopicIdx in topics.indices) {
            listState.animateScrollToItem(selectedTopicIdx)
        }
    }
    
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding + 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(topics) { index, topic ->
            VerticalTopicCard(
                topic = topic,
                isSelected = index == selectedTopicIdx,
                onClick = { onTopicClick(topic) },
                index = index,
                onPronunciationClick = onPronunciationClick,
                onFluencyClick = onFluencyClick,
                onVocabularyPracticeClick = onVocabularyPracticeClick,
                onGrammarPracticeClick = onGrammarPracticeClick,
                onListeningPracticeClick = onListeningPracticeClick,
                onConversationPracticeClick = onConversationPracticeClick,
                onPhrasesClick = onPhrasesClick,
                onDialogueClick = onDialogueClick,
                onVocabularyClick = onVocabularyClick
            )
        }
    }
}

@Composable
private fun VerticalTopicCard(
    topic: Topic,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int,
    onPronunciationClick: (String) -> Unit,
    onFluencyClick: (String) -> Unit,
    onVocabularyPracticeClick: (String) -> Unit,
    onGrammarPracticeClick: (String) -> Unit,
    onListeningPracticeClick: (String) -> Unit,
    onConversationPracticeClick: (String) -> Unit,
    onPhrasesClick: (String) -> Unit,
    onDialogueClick: (String) -> Unit,
    onVocabularyClick: (String) -> Unit
) {
    // Auto-expand newly unlocked topics (unlocked but not completed)
    val shouldAutoExpand = topic.unlocked && !topic.completed
    var isExpanded by remember { mutableStateOf(shouldAutoExpand) }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(200),
        label = "VerticalTopicCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = if (isSelected)
                    Brush.horizontalGradient(listOf(BrandCyan, BrandIndigo))
                else
                    SolidColor(Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Main card content (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = topic.unlocked,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock icon or number badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = if (topic.unlocked) {
                                Brush.linearGradient(listOf(BrandCyan, BrandIndigo))
                            } else {
                                Brush.linearGradient(listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.1f)
                                ))
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (topic.unlocked) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Topic info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (topic.unlocked) {
                        Spacer(modifier = Modifier.height(6.dp))
                        // Compact progress indicator (4 required practices: Pronunciation, Fluency, Vocabulary, Grammar)
                        val scores = topic.practiceScores
                        val completedPractices = listOf(
                            (topic.phraseProgress?.isAllPhrasesCompleted == true) || 
                            ((scores?.pronunciation ?: 0) > 0),
                            (topic.fluencyProgress?.completed == true) || 
                            ((scores?.fluency ?: 0) > 0),
                            (scores?.vocabulary ?: 0) > 0,
                            (scores?.grammar ?: 0) > 0
                        ).count { it }
                        
                        val progress = completedPractices / 4f
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (progress >= 1f) Color(0xFF4CAF50) else BrandCyan,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Completion checkmark
                    if (topic.completed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Expand/collapse icon
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Expanded content
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Description
                    Text(
                        text = topic.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Lessons section label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lessons",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Topic stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Vocabulary count - Opens TopicVocabularyScreen
                        TopicStatItem(
                            label = "Vocabulary",
                            value = "${topic.vocabulary.size}",
                            icon = Icons.Default.School,
                            onClick = if (topic.unlocked) {
                                { onVocabularyClick(topic.id) }
                            } else null
                        )
                        
                        // Phrases count - Opens LearnTopicWithViviScreen
                        TopicStatItem(
                            label = "Phrases",
                            value = "${topic.material.size}",
                            icon = Icons.Default.RecordVoiceOver,
                            onClick = if (topic.unlocked) {
                                { onPhrasesClick(topic.id) }
                            } else null
                        )
                        
                        // Conversation turns - Opens ConversationLesson
                        TopicStatItem(
                            label = "Dialogue",
                            value = "${topic.conversation.size}",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onClick = if (topic.unlocked) {
                                { onDialogueClick(topic.id) }
                            } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Practice Missions label (changes when completed)
                    val isCompleted = topic.completed || (topic.practiceScores?.meetsRequirement == true)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF4CAF50) else BrandCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "Missions Completed!" else "Practice Missions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color(0xFF4CAF50) else Color.White
                        )
                        if (!isCompleted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Complete to unlock next topic",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Practice quick-actions row with progress-to-75 rings
                    val scores = topic.practiceScores
                    val pronScore = (scores?.pronunciation ?: 0).coerceIn(0, 100)
                    val fluScore = (scores?.fluency ?: 0).coerceIn(0, 100)
                    val vocabScore = (scores?.vocabulary ?: 0).coerceIn(0, 100)
                    val gramScore = (scores?.grammar ?: 0).coerceIn(0, 100)
                    val listenScore = (scores?.listening ?: 0).coerceIn(0, 100)
                    val convScore = (topic.conversationScore ?: 0).coerceIn(0, 100)

                    // Check if practices are actually completed (all phrases/prompts done)
                    val isPronCompleted = topic.phraseProgress?.isAllPhrasesCompleted == true
                    val isFluCompleted = topic.fluencyProgress?.completed == true
                    val isConvCompleted = topic.conversationCompleted == true

                    fun toFraction(score: Int): Float = if (score >= 75) 1f else (score / 75f).coerceIn(0f, 1f)

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            PracticeIconProgressButton(
                                icon = Icons.Default.Mic,
                                color = Color(0xFF06FFA5),
                                progress = toFraction(pronScore),
                                enabled = topic.unlocked,
                                showCheck = isPronCompleted && pronScore >= 75,
                                onClick = { if (topic.unlocked) onPronunciationClick(topic.id) }
                            )
                        }
                        item {
                            PracticeIconProgressButton(
                                icon = Icons.Default.RecordVoiceOver,
                                color = Color(0xFF06FFA5),
                                progress = toFraction(fluScore),
                                enabled = topic.unlocked,
                                showCheck = isFluCompleted && fluScore >= 75,
                                onClick = { if (topic.unlocked) onFluencyClick(topic.id) }
                            )
                        }
                        item {
                            PracticeIconProgressButton(
                                icon = Icons.Default.Translate,
                                color = Color(0xFF06FFA5),
                                progress = toFraction(vocabScore),
                                enabled = topic.unlocked,
                                showCheck = vocabScore >= 75,
                                onClick = { if (topic.unlocked) onVocabularyPracticeClick(topic.id) }
                            )
                        }
                        item {
                            PracticeIconProgressButton(
                                icon = Icons.Default.Spellcheck,
                                color = Color(0xFF06FFA5),
                                progress = toFraction(gramScore),
                                enabled = topic.unlocked,
                                showCheck = gramScore >= 75,
                                onClick = { if (topic.unlocked) onGrammarPracticeClick(topic.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BrandIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bonus Practices",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            PracticeIconProgressButton(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                color = Color(0xFF06FFA5),
                                progress = toFraction(listenScore),
                                enabled = topic.unlocked,
                                showCheck = listenScore >= 75,
                                onClick = { if (topic.unlocked) onListeningPracticeClick(topic.id) }
                            )
                        }
                        item {
                            PracticeIconProgressButton(
                                icon = Icons.Default.School,
                                color = Color(0xFF06FFA5),
                                progress = toFraction(convScore),
                                enabled = topic.unlocked,
                                showCheck = isConvCompleted && convScore >= 75,
                                onClick = { if (topic.unlocked) onConversationPracticeClick(topic.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeIconProgressButton(
    icon: ImageVector,
    color: Color,
    progress: Float,
    enabled: Boolean,
    showCheck: Boolean,
    onClick: () -> Unit
) {
    val alphaVal = if (enabled) 1f else 0.5f
    Box(
        modifier = Modifier
            .size(56.dp)
            .alpha(alphaVal)
    ) {
        // Track background
        CircularProgressIndicator(
            progress = 1f,
            color = Color.White.copy(alpha = 0.15f),
            strokeWidth = 4.dp,
            modifier = Modifier.matchParentSize()
        )
        // Foreground progress
        CircularProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            color = if (showCheck) color else Color(0xFFFFB347), // Warm amber for in-progress, mint/cyan when complete
            strokeWidth = 4.dp,
            modifier = Modifier.matchParentSize()
        )
        // Center icon hit target
        Surface(
            onClick = { if (enabled) onClick() },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (showCheck) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun TopicStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (onClick != null) BrandCyan.copy(alpha = 0.3f) else Color.Transparent
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = BrandCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
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
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    BrandCyan.copy(alpha = 0.5f),
                    BrandIndigo.copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Topic",
                style = MaterialTheme.typography.labelSmall,
                color = BrandCyan
            )
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = topic.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0BEC5),
            )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        defaultElevation = 0.dp
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
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    BrandCyan.copy(alpha = 0.5f),
                    BrandIndigo.copy(alpha = 0.5f)
                )
            )
        )
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
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onSpeak(combined) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play conversation",
                        tint = BrandIndigo
                    )
                }
            }
            Text(
                text = combined,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0BEC5)
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
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play recording")
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
        Color(0xFF3A86FF),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFD700),
        Color(0xFFFB5607),
        Color(0xFF3A86FF),
        Color(0xFFFB5607),
        Color(0xFFFF006E)
    ).random(),
    val maxAlpha: Float = (0.3f..0.7f).random()
)

private fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + kotlin.random.Random.nextFloat() * (endInclusive - start)
}

@Composable
private fun AnimatedResultCard(
    result: PhraseSubmissionResultUi,
    totalScore: Int,
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

    val isSuccess = result.success
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

                // XP Earned (show only when XP was awarded)
                if (result.xpAwarded > 0) {
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
                                text = "+${result.xpAwarded} XP",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                        }
                    }
                }

                // Total Score across practiced phrases
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Total score: $totalScore",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play recording")
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

/**
 * Data class representing a particle (dot) in the network
 */
private data class NetworkParticle(
    val x: Float,
    val y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float
)

/**
 * Ethereal Network Background with gradient and animated glowing dots/lines
 */
@Composable
private fun EtherealNetworkBackground() {
    // Gradient colors: darker edges → very subtle light center (middle light source)
    val gradientColors = listOf(
        Color(0xFF3D3A7C),  // Deep purple-indigo (top)
        Color(0xFF4A4E96),  // Deep indigo
        Color(0xFF5367B8),  // Indigo
        Color(0xFF5C72BE),  // Slightly lighter indigo (center - very subtle)
        Color(0xFF5367B8),  // Indigo
        Color(0xFF4A4E96),  // Deep indigo
        Color(0xFF3D3A7C)   // Deep purple-indigo (bottom)
    )

    // Remember particles with mutable positions
    val particlePositions = remember {
        mutableStateOf(
            List(18) { // 18 particles for ultra-subtle network
                NetworkParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speedX = (Random.nextFloat() - 0.5f) * 0.0003f,
                    speedY = (Random.nextFloat() - 0.5f) * 0.0003f,
                    size = Random.nextFloat() * 1.5f + 1f // Smaller: 1-2.5px instead of 2-5px
                )
            }
        )
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                // Update particle positions
                particlePositions.value = particlePositions.value.map { particle ->
                    var newX = particle.x + particle.speedX
                    var newY = particle.y + particle.speedY
                    
                    // Wrap around edges
                    if (newX < 0f) newX = 1f
                    if (newX > 1f) newX = 0f
                    if (newY < 0f) newY = 1f
                    if (newY > 1f) newY = 0f
                    
                    particle.copy(x = newX, y = newY)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
    ) {
        // Network overlay with dots and lines
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            
            // Get current particle positions in screen coordinates
            val particles = particlePositions.value
            val updatedPositions = particles.map { particle ->
                Offset(particle.x * width, particle.y * height)
            }
            
            // Draw connecting lines between nearby particles
            val maxConnectionDistance = 200f
            for (i in updatedPositions.indices) {
                for (j in i + 1 until updatedPositions.size) {
                    val pos1 = updatedPositions[i]
                    val pos2 = updatedPositions[j]
                    
                    val dx = pos2.x - pos1.x
                    val dy = pos2.y - pos1.y
                    val distance = sqrt(dx * dx + dy * dy)
                    
                    if (distance < maxConnectionDistance) {
                        // Calculate alpha based on distance (closer = more visible)
                        val alpha = (1f - distance / maxConnectionDistance) * 0.25f
                        
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = pos1,
                            end = pos2,
                            strokeWidth = 1f
                        )
                    }
                }
            }
            
            // Draw glowing dots
            updatedPositions.forEachIndexed { index, position ->
                val particle = particles[index]
                
                // Outer glow (larger, more transparent)
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = particle.size * 3f,
                    center = position
                )
                
                // Middle glow
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = particle.size * 1.8f,
                    center = position
                )
                
                // Inner bright dot
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = particle.size,
                    center = position
                )
                
                // Core bright center
                drawCircle(
                    color = Color.White,
                    radius = particle.size * 0.5f,
                    center = position
                )
            }
        }
    }
}
