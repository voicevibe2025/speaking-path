package com.example.voicevibe.presentation.screens.speakingjourney

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import com.example.voicevibe.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicConversationScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Local TTS setup (matches SpeakingJourneyScreen approach)
    val tts = remember(context) {
        var ref: TextToSpeech? = null
        val created = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                try { ref?.language = Locale.US } catch (_: Throwable) {}
            }
        }
        ref = created
        created
    }
    DisposableEffect(tts) {
        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Throwable) { }
        }
    }
    fun speak(text: String) {
        try { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.currentTimeMillis()}") } catch (_: Throwable) { }
    }

    val viewModel: SpeakingJourneyViewModel = hiltViewModel()
    val ui by viewModel.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }
    
    // State for animated entry of messages
    var showMessages by remember { mutableStateOf(false) }
    
    // Start animation after a brief delay
    LaunchedEffect(topic) {
        delay(300)
        showMessages = true
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
                            text = topic?.title ?: "Conversation",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (topic?.conversation?.isNotEmpty() == true) {
                FloatingActionButton(
                    onClick = {
                        val combined = topic.conversation.joinToString("\n") { it.text }
                        speak(combined)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play All",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            when {
                topic == null && ui.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                topic == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Conversation not available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 24.dp, horizontal = 32.dp)
                            )
                        }
                    }
                }
                topic.conversation.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "No conversation example for this topic",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 24.dp, horizontal = 32.dp)
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Conversation topic title
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Conversation Example",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Conversation bubbles in a LazyColumn for better performance
                        val listState = rememberLazyListState()
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(topic.conversation) { index, turn ->
                                val isSpeakerA = turn.speaker.equals("A", ignoreCase = true)
                                
                                // Animated entry for each message
                                AnimatedVisibility(
                                    visible = showMessages,
                                    enter = slideInVertically(
                                        initialOffsetY = { 100 },
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = FastOutSlowInEasing,
                                            delayMillis = 100 * index
                                        )
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            delayMillis = 100 * index
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isSpeakerA) Arrangement.Start else Arrangement.End
                                    ) {
                                        if (!isSpeakerA) {
                                            Spacer(modifier = Modifier.weight(0.15f))
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .padding(vertical = 2.dp)
                                        ) {
                                            // Chat bubble with avatar
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = if (isSpeakerA) Arrangement.Start else Arrangement.End,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isSpeakerA) {
                                                    // Avatar for Speaker A
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(end = 8.dp)
                                                            .size(40.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                                CircleShape
                                                            )
                                                            .padding(4.dp)
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_male_head),
                                                            contentDescription = "Speaker A",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                        )
                                                    }
                                                }
                                                
                                                // Chat bubble
                                                Card(
                                                    shape = RoundedCornerShape(
                                                        topStart = if (isSpeakerA) 4.dp else 16.dp,
                                                        topEnd = if (isSpeakerA) 16.dp else 4.dp,
                                                        bottomStart = 16.dp,
                                                        bottomEnd = 16.dp
                                                    ),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSpeakerA) 
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                                        else 
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    ),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                                                    ) {
                                                        Text(
                                                            text = turn.text,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isSpeakerA) 
                                                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                                                  else 
                                                                    Color.White
                                                        )
                                                        
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        
                                                        // Play button for this message
                                                        IconButton(
                                                            onClick = { speak(turn.text) },
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSpeakerA)
                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                                    else
                                                                        Color.White.copy(alpha = 0.15f)
                                                                )
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                                contentDescription = "Play",
                                                                tint = if (isSpeakerA) 
                                                                        MaterialTheme.colorScheme.primary
                                                                      else 
                                                                        Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                if (!isSpeakerA) {
                                                    // Avatar for Speaker B
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = 8.dp)
                                                            .size(40.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                                CircleShape
                                                            )
                                                            .padding(4.dp)
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_female_head),
                                                            contentDescription = "Speaker B",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (isSpeakerA) {
                                            Spacer(modifier = Modifier.weight(0.15f))
                                        }
                                    }
                                }
                            }
                            
                            // Add space at the bottom for FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
