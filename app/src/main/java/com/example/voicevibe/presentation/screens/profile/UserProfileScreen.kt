package com.example.voicevibe.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.voicevibe.R
import com.example.voicevibe.domain.model.*
import com.example.voicevibe.presentation.components.LoadingScreen
import com.example.voicevibe.presentation.components.ErrorScreen
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: (String) -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMoreOptions by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UserProfileEvent.NavigateBack -> onNavigateBack()
                is UserProfileEvent.NavigateToEditProfile -> onNavigateToEditProfile()
                is UserProfileEvent.NavigateToSettings -> onNavigateToSettings()
                is UserProfileEvent.NavigateToAchievements -> onNavigateToAchievements(event.userId)
                is UserProfileEvent.NavigateToFollowers -> onNavigateToFollowers(event.userId)
                is UserProfileEvent.NavigateToFollowing -> onNavigateToFollowing(event.userId)
                is UserProfileEvent.ShowChallengeDialog -> {
                    // Handle challenge dialog
                }
                is UserProfileEvent.ShareProfile -> {
                    snackbarHostState.showSnackbar("Sharing profile")
                }
                is UserProfileEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val userProfile = uiState.userProfile
                    if (!uiState.isOwnProfile && userProfile != null) {
                        Text(
                            "@${userProfile.username}",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("Profile", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.isOwnProfile) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                        IconButton(onClick = onNavigateToEditProfile) {
                            Icon(Icons.Default.Edit, "Edit Profile")
                        }
                    } else {
                        IconButton(onClick = viewModel::shareProfile) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        IconButton(onClick = { showMoreOptions = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val error = uiState.error
        val userProfile = uiState.userProfile
        
        when {
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            error != null -> {
                ErrorScreen(
                    message = error,
                    onRetry = viewModel::refreshProfile,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            userProfile != null -> {
                ProfileContent(
                    profile = userProfile,
                    activities = uiState.activities,
                    selectedTab = uiState.selectedTab,
                    isOwnProfile = uiState.isOwnProfile,
                    onTabSelected = viewModel::selectTab,
                    onFollowClick = {
                        if (userProfile.isFollowing) {
                            viewModel.unfollowUser()
                        } else {
                            viewModel.followUser()
                        }
                    },
                    onChallengeClick = viewModel::challengeUser,
                    onViewAchievements = viewModel::viewAchievements,
                    onViewFollowers = viewModel::viewFollowers,
                    onViewFollowing = viewModel::viewFollowing,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // More Options Menu
    if (showMoreOptions && !uiState.isOwnProfile) {
        DropdownMenu(
            expanded = showMoreOptions,
            onDismissRequest = { showMoreOptions = false }
        ) {
            DropdownMenuItem(
                text = { Text("Report User") },
                onClick = {
                    showMoreOptions = false
                    showReportDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.Flag, contentDescription = "Report")
                }
            )
            DropdownMenuItem(
                text = { Text("Block User") },
                onClick = {
                    showMoreOptions = false
                    viewModel.blockUser()
                },
                leadingIcon = {
                    Icon(Icons.Default.Block, contentDescription = "Block")
                }
            )
        }
    }

    // Report Dialog
    if (showReportDialog) {
        ReportUserDialog(
            onConfirm = { reason ->
                viewModel.reportUser(reason)
                showReportDialog = false
            },
            onDismiss = { showReportDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    profile: UserProfile,
    activities: List<UserActivity>,
    selectedTab: ProfileTab,
    isOwnProfile: Boolean,
    onTabSelected: (ProfileTab) -> Unit,
    onFollowClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onViewAchievements: () -> Unit,
    onViewFollowers: () -> Unit,
    onViewFollowing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { ProfileTab.values().size })

    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab.ordinal)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Profile Header
        ProfileHeader(
            profile = profile,
            isOwnProfile = isOwnProfile,
            onFollowClick = onFollowClick,
            onChallengeClick = onChallengeClick,
            onViewFollowers = onViewFollowers,
            onViewFollowing = onViewFollowing
        )

        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ProfileTab.values().forEach { tab ->
                Tab(
                    selected = pagerState.currentPage == tab.ordinal,
                    onClick = {
                        onTabSelected(tab)
                    },
                    text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    icon = {
                        Icon(
                            imageVector = when (tab) {
                                ProfileTab.OVERVIEW -> Icons.Default.Dashboard
                                ProfileTab.ACTIVITY -> Icons.Default.Timeline
                                ProfileTab.ACHIEVEMENTS -> Icons.Default.EmojiEvents
                                ProfileTab.STATISTICS -> Icons.Default.Analytics
                            },
                            contentDescription = tab.name
                        )
                    }
                )
            }
        }

        // Tab Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (ProfileTab.values()[page]) {
                ProfileTab.OVERVIEW -> OverviewTab(profile, onViewAchievements)
                ProfileTab.ACTIVITY -> ActivityTab(activities)
                ProfileTab.ACHIEVEMENTS -> AchievementsTab(profile.badges, onViewAchievements)
                ProfileTab.STATISTICS -> StatisticsTab(profile.stats)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onFollowClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onViewFollowers: () -> Unit,
    onViewFollowing: () -> Unit
) {
    Column {
        // Cover Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (profile.coverImageUrl != null) {
                AsyncImage(
                    model = profile.coverImageUrl,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                )
            }

            // Avatar
            if (profile.avatarUrl != null) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Profile Info
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (profile.isPremium) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = "Premium",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }

                    Text(
                        "@${profile.username}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Level and XP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Level ${profile.level}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${profile.xp} XP",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (profile.streakDays > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF6B35)
                                )
                                Text(
                                    "${profile.streakDays} days",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF6B35)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                if (!isOwnProfile) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (profile.isFollowing) {
                            OutlinedButton(
                                onClick = onFollowClick,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Following")
                            }
                        } else {
                            Button(
                                onClick = onFollowClick,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Follow")
                            }
                        }

                        OutlinedButton(
                            onClick = onChallengeClick,
                            modifier = Modifier.height(36.dp)
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

            // Bio
            profile.bio?.let { bio ->
                Text(
                    bio,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = profile.stats.followersCount.toString(),
                    label = "Followers",
                    onClick = onViewFollowers
                )
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )
                StatItem(
                    value = profile.stats.followingCount.toString(),
                    label = "Following",
                    onClick = onViewFollowing
                )
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )
                StatItem(
                    value = profile.stats.achievementsUnlocked.toString(),
                    label = "Achievements"
                )
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )
                StatItem(
                    value = profile.stats.totalPracticeSessions.toString(),
                    label = "Sessions"
                )
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        }
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportUserDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    val reasons = listOf(
        "Inappropriate content",
        "Harassment or bullying",
        "Spam",
        "Impersonation",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report User") },
        text = {
            Column {
                Text(
                    "Why are you reporting this user?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        onConfirm(selectedReason)
                    }
                },
                enabled = selectedReason.isNotEmpty()
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
