package com.example.voicevibe.presentation.screens.speakingjourney

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.R
import com.example.voicevibe.data.remote.api.CoachAnalysisDto
import com.example.voicevibe.presentation.components.FloatingParticles
import com.example.voicevibe.presentation.components.ModernTopBar
import com.example.voicevibe.ui.theme.BrandCyan
import com.example.voicevibe.ui.theme.BrandFuchsia
import com.example.voicevibe.ui.theme.BrandIndigo
import java.util.Locale

/**
 * Screen showing lesson content for the selected topic in Speaking Journey.
 * Contains ConversationLesson, AI Coach card, Learn with Vivi button, and Master button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakingLessonScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTopicMaster: (String) -> Unit,
    onNavigateToLearnWithVivi: (String) -> Unit,
    onNavigateToConversationPractice: (String) -> Unit = {},
    onNavigateToVocabularyLesson: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    
    // Find the topic by ID
    val topic = ui.topics.firstOrNull { it.id == topicId }
    
    // Animated brand background
    val backgroundTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by backgroundTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2642))
    ) {
        FloatingParticles()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ModernTopBar(
                    title = topic?.title ?: "Speaking Lesson",
                    onNavigationIconClick = onNavigateBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (ui.isLoading && topic == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (topic == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Topic not found",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    // Compute dynamic height for the Conversation area
                    val configuration = LocalConfiguration.current
                    val conversationAreaHeight = remember(configuration) {
                        val h = configuration.screenHeightDp.toFloat()
                        ((h * 0.55f).coerceIn(305f, 425f)).dp
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Conversation Lesson area
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.03f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(conversationAreaHeight)
                            ) {
                                ConversationLessonScreen(
                                    topicId = topic.id,
                                    onNavigateBack = {},
                                    embedded = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(conversationAreaHeight)
                                )
                            }
                        }

                        // AI Coach Card
                        val coachVM: CoachViewModel = hiltViewModel()
                        val coachUi by coachVM.ui
                        LaunchedEffect(Unit) { coachVM.refreshIfNeeded() }

                        fun handleNextBestAction(analysis: CoachAnalysisDto) {
                            val nba = analysis.nextBestActions.firstOrNull()
                            if (nba == null) {
                                onNavigateToTopicMaster(topic.id)
                                return
                            }
                            val link = nba.deeplink
                            try {
                                val uri = Uri.parse(link)
                                val segs = uri.pathSegments
                                val tIdx = segs.indexOfFirst { it.equals("topic", ignoreCase = true) }
                                val linkTopicId = if (tIdx != -1 && tIdx + 1 < segs.size) segs[tIdx + 1] else "current"
                                val mode = if (tIdx != -1 && tIdx + 2 < segs.size) segs[tIdx + 2] else "master"
                                val requestedId = if (linkTopicId == "current") topic.id else linkTopicId
                                val targetId = when {
                                    ui.topics.any { it.id == requestedId } -> requestedId
                                    else -> topic.id
                                }
                                when (mode.lowercase(Locale.ROOT)) {
                                    "conversation" -> onNavigateToConversationPractice(targetId)
                                    "vocab", "vocabulary" -> onNavigateToVocabularyLesson(targetId)
                                    else -> onNavigateToTopicMaster(targetId)
                                }
                            } catch (_: Throwable) {
                                onNavigateToTopicMaster(topic.id)
                            }
                        }

                        coachUi.analysis?.let { analysis ->
                            Spacer(modifier = Modifier.height(12.dp))
                            AiCoachCard(
                                analysis = analysis,
                                onActionClick = { handleNextBestAction(analysis) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Learn with Vivi button
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = { onNavigateToLearnWithVivi(topic.id) },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(2.dp, Color(0xFF667EEA)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF667EEA)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Learn with Vivi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Master [TOPIC] button
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = { onNavigateToTopicMaster(topic.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandIndigo,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Master ${topic.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
