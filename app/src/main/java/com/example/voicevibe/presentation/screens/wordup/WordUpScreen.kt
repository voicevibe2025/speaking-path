package com.example.voicevibe.presentation.screens.wordup

import android.Manifest
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.domain.model.WordDifficulty
import com.example.voicevibe.presentation.viewmodel.WordUpViewModel
import com.example.voicevibe.presentation.viewmodel.WordUpUiState
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
                        onDismissResult = { viewModel.clearEvaluation() }
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
    onDismissResult: () -> Unit
) {
    val word = uiState.currentWord!!
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Word Card
        WordCard(
            word = word.word,
            difficulty = word.difficulty,
            partOfSpeech = word.partOfSpeech,
            showDefinition = uiState.showDefinition,
            definition = word.definition,
            onShowDefinition = onShowDefinition,
            onSpeakWord = onSpeakWord
        )

        // Progress indicator
        if (uiState.progress != null) {
            AttemptsIndicator(attempts = uiState.progress.attempts)
        }

        // Evaluation result
        AnimatedVisibility(
            visible = uiState.evaluationResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.evaluationResult?.let { result ->
                EvaluationResultCard(
                    result = result,
                    onDismiss = onDismissResult,
                    onNextWord = onNextWord
                )
            }
        }

        // Input section (only show if not mastered)
        if (uiState.evaluationResult?.isMastered != true) {
            ExampleInputSection(
                exampleSentence = uiState.exampleSentence,
                isRecording = uiState.isRecording,
                hasAudioRecording = uiState.hasAudioRecording,
                isEvaluating = uiState.isEvaluating,
                hasAudioPermission = hasAudioPermission,
                onExampleChange = onExampleChange,
                onRequestPermission = onRequestPermission,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onEvaluate = onEvaluate
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WordCard(
    word: String,
    difficulty: WordDifficulty,
    partOfSpeech: String,
    showDefinition: Boolean,
    definition: String,
    onShowDefinition: () -> Unit,
    onSpeakWord: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (showDefinition) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = !showDefinition) { onShowDefinition() },
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

            if (partOfSpeech.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSpeakWord, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak",
                            tint = BrandCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "($partOfSpeech)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Tap to reveal hint
            if (!showDefinition) {
                Text(
                    text = "Tap to reveal definition",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandIndigo.copy(alpha = 0.7f)
                )
            }

            // Definition
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
private fun AttemptsIndicator(attempts: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(minOf(attempts, 5)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrandIndigo.copy(alpha = 0.6f))
            )
        }
        if (attempts > 0) {
            Text(
                text = "$attempts ${if (attempts == 1) "attempt" else "attempts"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EvaluationResultCard(
    result: com.example.voicevibe.domain.model.EvaluationResult,
    onDismiss: () -> Unit,
    onNextWord: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (result.isAcceptable) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (result.isAcceptable) BrandCyan else BrandFuchsia
            )

            Text(
                text = if (result.isMastered) "Word Mastered! 🎉" else if (result.isAcceptable) "Great Job!" else "Try Again",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (result.isAcceptable) BrandCyan else BrandFuchsia
            )

            Text(
                text = result.feedback,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (result.isMastered) {
                Button(
                    onClick = onNextWord,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandCyan
                    )
                ) {
                    Text("Next Word", color = BrandNavyDark)
                }
            } else if (result.isAcceptable) {
                Button(
                    onClick = onNextWord,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandCyan
                    )
                ) {
                    Text("Next Word", color = BrandNavyDark)
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandFuchsia
                    )
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun ExampleInputSection(
    exampleSentence: String,
    isRecording: Boolean,
    hasAudioRecording: Boolean,
    isEvaluating: Boolean,
    hasAudioPermission: Boolean,
    onExampleChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEvaluate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandNavy.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create your example sentence:",
                style = MaterialTheme.typography.titleMedium,
                color = BrandCyan,
                fontWeight = FontWeight.Bold
            )

            // Text input
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Record button
                OutlinedButton(
                    onClick = {
                        if (!hasAudioPermission) {
                            onRequestPermission()
                        } else if (isRecording) {
                            onStopRecording()
                        } else {
                            onStartRecording()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isEvaluating,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRecording) BrandFuchsia.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = if (isRecording) BrandFuchsia else BrandIndigo
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop" else "Speak")
                }

                // Master button (only for text input, recording auto-submits)
                Button(
                    onClick = onEvaluate,
                    modifier = Modifier.weight(1f),
                    enabled = exampleSentence.isNotEmpty() && !isRecording && !isEvaluating && !hasAudioRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandCyan,
                        contentColor = BrandNavyDark
                    )
                ) {
                    if (isEvaluating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandNavyDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Master")
                    }
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
