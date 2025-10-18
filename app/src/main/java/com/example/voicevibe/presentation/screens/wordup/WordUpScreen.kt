package com.example.voicevibe.presentation.screens.wordup

import android.Manifest
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.domain.model.WordDifficulty
import com.example.voicevibe.domain.model.PronunciationResult
import com.example.voicevibe.presentation.viewmodel.WordUpViewModel
import com.example.voicevibe.presentation.viewmodel.WordUpUiState
import com.example.voicevibe.presentation.viewmodel.LearningStep
import com.example.voicevibe.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WordUpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMasteredWords: () -> Unit,
    viewModel: WordUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current
    
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (uiState.currentWord == null) {
            viewModel.loadNewWord()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Error shown in UI
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WordUp!") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMasteredWords) {
                        Badge(
                            containerColor = BrandCyan,
                            contentColor = BrandNavyDark
                        ) {
                            Text("${stats?.masteredCount ?: 0}")
                        }
                    }
                    IconButton(onClick = onNavigateToMasteredWords) {
                        Icon(Icons.Default.List, "My Words")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandNavyDark
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandNavyDark, BrandNavy)
                    )
                )
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BrandCyan
                    )
                }
                uiState.currentWord != null -> {
                    WordPracticeContent(
                        uiState = uiState,
                        context = context,
                        hasAudioPermission = audioPermission.status.isGranted,
                        onRequestPermission = { audioPermission.launchPermissionRequest() },
                        onShowDefinition = { viewModel.showDefinition() },
                        onSpeakWord = {
                            uiState.currentWord?.let { w ->
                                viewModel.playWordPronunciation(w.word)
                            }
                        },
                        onExampleChange = { viewModel.updateExampleSentence(it) },
                        onStartRecording = {
                            val file = File(context.cacheDir, "wordup_recording.m4a")
                            viewModel.startRecording(file)
                        },
                        onStopRecording = { viewModel.stopRecording() },
                        onEvaluate = { viewModel.evaluateExample() },
                        onNextWord = { viewModel.loadNewWord() },
                        onDismissResult = { viewModel.clearEvaluation() },
                        onStartPronunciation = {
                            val file = File(context.cacheDir, "pronunciation_recording.m4a")
                            viewModel.startPronunciationPractice(file)
                        },
                        onStopPronunciation = { viewModel.stopPronunciationPractice() },
                        onRetryPronunciation = { viewModel.retryPronunciation() },
                        onSkipWord = { viewModel.skipCurrentWord() },
                        onSelectInputMode = { mode -> viewModel.selectSentenceInputMode(mode) }
                    )
                }
                else -> {
                    ErrorContent(
                        message = uiState.error ?: "No words available",
                        onRetry = { viewModel.loadNewWord() }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordPracticeContent(
    uiState: WordUpUiState,
    context: Context,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onShowDefinition: () -> Unit,
    onSpeakWord: () -> Unit,
    onExampleChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEvaluate: () -> Unit,
    onNextWord: () -> Unit,
    onDismissResult: () -> Unit,
    onStartPronunciation: () -> Unit,
    onStopPronunciation: () -> Unit,
    onRetryPronunciation: () -> Unit,
    onSkipWord: () -> Unit,
    onSelectInputMode: (com.example.voicevibe.presentation.viewmodel.SentenceInputMode) -> Unit
) {
    val word = uiState.currentWord!!
    val scrollState = rememberScrollState()
    
    // Confetti state for word mastery celebration
    var showConfetti by remember { mutableStateOf(false) }
    
    // Trigger confetti when word is mastered, reset when evaluation is cleared
    LaunchedEffect(uiState.evaluationResult) {
        if (uiState.evaluationResult?.isMastered == true) {
            showConfetti = true
        } else if (uiState.evaluationResult == null) {
            showConfetti = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Word Card based on learning step
        InteractiveWordCard(
            word = word.word,
            difficulty = word.difficulty,
            partOfSpeech = word.partOfSpeech,
            definition = word.definition,
            ipaPronunciation = word.ipaPronunciation,
            learningStep = uiState.learningStep,
            onShowDefinition = onShowDefinition,
            onSpeakWord = onSpeakWord
        )

        // Pronunciation practice section
        when (uiState.learningStep) {
            LearningStep.DEFINITION_REVEALED -> {
                PronunciationPracticePrompt(
                    word = word.word,
                    hasAudioPermission = hasAudioPermission,
                    onRequestPermission = onRequestPermission,
                    onStartPronunciation = onStartPronunciation
                )
            }
            LearningStep.PRONUNCIATION_PRACTICE -> {
                RecordingIndicator(
                    isRecording = uiState.isPronunciationRecording,
                    isEvaluating = uiState.isPronunciationEvaluating,
                    onStop = onStopPronunciation
                )
            }
            LearningStep.PRONUNCIATION_EVALUATED -> {
                uiState.pronunciationResult?.let { result ->
                    PronunciationFeedbackCard(
                        result = result,
                        onRetry = onRetryPronunciation,
                        onSkip = onSkipWord,
                        showSkip = uiState.pronunciationFailures >= 1 && !result.isCorrect
                    )
                }
            }
            LearningStep.SENTENCE_PRACTICE -> {
                // Only show sentence practice section if evaluation hasn't been done yet
                if (uiState.evaluationResult == null) {
                    ExampleSentenceSection(
                        exampleSentence = uiState.exampleSentence,
                        isRecording = uiState.isRecording,
                        hasAudioRecording = uiState.hasAudioRecording,
                        isEvaluating = uiState.isEvaluating,
                        hasAudioPermission = hasAudioPermission,
                        selectedMode = uiState.sentenceInputMode,
                        onExampleChange = onExampleChange,
                        onRequestPermission = onRequestPermission,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onEvaluate = onEvaluate,
                        onSelectMode = onSelectInputMode
                    )
                }
            }
            else -> {}
        }

        // Final evaluation result (replaces input section)
        AnimatedVisibility(
            visible = uiState.evaluationResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.evaluationResult?.let { result ->
                FinalEvaluationCard(
                    result = result,
                    onNextWord = onNextWord,
                    onRetry = onDismissResult
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Confetti overlay on top of everything when word is mastered
    if (showConfetti) {
        ConfettiOverlay()
    }
}

@Composable
private fun InteractiveWordCard(
    word: String,
    difficulty: WordDifficulty,
    partOfSpeech: String,
    definition: String,
    ipaPronunciation: String,
    learningStep: LearningStep,
    onShowDefinition: () -> Unit,
    onSpeakWord: () -> Unit
) {
    val showDefinition = learningStep != LearningStep.INITIAL
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = learningStep == LearningStep.INITIAL) { onShowDefinition() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandNavy.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Difficulty badge
            DifficultyBadge(difficulty)

            // Word
            Text(
                text = word,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = BrandCyan
            )
            
            // IPA pronunciation (if available)
            if (ipaPronunciation.isNotEmpty()) {
                Text(
                    text = ipaPronunciation,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandIndigo.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal
                )
            }

            // Speaker icon and part of speech
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onSpeakWord, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak",
                        tint = BrandCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (showDefinition && partOfSpeech.isNotEmpty()) {
                    Text(
                        text = "($partOfSpeech)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Tap to reveal hint (only in INITIAL step)
            if (learningStep == LearningStep.INITIAL) {
                Text(
                    text = "👆 Tap to reveal definition",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandIndigo,
                    fontWeight = FontWeight.Medium
                )
            }

            // Definition (show after INITIAL)
            AnimatedVisibility(
                visible = showDefinition,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BrandIndigo.copy(alpha = 0.3f)
                    )
                    Text(
                        text = definition,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: WordDifficulty) {
    val (color, label) = when (difficulty) {
        WordDifficulty.BEGINNER -> BrandCyan to "Beginner"
        WordDifficulty.INTERMEDIATE -> BrandIndigo to "Intermediate"
        WordDifficulty.ADVANCED -> BrandFuchsia to "Advanced"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PronunciationPracticePrompt(
    word: String,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartPronunciation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandIndigo.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = BrandCyan
            )
            
            Text(
                text = "Say the word!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandCyan
            )
            
            Text(
                text = "Pronounce \"$word\" clearly",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Button(
                onClick = {
                    if (hasAudioPermission) onStartPronunciation()
                    else onRequestPermission()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Recording", color = BrandNavyDark, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun RecordingIndicator(
    isRecording: Boolean,
    isEvaluating: Boolean,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEvaluating) BrandIndigo.copy(alpha = 0.15f) else BrandFuchsia.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEvaluating) BrandIndigo.copy(alpha = 0.8f) 
                        else BrandFuchsia.copy(alpha = alpha)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isEvaluating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 4.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
            
            Text(
                text = if (isEvaluating) "Evaluating..." else "Recording...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isEvaluating) BrandIndigo else BrandFuchsia
            )
            
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                enabled = isRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEvaluating) BrandIndigo else BrandFuchsia
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isEvaluating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Recording", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PronunciationFeedbackCard(
    result: PronunciationResult,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    showSkip: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isCorrect) 
                BrandCyan.copy(alpha = 0.15f) 
            else 
                BrandFuchsia.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (result.isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = if (result.isCorrect) BrandCyan else BrandFuchsia
            )
            
            Text(
                text = if (result.isCorrect) "Perfect! ✨" else "Try Again",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (result.isCorrect) BrandCyan else BrandFuchsia
            )
            
            if (result.transcribedText.isNotEmpty()) {
                Text(
                    text = "You said: \"${result.transcribedText}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            Text(
                text = result.feedback,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!result.isCorrect) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandFuchsia
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again")
                    }

                    if (showSkip) {
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Skip this word",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExampleSentenceSection(
    exampleSentence: String,
    isRecording: Boolean,
    hasAudioRecording: Boolean,
    isEvaluating: Boolean,
    hasAudioPermission: Boolean,
    selectedMode: com.example.voicevibe.presentation.viewmodel.SentenceInputMode?,
    onExampleChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEvaluate: () -> Unit,
    onSelectMode: (com.example.voicevibe.presentation.viewmodel.SentenceInputMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandNavy.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
                tint = BrandCyan
            )
            
            Text(
                text = "Use the word in a sentence",
                style = MaterialTheme.typography.titleLarge,
                color = BrandCyan,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Show your understanding by creating a sentence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Mode selection (show if no mode selected yet)
            if (selectedMode == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Keyboard button
                    OutlinedButton(
                        onClick = { onSelectMode(com.example.voicevibe.presentation.viewmodel.SentenceInputMode.TEXT) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandCyan
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Type",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Mic button
                    OutlinedButton(
                        onClick = { onSelectMode(com.example.voicevibe.presentation.viewmodel.SentenceInputMode.VOICE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandIndigo
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Speak",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Text input (show only if TEXT mode selected)
            if (selectedMode == com.example.voicevibe.presentation.viewmodel.SentenceInputMode.TEXT) {
                OutlinedTextField(
                value = if (hasAudioRecording) "" else exampleSentence,
                onValueChange = onExampleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        if (hasAudioRecording) "🎤 Evaluating your recording..." 
                        else "Type your sentence...",
                        fontSize = 18.sp
                    ) 
                },
                enabled = !isRecording && !isEvaluating && !hasAudioRecording,
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = BrandIndigo.copy(alpha = 0.5f)
                    )
                )

                // Submit button for text
                Button(
                    onClick = onEvaluate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = exampleSentence.isNotEmpty() && !isEvaluating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandCyan,
                        contentColor = BrandNavyDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isEvaluating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandNavyDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit")
                    }
                }
            }

            // Voice recording (show only if VOICE mode selected)
            if (selectedMode == com.example.voicevibe.presentation.viewmodel.SentenceInputMode.VOICE) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasAudioRecording) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = BrandIndigo,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Processing your recording...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandIndigo
                        )
                    } else if (isRecording) {
                        // Recording indicator
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(BrandFuchsia.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = BrandFuchsia
                            )
                        }
                        Text(
                            text = "Recording...",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandFuchsia,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandFuchsia
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop Recording")
                        }
                    } else {
                        // Start recording button
                        Button(
                            onClick = {
                                if (!hasAudioPermission) {
                                    onRequestPermission()
                                } else {
                                    onStartRecording()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandIndigo
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Recording",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalEvaluationCard(
    result: com.example.voicevibe.domain.model.EvaluationResult,
    onNextWord: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isAcceptable) 
                BrandCyan.copy(alpha = 0.15f) 
            else 
                BrandFuchsia.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (result.isMastered) Icons.Default.Star else if (result.isAcceptable) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (result.isAcceptable) BrandCyan else BrandFuchsia
            )

            Text(
                text = if (result.isMastered) "Word Mastered! 🎉" else if (result.isAcceptable) "Great Job!" else "Keep Trying!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (result.isAcceptable) BrandCyan else BrandFuchsia
            )

            Text(
                text = result.feedback,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (result.isAcceptable) {
                Button(
                    onClick = onNextWord,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Next Word", color = BrandNavyDark, fontSize = 16.sp)
                }
            } else {
                // Retry button for failed attempts
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandFuchsia
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = BrandFuchsia
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandCyan
            )
        ) {
            Text("Retry", color = BrandNavyDark)
        }
    }
}

@Composable
private fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    durationMillis: Int = 3500,
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
        val random = remember { kotlin.random.Random(System.currentTimeMillis()) }
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
                        localT < 0.75f -> 1f
                        else -> ((1f - localT) / 0.25f).coerceIn(0f, 1f)
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
