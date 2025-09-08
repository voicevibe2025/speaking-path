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
                TopicChatBody(modifier = Modifier.fillMaxSize(), vm = viewModel)
            }
        }
    }
}

@Composable
private fun TopicChatBody(modifier: Modifier = Modifier, vm: TopicPracticeChatViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(state.messages.reversed()) { message ->
                TopicChatMessageItem(message)
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
private fun TopicChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        val shape = RoundedCornerShape(16.dp)
        if (message.isFromUser) {
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
                        text = message.text,
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
                    text = message.text,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
