package com.example.voicevibe.presentation.screens.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicevibe.domain.model.*
import java.time.format.DateTimeFormatter

/**
 * Achievements screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AchievementsEvent.ShowAchievementDetails -> {
                    selectedAchievement = event.achievement
                }
                is AchievementsEvent.ShareAchievement -> {
                    // Handle share
                }
                is AchievementsEvent.ShowMessage -> {
                    // Show snackbar
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error,
                    onRetry = viewModel::retryLoading,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Stats Header
                    uiState.stats?.let { stats ->
                        AchievementStatsHeader(stats = stats)
                    }

                    // Category Filter Chips
                    CategoryFilterChips(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = viewModel::filterByCategory
                    )

                    // Achievements Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredAchievements) { achievement ->
                            AchievementCard(
                                achievement = achievement,
                                onClick = { viewModel.viewAchievementDetails(achievement) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            currentSortBy = uiState.sortBy,
            showOnlyUnlocked = uiState.showOnlyUnlocked,
            onSortByChanged = viewModel::sortAchievements,
            onToggleUnlocked = viewModel::toggleUnlockedFilter,
            onDismiss = { showFilterSheet = false }
        )
    }

    // Achievement Details Dialog
    selectedAchievement?.let { achievement ->
        AchievementDetailsDialog(
            achievement = achievement,
            onDismiss = { selectedAchievement = null },
            onShare = { viewModel.shareAchievement(achievement) },
            onClaimReward = { viewModel.claimReward(achievement) }
        )
    }
}

@Composable
private fun AchievementStatsHeader(
    stats: AchievementStats
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = "${stats.totalUnlocked}",
                label = "Unlocked",
                icon = Icons.Default.EmojiEvents
            )
            StatItem(
                value = "${stats.totalPoints}",
                label = "Points",
                icon = Icons.Default.Star
            )
            StatItem(
                value = "${(stats.totalUnlocked.toFloat() / stats.totalAvailable * 100).toInt()}%",
                label = "Progress",
                icon = Icons.Default.TrendingUp
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CategoryFilterChips(
    selectedCategory: AchievementCategory?,
    onCategorySelected: (AchievementCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All category
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }

        // Category chips
        items(AchievementCategory.values().toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        category.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )
                }
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    onClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else 0.6f,
        animationSpec = tween(300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .alpha(animatedAlpha),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                getRarityColor(achievement.rarity).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (achievement.isUnlocked) {
            BorderStroke(2.dp, getRarityColor(achievement.rarity))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon/Emoji
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked) {
                    // Animated glow effect for unlocked
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .size(60.dp)
                            .scale(scale),
                        shape = CircleShape,
                        color = getRarityColor(achievement.rarity).copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = achievement.iconEmoji ?: "🏆",
                                fontSize = 32.sp
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Progress or Points
            if (achievement.isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${achievement.points} pts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (achievement.progress > 0) {
                LinearProgressIndicator(
                    progress = achievement.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${achievement.progress.toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Rarity badge
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = getRarityColor(achievement.rarity).copy(alpha = 0.2f)
            ) {
                Text(
                    text = achievement.rarity.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = getRarityColor(achievement.rarity)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentSortBy: AchievementSortBy,
    showOnlyUnlocked: Boolean,
    onSortByChanged: (AchievementSortBy) -> Unit,
    onToggleUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Filter & Sort",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Show only unlocked toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show only unlocked")
                Switch(
                    checked = showOnlyUnlocked,
                    onCheckedChange = { onToggleUnlocked() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sort by",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            AchievementSortBy.values().forEach { sortBy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortByChanged(sortBy) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSortBy == sortBy,
                        onClick = { onSortByChanged(sortBy) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = sortBy.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AchievementDetailsDialog(
    achievement: Achievement,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onClaimReward: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = achievement.iconEmoji ?: "🏆",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = achievement.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column {
                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Achievement details
                DetailRow(
                    label = "Category",
                    value = achievement.category.name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                )
                DetailRow(label = "Points", value = "${achievement.points} pts")
                DetailRow(label = "Rarity", value = achievement.rarity.name)

                if (achievement.isUnlocked) {
                    achievement.unlockedAt?.let { date ->
                        DetailRow(
                            label = "Unlocked",
                            value = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        )
                    }
                }

                if (!achievement.isUnlocked && achievement.progress > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Text(
                            text = "Progress",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        LinearProgressIndicator(
                            progress = achievement.progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "${achievement.currentProgress} / ${achievement.maxProgress}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                achievement.reward?.let { reward ->
                    if (achievement.isUnlocked) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Reward",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = reward.description,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (achievement.isUnlocked) {
                    if (achievement.reward != null) {
                        TextButton(onClick = onClaimReward) {
                            Text("Claim Reward")
                        }
                    }
                    TextButton(onClick = onShare) {
                        Text("Share")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unable to load achievements",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

// Helper function
private fun getRarityColor(rarity: AchievementRarity): Color {
    return when (rarity) {
        AchievementRarity.COMMON -> Color(0xFF9E9E9E)
        AchievementRarity.UNCOMMON -> Color(0xFF4CAF50)
        AchievementRarity.RARE -> Color(0xFF2196F3)
        AchievementRarity.EPIC -> Color(0xFF9C27B0)
        AchievementRarity.LEGENDARY -> Color(0xFFFF9800)
    }
}
