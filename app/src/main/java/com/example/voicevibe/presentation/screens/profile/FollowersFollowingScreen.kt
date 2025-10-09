package com.example.voicevibe.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.ui.theme.BrandIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersFollowingScreen(
    userId: String?,
    initialTab: Int = 0, // 0 = Followers, 1 = Following
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (userId: String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var isRefreshing by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // Load followers and following when screen opens
    LaunchedEffect(userId) {
        userId?.let {
            viewModel.loadFollowers(it)
            viewModel.loadFollowing(it)
        } ?: run {
            viewModel.loadFollowers(null) // Current user
            viewModel.loadFollowing(null)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Followers & Following") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Followers (${uiState.followers.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Following (${uiState.following.size})") }
                )
            }

            // Content
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    if (selectedTab == 0) {
                        viewModel.loadFollowers(userId)
                    } else {
                        viewModel.loadFollowing(userId)
                    }
                    isRefreshing = false
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> UserList(
                        users = uiState.followers,
                        emptyMessage = "No followers yet",
                        onUserClick = onNavigateToProfile
                    )
                    1 -> UserList(
                        users = uiState.following,
                        emptyMessage = "Not following anyone yet",
                        onUserClick = onNavigateToProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<UserProfile>,
    emptyMessage: String,
    onUserClick: (String) -> Unit
) {
    if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                UserListItem(
                    user = user,
                    onClick = { onUserClick(user.id) }
                )
            }
        }
    }
}

@Composable
private fun UserListItem(
    user: UserProfile,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = user.avatarUrl
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = user.displayName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = BrandIndigo
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // User info
            Column(Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                user.bio?.let { bio ->
                    if (bio.isNotBlank()) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
