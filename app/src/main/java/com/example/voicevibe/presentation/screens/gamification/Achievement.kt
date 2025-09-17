package com.example.voicevibe.presentation.screens.gamification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsSimpleViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var uiState by mutableStateOf(AchievementsSimpleUiState())
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val profile = profileRepository.getProfile()
                val items = buildList<AchievementFeedItem> {
                    val prof = profile.currentProficiency?.trim()
                    if (!prof.isNullOrBlank()) {
                        val profTitle = "Obtained ${prof.replaceFirstChar { it.uppercase() }} proficiency level"
                        val profTime = profile.recentActivities
                            ?.firstOrNull { a ->
                                a.title.contains("proficien", ignoreCase = true) ||
                                a.title.contains(prof, ignoreCase = true)
                            }
                            ?.relativeTime ?: "Recently"
                        add(
                            AchievementFeedItem(
                                type = AchievementItemType.PROFICIENCY,
                                title = profTitle,
                                timeAgo = profTime
                            )
                        )
                    }

                    val level = profile.currentLevel
                    if (level != null && level > 0) {
                        val levelTitle = "Reached level $level"
                        val levelTime = profile.recentActivities
                            ?.firstOrNull { a ->
                                a.title.contains("level", ignoreCase = true) ||
                                a.type.contains("level", ignoreCase = true)
                            }
                            ?.relativeTime ?: "Recently"
                        add(
                            AchievementFeedItem(
                                type = AchievementItemType.LEVEL,
                                title = levelTitle,
                                timeAgo = levelTime
                            )
                        )
                    }
                }
                uiState = uiState.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Failed to load achievements")
            }
        }
    }
}

data class AchievementsSimpleUiState(
    val isLoading: Boolean = false,
    val items: List<AchievementFeedItem> = emptyList(),
    val error: String? = null
)

enum class AchievementItemType { PROFICIENCY, LEVEL }

data class AchievementFeedItem(
    val type: AchievementItemType,
    val title: String,
    val timeAgo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsSimpleViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                SimpleError(
                    message = state.error,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                if (state.items.isEmpty()) {
                    EmptyAchievements(modifier = Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            InfoCard()
                        }
                        items(state.items) { item ->
                            AchievementRow(item)
                        }
                        item {
                            BadgesComingSoon()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementFeedItem) {
    val (icon, accent) = when (item.type) {
        AchievementItemType.PROFICIENCY -> Icons.Default.WorkspacePremium to MaterialTheme.colorScheme.primary
        AchievementItemType.LEVEL -> Icons.Default.School to MaterialTheme.colorScheme.tertiary
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                color = accent.copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "We currently track your proficiency tier and level. Badges will be added soon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BadgesComingSoon() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Badges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Badge achievements are coming soon. Keep practicing to earn them when available!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimpleError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to load achievements",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

@Composable
private fun EmptyAchievements(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No achievements yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start practicing to unlock your first achievements!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
