package com.example.voicevibe.presentation.screens.main.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.R
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.presentation.components.*
import com.example.voicevibe.domain.model.PostComment
import com.example.voicevibe.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPractice: () -> Unit,
    onNavigateToPracticeAI: () -> Unit,
    onNavigateToLivePractice: () -> Unit,
    onNavigateToLearningPaths: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLearningPath: (String) -> Unit,
    onNavigateToSocialFeed: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToUserSearch: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToLearnWithVivi: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    // Refresh avatar when returning from Settings screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Observe events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToPractice -> onNavigateToPractice()
                is HomeEvent.NavigateToLearningPaths -> onNavigateToLearningPaths()
                is HomeEvent.NavigateToAchievements -> onNavigateToAchievements()
                is HomeEvent.NavigateToLearningPath -> onNavigateToLearningPath(event.pathId)
            }
        }
    }

    // Handle refresh
    fun onRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            delay(300) // Minimum refresh time for UX
            isRefreshing = false
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                EducationalTopBar(
                    avatarUrl = uiState.avatarUrl,
                    userInitials = uiState.userInitials ?: "VV",
                    unreadNotifications = uiState.unreadNotifications,
                    unreadMessages = uiState.unreadMessages,
                    onNotificationsClick = {
                        onNavigateToNotifications()
                    },
                    onMessagesClick = {
                        onNavigateToMessages()
                    },
                    onAvatarClick = onNavigateToProfile,
                    onSearchClick = onNavigateToUserSearch,
                    onSettingsClick = onNavigateToSettings
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = ::onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(vertical = 20.dp, horizontal = 16.dp)
                ) {
                    // Learning Progress
                    item {
                        LearningProgressSection(
                            totalPoints = uiState.totalPoints,
                            currentStreak = uiState.currentStreak,
                            completedLessons = uiState.completedLessons
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        GlassmorphismDivider()
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Quick Start Actions - Highlighted
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(400, 100))
                        ) {
                            QuickStartSection(
                                onStartPractice = viewModel::onStartPractice,
                                onPracticeWithAI = onNavigateToPracticeAI,
                                onLivePractice = onNavigateToLivePractice,
                                viviTopics = uiState.viviTopics,
                                onNavigateToLearnWithVivi = onNavigateToLearnWithVivi
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        GlassmorphismDivider()
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Study Tools
                    item {
                        StudyToolsSection(
                            onViewPaths = viewModel::onViewAllPaths,
                            onViewLeaderboard = onNavigateToLeaderboard,
                            onViewAchievements = viewModel::onViewAchievements,
                            hasNewAchievements = uiState.hasNewAchievements
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun SocialSection(
    onNavigateToSocialFeed: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Social",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SocialFeedEntryCard(onClick = onNavigateToSocialFeed)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: com.example.voicevibe.domain.model.Post,
    onLike: () -> Unit,
    onUnlike: () -> Unit,
    onAddComment: (String, () -> Unit) -> Unit,
    fetchComments: (Int, (List<PostComment>) -> Unit) -> Unit,
    likeComment: (Int) -> Unit,
    unlikeComment: (Int) -> Unit,
    replyToComment: (parentId: Int, text: String, () -> Unit) -> Unit,
    onUserClick: (String) -> Unit,
    onDeletePost: () -> Unit,
    onDeleteComment: (Int, () -> Unit) -> Unit,
    initialOpenComments: Boolean = false,
) {
    val context = LocalContext.current
    var showComments by remember { mutableStateOf(initialOpenComments) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(BrandIndigo.copy(alpha = 0.1f))
                            .clickable { onUserClick(post.author.id.toString()) },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = post.author.avatarUrl
                        if (avatarUrl != null) {
                            SubcomposeAsyncImage(
                                model = avatarUrl,
                                contentDescription = "Author Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = post.author.displayName.take(2).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Online status indicator
                    com.example.voicevibe.presentation.components.OnlineStatusIndicator(
                        isOnline = post.author.isOnline,
                        size = 10.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.author.displayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    // Relative time from createdAt
                    Text(relativeTime(post.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (post.canDelete) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!post.linkUrl.isNullOrBlank()) {
                val link = post.linkUrl!!
                Text(
                    text = link,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        } catch (_: Throwable) {}
                    }
                )
            } else {
                if (!post.text.isNullOrBlank()) {
                    Text(post.text!!, color = MaterialTheme.colorScheme.onSurface)
                }
                if (!post.imageUrl.isNullOrBlank()) {
                    if (!post.text.isNullOrBlank()) Spacer(Modifier.height(6.dp))
                    SubcomposeAsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Like (icon + count)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (post.isLikedByMe) onUnlike() else onLike()
                    }
                ) {
                    val likeTint = if (post.isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = likeTint
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(post.likesCount.toString(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                // Comment (icon + count)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showComments = true }
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(6.dp))
                    Text(post.commentsCount.toString(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }

    // Confirm deletion dialog inside PostCard scope
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete post?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeletePost()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showComments) {
        var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
        var newComment by remember { mutableStateOf("") }
        var replyToId by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(post.id, showComments) {
            if (showComments) fetchComments(post.id) { list -> comments = list }
        }

        ModalBottomSheet(
            onDismissRequest = { showComments = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Comments", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                // Build parent -> replies grouping for nested view
                val repliesByParent = remember(comments) { comments.filter { it.parent != null }.groupBy { it.parent!! } }
                val parentComments = remember(comments) { comments.filter { it.parent == null }.sortedByDescending { it.createdAt } }

                LazyColumn(
                    modifier = Modifier
                        .heightIn(min = 120.dp, max = 360.dp)
                        .fillMaxWidth()
                ) {
                    items(parentComments, key = { it.id }) { parent ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            // Parent comment row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .clickable { onUserClick(parent.author.id.toString()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val cAvatar = parent.author.avatarUrl
                                    if (cAvatar != null) {
                                        SubcomposeAsyncImage(
                                            model = cAvatar,
                                            contentDescription = "Commenter Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = parent.author.displayName.take(2).uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(parent.author.displayName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(relativeTime(parent.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(parent.text, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val currentlyLiked = parent.isLikedByMe
                                        val updated = comments.map { item ->
                                            if (item.id == parent.id) item.copy(
                                                isLikedByMe = !currentlyLiked,
                                                likesCount = if (!currentlyLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                                            ) else item
                                        }
                                        comments = updated
                                        if (currentlyLiked) unlikeComment(parent.id) else likeComment(parent.id)
                                    }
                                ) {
                                    val likeTint = if (parent.isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    Icon(
                                        imageVector = if (parent.isLikedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = likeTint
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(parent.likesCount.toString(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Text(
                                    text = "Reply",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { replyToId = parent.id }
                                )
                                if (parent.canDelete) {
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable {
                                            onDeleteComment(parent.id) {
                                                fetchComments(post.id) { list -> comments = list }
                                            }
                                        }
                                    )
                                }
                            }

                            // Replies nested under parent (newest first)
                            val replies = (repliesByParent[parent.id] ?: emptyList()).sortedByDescending { it.createdAt }
                            if (replies.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Column(modifier = Modifier.padding(start = 36.dp)) {
                                    replies.forEach { child ->
                                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                        .clickable { onUserClick(child.author.id.toString()) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val rAvatar = child.author.avatarUrl
                                                    if (rAvatar != null) {
                                                        SubcomposeAsyncImage(
                                                            model = rAvatar,
                                                            contentDescription = "Reply Avatar",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Text(
                                                            text = child.author.displayName.take(2).uppercase(),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(child.author.displayName, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(relativeTime(child.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(child.text, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        val currentlyLiked = child.isLikedByMe
                                                        val updated = comments.map { item ->
                                                            if (item.id == child.id) item.copy(
                                                                isLikedByMe = !currentlyLiked,
                                                                likesCount = if (!currentlyLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                                                            ) else item
                                                        }
                                                        comments = updated
                                                        if (currentlyLiked) unlikeComment(child.id) else likeComment(child.id)
                                                    }
                                                ) {
                                                    val likeTint = if (child.isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    Icon(
                                                        imageVector = if (child.isLikedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = null,
                                                        tint = likeTint
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(child.likesCount.toString(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                                Text(
                                                    text = "Reply",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.clickable { replyToId = parent.id }
                                                )
                                                if (child.canDelete) {
                                                    Text(
                                                        text = "Delete",
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.clickable {
                                                            onDeleteComment(child.id) {
                                                                fetchComments(post.id) { list -> comments = list }
                                                            }
                                                        }
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
                Spacer(Modifier.height(12.dp))

                replyToId?.let { pid ->
                    val name = comments.firstOrNull { it.id == pid }?.author?.displayName ?: "comment"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Replying to $name", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { replyToId = null }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        placeholder = { Text(if (replyToId != null) "Write a reply" else "Write a comment") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val c = newComment.trim()
                            if (c.isNotEmpty()) {
                                val done: () -> Unit = {
                                    fetchComments(post.id) { list -> comments = list }
                                }
                                val parent = replyToId
                                if (parent != null) {
                                    replyToComment(parent, c, done)
                                } else {
                                    onAddComment(c, done)
                                }
                                newComment = ""
                                replyToId = null
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post Comment", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun relativeTime(ts: LocalDateTime): String {
    return try {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val duration = java.time.Duration.between(ts, now)
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

@Composable
private fun EducationalTopBar(
    avatarUrl: String?,
    userInitials: String,
    unreadNotifications: Int,
    unreadMessages: Int,
    onNotificationsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Profile avatar
            IconButton(
                onClick = onAvatarClick,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = userInitials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Flexible space between left and right groups
            Spacer(modifier = Modifier.weight(1f))
            
            // Right group: Icons compacted together
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Notifications bell with badge
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    BadgedBox(badge = {
                        if (unreadNotifications > 0) {
                            Badge { Text(text = unreadNotifications.coerceAtMost(99).toString()) }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                }
                // Messages icon with badge
                IconButton(
                    onClick = onMessagesClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    BadgedBox(badge = {
                        if (unreadMessages > 0) {
                            Badge { Text(text = unreadMessages.coerceAtMost(99).toString()) }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Message,
                            contentDescription = "Messages",
                            tint = Color.White
                        )
                    }
                }
                // Search button
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Users",
                        tint = Color.White
                    )
                }
                // Settings gear icon
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

    }
}

@Composable
private fun WelcomeSection(
    userName: String,
    level: Int,
    userInitials: String,
    avatarUrl: String?,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                        .background(BrandIndigo.copy(alpha = 0.1f))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = userInitials,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandIndigo
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$userName!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralDarkGray
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PracticeAccuracyGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Level $level",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStartSection(
    onStartPractice: () -> Unit,
    onPracticeWithAI: () -> Unit,
    onLivePractice: () -> Unit,
    viviTopics: List<ViviTopic>,
    onNavigateToLearnWithVivi: (String) -> Unit
) {
    // Find the first unlocked topic, or fallback to first topic
    val defaultTopicId = remember(viviTopics) {
        viviTopics.firstOrNull { it.unlocked }?.id ?: viviTopics.firstOrNull()?.id
    }
    
    BoxWithConstraints {
        val isCompact = maxWidth < 360.dp
        Column {
            Text(
                text = "Start Learning",
                fontSize = if (isCompact) 18.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Primary Action - Speaking Practice (Blue)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartPractice() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 16.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Practice Speaking",
                        fontSize = if (isCompact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Practice speaking with selected topic",
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Learn With Vivi - Topic-based Learning (Purple)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        defaultTopicId?.let { onNavigateToLearnWithVivi(it) }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 16.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Learn with Vivi",
                        fontSize = if (isCompact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Learn common phrases with Vivi",
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Live Practice - Gemini Live (Slate/Gray)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLivePractice() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 16.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Practice with Vivi",
                        fontSize = if (isCompact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Practice speaking with AI tutor",
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LearningProgressSection(
    totalPoints: Int,
    currentStreak: Int,
    completedLessons: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            val isCompact = maxWidth < 360.dp
            Column {
                Text(
                    text = "Progress",
                    fontSize = if (isCompact) 18.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProgressItem(
                        icon = Icons.Default.EmojiEvents,
                        label = "Points",
                        value = NumberFormat.getNumberInstance(Locale.US).format(totalPoints),
                        color = Color(0xFFFB923C),
                        isCompact = isCompact
                    )
                    
                    ProgressItem(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Streak",
                        value = "$currentStreak days",
                        color = Color(0xFFA855F7),
                        isCompact = isCompact
                    )
                    
                    ProgressItem(
                        icon = Icons.Default.CheckCircle,
                        label = "Completed",
                        value = "$completedLessons lessons",
                        color = Color(0xFF4ADE80),
                        isCompact = isCompact
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    isCompact: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 120.dp)
    ) {
        val iconSize = if (isCompact) 48.dp else 64.dp
        val iconInnerSize = if (isCompact) 24.dp else 32.dp
        
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconInnerSize)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = if (isCompact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontSize = if (isCompact) 12.sp else 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StudyToolsSection(
    onViewPaths: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onViewAchievements: () -> Unit,
    hasNewAchievements: Boolean = false
) {
    BoxWithConstraints {
        val isCompact = maxWidth < 360.dp
        val cardSpacing = if (isCompact) 8.dp else 12.dp
        
        Column {
            Text(
                text = "Study Tools",
                fontSize = if (isCompact) 18.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                StudyToolCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Map,
                    title = "Quest",
                    color = Color(0xFF3B82F6),
                    onClick = onViewPaths,
                    isCompact = isCompact
                )

                StudyToolCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Leaderboard,
                    title = "Leaderboard",
                    color = Color(0xFFFB923C),
                    onClick = onViewLeaderboard,
                    isCompact = isCompact
                )

                StudyToolCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    title = "Achievements",
                    color = Color(0xFFEC4899),
                    onClick = onViewAchievements,
                    isCompact = isCompact,
                    showBadge = hasNewAchievements
                )
            }
        }
    }
}

@Composable
private fun StudyToolCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    isCompact: Boolean = false,
    showBadge: Boolean = false
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCompact) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val iconBgSize = if (isCompact) 40.dp else 48.dp
                val iconSize = if (isCompact) 20.dp else 24.dp
                
                Box(
                    modifier = Modifier
                        .size(iconBgSize)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Badge indicator
        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .border(2.dp, Color(0xFF243454), CircleShape)
            )
        }
    }
}

@Composable
private fun CurrentCoursesSection(
    paths: List<LearningPath>,
    onPathClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Courses",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = {}) {
                Text(
                    text = "See All",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        paths.forEachIndexed { index, path ->
            CourseCard(
                path = path,
                onClick = { onPathClick(path.id) }
            )
            
            if (index < paths.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CourseCard(
    path: LearningPath,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = path.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${path.completedLessons} of ${path.totalLessons} lessons",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (path.progress >= 80) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${path.progress}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { path.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (path.progress >= 80) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun AchievementsSection(
    badges: List<String>,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Achievements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(badges) { index, badge ->
                AchievementBadge(badge = badge, index = index)
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    badge: String,
    index: Int
) {
    val colors = listOf(
        PracticeXPYellow,
        PracticeAccuracyGreen,
        BrandFuchsia,
        BrandIndigo
    )
    
    val badgeColor = colors[index % colors.size]

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (index % 4) {
                    0 -> Icons.Default.EmojiEvents
                    1 -> Icons.Default.Star
                    2 -> Icons.Default.School
                    else -> Icons.Default.Grade
                },
                contentDescription = badge,
                tint = badgeColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun SocialFeedEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = BrandIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Community Feed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Share updates and see friends' posts", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AccentBlueGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicSelectionDialog(
    topics: List<ViviTopic>,
    onDismiss: () -> Unit,
    onTopicSelected: (String) -> Unit
) {
    val sortedTopics = remember(topics) {
        topics.sortedWith(
            compareByDescending<ViviTopic> { it.unlocked }
                .thenBy { it.title.lowercase(Locale.getDefault()) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Select a Topic",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            if (sortedTopics.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentBlueGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No topics available yet",
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Complete practices to unlock topics",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedTopics) { topic ->
                        val canOpen = topic.unlocked
                        val statusIcon = if (topic.unlocked) Icons.Default.LockOpen else Icons.Default.Lock
                        val statusText = if (topic.unlocked) "Unlocked" else "Locked"
                        val statusColor = if (topic.unlocked) PracticeAccuracyGreen else AccentBlueGray
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (canOpen) 1f else 0.6f)
                                .clickable(enabled = canOpen) { onTopicSelected(topic.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (topic.unlocked) 0.5f else 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topic.title,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = topic.description,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = statusIcon,
                                            contentDescription = statusText,
                                            tint = statusColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = statusText,
                                            color = statusColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (canOpen) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = AccentBlueGray
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AccentBlueGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BrandIndigo)
            }
        }
    )
}

@Composable
private fun GlassmorphismDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
    )
}

