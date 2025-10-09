package com.example.voicevibe.presentation.screens.main.social

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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.domain.model.SocialNotification
import com.example.voicevibe.presentation.screens.main.home.HomeViewModel
import com.example.voicevibe.ui.theme.BrandIndigo
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.Duration as JDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onOpenNotification: (postId: Int, commentId: Int?) -> Unit,
    onNavigateToProfile: (userId: Int) -> Unit,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications(limit = 50)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.unreadNotifications > 0) {
                        TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadNotifications(limit = 50)
                viewModel.loadUnreadNotificationsCount()
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.notifications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You're all caught up")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(uiState.notifications, key = { it.id }) { n ->
                        NotificationRow(
                            notif = n,
                            onClick = {
                                viewModel.markNotificationRead(n.id)
                                when (n.type) {
                                    "user_follow" -> onNavigateToProfile(n.actor.id)
                                    else -> n.postId?.let { onOpenNotification(it, n.commentId) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notif: SocialNotification,
    onClick: () -> Unit
) {
    val message = when (notif.type) {
        "post_like" -> "liked your post"
        "post_comment" -> "commented on your post"
        "comment_like" -> "liked your comment"
        "comment_reply" -> "replied to your comment"
        "user_follow" -> "is following you"
        else -> notif.type
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actor avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = notif.actor.avatarUrl
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = "Actor Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = notif.actor.displayName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = BrandIndigo
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = "${notif.actor.displayName} $message")
                Text(text = relativeTimeLocal(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            if (!notif.read) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun relativeTimeLocal(ts: LocalDateTime): String {
    return try {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val duration = JDuration.between(ts, now)
        if (duration.isNegative) return "just now"
        val seconds = duration.seconds
        when {
            seconds < 60 -> "just now"
            seconds < 3600 -> {
                val m = seconds / 60
                if (m == 1L) "1 minute ago" else "$m minutes ago"
            }
            seconds < 86400 -> {
                val h = seconds / 3600
                if (h == 1L) "1 hour ago" else "$h hours ago"
            }
            seconds < 172800 -> "yesterday"
            else -> {
                val d = seconds / 86400
                if (d == 1L) "1 day ago" else "$d days ago"
            }
        }
    } catch (_: Exception) {
        ts.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}
