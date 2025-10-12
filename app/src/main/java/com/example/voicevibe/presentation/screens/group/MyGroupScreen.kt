package com.example.voicevibe.presentation.screens.group

import androidx.compose.foundation.background
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.voicevibe.domain.model.GroupMember
import com.example.voicevibe.domain.model.GroupMessage
import com.example.voicevibe.domain.model.Resource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupScreen(
    onBackPressed: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: GroupViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedNavTab by remember { mutableIntStateOf(1) } // Group tab selected
    val tabs = listOf("Members", "Chat")

    val membersState by viewModel.membersState.collectAsState()
    val messagesState by viewModel.messagesState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyGroupMembers()
        viewModel.loadMyGroupMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        when (membersState) {
                            is Resource.Success -> {
                                val (group, _) = (membersState as Resource.Success).data!!
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = group.icon,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column {
                                        Text(
                                            group.displayName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            "${group.memberCount} members",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text("My Group")
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            FloatingBottomNavigationForGroup(
                selectedTab = selectedNavTab,
                onTabSelected = { index ->
                    selectedNavTab = index
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> { /* Already on Group */ }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            // Content
            when (selectedTab) {
                0 -> MembersTab(membersState, viewModel)
                1 -> ChatTab(messagesState, viewModel)
            }
        }
    }
}

@Composable
private fun MembersTab(
    membersState: Resource<Pair<com.example.voicevibe.domain.model.Group, List<GroupMember>>>,
    viewModel: GroupViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (membersState) {
            is Resource.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is Resource.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = membersState.message ?: "Failed to load members",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadMyGroupMembers() }) {
                        Text("Retry")
                    }
                }
            }
            is Resource.Success -> {
                val (group, members) = membersState.data!!
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Group info header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = parseColor(group.color).copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = group.icon,
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    group.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Members header
                    item {
                        Text(
                            "Top Members",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Members list (sorted by XP)
                    items(members) { member ->
                        MemberCard(member, members.indexOf(member) + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(member: GroupMember, rank: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            AsyncImage(
                model = member.avatarUrl,
                contentDescription = member.displayName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Level
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            " Lv.${member.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Streak
                    if (member.streakDays > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🔥",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                " ${member.streakDays}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // XP
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${member.xp}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatTab(
    messagesState: Resource<Triple<com.example.voicevibe.domain.model.Group, List<GroupMessage>, Boolean>>,
    viewModel: GroupViewModel
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = messagesState.message ?: "Failed to load messages",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMyGroupMessages() }) {
                            Text("Retry")
                        }
                    }
                }
                is Resource.Success -> {
                    val (_, messages, _) = messagesState.data!!
                    
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "💬",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No messages yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "Be the first to say hello!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            reverseLayout = false
                        ) {
                            items(messages) { message ->
                                MessageItem(message)
                            }
                        }
                        
                        // Auto-scroll to bottom on new messages
                        LaunchedEffect(messages.size) {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                }
            }
        }

        // Message input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText.trim())
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (messageText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageItem(message: GroupMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        AsyncImage(
            model = message.senderAvatar,
            contentDescription = message.senderName,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Message content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = java.time.Duration.between(timestamp, now)

    return when {
        diff.toMinutes() < 1 -> "Just now"
        diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
        diff.toHours() < 24 -> "${diff.toHours()}h ago"
        diff.toDays() < 7 -> "${diff.toDays()}d ago"
        else -> timestamp.format(DateTimeFormatter.ofPattern("MMM dd"))
    }
}

private fun parseColor(hexColor: String): Color {
    return try {
        val colorString = hexColor.removePrefix("#")
        val colorInt = colorString.toLong(16)
        Color(0xFF000000 or colorInt)
    } catch (e: Exception) {
        Color(0xFF1976D2) // Default blue
    }
}

/**
 * Floating Bottom Navigation Bar for Group Screen
 */
@Composable
fun FloatingBottomNavigationForGroup(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1E2761).copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            FloatingNavItemForGroup(
                icon = Icons.Filled.Home,
                label = "Home",
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            
            // Group Tab
            FloatingNavItemForGroup(
                icon = Icons.Filled.Groups,
                label = "Group",
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
        }
    }
}

/**
 * Single Navigation Item for Group Screen
 */
@Composable
fun FloatingNavItemForGroup(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        animationSpec = tween(300),
        label = "alpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val accentColor = Color(0xFF00BCD4) // Cyan
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = if (selected) accentColor else Color.White.copy(alpha = animatedAlpha)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accentColor else Color.White.copy(alpha = animatedAlpha),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
