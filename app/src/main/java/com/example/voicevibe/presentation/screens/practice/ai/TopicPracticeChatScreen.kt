package com.example.voicevibe.presentation.screens.practice.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicevibe.presentation.screens.practice.ai.*
import com.example.voicevibe.presentation.screens.speakingjourney.ConversationTurn
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPracticeChatScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    viewModel: TopicPracticeChatViewModel = hiltViewModel()
) {
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val ui by sjVM.uiState
    val topic = ui.topics.firstOrNull { it.id == topicId }

    LaunchedEffect(topicId) {
        if (topic == null) sjVM.reloadTopics()
    }

    LaunchedEffect(topic) {
        topic?.let { viewModel.startForTopic(it) }
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1a1a2e),
            Color(0xFF16213e),
            Color(0xFF0f3460)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            topic?.title ?: "Topic Chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            if (topic == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(0xFF64B5F6),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                TopicChatBody(
                    modifier = Modifier.fillMaxSize(),
                    vm = viewModel,
                    sjVM = sjVM
                )
            }
        }
    }
}

@Composable
private fun TopicChatBody(
    modifier: Modifier = Modifier,
    vm: TopicPracticeChatViewModel,
    sjVM: SpeakingJourneyViewModel
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    val maleVoiceName = "Puck"
    val femaleVoiceName = "Zephyr"

    fun playTts(turn: ConversationTurn) {
        val id = turn.text
        val voice = if (turn.speaker.equals("A", ignoreCase = true)) maleVoiceName else femaleVoiceName
        sjVM.markSpeakingActivity()
        sjVM.speakWithBackendTts(
            text = turn.text,
            voiceName = voice,
            onStart = { currentlyPlayingId = id },
            onDone = { currentlyPlayingId = null },
            onError = { currentlyPlayingId = null }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(state.items.reversed()) { item ->
                when (item) {
                    is TopicChatItem.UserText -> TopicChatBubble(text = item.text, isFromUser = true)
                    is TopicChatItem.AiText -> TopicChatBubble(text = item.text, isFromUser = false)
                    is TopicChatItem.PracticeMenu -> PracticeMenuCard(
                        onSelect = { mode -> vm.onSelectPracticeMode(mode) }
                    )
                    is TopicChatItem.ConversationExample -> ConversationExampleInline(
                        turns = item.turns,
                        currentlyPlayingId = currentlyPlayingId,
                        onPlay = { playTts(it) },
                        onExplain = { vm.explainConversationTurn(it.text) },
                        onPracticeWithAi = {
                            vm.sendMessage("I want to practice this conversation with AI. Please call start_practice_conversation to show role selection.")
                        }
                    )
                    is TopicChatItem.RoleSelection -> RoleSelectionCard(
                        onRoleSelected = { role -> vm.onRoleSelected(role) }
                    )
                    is TopicChatItem.PracticeTurn -> PracticeTurnCard(
                        text = item.text,
                        speaker = item.speaker,
                        isUserTurn = item.isUserTurn,
                        currentlyPlayingId = currentlyPlayingId,
                        onPlay = { 
                            val voice = if (item.speaker == "A") "Puck" else "Zephyr"
                            sjVM.speakWithBackendTts(
                                text = item.text,
                                voiceName = voice,
                                onStart = { currentlyPlayingId = item.text },
                                onDone = { currentlyPlayingId = null },
                                onError = { currentlyPlayingId = null }
                            )
                        }
                    )
                    is TopicChatItem.RecordingPrompt -> RecordingPromptCard(
                        expectedText = item.expectedText,
                        onRecordingComplete = { transcript -> vm.onUserRecordingComplete(transcript) }
                    )
                    is TopicChatItem.PracticeHint -> PracticeHintCard(
                        hint = item.hint,
                        expectedText = item.expectedText
                    )
                    is TopicChatItem.RevealAnswer -> RevealAnswerCard(
                        correctText = item.correctText
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message", color = Color(0xFF78909C)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color(0xFF37474F),
                    cursorColor = Color(0xFF64B5F6),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Button(
                onClick = {
                    vm.sendMessage(messageText)
                    messageText = ""
                },
                enabled = !state.isLoading && messageText.isNotBlank(),
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun TopicChatBubble(text: String, isFromUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        val shape = RoundedCornerShape(16.dp)
        if (isFromUser) {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .shadow(4.dp, shape)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF667eea),
                                    Color(0xFF764ba2)
                                )
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White
                    )
                }
            }
        } else {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = text,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PracticeMenuCard(onSelect: (PracticeMode) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose a practice mode", color = Color.White, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeMenuRow(
                    items = listOf(
                        PracticeMenuItem(Icons.Filled.Chat, "Conversation", PracticeMode.CONVERSATION),
                        PracticeMenuItem(Icons.Filled.RecordVoiceOver, "Pronunciation", PracticeMode.PRONUNCIATION),
                        PracticeMenuItem(Icons.Filled.GraphicEq, "Fluency", PracticeMode.FLUENCY)
                    ),
                    onSelect = onSelect
                )
                PracticeMenuRow(
                    items = listOf(
                        PracticeMenuItem(Icons.Filled.LibraryBooks, "Vocabulary", PracticeMode.VOCABULARY),
                        PracticeMenuItem(Icons.Filled.VolumeUp, "Listening", PracticeMode.LISTENING),
                        PracticeMenuItem(Icons.Filled.School, "Grammar", PracticeMode.GRAMMAR)
                    ),
                    onSelect = onSelect
                )
            }
        }
    }
}

private data class PracticeMenuItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val mode: PracticeMode)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeMenuRow(items: List<PracticeMenuItem>, onSelect: (PracticeMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (item in items) {
            Surface(
                onClick = { onSelect(item.mode) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, contentDescription = item.title, tint = Color.White)
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White)
                        if (item.mode != PracticeMode.CONVERSATION) {
                            Text("Coming soon", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationExampleInline(
    turns: List<ConversationTurn>,
    currentlyPlayingId: String?,
    onPlay: (ConversationTurn) -> Unit,
    onExplain: (ConversationTurn) -> Unit,
    onPracticeWithAi: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Conversation Example", color = Color.White, fontWeight = FontWeight.SemiBold)
            for (turn in turns) {
                val isActive = currentlyPlayingId == turn.text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isActive) Color.White.copy(alpha = 0.06f) else Color.Transparent, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f)) {
                        Text(
                            turn.speaker,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(turn.text, color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onPlay(turn) }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Play", tint = Color.White)
                    }
                    IconButton(onClick = { onExplain(turn) }) {
                        Icon(Icons.Filled.Info, contentDescription = "Explain", tint = Color(0xFF64B5F6))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = onPracticeWithAi,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.06f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = Color.White)
                    Column(Modifier.weight(1f)) {
                        Text("Practice this conversation with AI", color = Color.White)
                        Text("Coming soon", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
