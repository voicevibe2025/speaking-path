package com.example.voicevibe.presentation.screens.group

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.GroupMember
import com.example.voicevibe.presentation.components.LoadingScreen
import com.example.voicevibe.presentation.components.ErrorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupProfileScreen(
    groupId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToGroupChat: () -> Unit,
    viewModel: GroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) {
        viewModel.loadGroupProfile(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Show chat icon if user is part of this group
                    if (uiState.currentUserGroupId == groupId) {
                        IconButton(onClick = onNavigateToGroupChat) {
                            Icon(Icons.Default.Chat, "Group Chat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorScreen(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadGroupProfile(groupId) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                val group = uiState.selectedGroup
                val members = uiState.groupMembers

                if (group != null) {
                    GroupProfileContent(
                        group = group,
                        members = members,
                        totalXp = members.sumOf { it.xp },
                        onMemberClick = { member ->
                            onNavigateToUserProfile(member.userId.toString())
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupProfileContent(
    group: Group,
    members: List<GroupMember>,
    totalXp: Int,
    onMemberClick: (GroupMember) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group Header Card
        item {
            GroupHeaderCard(
                group = group,
                totalXp = totalXp,
                memberCount = members.size
            )
        }

        // Group Description Card
        item {
            GroupDescriptionCard(
                description = group.description
            )
        }

        // Stats Overview
        item {
            GroupStatsCard(
                totalXp = totalXp,
                memberCount = members.size,
                averageXp = if (members.isNotEmpty()) totalXp / members.size else 0,
                topMemberXp = members.maxOfOrNull { it.xp } ?: 0
            )
        }

        // Members Section Header
        item {
            Text(
                text = "Members (${members.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Member List
        items(
            items = members.sortedByDescending { it.xp },
            key = { it.userId }
        ) { member ->
            GroupMemberCard(
                member = member,
                rank = members.sortedByDescending { it.xp }.indexOf(member) + 1,
                onClick = { onMemberClick(member) }
            )
        }
    }
}

@Composable
private fun GroupHeaderCard(
    group: Group,
    totalXp: Int,
    memberCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = parseColor(group.color).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Group Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                parseColor(group.color),
                                parseColor(group.color).copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.icon,
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group Name
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = parseColor(group.color)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                QuickStatItem(
                    icon = Icons.Default.EmojiEvents,
                    label = "Total XP",
                    value = totalXp.toString()
                )
                QuickStatItem(
                    icon = Icons.Default.People,
                    label = "Members",
                    value = memberCount.toString()
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GroupDescriptionCard(
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupStatsCard(
    totalXp: Int,
    memberCount: Int,
    averageXp: Int,
    topMemberXp: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "Statistics",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total XP",
                    value = totalXp.toString(),
                    icon = Icons.Default.EmojiEvents
                )
                StatItem(
                    label = "Average XP",
                    value = averageXp.toString(),
                    icon = Icons.Default.TrendingUp
                )
                StatItem(
                    label = "Top Member",
                    value = "${topMemberXp} XP",
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMemberCard(
    member: GroupMember,
    rank: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank Badge
                Surface(
                    shape = CircleShape,
                    color = when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Avatar
                val initials = remember(member.displayName) {
                    generateInitials(member.displayName)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!member.avatarUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = member.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Text(
                                    text = initials,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            },
                            error = {
                                Text(
                                    text = initials,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        )
                    } else {
                        Text(
                            text = initials,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Name and Level
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Level ${member.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (member.streakDays > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFFF6B35)
                                )
                                Text(
                                    "${member.streakDays}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF6B35)
                                )
                            }
                        }
                    }
                }
            }

            // XP
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${member.xp} XP",
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
