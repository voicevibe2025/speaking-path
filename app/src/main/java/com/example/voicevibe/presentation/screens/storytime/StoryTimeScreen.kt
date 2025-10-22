package com.example.voicevibe.presentation.screens.storytime

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioAttributes as ExoAudioAttributes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.ui.theme.*
import com.example.voicevibe.utils.Constants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.google.gson.Gson

data class WordAlignment(
    val word: String,
    val start: Double,
    val end: Double,
    val score: Double? = null
)

data class SegmentAlignment(
    val start: Double,
    val end: Double,
    val text: String,
    val words: List<WordAlignment>? = null
)

data class AlignmentFile(
    val segments: List<SegmentAlignment>? = null
)

data class SceneDef(
    val id: String? = null,
    val title: String? = null,
    val start: Double,
    val end: Double,
    val image: String
)

data class ScenesMeta(
    val slug: String? = null,
    val duration: Double? = null,
    val version: Int? = null
)

data class ScenesFile(
    val scenes: List<SceneDef>? = null,
    val meta: ScenesMeta? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun StoryTimeScreen(
    storySlug: String,
    onNavigateBack: () -> Unit,
    onNavigateToStory: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { 64.dp.toPx() } }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val attrs = ExoAudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(attrs, true)
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var textError by remember { mutableStateOf<String?>(null) }
    var storyText by remember { mutableStateOf<String?>(null) }
    var alignment by remember { mutableStateOf<AlignmentFile?>(null) }
    var alignmentError by remember { mutableStateOf<String?>(null) }
    var scenes by remember { mutableStateOf<ScenesFile?>(null) }
    var scenesError by remember { mutableStateOf<String?>(null) }
    val gson = remember { Gson() }
    val listState = rememberLazyListState()
    var playbackPositionMs by remember { mutableStateOf(0L) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }

    val story = remember(storySlug) { StoryCatalog.bySlug(storySlug) }
    val audioUrl = remember(story) {
        story?.let { Constants.SUPABASE_PUBLIC_STORY_AUDIO_BASE_URL + "/" + it.audioFile }
    }

    LaunchedEffect(audioUrl) {
        error = null
        if (audioUrl != null) {
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            player.prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    pendingSeekMs?.let {
                        try { player.seekTo(it) } catch (_: Throwable) {}
                        pendingSeekMs = null
                    }
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlayerError(errorEx: PlaybackException) {
                error = "Failed to play audio: ${errorEx.errorCodeName}"
                isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            try { player.release() } catch (_: Throwable) {}
        }
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

    LaunchedEffect(storySlug) {
        alignmentError = null
        alignment = null
        try {
            val json = withContext(Dispatchers.IO) {
                val path = "words/${storySlug}.words.json"
                context.assets.open(path).bufferedReader().use { it.readText() }
            }
            alignment = gson.fromJson(json, AlignmentFile::class.java)
        } catch (t: Throwable) {
            alignmentError = "Alignment not found for '${storySlug}'. Add assets/words/${storySlug}.words.json"
        }
    }

    LaunchedEffect(storySlug) {
        scenesError = null
        scenes = null
        try {
            val json = withContext(Dispatchers.IO) {
                val path = "scenes/${storySlug}.scenes.json"
                context.assets.open(path).bufferedReader().use { it.readText() }
            }
            scenes = gson.fromJson(json, ScenesFile::class.java)
        } catch (_: Throwable) {
            scenesError = null
        }
    }

    if (story == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Story time",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.sp
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f),
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
                            colors = listOf(
                                Color(0xFF0F0F1E),
                                Color(0xFF1A1A2E)
                            )
                        )
                    )
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Story not found", 
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    fun startPlayback() {
        error = null
        if (audioUrl == null) return
        try {
            pendingSeekMs?.let { player.seekTo(it) ; pendingSeekMs = null }
            player.playWhenReady = true
            player.play()
        } catch (e: Exception) {
            error = "Unable to start playback"
        }
    }

    fun pausePlayback() {
        try { player.pause() } catch (_: Throwable) {}
    }

    fun resumePlayback() {
        try { player.play() } catch (_: Throwable) { startPlayback() }
    }

    fun seekToMs(targetMs: Long) {
        try {
            player.seekTo(targetMs)
            playbackPositionMs = targetMs
        } catch (_: Throwable) {
            pendingSeekMs = targetMs
            startPlayback()
        }
    }

    LaunchedEffect(isPlaying, player) {
        while (isPlaying) {
            playbackPositionMs = player.currentPosition
            delay(80)
        }
    }

    // Animations
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            story.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        AnimatedVisibility(
                            visible = isPlaying,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .scale(pulseScale)
                                )
                                Text(
                                    "Playing",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White
                ),
                modifier = Modifier.shadow(8.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
                .padding(top = padding.calculateTopPadding())
                .pointerInput(storySlug, swipeThresholdPx) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragX += dragAmount
                        },
                        onDragEnd = {
                            if (dragX <= -swipeThresholdPx) {
                                val list = StoryCatalog.stories
                                if (list.isNotEmpty()) {
                                    val idx = list.indexOfFirst { it.slug == storySlug }
                                    val next = if (idx >= 0) (idx + 1) % list.size else 0
                                    onNavigateToStory(list[next].slug)
                                }
                            } else if (dragX >= swipeThresholdPx) {
                                val list = StoryCatalog.stories
                                if (list.isNotEmpty()) {
                                    val idx = list.indexOfFirst { it.slug == storySlug }
                                    val prev = if (idx > 0) idx - 1 else list.size - 1
                                    onNavigateToStory(list[prev].slug)
                                }
                            }
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f }
                    )
                }
        ) {
            val currentTimeSec = playbackPositionMs.toDouble() / 1000.0
            val activeScene = scenes?.scenes?.firstOrNull { s -> currentTimeSec >= s.start && currentTimeSec < s.end }
                ?: scenes?.scenes?.firstOrNull()

            // Background Scene Image with Ken Burns effect
            if (activeScene != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "kenBurns")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/${activeScene.image}")
                        .crossfade(600)
                        .build(),
                    contentDescription = activeScene.title ?: "Scene",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(scale)
                )
            }

            // Sophisticated gradient overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Moral Badge with Glass Morphism
                AnimatedVisibility(
                    visible = story.moral.isNotEmpty(),
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.15f),
                                        Color(0xFFFFA500).copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .graphicsLayer {
                                // Glass effect
                                alpha = 0.95f
                            }
                            .blur(0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                story.moral,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // Summary Card with Modern Design
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Text(
                            story.summary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 24.sp,
                                letterSpacing = 0.4.sp,
                                fontFamily = FontFamily.Default
                            ),
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                // Elegant Playback Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val hasStarted = playbackPositionMs > 0
                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    
                    FilledTonalButton(
                        onClick = { resumePlayback() },
                        enabled = !isPlaying && audioUrl != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(if (isPlayPressed) 0.96f else 1f)
                            .shadow(
                                elevation = if (isPlaying) 20.dp else 8.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                spotColor = Color(0xFFFFD700).copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isPlaying) Color(0xFF4CAF50) else Color(0xFFFFD700),
                            contentColor = Color.Black
                        ),
                        interactionSource = playInteractionSource
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                strokeWidth = 2.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Loading...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        } else {
                            Icon(
                                if (hasStarted) Icons.Default.PlayArrow else Icons.Default.Headphones,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (hasStarted) "Resume" else "Listen",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    val pauseInteractionSource = remember { MutableInteractionSource() }
                    val isPausePressed by pauseInteractionSource.collectIsPressedAsState()
                    
                    OutlinedButton(
                        onClick = { pausePlayback() },
                        enabled = isPlaying,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(if (isPausePressed) 0.96f else 1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = if (isPlaying) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        interactionSource = pauseInteractionSource
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Pause",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Error Messages with Better Styling
                AnimatedVisibility(
                    visible = error != null || textError != null || alignmentError != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE91E63).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            error?.let {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = it,
                                        color = Color(0xFFE91E63),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            textError?.let {
                                Text(
                                    text = it,
                                    color = Color(0xFFE91E63),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            alignmentError?.let {
                                Text(
                                    text = it,
                                    color = Color(0xFFE91E63),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Scene Title Badge
                AnimatedVisibility(
                    visible = activeScene != null,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    activeScene?.let { scene ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFFD700),
                                                    Color(0xFFFFA500)
                                                )
                                            )
                                        )
                                        .scale(pulseScale)
                                )
                                Text(
                                    text = scene.title ?: "Scene",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.8.sp,
                                        fontFamily = FontFamily.Default
                                    ),
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Transcript with Sophisticated Design
                val segments = alignment?.segments
                if (!segments.isNullOrEmpty()) {
                    val timeSec = playbackPositionMs.toDouble() / 1000.0
                    val activeSegmentIndex = segments.indexOfFirst { timeSec >= it.start && timeSec < it.end }
                    val activeWordIndex = if (activeSegmentIndex >= 0) {
                        val ws = segments[activeSegmentIndex].words
                        ws?.indexOfFirst { timeSec >= it.start && timeSec < it.end } ?: -1
                    } else -1

                    LaunchedEffect(activeSegmentIndex) {
                        if (activeSegmentIndex >= 0) {
                            listState.animateScrollToItem(activeSegmentIndex)
                        }
                    }

                    val configuration = LocalConfiguration.current
                    val maxTextHeight = (configuration.screenHeightDp * 0.25f).dp
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxTextHeight)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(alpha = 0.4f),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.03f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            itemsIndexed(segments) { index, seg ->
                                val ranges = remember(seg.text, seg.words) {
                                    buildWordRanges(seg.text, seg.words ?: emptyList())
                                }
                                val annotated = remember(seg.text, ranges, index == activeSegmentIndex, activeWordIndex) {
                                    buildAnnotatedSegment(seg.text, ranges, if (index == activeSegmentIndex && activeWordIndex >= 0) activeWordIndex else null)
                                }
                                val isActive = index == activeSegmentIndex
                                
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300)),
                                    exit = fadeOut(animationSpec = tween(300))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isActive) {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.08f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                } else {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color.Transparent
                                                        )
                                                    )
                                                }
                                            )
                                            .clickable {
                                                val targetMs = (seg.start * 1000).toLong()
                                                seekToMs(targetMs)
                                            }
                                            .padding(
                                                start = if (isActive) 20.dp else 16.dp,
                                                end = 16.dp,
                                                top = 12.dp,
                                                bottom = 12.dp
                                            )
                                    ) {
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(24.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFFFFD700),
                                                                Color(0xFFFFA500)
                                                            )
                                                        )
                                                    )
                                            )
                                            Spacer(Modifier.width(16.dp))
                                        }
                                        
                                        ClickableText(
                                            text = annotated,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = if (isActive) 18.sp else 16.sp,
                                                lineHeight = 26.sp,
                                                letterSpacing = 0.4.sp,
                                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                                color = Color.White.copy(alpha = if (isActive) 1.0f else 0.7f),
                                                fontFamily = FontFamily.Default
                                            ),
                                            modifier = Modifier.animateContentSize(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ),
                                            onClick = { offset ->
                                                val idx = ranges.indexOfFirst { r -> offset >= r.first && offset < r.second }
                                                if (idx >= 0) {
                                                    val target = seg.words?.getOrNull(idx)?.start
                                                    if (target != null) {
                                                        seekToMs((target * 1000).toLong())
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val configuration = LocalConfiguration.current
                    val maxTextHeight = (configuration.screenHeightDp * 0.25f).dp
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxTextHeight)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(alpha = 0.4f),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.03f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .verticalScroll(scrollState)
                                .padding(24.dp)
                        ) {
                            Text(
                                text = storyText ?: "",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 28.sp,
                                    letterSpacing = 0.4.sp,
                                    fontFamily = FontFamily.Default
                                ),
                                color = Color.White.copy(alpha = 0.95f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildWordRanges(text: String, words: List<WordAlignment>): List<Pair<Int, Int>> {
    val lower = text.lowercase()
    var cursor = 0
    val ranges = mutableListOf<Pair<Int, Int>>()
    for (w in words) {
        val token = w.word.trim().lowercase()
        if (token.isEmpty()) { ranges.add(0 to 0); continue }
        val idx = lower.indexOf(token, cursor)
        if (idx >= 0) {
            ranges.add(idx to (idx + token.length))
            cursor = idx + token.length
        } else {
            val idx2 = lower.indexOf(token)
            if (idx2 >= 0) {
                ranges.add(idx2 to (idx2 + token.length))
                cursor = idx2 + token.length
            } else {
                ranges.add(0 to 0)
            }
        }
    }
    return ranges
}

private fun buildAnnotatedSegment(
    text: String,
    ranges: List<Pair<Int, Int>>,
    activeWordIndex: Int?
): AnnotatedString {
    if (activeWordIndex == null || activeWordIndex !in ranges.indices) return AnnotatedString(text)
    val (start, end) = ranges[activeWordIndex]
    if (start >= end || start < 0 || end > text.length) return AnnotatedString(text)
    val builder = AnnotatedString.Builder()
    builder.append(text.substring(0, start))
    builder.pushStyle(
        SpanStyle(
            background = Color(0xFFFFD700).copy(alpha = 0.25f),
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = Color(0xFFFFD700).copy(alpha = 0.3f),
                offset = Offset(0f, 0f),
                blurRadius = 8f
            )
        )
    )
    builder.append(text.substring(start, end))
    builder.pop()
    builder.append(text.substring(end))
    return builder.toAnnotatedString()
}