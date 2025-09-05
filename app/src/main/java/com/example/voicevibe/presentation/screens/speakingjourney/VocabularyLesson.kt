package com.example.voicevibe.presentation.screens.speakingjourney

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.example.voicevibe.BuildConfig
import kotlinx.coroutines.launch

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

    // Keep ViewModel's selectedTopicIdx in sync and update last visited topic
    LaunchedEffect(topicId, ui.topics) {
        val idx = ui.topics.indexOfFirst { it.id == topicId }
        if (idx >= 0 && ui.selectedTopicIdx != idx) {
            sjVM.selectTopic(idx)
        }
    }

    // Overlay state for AI response
    var showOverlay by remember { mutableStateOf(false) }
    var overlayTitle by remember { mutableStateOf("") }
    var overlayText by remember { mutableStateOf("") }
    var overlayLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val ai = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    val vocabulary = topic?.vocabulary ?: emptyList()
    if (selectedIndex !in vocabulary.indices) selectedIndex = 0

    fun askAI(title: String, prompt: String) {
        overlayTitle = title
        overlayText = ""
        overlayLoading = true
        showOverlay = true
        scope.launch {
            try {
                val res = ai.generateContent(prompt)
                overlayText = res.text?.trim().orEmpty()
            } catch (t: Throwable) {
                overlayText = t.message ?: "Something went wrong. Please try again."
            } finally {
                overlayLoading = false
            }
        }
    }

    fun pronounce(word: String) {
        sjVM.speakWithBackendTts(
            text = word,
            onStart = {},
            onDone = {},
            onError = { err ->
                overlayTitle = "Playback error"
                overlayText = err
                overlayLoading = false
                showOverlay = true
            }
        )
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )

    DisposableEffect(Unit) { onDispose { sjVM.stopPlayback() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vocabulary",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            sjVM.stopPlayback()
                            onNavigateBack()
                        },
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
                .background(background)
                .padding(innerPadding)
        ) {
            when {
                topic == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
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
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
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
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(12.dp))

                        ElevatedCard(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = vocabulary[selectedIndex].uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        @Composable
                        fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
                            FilledTonalButton(
                                onClick = onClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(label)
                                }
                            }
                        }

                        // Two-column grid: 3 rows
                        Row(Modifier.fillMaxWidth()) {
                            ActionButton(Icons.Filled.Translate, "Explain") {
                                val w = vocabulary[selectedIndex]
                                askAI(
                                    title = "Explain: ${w}",
                                    prompt = "Explain the word '" + w + "' in simple terms for English learners (A2-B1). Include a short definition and 2 key usage notes. Keep under 120 words."
                                )
                            }
                            ActionButton(Icons.Filled.LibraryBooks, "Example") {
                                val w = vocabulary[selectedIndex]
                                askAI(
                                    title = "Examples: ${w}",
                                    prompt = "Give 3 example sentences using the word '" + w + "' at A2-B1 level. Put each sentence on a new line and keep them short."
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            ActionButton(Icons.Filled.Help, "Synonyms") {
                                val w = vocabulary[selectedIndex]
                                askAI(
                                    title = "Synonyms: ${w}",
                                    prompt = "List 5 common synonyms of '" + w + "' and 3 antonyms if relevant. Provide brief notes on subtle differences. Format as bullet points."
                                )
                            }
                            ActionButton(Icons.Filled.Science, "Class") {
                                val w = vocabulary[selectedIndex]
                                askAI(
                                    title = "Word Class: ${w}",
                                    prompt = "State the part(s) of speech of '" + w + "' (noun/verb/adjective/etc.). If multiple, list each with a short usage note."
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            ActionButton(Icons.Filled.LibraryBooks, "Origin") {
                                val w = vocabulary[selectedIndex]
                                askAI(
                                    title = "Origin: ${w}",
                                    prompt = "Briefly explain the origin/etymology of '" + w + "' in one short paragraph."
                                )
                            }
                            ActionButton(Icons.Filled.Mic, "Pronounce") {
                                pronounce(vocabulary[selectedIndex])
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (selectedIndex > 0) selectedIndex-- },
                                enabled = selectedIndex > 0,
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                                Spacer(Modifier.size(6.dp))
                                Text("Prev")
                            }
                            OutlinedButton(
                                onClick = { if (selectedIndex < vocabulary.lastIndex) selectedIndex++ },
                                enabled = selectedIndex < vocabulary.lastIndex,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Next")
                                Spacer(Modifier.size(6.dp))
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                            }
                        }
                    }
                }
            }

            if (showOverlay) {
                AlertDialog(
                    onDismissRequest = { showOverlay = false },
                    confirmButton = {
                        TextButton(onClick = { showOverlay = false }) { Text("Close") }
                    },
                    title = { Text(overlayTitle) },
                    text = {
                        if (overlayLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(Modifier.size(12.dp))
                                Text("Thinking…")
                            }
                        } else {
                            Text(overlayText)
                        }
                    }
                )
            }
        }
    }
}
