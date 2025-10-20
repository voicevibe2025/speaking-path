package com.example.voicevibe.presentation.screens.gamification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.domain.model.LeaderboardEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicLeaderboardScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: TopicLeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(topicId) {
        viewModel.load(topicId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topic Leaderboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Error")
                }
            }
            else -> {
                val data = uiState.data
                if (data == null) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No data")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(data.entries, key = { it.userId }) { entry ->
                            TopicLeaderboardItem(entry = entry, onClick = { onNavigateToProfile(entry.userId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicLeaderboardItem(entry: LeaderboardEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("#${entry.rank}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(shape = MaterialTheme.shapes.small) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (!entry.avatarUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = entry.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(entry.displayName.take(1).uppercase())
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Score: ${entry.score}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
