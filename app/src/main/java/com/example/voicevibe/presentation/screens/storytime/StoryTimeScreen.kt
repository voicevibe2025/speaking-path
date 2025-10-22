package com.example.voicevibe.presentation.screens.storytime

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioAttributes as ExoAudioAttributes
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
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
                error = "Failed to play audio: ${'$'}{errorEx.errorCodeName}"
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

            if (activeScene != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/${activeScene.image}")
                        .crossfade(true)
                        .build(),
                    contentDescription = activeScene.title ?: "Scene",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    story.moral,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFFFFD700)
                )
                Text(
                    story.summary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = Color.White.copy(alpha = 0.98f)
                )

                // Playback controls
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val hasStarted = playbackPositionMs > 0
                    Button(
                        onClick = { resumePlayback() },
                        enabled = !isPlaying && audioUrl != null,
                        modifier = Modifier.weight(1f),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BrandNavyDark,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Preparing...")
                        } else {
                            Icon(Icons.Default.Headphones, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (hasStarted) "Resume" else "Play")
                        }
                    }

                    OutlinedButton(
                        onClick = { pausePlayback() },
                        enabled = isPlaying,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Pause", fontWeight = FontWeight.SemiBold)
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
                if (alignmentError != null) {
                    Text(
                        text = alignmentError!!,
                        color = BrandFuchsia,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (activeScene != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFFFFD700))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = activeScene.title ?: "Scene",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                
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
                            // bring active segment into view
                            listState.animateScrollToItem(activeSegmentIndex)
                        }
                    }

                    val configuration = LocalConfiguration.current
                    val maxTextHeight = (configuration.screenHeightDp * 0.33f).dp
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxTextHeight),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
                    ) {
                        itemsIndexed(segments) { index, seg ->
                            val ranges = remember(seg.text, seg.words) {
                                buildWordRanges(seg.text, seg.words ?: emptyList())
                            }
                            val annotated = remember(seg.text, ranges, index == activeSegmentIndex, activeWordIndex) {
                                buildAnnotatedSegment(seg.text, ranges, if (index == activeSegmentIndex && activeWordIndex >= 0) activeWordIndex else null)
                            }
                            val isActive = index == activeSegmentIndex
                            val isLast = index == segments.lastIndex
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val targetMs = (seg.start * 1000).toLong()
                                        seekToMs(targetMs)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Row {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color(0xFFFFD700),
                                                            Color(0xFFFFA500)
                                                        )
                                                    )
                                                )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(
                                                top = 12.dp,
                                                bottom = if (isLast) 0.dp else 12.dp,
                                                start = if (isActive) 8.dp else 16.dp,
                                                end = if (isActive) 8.dp else 16.dp
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = if (isActive) 0.65f else 0.45f),
                                                        Color.Black.copy(alpha = if (isActive) 0.5f else 0.35f)
                                                    )
                                                )
                                            )
                                            .padding(
                                                start = 16.dp,
                                                top = 16.dp,
                                                end = 16.dp,
                                                bottom = if (isLast) 4.dp else 16.dp
                                            )
                                    ) {
                                        ClickableText(
                                            text = annotated,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = if (isActive) 19.sp else 17.sp,
                                                lineHeight = 26.sp,
                                                letterSpacing = 0.3.sp,
                                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                                color = Color.White.copy(alpha = if (isActive) 1.0f else 0.85f)
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
                    val maxTextHeight = (configuration.screenHeightDp * 0.33f).dp
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxTextHeight)
                            .verticalScroll(scrollState),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = storyText ?: "",
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White.copy(alpha = 0.98f),
                            textAlign = TextAlign.Start
                        )
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
            // Fallback: try from beginning to handle wrap or punctuation spacing
            val idx2 = lower.indexOf(token)
            if (idx2 >= 0) {
                ranges.add(idx2 to (idx2 + token.length))
                cursor = idx2 + token.length
            } else {
                // No match; add empty range
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
            background = Color(0xFFFFD700).copy(alpha = 0.5f),
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    )
    builder.append(text.substring(start, end))
    builder.pop()
    builder.append(text.substring(end))
    return builder.toAnnotatedString()
}
