package com.example.voicevibe.presentation.screens.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.R
import com.example.voicevibe.domain.model.*
import com.example.voicevibe.presentation.components.LoadingScreen
import com.example.voicevibe.presentation.components.ErrorScreen
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LeaderboardEvent.NavigateToProfile -> {
                    onNavigateToProfile(event.userId)
                }
                is LeaderboardEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LeaderboardEvent.ScrollToPosition -> {
                    listState.animateScrollToItem(event.position)
                }
                is LeaderboardEvent.ShareRank -> {
                    // Handle share intent
                    snackbarHostState.showSnackbar("Sharing rank #${event.rank}")
                }
                is LeaderboardEvent.ShowChallengeDialog -> {
                    // Challenge dialog will be shown
                }
                is LeaderboardEvent.ShowLeagueInfo -> {
                    // League info dialog will be shown
                }
            }
        }
    }

    var showLeagueDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Leaderboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                    IconButton(onClick = viewModel::shareRank) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = viewModel::refreshLeaderboard) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::scrollToCurrentUser,
                icon = { Icon(Icons.Default.Person, "My Position") },
                text = { Text("My Position") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        val currentError = uiState.error
        when {
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            currentError != null -> {
                val errorMessage = currentError
                ErrorScreen(
                    message = errorMessage,
                    onRetry = viewModel::refreshLeaderboard,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Competition Stats Card
                    uiState.competitionStats?.let { stats ->
                        CompetitionStatsCard(
                            stats = stats,
                            onViewLeague = { showLeagueDialog = true },
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Leaderboard Type Tabs
                    LeaderboardTypeTabs(
                        selectedType = uiState.selectedType,
                        onTypeSelected = viewModel::selectLeaderboardType,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Leaderboard List
                    uiState.leaderboardData?.let { data ->
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top 3 Podium
                            if (data.entries.size >= 3) {
                                item {
                                    TopThreePodium(
                                        entries = data.entries.take(3),
                                        onUserClick = { viewModel.viewUserProfile(it) }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // Rest of leaderboard
                            items(
                                items = if (data.entries.size >= 3) data.entries.drop(3) else data.entries,
                                key = { it.userId }
                            ) { entry ->
                                LeaderboardEntryCard(
                                    entry = entry,
                                    onClick = { viewModel.viewUserProfile(entry.userId) },
                                    onFollow = { viewModel.followUser(entry.userId) },
                                    onChallenge = { viewModel.challengeUser(entry.userId) }
                                )
                            }

                            // Load more indicator
                            if (data.entries.size < data.totalParticipants) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Showing ${data.entries.size} of ${data.totalParticipants} participants",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // League Info Dialog
    if (showLeagueDialog) {
        uiState.competitionStats?.currentLeague?.let { league ->
            LeagueInfoDialog(
                league = league,
                onDismiss = { showLeagueDialog = false }
            )
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = { filter ->
                viewModel.selectFilter(filter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun CompetitionStatsCard(
    stats: CompetitionStats,
    onViewLeague: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = getLeagueColor(stats.currentLeague.tier).copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            2.dp,
            getLeagueColor(stats.currentLeague.tier)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stats.currentLeague.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = getLeagueColor(stats.currentLeague.tier)
                    )
                    Text(
                        "Rank #${stats.currentRank}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                IconButton(
                    onClick = onViewLeague,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = getLeagueColor(stats.currentLeague.tier).copy(alpha = 0.2f)
                    )
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "League Info",
                        tint = getLeagueColor(stats.currentLeague.tier)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress to next league
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Progress to next league",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${stats.pointsToNextLeague} points needed",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                LinearProgressIndicator(
                    progress = stats.weeklyProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = getLeagueColor(stats.currentLeague.tier)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Best Rank",
                    value = "#${stats.personalBestRank}"
                )
                StatItem(
                    label = "Competitions Won",
                    value = stats.totalCompetitionsWon.toString()
                )
                StatItem(
                    label = "Days Left",
                    value = stats.daysUntilReset.toString()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardTypeTabs(
    selectedType: LeaderboardType,
    onTypeSelected: (LeaderboardType) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf(
        LeaderboardType.DAILY to "Daily",
        LeaderboardType.WEEKLY to "Weekly",
        LeaderboardType.MONTHLY to "Monthly",
        LeaderboardType.ALL_TIME to "All Time",
        LeaderboardType.FRIENDS to "Friends",
        LeaderboardType.COUNTRY to "Country"
    )

    ScrollableTabRow(
        selectedTabIndex = types.indexOfFirst { it.first == selectedType },
        modifier = modifier,
        edgePadding = 16.dp
    ) {
        types.forEachIndexed { index, (type, label) ->
            Tab(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                text = { Text(label) }
            )
        }
    }
}

@Composable
private fun TopThreePodium(
    entries: List<LeaderboardEntry>,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place
        if (entries.size > 1) {
            PodiumItem(
                entry = entries[1],
                rank = 2,
                height = 100.dp,
                onClick = { onUserClick(entries[1].userId) }
            )
        }

        // First place
        if (entries.isNotEmpty()) {
            PodiumItem(
                entry = entries[0],
                rank = 1,
                height = 120.dp,
                onClick = { onUserClick(entries[0].userId) }
            )
        }

        // Third place
        if (entries.size > 2) {
            PodiumItem(
                entry = entries[2],
                rank = 3,
                height = 80.dp,
                onClick = { onUserClick(entries[2].userId) }
            )
        }
    }
}

@Composable
private fun PodiumItem(
    entry: LeaderboardEntry,
    rank: Int,
    height: Dp,
    onClick: () -> Unit
) {
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        val initials = remember(entry.displayName) { generateInitials(entry.displayName) }
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(3.dp, medalColor, CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (!entry.avatarUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Text(
                            text = initials,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    error = {
                        Text(
                            text = initials,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                )
            } else {
                Text(
                    text = initials,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Medal badge
        Surface(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .offset(y = 8.dp),
            shape = CircleShape,
            color = medalColor,
            shadowElevation = 4.dp
        ) {
            Text(
                text = rank.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            "${entry.score} XP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .padding(top = 8.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            medalColor.copy(alpha = 0.3f),
                            medalColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    onChallenge: () -> Unit
) {
    val backgroundColor = if (entry.isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        val initials = remember(entry.displayName) { generateInitials(entry.displayName) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank and change
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )

                RankChangeIndicator(entry.change)

                // User info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!entry.avatarUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = entry.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Text(
                                        text = initials,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                },
                                error = {
                                    Text(
                                        text = initials,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = initials,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                entry.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            if (entry.isCurrentUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        "YOU",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Level ${entry.level}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (entry.streakDays > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFFFF6B35)
                                    )
                                    Text(
                                        "${entry.streakDays}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF6B35)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Score and actions
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${entry.score} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!entry.isCurrentUser) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onFollow,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Follow",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onChallenge,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = "Challenge",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankChangeIndicator(change: RankChange) {
    when (change) {
        RankChange.UP -> {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = "Rank Up",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
        RankChange.DOWN -> {
            Icon(
                Icons.Default.TrendingDown,
                contentDescription = "Rank Down",
                tint = Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
        }
        RankChange.NEW -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF2196F3).copy(alpha = 0.2f)
            ) {
                Text(
                    "NEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        RankChange.NONE -> {}
    }
}

@Composable
private fun LeagueInfoDialog(
    league: League,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(league.name)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getLeagueColor(league.tier).copy(alpha = 0.2f)
                ) {
                    Text(
                        league.tier.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = getLeagueColor(league.tier),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    "Score Range: ${league.minScore} - ${league.maxScore} XP",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Rewards:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                league.rewards.forEach { reward ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Top ${reward.position}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            reward.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FilterDialog(
    selectedFilter: LeaderboardFilter,
    onFilterSelected: (LeaderboardFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Leaderboard") },
        text = {
            Column {
                LeaderboardFilter.values().forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(filter) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            filter.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (filter == selectedFilter) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun getLeagueColor(tier: LeagueTier): Color {
    return when (tier) {
        LeagueTier.BRONZE -> Color(0xFFCD7F32)
        LeagueTier.SILVER -> Color(0xFFC0C0C0)
        LeagueTier.GOLD -> Color(0xFFFFD700)
        LeagueTier.PLATINUM -> Color(0xFFE5E4E2)
        LeagueTier.DIAMOND -> Color(0xFFB9F2FF)
        LeagueTier.MASTER -> Color(0xFF9370DB)
        LeagueTier.GRANDMASTER -> Color(0xFFFF1744)
    }
}

// Helper to generate initials from display name for avatar fallback
private fun generateInitials(displayName: String): String {
    val parts = displayName.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 && parts[0].length >= 2 -> "${parts[0][0].uppercaseChar()}${parts[0][1].uppercaseChar()}"
        parts.size == 1 -> "${parts[0].first().uppercaseChar()}${parts[0].first().uppercaseChar()}"
        else -> "VV"
    }
}
