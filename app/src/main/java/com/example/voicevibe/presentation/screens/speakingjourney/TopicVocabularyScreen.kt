package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.presentation.components.ModernTopBar
import com.example.voicevibe.ui.theme.BrandCyan
import com.example.voicevibe.ui.theme.BrandFuchsia
import com.example.voicevibe.ui.theme.BrandIndigo
import kotlinx.coroutines.launch

data class VocabularyCard(
    val word: String,
    val definition: String = "",
    val example: String = "",
    val phonetic: String = "",
    val isLearned: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopicVocabularyScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    viewModel: SpeakingJourneyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState
    val currentTopic = uiState.topics.find { it.id == topicId }
    
    // Initialize vocabulary cards
    var vocabularyCards by remember { mutableStateOf<List<VocabularyCard>>(emptyList()) }
    
    LaunchedEffect(currentTopic) {
        currentTopic?.vocabulary?.let { words ->
            vocabularyCards = words.map { word ->
                VocabularyCard(
                    word = word,
                    isLoading = true
                )
            }
            // Generate content for each word
            words.forEachIndexed { index, word ->
                viewModel.generateVocabularyContent(
                    word = word,
                    topicTitle = currentTopic.title,
                    onResult = { definition, example, phonetic ->
                        vocabularyCards = vocabularyCards.toMutableList().also {
                            it[index] = it[index].copy(
                                definition = definition,
                                example = example,
                                phonetic = phonetic,
                                isLoading = false
                            )
                        }
                    }
                )
            }
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { if (vocabularyCards.isEmpty()) 1 else vocabularyCards.size })
    val scope = rememberCoroutineScope()
    
    val learnedCount = vocabularyCards.count { it.isLearned }
    val totalCount = vocabularyCards.size
    val progress = if (totalCount > 0) learnedCount.toFloat() / totalCount else 0f
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        VocabularyBackground()
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ModernTopBar(
                    title = currentTopic?.title ?: "Vocabulary",
                    onNavigationIconClick = onNavigateBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Header with progress
                VocabularyHeader(
                    learnedCount = learnedCount,
                    totalCount = totalCount,
                    progress = progress
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pager for flashcards
                if (vocabularyCards.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        val card = vocabularyCards[page]
                        FlashcardView(
                            card = card,
                            onPlayAudio = {
                                viewModel.playTTS(card.word, context)
                            },
                            onMarkLearned = {
                                vocabularyCards = vocabularyCards.toMutableList().also {
                                    it[page] = it[page].copy(isLearned = !it[page].isLearned)
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Navigation controls
                    NavigationControls(
                        currentPage = pagerState.currentPage,
                        totalPages = vocabularyCards.size,
                        onPrevious = {
                            scope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        onNext = {
                            scope.launch {
                                if (pagerState.currentPage < vocabularyCards.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        onPlayAudio = {
                            val currentCard = vocabularyCards[pagerState.currentPage]
                            viewModel.playTTS(currentCard.word, context)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No vocabulary words available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabularyBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F1B4A),
                        Color(0xFF2C2F6D)
                    )
                )
            )
    )
}

@Composable
private fun VocabularyHeader(
    learnedCount: Int,
    totalCount: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = BrandCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vocabulary Practice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "$learnedCount / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BrandCyan,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Swipe or use buttons to navigate • Tap card to flip",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FlashcardView(
    card: VocabularyCard,
    onPlayAudio: () -> Unit,
    onMarkLearned: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "CardFlip"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .padding(horizontal = 24.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isFlipped = !isFlipped },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BrandIndigo.copy(alpha = 0.3f),
                                BrandFuchsia.copy(alpha = 0.2f)
                            )
                        )
                    )
            ) {
                if (rotation <= 90f) {
                    // Front side - Word
                    FlashcardFront(card = card)
                } else {
                    // Back side - Definition & Example (flipped horizontally for readability)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        FlashcardBack(card = card, onPlayAudio = onPlayAudio)
                    }
                }
                
                // Learned badge (always visible)
                if (card.isLearned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                color = Color(0xFF4CAF50),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Learned",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Mark as learned button (floating bottom)
        FloatingActionButton(
            onClick = onMarkLearned,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (card.isLearned) Color(0xFF4CAF50) else BrandCyan,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (card.isLearned) Icons.Default.Check else Icons.Default.Star,
                contentDescription = if (card.isLearned) "Mark as not learned" else "Mark as learned"
            )
        }
    }
}

@Composable
private fun FlashcardFront(card: VocabularyCard) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = BrandCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (card.isLoading) {
                CircularProgressIndicator(
                    color = BrandCyan,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = card.word,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                if (card.phonetic.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.phonetic,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandCyan.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Tap to see definition",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FlashcardBack(card: VocabularyCard, onPlayAudio: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (card.isLoading) {
            CircularProgressIndicator(
                color = BrandCyan,
                modifier = Modifier.size(40.dp)
            )
        } else {
            // Word title
            Text(
                text = card.word,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Audio button
            IconButton(
                onClick = onPlayAudio,
                modifier = Modifier
                    .size(48.dp)
                    .background(BrandCyan.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Play pronunciation",
                    tint = BrandCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Definition
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Definition",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.definition.ifEmpty { "No definition available" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        lineHeight = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Example
            if (card.example.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = BrandFuchsia,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Example",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandFuchsia,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${card.example}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 22.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationControls(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayAudio: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentPage > 0,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Previous")
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Play audio button (center)
        FloatingActionButton(
            onClick = onPlayAudio,
            containerColor = BrandCyan,
            contentColor = Color.White,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Play audio",
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Next button
        Button(
            onClick = onNext,
            enabled = currentPage < totalPages - 1,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandIndigo,
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Next")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Next",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
