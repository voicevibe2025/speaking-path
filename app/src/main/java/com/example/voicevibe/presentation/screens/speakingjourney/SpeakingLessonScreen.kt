package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.R
import com.example.voicevibe.presentation.components.ModernTopBar
import com.example.voicevibe.ui.theme.BrandIndigo

/**
 * Screen showing lesson content for the selected topic in Speaking Journey.
 * Contains ConversationLesson, Learn with Vivi button, and Master button.
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

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Conversation Lesson area - dynamically fills available space
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Fill remaining space
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.03f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            ConversationLessonScreen(
                                topicId = topic.id,
                                onNavigateBack = {},
                                embedded = true,
                                modifier = Modifier.fillMaxSize()
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

