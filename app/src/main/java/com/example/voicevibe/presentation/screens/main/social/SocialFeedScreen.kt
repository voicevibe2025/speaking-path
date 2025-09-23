package com.example.voicevibe.presentation.screens.main.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.presentation.screens.main.home.HomeViewModel
import com.example.voicevibe.presentation.screens.main.home.PostCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    postId: Int? = null,
    commentId: Int? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    // Ensure posts are loaded when entering the screen
    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

    // If a specific postId is provided, ensure it's loaded
    LaunchedEffect(postId) {
        postId?.let { viewModel.ensurePostLoaded(it) }
    }

    // Simple gradient background
    val background = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFECF0F1)))
                )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            background()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.refresh()
                        delay(300)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        StatusComposer(
                            avatarUrl = uiState.avatarUrl,
                            userInitials = uiState.userInitials ?: "VV",
                            onPostText = { text -> viewModel.createTextPost(text) },
                            onPostLink = { link -> viewModel.createLinkPost(link) },
                            onPostImage = { part, text -> viewModel.createImagePost(part, text) }
                        )
                    }

                    if (uiState.posts.isNotEmpty()) {
                        items(uiState.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onLike = { viewModel.likePost(post.id) },
                                onUnlike = { viewModel.unlikePost(post.id) },
                                onAddComment = { text, done -> viewModel.addComment(post.id, text) { done() } },
                                fetchComments = { id, cb -> viewModel.fetchComments(id, cb) },
                                likeComment = { commentId -> viewModel.likeComment(commentId) },
                                unlikeComment = { commentId -> viewModel.unlikeComment(commentId) },
                                replyToComment = { parentId, text, done -> viewModel.addComment(post.id, text, parentId) { done() } },
                                onUserClick = onNavigateToUserProfile,
                                onDeletePost = { viewModel.deletePost(post.id) { success -> if (success) viewModel.refresh() } },
                                onDeleteComment = { commentId, done -> viewModel.deleteComment(commentId, post.id) { done() } },
                                initialOpenComments = (postId != null && post.id == postId)
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "No posts yet. Be the first to share!",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusComposer(
    avatarUrl: String?,
    userInitials: String,
    onPostText: (String) -> Unit,
    onPostLink: (String) -> Unit,
    onPostImage: (MultipartBody.Part, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    suspend fun buildImagePart(uri: Uri): MultipartBody.Part? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val stream = contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = stream.use { it.readBytes() }
            val mime = contentResolver.getType(uri) ?: "image/*"
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val filename = "upload_${System.currentTimeMillis()}.jpg"
            MultipartBody.Part.createFormData("image", filename, body)
        } catch (_: Throwable) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "What's on your mind?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new },
                placeholder = { Text("Write a status...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 5
            )

            if (selectedImageUri != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Selected image: $selectedImageUri",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    label = { Text("Photo") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                )

                val canPost = (!isPosting) && ((selectedImageUri != null) || text.isNotBlank())
                Button(
                    onClick = {
                        if (isPosting) return@Button
                        isPosting = true
                        val uri = selectedImageUri
                        val content = text.trim()
                        if (uri != null) {
                            scope.launch {
                                val part = buildImagePart(uri)
                                if (part != null) onPostImage(part, content.ifBlank { null })
                                selectedImageUri = null
                                text = ""
                                isPosting = false
                            }
                        } else if (content.isNotBlank()) {
                            val isLinkOnly = (content.startsWith("http://") || content.startsWith("https://")) && !content.contains(" ")
                            if (isLinkOnly) onPostLink(content) else onPostText(content)
                            text = ""
                            isPosting = false
                        } else {
                            isPosting = false
                        }
                    },
                    enabled = canPost
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Post")
                }
            }
        }
    }
}
