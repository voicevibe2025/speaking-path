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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryTimeScreen(
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val audioUrl = remember {
        Constants.SUPABASE_PUBLIC_STORY_AUDIO_BASE_URL + "/malin_kundang.ogg"
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
                setDataSource(audioUrl)
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
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Malin Kundang",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )

                // Listen CTA
                Button(
                    onClick = { startPlayback() },
                    enabled = !isPreparing && !isPlaying,
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

                // Story content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = MALIN_KUNDANG_STORY,
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

private const val MALIN_KUNDANG_STORY = """
Di sebuah kampung nelayan di Sumatra Barat, hiduplah seorang anak bernama Malin Kundang bersama ibunya. Mereka hidup sederhana. Ketika dewasa, Malin merantau untuk mencari keberuntungan di perantauan.

Bertahun-tahun kemudian, Malin kembali sebagai saudagar kaya dengan kapal megah. Ibunya yang rindu berlari menyambut, namun Malin malu mengakui ibunya di depan istri dan awak kapalnya. Ia menghardik dan mengusir ibunya.

Dengan hati hancur, sang ibu menengadahkan tangan dan berdoa. Tiba-tiba badai dahsyat datang, petir menyambar, dan kapal Malin hancur. Malin pun dikutuk menjadi batu di tepi pantai, sebagai pelajaran tentang durhaka pada orang tua.
"""
