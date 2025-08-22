package com.example.voicevibe.presentation.screens.evaluation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicevibe.domain.model.*

/**
 * Evaluation Results screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPractice: () -> Unit,
    viewModel: EvaluationResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Cache nullable fields to avoid smart-cast on delegated properties
    val errorMessage = uiState.error
    val evaluation = uiState.evaluation

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EvaluationResultEvent.NavigateToPractice -> {
                    onNavigateToPractice()
                }
                is EvaluationResultEvent.ShareResults -> {
                    // Handle share intent
                }
                is EvaluationResultEvent.ShowDetailedFeedback -> {
                    // Handle detailed feedback dialog
                }
                is EvaluationResultEvent.ShowMessage -> {
                    // Show snackbar
                }
                is EvaluationResultEvent.ShowReportDialog -> {
                    // Show report dialog
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluation Results") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::shareResults) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                ErrorContent(
                    error = errorMessage,
                    onRetry = viewModel::retryLoading,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            evaluation != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overall Score Card
                    item {
                        OverallScoreCard(
                            score = evaluation.overallScore,
                            level = getOverallLevel(evaluation.overallScore)
                        )
                    }

                    // Quick Stats
                    item {
                        QuickStatsRow(evaluation = evaluation)
                    }

                    // Detailed Scores
                    item {
                        DetailedScoresCard(evaluation = evaluation)
                    }

                    // Feedback Section
                    item {
                        FeedbackCard(
                            feedback = evaluation.feedback,
                            suggestions = evaluation.suggestions
                        )
                    }

                    // Phonetic Errors
                    if (evaluation.phoneticErrors.isNotEmpty()) {
                        item {
                            PhoneticErrorsCard(errors = evaluation.phoneticErrors)
                        }
                    }

                    // Action Buttons
                    item {
                        ActionButtons(
                            onPracticeAgain = viewModel::practiceSimilar,
                            onSaveResults = viewModel::saveToHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallScoreCard(
    score: Float,
    level: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Score Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = getScoreColor(score)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${score.toInt()}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "/ 100",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Level Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = getScoreColor(score).copy(alpha = 0.2f)
            ) {
                Text(
                    text = level,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = getScoreColor(score)
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    evaluation: SpeakingEvaluation
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Find strongest area
        val strongestArea = findStrongestArea(evaluation)
        QuickStatCard(
            icon = Icons.Default.TrendingUp,
            label = "Strongest",
            value = strongestArea.first,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )

        // Find weakest area
        val weakestArea = findWeakestArea(evaluation)
        QuickStatCard(
            icon = Icons.Default.Warning,
            label = "Needs Work",
            value = weakestArea.first,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailedScoresCard(
    evaluation: SpeakingEvaluation
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Detailed Scores",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ScoreItem(
                label = "Pronunciation",
                score = evaluation.pronunciation,
                icon = Icons.Default.RecordVoiceOver
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScoreItem(
                label = "Fluency",
                score = evaluation.fluency,
                icon = Icons.Default.Speed
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScoreItem(
                label = "Vocabulary",
                score = evaluation.vocabulary,
                icon = Icons.Default.Book
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScoreItem(
                label = "Grammar",
                score = evaluation.grammar,
                icon = Icons.Default.Spellcheck
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScoreItem(
                label = "Coherence",
                score = evaluation.coherence,
                icon = Icons.Default.Link
            )

            evaluation.culturalAppropriateness?.let { cultural ->
                Spacer(modifier = Modifier.height(12.dp))
                ScoreItem(
                    label = "Cultural Context",
                    score = cultural,
                    icon = Icons.Default.Language
                )
            }
        }
    }
}

@Composable
private fun ScoreItem(
    label: String,
    score: EvaluationScore,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${score.score.toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(score.score)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = score.score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = getScoreColor(score.score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (score.feedback.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = score.feedback,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: String,
    suggestions: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Feedback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Feedback",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = feedback,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Suggestions for Improvement",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneticErrorsCard(
    errors: List<PhoneticError>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pronunciation Corrections",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            errors.take(5).forEach { error ->
                PhoneticErrorItem(error = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (errors.size > 5) {
                Text(
                    text = "And ${errors.size - 5} more...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PhoneticErrorItem(
    error: PhoneticError
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = error.word,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Row {
                Text(
                    text = "Expected: ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = error.expected,
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            Row {
                Text(
                    text = "Your pronunciation: ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = error.actual,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Text(
            text = "${error.timestamp}s",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ActionButtons(
    onPracticeAgain: () -> Unit,
    onSaveResults: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPracticeAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Practice Similar Prompt")
        }

        OutlinedButton(
            onClick = onSaveResults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to History")
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unable to load results",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

// Helper functions
private fun getScoreColor(score: Float): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFF2196F3)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getOverallLevel(score: Float): String {
    return when {
        score >= 90 -> "EXCELLENT"
        score >= 75 -> "VERY GOOD"
        score >= 60 -> "GOOD"
        score >= 45 -> "FAIR"
        else -> "NEEDS IMPROVEMENT"
    }
}

private fun findStrongestArea(evaluation: SpeakingEvaluation): Pair<String, Float> {
    val areas = listOf(
        "Pronunciation" to evaluation.pronunciation.score,
        "Fluency" to evaluation.fluency.score,
        "Vocabulary" to evaluation.vocabulary.score,
        "Grammar" to evaluation.grammar.score,
        "Coherence" to evaluation.coherence.score
    )
    return areas.maxByOrNull { it.second } ?: (" " to 0f)
}

private fun findWeakestArea(evaluation: SpeakingEvaluation): Pair<String, Float> {
    val areas = listOf(
        "Pronunciation" to evaluation.pronunciation.score,
        "Fluency" to evaluation.fluency.score,
        "Vocabulary" to evaluation.vocabulary.score,
        "Grammar" to evaluation.grammar.score,
        "Coherence" to evaluation.coherence.score
    )
    return areas.minByOrNull { it.second } ?: (" " to 0f)
}
