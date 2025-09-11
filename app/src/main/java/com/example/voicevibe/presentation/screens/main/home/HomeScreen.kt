package com.example.voicevibe.presentation.screens.main.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.R
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.presentation.components.*
import com.example.voicevibe.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPractice: () -> Unit,
    onNavigateToPracticeAI: () -> Unit,
    onNavigateToLearningPaths: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLearningPath: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
            viewModel.refresh()
            delay(300) // Minimum refresh time for UX
            isRefreshing = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(animatedOffset)
        FloatingParticles()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                ModernTopBar(
                    title = "VozVibe",
                    onNavigationIconClick = onNavigateToProfile,
                    navigationIcon = {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = "VoiceVibe Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
                // Animated Header Section
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        HeroSection(
                            userName = uiState.userName ?: "Learner",
                            level = uiState.userLevel,
                            userInitials = uiState.userInitials ?: "VV",
                            avatarUrl = uiState.avatarUrl,
                            onProfileClick = onNavigateToProfile
                        )
                    }
                }

                // Enhanced Stats Cards
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(400, 100)) + slideInVertically()
                    ) {
                        StatsSection(
                            totalPoints = uiState.totalPoints,
                            currentStreak = uiState.currentStreak,
                            completedLessons = uiState.completedLessons,
                            onViewAchievements = onNavigateToAchievements
                        )
                    }
                }

                // Quick Actions
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(400, 200)) + slideInVertically()
                    ) {
                        QuickActionsSection(
                            onStartPractice = viewModel::onStartPractice,
                            onPracticeWithAI = onNavigateToPracticeAI,
                            onViewPaths = viewModel::onViewAllPaths,
                            onViewLeaderboard = onNavigateToLeaderboard
                        )
                    }
                }

                // Active Learning Paths
                if (uiState.activeLearningPaths.isNotEmpty()) {
                    item {
                        ActivePathsSection(
                            paths = uiState.activeLearningPaths,
                            onPathClick = viewModel::onContinueLearning
                        )
                    }
                }

                // Recent Badges
                if (uiState.badges.isNotEmpty()) {
                    item {
                        RecentBadgesSection(
                            badges = uiState.badges,
                            onViewAll = viewModel::onViewAchievements
                        )
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


@Composable
private fun StatsSection(
    totalPoints: Int,
    currentStreak: Int,
    completedLessons: Int,
    onViewAchievements: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.EmojiEvents,
            title = "Total Points",
            value = NumberFormat.getNumberInstance(Locale.US).format(totalPoints),
            color = Color(0xFFFFD700),
            onClick = onViewAchievements
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocalFireDepartment,
            title = "Day Streak",
            value = "$currentStreak",
            color = Color(0xFFFF6B6B)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            title = "Completed",
            value = "$completedLessons",
            color = Color(0xFF4ECDC4)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .blur(30.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onStartPractice: () -> Unit,
    onPracticeWithAI: () -> Unit,
    onViewPaths: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Actions",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.White
        )

        QuickActionCard(
            title = "Start Speaking Practice",
            description = "Practice your pronunciation now",
            icon = Icons.Default.Mic,
            gradient = listOf(Color(0xFF6C63FF), Color(0xFF00D9FF)),
            onClick = onStartPractice
        )

        Spacer(modifier = Modifier.height(16.dp))

        QuickActionCard(
            title = "Practice with Vivi",
            description = "AI-powered conversation",
            icon = Icons.Default.Psychology,
            gradient = listOf(Color(0xFFFF006E), Color(0xFFFF4081)),
            onClick = onPracticeWithAI
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Learning Paths",
                icon = Icons.Outlined.School,
                gradient = listOf(Color(0xFFFFBE0B), Color(0xFFFB8500)),
                onClick = onViewPaths
            )

            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Leaderboard",
                icon = Icons.Outlined.Leaderboard,
                gradient = listOf(Color(0xFF8338EC), Color(0xFF6C63FF)),
                onClick = onViewLeaderboard
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    gradient[0].copy(alpha = 0.5f),
                    gradient[1].copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = gradient
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePathsSection(
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
                text = "Continue Learning",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            TextButton(
                onClick = { /* Handle view all */ }
            ) {
                Text(
                    text = "View All",
                    color = BrandCyan,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        paths.forEachIndexed { index, path ->
            PathCard(
                path = path,
                onClick = { onPathClick(path.id) },
                index = index
            )
            
            if (index < paths.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PathCard(
    path: LearningPath,
    onClick: () -> Unit,
    index: Int
) {
    val colors = listOf(
        listOf(BrandCyan, BrandIndigo),
        listOf(BrandIndigo, BrandFuchsia),
        listOf(BrandFuchsia, BrandCyan)
    )
    
    val gradientColors = colors[index % colors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = gradientColors.first().copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = path.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = gradientColors.first().copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${path.completedLessons}/${path.totalLessons} lessons",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.2f) })
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${path.progress}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradientColors.first()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(path.progress / 100f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(gradientColors)
                        )
                )
            }
        }
    }
}

@Composable
private fun RecentBadgesSection(
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    color = BrandCyan,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(badges) { index, badge ->
                BadgeItem(badge = badge, index = index)
            }
        }
    }
}

@Composable
private fun BadgeItem(
    badge: String,
    index: Int
) {
    val colors = listOf(
        BrandCyan,
        BrandIndigo,
        BrandFuchsia,
        Color(0xFFFFD700)
    )
    
    val badgeColor = colors[index % colors.size]

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = badgeColor.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            badgeColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = badge,
                tint = badgeColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
