package com.example.voicevibe.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.R
import com.example.voicevibe.domain.model.*
import com.example.voicevibe.presentation.components.LoadingScreen
import com.example.voicevibe.presentation.components.ErrorScreen
import com.example.voicevibe.presentation.components.OnlineStatusIndicator
import com.example.voicevibe.presentation.components.FullScreenImageViewer
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
    onNavigateToMessage: (String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Full-screen image viewer state
    var avatarToView by remember { mutableStateOf<String?>(null) }

    // Read account preference for showing email
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val showEmailOnProfile = settingsViewModel.showEmailOnProfile.value

    // Refresh avatar when returning from Settings screen
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumedOnce by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumedOnce) {
                    viewModel.refreshProfile()
                } else {
                    hasResumedOnce = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showMoreOptions by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    
    // Collect one-off events (navigation, toasts)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UserProfileEvent.NavigateBack -> onNavigateBack()
                is UserProfileEvent.NavigateToEditProfile -> onNavigateToEditProfile()
                is UserProfileEvent.NavigateToSettings -> onNavigateToSettings()
                is UserProfileEvent.NavigateToAchievements -> onNavigateToAchievements(event.userId)
                is UserProfileEvent.NavigateToFollowers -> onNavigateToFollowers(event.userId)
                is UserProfileEvent.NavigateToFollowing -> onNavigateToFollowing(event.userId)
                is UserProfileEvent.ShowChallengeDialog -> { /* no-op placeholder */ }
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
                            userProfile.displayName,
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

        if (userProfile != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ProfileContent(
                    profile = userProfile,
                    speakingOverview = uiState.speakingOverview,
                    activities = uiState.activities,
                    selectedTab = uiState.selectedTab,
                    isOwnProfile = uiState.isOwnProfile,
                    showEmailOnProfile = showEmailOnProfile,
                    onTabSelected = viewModel::selectTab,
                    onFollowClick = {
                        if (userProfile.isFollowing) {
                            viewModel.unfollowUser()
                        } else {
                            viewModel.followUser()
                        }
                    },
                    onChallengeClick = viewModel::challengeUser,
                    onMessageClick = { onNavigateToMessage(userProfile.id) },
                    onAvatarClick = {
                        if (!userProfile.avatarUrl.isNullOrBlank()) {
                            avatarToView = userProfile.avatarUrl
                        }
                    },
                    onViewAchievements = viewModel::viewAchievements,
                    onViewFollowers = viewModel::viewFollowers,
                    onViewFollowing = viewModel::viewFollowing,
                    modifier = Modifier.fillMaxSize()
                )

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        } else {
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
                else -> {
                    // Fallback to avoid a blank frame before first load emits
                    LoadingScreen(modifier = Modifier.padding(paddingValues))
                }
            }
        }
    }

    // Full-screen avatar viewer dialog
    avatarToView?.let { url ->
        FullScreenImageViewer(
            imageUrl = url,
            contentDescription = "Avatar",
            onDismiss = { avatarToView = null }
        )
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
    speakingOverview: SpeakingOverview?,
    activities: List<UserActivity>,
    selectedTab: ProfileTab,
    isOwnProfile: Boolean,
    showEmailOnProfile: Boolean,
    onTabSelected: (ProfileTab) -> Unit,
    onFollowClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    onViewAchievements: () -> Unit,
    onViewFollowers: () -> Unit,
    onViewFollowing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState
    ) {
        // Profile Header
        item {
            ProfileHeader(
                profile = profile,
                isOwnProfile = isOwnProfile,
                showEmailOnProfile = showEmailOnProfile,
                onFollowClick = onFollowClick,
                onChallengeClick = onChallengeClick,
                onMessageClick = onMessageClick,
                onAvatarClick = onAvatarClick,
                onViewFollowers = onViewFollowers,
                onViewFollowing = onViewFollowing
            )
        }

        // Tabs Row
        item {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ProfileTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
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
        }

        // Selected Tab Content (flattened into parent list to avoid nested scrolling)
        when (selectedTab) {
            ProfileTab.OVERVIEW -> {
                OverviewTabContent(profile, speakingOverview, onViewAchievements)
            }
            ProfileTab.ACTIVITY -> {
                ActivityTabContent(activities)
            }
            ProfileTab.ACHIEVEMENTS -> {
                AchievementsTabContent(
                    badges = profile.badges,
                    proficiency = profile.proficiency,
                    level = profile.level,
                    onViewAll = onViewAchievements
                )
            }
            ProfileTab.STATISTICS -> {
                StatisticsTabContent(profile.stats)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isOwnProfile: Boolean,
    showEmailOnProfile: Boolean,
    onFollowClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    onViewFollowers: () -> Unit,
    onViewFollowing: () -> Unit
) {
    Column {
        // Cover Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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

            // Centered Avatar at bottom of cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset(y = 50.dp),
                contentAlignment = Alignment.Center
            ) {
                val initials = remember(profile.displayName) { generateInitials(profile.displayName) }
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .then(
                            if (onAvatarClick != null && !profile.avatarUrl.isNullOrBlank()) Modifier.clickable { onAvatarClick() } else Modifier
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Text(
                                        text = initials,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                },
                                error = {
                                    Text(
                                        text = initials,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = initials,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Online status indicator
                    OnlineStatusIndicator(
                        isOnline = profile.isOnline,
                        size = 16.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(70.dp))

        // Profile Info
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Name and verification badges - centered
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val nameToShow = if (!showEmailOnProfile && looksLikeEmail(profile.displayName)) {
                    // Hide raw email-like display names
                    "User"
                } else profile.displayName
                Text(
                    nameToShow,
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

            if (showEmailOnProfile || !looksLikeEmail(profile.username)) {
                Text(
                    "@${profile.username}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bio - right below username
            profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = Color.Transparent
                ) {
                    Text(
                        bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons (Follow/Unfollow, Message, Challenge) for other users
            if (!isOwnProfile) {
                // First row: Follow and Message buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFollowing = profile.isFollowing
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onFollowClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Unfollow")
                            Spacer(Modifier.width(6.dp))
                            Text("Unfollow")
                        }
                    } else {
                        Button(
                            onClick = onFollowClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Follow")
                            Spacer(Modifier.width(6.dp))
                            Text("Follow")
                        }
                    }

                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "Message")
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Second row: Challenge button (full width)
                OutlinedButton(
                    onClick = onChallengeClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SportsKabaddi, contentDescription = "Challenge")
                    Spacer(Modifier.width(6.dp))
                    Text("Challenge")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Level and XP - centered
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
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
                val threshold = (profile.xp + profile.xpToNextLevel).coerceAtLeast(1)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Level XP: ${profile.xp} / $threshold",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = (profile.xp.toFloat() / threshold.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier
                            .width(160.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Total XP: ${profile.totalXp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

// Helper to generate initials from display name for avatar fallback
private fun generateInitials(displayName: String): String {
    val parts = displayName.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "U"
    }
}

// Helper to detect email-like identifiers so we can hide them when the user prefers privacy
private fun looksLikeEmail(text: String?): Boolean {
    val s = text?.trim() ?: return false
    // Simple RFC-like pattern
    val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailRegex.matches(s)
}
