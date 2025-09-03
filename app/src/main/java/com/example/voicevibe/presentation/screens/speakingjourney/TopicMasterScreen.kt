package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

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
    
    // State for animated entry of UI elements
    var showContent by remember { mutableStateOf(false) }
    
    // Start animation after a brief delay
    LaunchedEffect(topic) {
        delay(300)
        showContent = true
    }
    
    // Modern gradient background colors
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128), // Dark blue top
            Color(0xFF1E2761), // Mid blue-purple
            Color(0xFF0A1128)  // Dark blue bottom
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic?.title ?: "Master Topic",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Master Your Speaking Skills",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select a practice mode to improve your skills",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Practice Mode Buttons
                    PracticeModeButton(
                        title = "Pronunciation Practice",
                        description = "Perfect your pronunciation with targeted practice",
                        icon = Icons.Default.Mic,
                        onClick = { onNavigateToPronunciationPractice(topicId) },
                        isPrimary = true,
                        animationDelay = 100
                    )
                    
                    PracticeModeButton(
                        title = "Fluency Practice",
                        description = "Improve your speaking flow and natural rhythm",
                        icon = Icons.Default.RecordVoiceOver,
                        onClick = { onNavigateToFluencyPractice(topicId) },
                        isPrimary = false,
                        animationDelay = 200
                    )
                    
                    PracticeModeButton(
                        title = "Vocabulary Practice",
                        description = "Expand your vocabulary with topic-specific words",
                        icon = Icons.Default.Translate,
                        onClick = { onNavigateToVocabularyPractice(topicId) },
                        isPrimary = false,
                        animationDelay = 300
                    )
                    
                    PracticeModeButton(
                        title = "Listening Practice",
                        description = "Improve your comprehension with listening exercises",
                        icon = Icons.Default.VolumeUp,
                        onClick = { onNavigateToListeningPractice(topicId) },
                        isPrimary = false,
                        animationDelay = 400
                    )
                    
                    PracticeModeButton(
                        title = "Grammar Practice",
                        description = "Master the grammar rules for this topic",
                        icon = Icons.Default.Spellcheck,
                        onClick = { onNavigateToGrammarPractice(topicId) },
                        isPrimary = false,
                        animationDelay = 500
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Conversation Button
                    PracticeModeButton(
                        title = "Conversation Example",
                        description = "See a full conversation using this topic",
                        icon = Icons.Default.School,
                        onClick = { onNavigateToConversation(topicId) },
                        isPrimary = true,
                        animationDelay = 600
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeModeButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPrimary) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                else 
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            )
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = null
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
                        modifier = Modifier.size(32.dp),
                        tint = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = description,
                            fontSize = 14.sp,
                            color = if (isPrimary) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
