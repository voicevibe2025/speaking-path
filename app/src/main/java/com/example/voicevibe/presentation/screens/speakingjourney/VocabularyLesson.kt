package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.example.voicevibe.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SheetContent(
    val title: String,
    val icon: ImageVector,
    val content: String,
    val isLoading: Boolean = false,
    val color: Color
)

data class ActionItem(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyLessonScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val ui by sjVM.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }

    var selectedIndex by remember { mutableStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var sheetContent by remember { mutableStateOf<SheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Keep ViewModel's selectedTopicIdx in sync
    LaunchedEffect(topicId, ui.topics) {
        val idx = ui.topics.indexOfFirst { it.id == topicId }
        if (idx >= 0 && ui.selectedTopicIdx != idx) {
            sjVM.selectTopic(idx)
        }
    }

    val scope = rememberCoroutineScope()
    val ai = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    val vocabulary = topic?.vocabulary ?: emptyList()
    if (selectedIndex !in vocabulary.indices) selectedIndex = 0

    fun showContent(title: String, icon: ImageVector, prompt: String, color: Color) {
        sheetContent = SheetContent(title, icon, "", true, color)
        showBottomSheet = true
        scope.launch {
            try {
                val res = ai.generateContent(prompt)
                val cleanedText = res.text?.trim()?.replace("*", "") ?: ""
                sheetContent = sheetContent?.copy(
                    content = cleanedText,
                    isLoading = false
                )
            } catch (t: Throwable) {
                sheetContent = sheetContent?.copy(
                    content = "Something went wrong. Please try again.",
                    isLoading = false
                )
            }
        }
    }

    fun pronounce(word: String) {
    sjVM.speakWithBackendTts(
        text = word,
        onStart = {},
        onDone = {},
        onError = { err ->
            sheetContent = SheetContent(
                title = "Playback Error",
                icon = Icons.Filled.Close,
                content = err,
                color = Color(0xFFE91E63)  // Use a hardcoded error color instead
            )
            showBottomSheet = true
        }
    )
}

    val backgroundColors = listOf(
        Color(0xFF1a1a2e),
        Color(0xFF16213e),
        Color(0xFF0f3460),
        Color(0xFF16213e),
        Color(0xFF1a1a2e)
    )

    DisposableEffect(Unit) { onDispose { sjVM.stopPlayback() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vocabulary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (vocabulary.isNotEmpty()) {
                            Text(
                                text = "${selectedIndex + 1} of ${vocabulary.size} words",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            sjVM.stopPlayback()
                            onNavigateBack()
                        }
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
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = backgroundColors)
                )
                .padding(innerPadding)
        ) {
            when {
                topic == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Topic not found",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                vocabulary.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = "No vocabulary for this topic yet",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))

                        // Word Card with Animation
                        AnimatedContent(
                            targetState = selectedIndex,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it } + fadeOut()
                                } else {
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }
                            },
                            label = "word_animation"
                        ) { index ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        spotColor = Color(0xFF4a7c7e)
                                    ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFe8f5e9),
                                                    Color(0xFFc8e6c9),
                                                    Color(0xFFa5d6a7)
                                                ),
                                                radius = 600f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = vocabulary[index],
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1b5e20)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Action Cards Grid
                        val actions = listOf(
                            ActionItem(
                                icon = Icons.Filled.Lightbulb,
                                label = "Definition",
                                color = Color(0xFF4CAF50),
                                onClick = {
                                    val w = vocabulary[selectedIndex]
                                    showContent(
                                        title = "Definition",
                                        icon = Icons.Filled.Lightbulb,
                                        prompt = "Define '$w' clearly and concisely for English learners (A2-B1 level). Include the core meaning, common usage context, and any important nuances. Keep it under 100 words.",
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            ),
                            ActionItem(
                                icon = Icons.Filled.AutoStories,
                                label = "Examples",
                                color = Color(0xFF2196F3),
                                onClick = {
                                    val w = vocabulary[selectedIndex]
                                    showContent(
                                        title = "Example Sentences",
                                        icon = Icons.Filled.AutoStories,
                                        prompt = "Create 4 diverse example sentences using '$w' that show different contexts and uses. Make them practical and relevant for everyday situations. Each sentence should be on a new line.",
                                        color = Color(0xFF2196F3)
                                    )
                                }
                            ),
                            ActionItem(
                                icon = Icons.Filled.SwapHoriz,
                                label = "Synonyms",
                                color = Color(0xFFFF9800),
                                onClick = {
                                    val w = vocabulary[selectedIndex]
                                    showContent(
                                        title = "Synonyms & Antonyms",
                                        icon = Icons.Filled.SwapHoriz,
                                        prompt = "List 5 synonyms for '$w' with brief explanations of subtle differences. Then list 3 antonyms if applicable. Format clearly with bullet points.",
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            ),
                            ActionItem(
                                icon = Icons.Filled.Category,
                                label = "Grammar",
                                color = Color(0xFF9C27B0),
                                onClick = {
                                    val w = vocabulary[selectedIndex]
                                    showContent(
                                        title = "Grammar & Usage",
                                        icon = Icons.Filled.Category,
                                        prompt = "Explain the grammatical properties of '$w': part of speech, common patterns, collocations, and any irregular forms. Include practical usage tips.",
                                        color = Color(0xFF9C27B0)
                                    )
                                }
                            ),
                            ActionItem(
                                icon = Icons.Filled.History,
                                label = "Etymology",
                                color = Color(0xFFE91E63),
                                onClick = {
                                    val w = vocabulary[selectedIndex]
                                    showContent(
                                        title = "Word Origin",
                                        icon = Icons.Filled.History,
                                        prompt = "Tell the fascinating story of where '$w' comes from. Include its etymology, how its meaning evolved, and any interesting historical facts. Make it engaging and memorable.",
                                        color = Color(0xFFE91E63)
                                    )
                                }
                            ),
                            ActionItem(
                                icon = Icons.Filled.RecordVoiceOver,
                                label = "Pronounce",
                                color = Color(0xFF00BCD4),
                                onClick = {
                                    pronounce(vocabulary[selectedIndex])
                                }
                            )
                        )

                        actions.chunked(3).forEach { rowActions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowActions.forEach { action ->
                                    ActionCard(
                                        action = action,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty spaces in the last row
                                repeat(3 - rowActions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Spacer(Modifier.weight(1f))

                        // Word Navigation
                        WordNavigationBar(
                            currentIndex = selectedIndex,
                            totalWords = vocabulary.size,
                            onIndexChanged = { selectedIndex = it }
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // Bottom Sheet for AI Content
            if (showBottomSheet) {
                sheetContent?.let { content ->
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState,
                        containerColor = Color(0xFF1e1e2e),
                        contentColor = Color.White,
                        dragHandle = {
                            Surface(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 48.dp, height = 4.dp)
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = content.icon,
                                    contentDescription = null,
                                    tint = content.color,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = content.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            if (content.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = content.color,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "Generating insights...",
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF2d2d44)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = content.content,
                                        modifier = Modifier.padding(20.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.9f),
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    action: ActionItem,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                isPressed = true
                action.onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.15f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    action.color.copy(alpha = 0.5f),
                    action.color.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = action.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = action.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
fun WordNavigationBar(
    currentIndex: Int,
    totalWords: Int,
    onIndexChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(List(totalWords) { it }) { index, _ ->
                WordIndicator(
                    isSelected = index == currentIndex,
                    onClick = { onIndexChanged(index) }
                )
                if (index < totalWords - 1) {
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun WordIndicator(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "indicator_color"
    )
    
    val width by animateFloatAsState(
        targetValue = if (isSelected) 32f else 8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicator_width"
    )

    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() }
    )
}
