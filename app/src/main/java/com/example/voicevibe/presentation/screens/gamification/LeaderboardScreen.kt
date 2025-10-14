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
    onNavigateToGroupProfile: (Int) -> Unit,
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
                    // Only show filter in Individual mode
                    if (uiState.selectedMode == LeaderboardMode.INDIVIDUAL) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                    }
                    // TODO: Share button - Hidden until functionality is implemented
                    // IconButton(onClick = viewModel::shareRank) {
                    //     Icon(Icons.Default.Share, "Share")
                    // }
                    IconButton(onClick = viewModel::refreshLeaderboard) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Only show FAB for Individual mode
            if (uiState.selectedMode == LeaderboardMode.INDIVIDUAL) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::scrollToCurrentUser,
                    icon = { Icon(Icons.Default.Person, "My Position") },
                    text = { Text("My Position") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
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
                    // Mode Switcher (Individual/Group)
                    LeaderboardModeTabs(
                        selectedMode = uiState.selectedMode,
                        onModeSelected = viewModel::selectMode,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Competition Stats Card (only for Individual mode)
                    if (uiState.selectedMode == LeaderboardMode.INDIVIDUAL) {
                        uiState.competitionStats?.let { stats ->
                            CompetitionStatsCard(
                                stats = stats,
                                onViewLeague = { showLeagueDialog = true },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Leaderboard Type Tabs (only for Individual mode)
                    if (uiState.selectedMode == LeaderboardMode.INDIVIDUAL) {
                        LeaderboardTypeTabs(
                            selectedType = uiState.selectedType,
                            onTypeSelected = viewModel::selectLeaderboardType,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Lingo League Skill Sub-Tabs (shown only when Lingo League is selected)
                        if (uiState.selectedType == LeaderboardType.LINGO_LEAGUE) {
                            LingoLeagueSkillTabs(
                                selectedSkill = uiState.selectedLingoLeagueSkill,
                                onSkillSelected = viewModel::selectLingoLeagueSkill,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Leaderboard List (Individual or Group)
                    when (uiState.selectedMode) {
                        LeaderboardMode.INDIVIDUAL -> {
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
                        LeaderboardMode.GROUP -> {
                            GroupLeaderboardList(
                                entries = uiState.groupLeaderboardEntries,
                                onGroupClick = { groupId ->
                                    onNavigateToGroupProfile(groupId)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
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
            selectedType = uiState.selectedType,
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
private fun LeaderboardModeTabs(
    selectedMode: LeaderboardMode,
    onModeSelected: (LeaderboardMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        LeaderboardMode.INDIVIDUAL to "Individual",
        LeaderboardMode.GROUP to "Group"
    )

    val selectedIndex = modes.indexOfFirst { it.first == selectedMode }.coerceAtLeast(0)

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        modes.forEachIndexed { index, (mode, label) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onModeSelected(mode) },
                text = { 
                    Text(
                        label,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                    ) 
                }
            )
        }
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
        LeaderboardType.LINGO_LEAGUE to "Lingo League"
    )

    val selectedIndex = types.indexOfFirst { it.first == selectedType }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        types.forEachIndexed { index, (type, label) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTypeSelected(type) },
                text = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LingoLeagueSkillTabs(
    selectedSkill: LeaderboardFilter,
    onSkillSelected: (LeaderboardFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val skills = listOf(
        LeaderboardFilter.PRONUNCIATION to "Pronunciation",
        LeaderboardFilter.FLUENCY to "Fluency",
        LeaderboardFilter.VOCABULARY to "Vocabulary",
        LeaderboardFilter.GRAMMAR to "Grammar",
        LeaderboardFilter.LISTENING to "Listening",
        LeaderboardFilter.CONVERSATION to "Conversation"
    )

    val selectedIndex = skills.indexOfFirst { it.first == selectedSkill }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    ) {
        skills.forEachIndexed { index, (skill, label) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSkillSelected(skill) },
                text = { 
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                }
            )
        }
    }
}

@Composable
private fun GroupLeaderboardList(
    entries: List<GroupLeaderboardEntry>,
    onGroupClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = "No groups",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading group leaderboard...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            // Top 3 Podium for groups
            if (entries.size >= 3) {
                item {
                    GroupTopThreePodium(
                        entries = entries.take(3),
                        onGroupClick = onGroupClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Rest of group leaderboard
            items(
                items = if (entries.size >= 3) entries.drop(3) else entries,
                key = { it.groupId }
            ) { entry ->
                GroupLeaderboardCard(
                    entry = entry,
                    onClick = { onGroupClick(entry.groupId) }
                )
            }
        }
    }
}

@Composable
private fun GroupTopThreePodium(
    entries: List<GroupLeaderboardEntry>,
    onGroupClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place
        if (entries.size > 1) {
            GroupPodiumItem(
                entry = entries[1],
                rank = 2,
                height = 100.dp,
                onClick = { onGroupClick(entries[1].groupId) }
            )
        }

        // First place
        if (entries.isNotEmpty()) {
            GroupPodiumItem(
                entry = entries[0],
                rank = 1,
                height = 120.dp,
                onClick = { onGroupClick(entries[0].groupId) }
            )
        }

        // Third place
        if (entries.size > 2) {
            GroupPodiumItem(
                entry = entries[2],
                rank = 3,
                height = 80.dp,
                onClick = { onGroupClick(entries[2].groupId) }
            )
        }
    }
}

@Composable
private fun GroupPodiumItem(
    entry: GroupLeaderboardEntry,
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
        // Group icon with color
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(3.dp, medalColor, CircleShape)
                .background(parseColor(entry.color)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.icon,
                fontSize = 28.sp
            )
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
            entry.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            "${entry.totalScore} XP",
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
private fun GroupLeaderboardCard(
    entry: GroupLeaderboardEntry,
    onClick: () -> Unit
) {
    val backgroundColor = if (entry.isCurrentUserGroup) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Rank, icon, and name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )

                // Group icon with color
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(parseColor(entry.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.icon,
                        fontSize = 24.sp
                    )
                }

                // Group name and member count
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.isCurrentUserGroup) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    "YOUR GROUP",
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
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Members",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${entry.memberCount} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: Total XP
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${entry.totalScore} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Helper function to parse color string
private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
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
            // Left side: Rank, avatar, and name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )

                RankChangeIndicator(entry.change)

                // Avatar
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

                // Name and level info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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

            Spacer(modifier = Modifier.width(8.dp))

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
    selectedType: LeaderboardType,
    selectedFilter: LeaderboardFilter,
    onFilterSelected: (LeaderboardFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Leaderboard") },
        text = {
            Column {
                val filtersToShow = if (selectedType == LeaderboardType.LINGO_LEAGUE) {
                    // For Lingo League, show time-based filters
                    listOf(
                        LeaderboardFilter.DAILY_XP to "Daily",
                        LeaderboardFilter.WEEKLY_XP to "Weekly",
                        LeaderboardFilter.MONTHLY_XP to "Monthly",
                        LeaderboardFilter.OVERALL_XP to "All Time"
                    )
                } else {
                    listOf(
                        LeaderboardFilter.OVERALL_XP to "XP (total)",
                        LeaderboardFilter.STREAK to "Streak",
                        LeaderboardFilter.ACCURACY to "Accuracy",
                        LeaderboardFilter.PRACTICE_TIME to "Practice time",
                        LeaderboardFilter.ACHIEVEMENTS to "Proficiency (topics completed)"
                    )
                }
                filtersToShow.forEach { (filter, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(filter) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
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
