package com.example.voicevibe.presentation.screens.practice.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topic Practice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Choose a topic to chat about",
                style = MaterialTheme.typography.titleMedium,
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
    val bg = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val onBg = if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = onBg,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = onBg)
                if (description.isNotBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBg.copy(alpha = 0.8f)
                    )
                }
                if (completed) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = onBg.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
