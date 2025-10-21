package com.example.voicevibe.presentation.screens.storytime

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.ui.theme.*
import com.example.voicevibe.utils.Constants
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryTimeScreen(
    storySlug: String,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var textError by remember { mutableStateOf<String?>(null) }
    var storyText by remember { mutableStateOf<String?>(null) }

    val story = remember(storySlug) { StoryCatalog.bySlug(storySlug) }
    val audioUrl = remember(story) {
        story?.let { Constants.SUPABASE_PUBLIC_STORY_AUDIO_BASE_URL + "/" + it.audioFile }
    }

    LaunchedEffect(storySlug) {
        textError = null
        storyText = null
        try {
            val text = withContext(Dispatchers.IO) {
                val path = "stories/${storySlug}.txt"
                context.assets.open(path).bufferedReader().use { it.readText() }
            }
            storyText = text
        } catch (t: Throwable) {
            textError = "Story text not found for '${storySlug}'. Please add assets/stories/${storySlug}.txt"
        }
    }

    if (story == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Story time") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandNavyDark,
                        titleContentColor = Color.White
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
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Story not found", color = Color.White)
            }
        }
        return
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (_: Throwable) {}
            mediaPlayer = null
        }
    }

    fun startPlayback() {
        if (isPreparing || isPlaying) return
        isPreparing = true
        error = null
        try {
            // Release previous instance if any
            try { mediaPlayer?.release() } catch (_: Throwable) {}

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                audioUrl?.let { setDataSource(it) }
                setOnPreparedListener {
                    it.start()
                    isPreparing = false
                    isPlaying = true
                }
                setOnCompletionListener {
                    try { it.release() } catch (_: Throwable) {}
                    if (mediaPlayer === it) mediaPlayer = null
                    isPlaying = false
                }
                setOnErrorListener { mpErr, what, extra ->
                    try { mpErr.release() } catch (_: Throwable) {}
                    if (mediaPlayer === mpErr) mediaPlayer = null
                    isPreparing = false
                    isPlaying = false
                    error = "Failed to play audio ($what/$extra)"
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            isPreparing = false
            isPlaying = false
            error = "Unable to start playback"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandNavyDark,
                    titleContentColor = Color.White
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Moral and summary
                Text(story.moral, style = MaterialTheme.typography.titleMedium, color = BrandCyan)
                Text(story.summary, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))

                // Listen CTA
                Button(
                    onClick = { startPlayback() },
                    enabled = !isPreparing && !isPlaying && audioUrl != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandCyan,
                        contentColor = BrandNavyDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BrandNavyDark,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Preparing...")
                    } else if (isPlaying) {
                        Icon(Icons.Default.Headphones, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Playing...")
                    } else {
                        Icon(Icons.Default.Headphones, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Listen")
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = BrandFuchsia,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (textError != null) {
                    Text(
                        text = textError!!,
                        color = BrandFuchsia,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Story content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = storyText ?: "",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
