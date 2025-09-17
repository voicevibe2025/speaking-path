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

// Educational color palette
private val EduPrimary = Color(0xFF2D3E50)
private val EduSecondary = Color(0xFF3498DB)
private val EduAccent = Color(0xFFE74C3C)
private val EduSuccess = Color(0xFF27AE60)
private val EduWarning = Color(0xFFF39C12)
private val EduBackground = Color(0xFFF8F9FA)
private val EduSurface = Color(0xFFFFFFFF)
private val EduTextPrimary = Color(0xFF2C3E50)
private val EduTextSecondary = Color(0xFF7F8C8D)

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
    val pullToRefreshState = rememberPullToRefreshState()

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Simple gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EduBackground,
                            Color(0xFFECF0F1)
                        )
                    )
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                EducationalTopBar(
                    title = "VozVibe",
                    avatarUrl = uiState.avatarUrl,
                    userInitials = uiState.userInitials ?: "VV",
                    onNavigationIconClick = onNavigateToProfile
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
                        .fillMaxSize()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Welcome Section
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            WelcomeSection(
                                userName = uiState.userName ?: "Student",
                                level = uiState.userLevel,
                                userInitials = uiState.userInitials ?: "VV",
                                avatarUrl = uiState.avatarUrl,
                                onProfileClick = onNavigateToProfile
                            )
                        }
                    }

                    // Quick Start Actions - Highlighted
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(400, 100))
                        ) {
                            QuickStartSection(
                                onStartPractice = viewModel::onStartPractice,
                                onPracticeWithAI = onNavigateToPracticeAI
                            )
                        }
                    }

                    // Learning Progress
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(400, 200))
                        ) {
                            LearningProgressSection(
                                totalPoints = uiState.totalPoints,
                                currentStreak = uiState.currentStreak,
                                completedLessons = uiState.completedLessons
                            )
                        }
                    }

                    // Study Tools
                    item {
                        StudyToolsSection(
                            onViewPaths = viewModel::onViewAllPaths,
                            onViewLeaderboard = onNavigateToLeaderboard,
                            onViewAchievements = onNavigateToAchievements
                        )
                    }

                    // Current Courses
                    if (uiState.activeLearningPaths.isNotEmpty()) {
                        item {
                            CurrentCoursesSection(
                                paths = uiState.activeLearningPaths,
                                onPathClick = viewModel::onContinueLearning
                            )
                        }
                    }

                    // Achievements
                    if (uiState.badges.isNotEmpty()) {
                        item {
                            AchievementsSection(
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
}

@Composable
private fun EducationalTopBar(
    title: String,
    avatarUrl: String?,
    userInitials: String,
    onNavigationIconClick: () -> Unit
) {
    Surface(
        color = EduSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "VoiceVibe Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduTextPrimary
                )
            }
            
            IconButton(onClick = onNavigationIconClick) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EduSecondary.copy(alpha = 0.1f)),
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
                            fontWeight = FontWeight.Bold,
                            color = EduSecondary
                        )
                    }
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
        colors = CardDefaults.cardColors(containerColor = EduSurface),
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
                    .background(EduSecondary.copy(alpha = 0.1f))
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
                        color = EduSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$userName!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduTextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EduSuccess
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Level $level",
                        fontSize = 14.sp,
                        color = EduTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStartSection(
    onStartPractice: () -> Unit,
    onPracticeWithAI: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Start Learning",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Primary Action - Speaking Practice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartPractice() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EduSecondary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Speaking Practice",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Practice pronunciation with instant feedback",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Action - AI Practice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPracticeWithAI() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EduAccent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Practice with Vivi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "AI-powered conversation partner",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Your Progress",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressItem(
                    icon = Icons.Default.EmojiEvents,
                    label = "Points",
                    value = NumberFormat.getNumberInstance(Locale.US).format(totalPoints),
                    color = EduWarning
                )
                
                ProgressItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Streak",
                    value = "$currentStreak days",
                    color = EduAccent
                )
                
                ProgressItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Completed",
                    value = "$completedLessons lessons",
                    color = EduSuccess
                )
            }
        }
    }
}

@Composable
private fun ProgressItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EduTextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = EduTextSecondary
        )
    }
}

@Composable
private fun StudyToolsSection(
    onViewPaths: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onViewAchievements: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Study Tools",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudyToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Route,
                title = "Learning Paths",
                color = EduSuccess,
                onClick = onViewPaths
            )

            StudyToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Leaderboard,
                title = "Leaderboard",
                color = EduWarning,
                onClick = onViewLeaderboard
            )

            StudyToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Stars,
                title = "Achievements",
                color = EduAccent,
                onClick = onViewAchievements
            )
        }
    }
}

@Composable
private fun StudyToolCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EduSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EduTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                color = EduTextPrimary
            )

            TextButton(onClick = {}) {
                Text(
                    text = "See All",
                    color = EduSecondary,
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
        colors = CardDefaults.cardColors(containerColor = EduSurface),
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
                        color = EduTextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = EduTextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${path.completedLessons} of ${path.totalLessons} lessons",
                            fontSize = 13.sp,
                            color = EduTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (path.progress >= 80) EduSuccess else EduSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${path.progress}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
                color = if (path.progress >= 80) EduSuccess else EduSecondary,
                trackColor = Color(0xFFE0E0E0),
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
                color = EduTextPrimary
            )

            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    color = EduSecondary,
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
        EduWarning,
        EduSuccess,
        EduAccent,
        EduSecondary
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