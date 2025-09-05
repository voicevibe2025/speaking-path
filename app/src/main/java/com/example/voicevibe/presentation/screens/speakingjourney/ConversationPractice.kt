package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.R

/**
 * ConversationPractice screen
 * - Tap Start to play the first turn
 * - Long press Start to play the whole conversation
 * - Prev/Next buttons to step through turns manually
 * - Uses SpeakingJourneyViewModel.speakWithBackendTts for TTS (same as TopicConversationScreen)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }
    val conversation = topic?.conversation ?: emptyList()

    // Playback state
    var currentIndex by remember(conversation) { mutableStateOf(0) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlayingAll by remember { mutableStateOf(false) }

    fun resetPlaybackFlags() {
        isPlayingAll = false
        currentlyPlayingId = null
    }

    fun playTurn(index: Int) {
        if (conversation.isEmpty()) return
        val i = index.coerceIn(0, conversation.lastIndex)
        currentIndex = i
        val turn = conversation[i]
        val id = turn.text
        isPlayingAll = false // single play
        viewModel.speakWithBackendTts(
            text = turn.text,
            onStart = { currentlyPlayingId = id },
            onDone = { currentlyPlayingId = null },
            onError = { _ -> resetPlaybackFlags() }
        )
    }

    fun playAllFrom(start: Int = 0) {
        if (conversation.isEmpty()) return
        if (isPlayingAll || currentlyPlayingId != null) return
        val startIdx = start.coerceIn(0, conversation.lastIndex)
        isPlayingAll = true
        currentIndex = startIdx

        fun playNext(i: Int) {
            if (!isPlayingAll) return
            if (i >= conversation.size) {
                resetPlaybackFlags()
                return
            }
            val t = conversation[i]
            val id = t.text
            currentIndex = i
            viewModel.speakWithBackendTts(
                text = t.text,
                onStart = { currentlyPlayingId = id },
                onDone = { playNext(i + 1) },
                onError = { _ -> resetPlaybackFlags() }
            )
        }
        playNext(startIdx)
    }

    // Stop playback when leaving
    DisposableEffect(Unit) {
        onDispose {
            resetPlaybackFlags()
            viewModel.stopPlayback()
        }
    }

    // Background gradient and image per topic
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topic?.title ?: "Conversation",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        resetPlaybackFlags()
                        viewModel.stopPlayback()
                        onNavigateBack()
                    }) {
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
            // Background image based on topic title
            topic?.let { t ->
                val resId = remember(t.title) { getTopicDrawableId(context, t.title) }
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Scrim for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }

            when {
                topic == null && ui.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                topic == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Conversation not available",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                conversation.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No conversation example for this topic",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    val current = conversation.getOrNull(currentIndex)
                    val isSpeakerA = current?.speaker.equals("A", ignoreCase = true)
                    val playing = currentlyPlayingId == current?.text
                    val scale by animateFloatAsState(
                        targetValue = if (playing) 1.03f else 1f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Characters row with speech bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left character (Speaker A)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_male_head),
                                    contentDescription = "Speaker A",
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                )
                            }

                            // Speech bubble centered, aligned towards speaking side
                            Box(modifier = Modifier.weight(1f)) {
                                current?.let { turn ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isSpeakerA) Arrangement.Start else Arrangement.End
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSpeakerA)
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = if (playing) 8.dp else 2.dp),
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .scale(scale)
                                        ) {
                                            Text(
                                                text = turn.text,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                color = if (isSpeakerA) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Right character (Speaker B)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_female_head),
                                    contentDescription = "Speaker B",
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }

                        // Controls row: Prev - Start (click/long press) - Next
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newIdx = (currentIndex - 1).coerceAtLeast(0)
                                    playTurn(newIdx)
                                },
                                enabled = currentIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Start button with long-press to play all
                            Surface(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = { playTurn(0) },
                                        onLongClick = { playAllFrom(0) }
                                    )
                                    .clip(RoundedCornerShape(24.dp)),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Start",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val newIdx = (currentIndex + 1).coerceAtMost(conversation.lastIndex)
                                    playTurn(newIdx)
                                },
                                enabled = currentIndex < conversation.lastIndex
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper: find background image by topic title; falls back to launcher background
private fun getTopicDrawableId(context: android.content.Context, topicTitle: String): Int {
    val resourceName = topicTitle.lowercase(java.util.Locale.ROOT)
        .replace(" ", "_")
        .replace("-", "_")
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    return if (resId != 0) resId else R.drawable.ic_launcher_background
}
