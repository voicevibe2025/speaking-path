package com.example.voicevibe.presentation.screens.storytime

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.ui.theme.*
import com.example.voicevibe.utils.Constants
import androidx.compose.ui.platform.LocalContext
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
    var alignment by remember { mutableStateOf<AlignmentFile?>(null) }
    var alignmentError by remember { mutableStateOf<String?>(null) }
    val gson = remember { Gson() }
    val listState = rememberLazyListState()
    var playbackPositionMs by remember { mutableStateOf(0L) }

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

    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isPlaying) {
            playbackPositionMs = mediaPlayer?.currentPosition?.toLong() ?: 0L
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
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                if (alignmentError != null) {
                    Text(
                        text = alignmentError!!,
                        color = BrandFuchsia,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Story content (highlighted if alignment available)
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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(segments) { index, seg ->
                            val annotated = remember(seg.text, index == activeSegmentIndex, activeWordIndex) {
                                val ranges = buildWordRanges(seg.text, seg.words ?: emptyList())
                                buildAnnotatedSegment(seg.text, ranges, if (index == activeSegmentIndex && activeWordIndex >= 0) activeWordIndex else null)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (index == activeSegmentIndex) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = annotated,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
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
                }

                Spacer(modifier = Modifier.height(24.dp))
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
    builder.pushStyle(SpanStyle(background = BrandCyan.copy(alpha = 0.35f), color = Color.White))
    builder.append(text.substring(start, end))
    builder.pop()
    builder.append(text.substring(end))
    return builder.toAnnotatedString()
}
