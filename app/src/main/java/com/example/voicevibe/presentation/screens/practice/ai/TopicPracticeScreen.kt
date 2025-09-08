package com.example.voicevibe.presentation.screens.practice.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.presentation.screens.speakingjourney.SpeakingJourneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPracticeScreen(
    onNavigateBack: () -> Unit,
    onOpenTopicChat: (String) -> Unit
) {
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val ui by sjVM.uiState

    LaunchedEffect(Unit) {
        if (ui.topics.isEmpty()) sjVM.reloadTopics()
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
                            "Topic Practice",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Choose a topic to chat about",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ui.topics) { t ->
                    TopicRow(
                        title = t.title,
                        description = t.description,
                        unlocked = t.unlocked,
                        completed = t.completed,
                        onClick = { if (t.unlocked) onOpenTopicChat(t.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicRow(
    title: String,
    description: String,
    unlocked: Boolean,
    completed: Boolean,
    onClick: () -> Unit
) {
    val container = Color(0xFF2a2d3a)
    val titleColor = Color.White
    val descColor = Color(0xFFB0BEC5)
    val iconColor = if (unlocked) Color(0xFF64B5F6) else Color(0xFF78909C)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                if (description.isNotBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = descColor
                    )
                }
                if (completed) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64B5F6)
                    )
                }
            }
        }
    }
}
