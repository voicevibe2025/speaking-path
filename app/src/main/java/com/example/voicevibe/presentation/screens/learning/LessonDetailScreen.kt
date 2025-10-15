package com.example.voicevibe.presentation.screens.learning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    pathId: String,
    moduleId: String,
    lessonId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNextLesson: (String, String, String) -> Unit,
    viewModel: LessonDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(pathId, moduleId, lessonId) {
        viewModel.loadLesson(pathId, moduleId, lessonId)
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    LaunchedEffect(uiState.completionMessage) {
        uiState.completionMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearCompletionMessage()
        }
    }
    
    LaunchedEffect(uiState.isLessonCompleted) {
        if (uiState.isLessonCompleted) {
            delay(2000)
            uiState.nextLesson?.let { next ->
                onNavigateToNextLesson(next.pathId, next.moduleId, next.lessonId)
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.lesson?.title ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${uiState.xpEarned} XP earned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            uiState.lesson?.let { lesson ->
                LessonBottomBar(
                    currentSection = uiState.currentSectionIndex,
                    totalSections = lesson.sections.size,
                    canNavigateBack = uiState.currentSectionIndex > 0,
                    canNavigateNext = uiState.currentSectionIndex < lesson.sections.size - 1,
                    isLastSection = uiState.currentSectionIndex == lesson.sections.size - 1,
                    onNavigateBack = { viewModel.navigateToPreviousSection() },
                    onNavigateNext = { viewModel.navigateToNextSection() },
                    onCompleteLesson = { viewModel.completeLesson() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.lesson != null -> {
                    val lesson = uiState.lesson!!
                    val currentSection = lesson.sections.getOrNull(uiState.currentSectionIndex)
                    currentSection?.let { section ->
                        AnimatedContent(
                            targetState = section,
                            transitionSpec = {
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            },
                            label = "section_animation"
                        ) { targetSection ->
                            LessonSectionContent(
                                section = targetSection,
                                quizAnswer = uiState.quizAnswers[targetSection.id],
                                isPracticeCompleted = targetSection.id in uiState.practiceCompleted,
                                onQuizAnswerSelected = { answer ->
                                    viewModel.selectQuizAnswer(targetSection.id, answer)
                                },
                                onPracticeCompleted = {
                                    viewModel.completePractice(targetSection.id)
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
fun LessonSectionContent(
    section: LessonSection,
    quizAnswer: Int?,
    isPracticeCompleted: Boolean,
    onQuizAnswerSelected: (Int) -> Unit,
    onPracticeCompleted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (section.type) {
                        LessonSectionType.VIDEO -> Icons.Default.PlayCircle
                        LessonSectionType.TEXT -> Icons.Default.Article
                        LessonSectionType.QUIZ -> Icons.Default.Quiz
                        LessonSectionType.PRACTICE -> Icons.Default.Mic
                        LessonSectionType.AUDIO -> Icons.Default.Headphones
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (section.type) {
                            LessonSectionType.VIDEO -> "Video Lesson"
                            LessonSectionType.TEXT -> "Reading Material"
                            LessonSectionType.QUIZ -> "Quick Quiz"
                            LessonSectionType.PRACTICE -> "Practice Exercise"
                            LessonSectionType.AUDIO -> "Audio Lesson"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        when (section.type) {
            LessonSectionType.VIDEO -> VideoSectionContent(section)
            LessonSectionType.TEXT -> TextSectionContent(section)
            LessonSectionType.QUIZ -> QuizSectionContent(
                section = section,
                selectedAnswer = quizAnswer,
                onAnswerSelected = onQuizAnswerSelected
            )
            LessonSectionType.PRACTICE -> PracticeSectionContent(
                section = section,
                isCompleted = isPracticeCompleted,
                onCompleted = onPracticeCompleted
            )
            LessonSectionType.AUDIO -> AudioSectionContent(section)
        }
    }
}

@Composable
private fun VideoSectionContent(section: LessonSection) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video Player Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }

        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = section.content,
            style = MaterialTheme.typography.bodyLarge
        )

        // Video controls would go here
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { /* Rewind */ }) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                Spacer(modifier = Modifier.width(4.dp))
                Text("10s")
            }

            TextButton(onClick = { /* Playback speed */ }) {
                Icon(Icons.Default.Speed, contentDescription = "Speed")
                Spacer(modifier = Modifier.width(4.dp))
                Text("1x")
            }

            TextButton(onClick = { /* Forward */ }) {
                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                Spacer(modifier = Modifier.width(4.dp))
                Text("10s")
            }
        }
    }
}

@Composable
private fun TextSectionContent(section: LessonSection) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = section.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Key points if available
        section.keyPoints?.let { points ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Key Points",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        points.forEach { point ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizSectionContent(
    section: LessonSection,
    selectedAnswer: Int?,
    onAnswerSelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quiz Question",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = section.content,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        itemsIndexed(section.quizOptions ?: emptyList()) { index, option ->
            val isSelected = selectedAnswer == index
            val isCorrect = selectedAnswer != null && index == section.correctAnswer
            val isIncorrect = selectedAnswer != null && isSelected && index != section.correctAnswer

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectedAnswer == null) {
                        onAnswerSelected(index)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        isIncorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = when {
                        isCorrect -> Color(0xFF4CAF50)
                        isIncorrect -> Color(0xFFF44336)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCorrect -> Color(0xFF4CAF50)
                                    isIncorrect -> Color(0xFFF44336)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Correct",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        } else if (isIncorrect) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Incorrect",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        } else {
                            Text(
                                text = ('A' + index).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (selectedAnswer != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Explanation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = section.explanation ?: "Great job! Keep practicing to improve your skills.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeSectionContent(
    section: LessonSection,
    isCompleted: Boolean,
    onCompleted: () -> Unit
) {
    var isPracticing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Speaking Practice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = section.content,
                    style = MaterialTheme.typography.bodyLarge
                )

                section.practicePrompt?.let { prompt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "\"$prompt\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Practice controls
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isPracticing) {
                    // Recording indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text = "Recording...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red
                        )
                    }

                    // Waveform placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isCompleted) {
                        Button(
                            onClick = {
                                if (isPracticing) {
                                    isPracticing = false
                                    onCompleted()
                                } else {
                                    isPracticing = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPracticing) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPracticing) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPracticing) "Stop Recording" else "Start Practice")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                isPracticing = false
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try Again")
                        }

                        Button(
                            onClick = { /* Play recording */ }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Recording")
                        }
                    }
                }

                if (isCompleted) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "Great job! Practice completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSectionContent(section: LessonSection) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio player placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { /* Play/Pause */ },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = 0.3f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1:23",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "4:56",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text(
                    text = section.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun LessonBottomBar(
    currentSection: Int,
    totalSections: Int,
    canNavigateBack: Boolean,
    canNavigateNext: Boolean,
    isLastSection: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit,
    onCompleteLesson: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding()
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentSection + 1).toFloat() / totalSections,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    enabled = canNavigateBack
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous")
                }

                Text(
                    text = "Section ${currentSection + 1} of $totalSections",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isLastSection) {
                    Button(onClick = onCompleteLesson) {
                        Text("Complete Lesson")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = onNavigateNext,
                        enabled = canNavigateNext
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
