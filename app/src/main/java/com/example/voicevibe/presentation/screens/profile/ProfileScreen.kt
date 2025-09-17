package com.example.voicevibe.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicevibe.data.model.Achievement
import com.example.voicevibe.presentation.components.LoadingScreen
import coil.compose.SubcomposeAsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // Refresh Quick Stats when screen resumes (after practice, etc.)
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
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Progress", "Activity")
    val userName by viewModel.userName
    val proficiency by viewModel.proficiency
    val xp by viewModel.xp
    val streak by viewModel.streak
    val practiceHours by viewModel.practiceHours
    val lessonsCompleted by viewModel.lessonsCompleted
    val recordingsCount by viewModel.recordingsCount
    val avgScore by viewModel.avgScore
    val followersCount by viewModel.followersCount
    val followingCount by viewModel.followingCount
    val recentAchievements by viewModel.recentAchievements
    val recentActivities by viewModel.recentActivities
    val avatarUrl by viewModel.avatarUrl
    val userInitials by viewModel.userInitials
    val nextLevelXp by viewModel.nextLevelXp
    val totalXp by viewModel.totalXp

    // Learning Preferences data
    val dailyPracticeGoal by viewModel.dailyPracticeGoal
    val learningGoal by viewModel.learningGoal
    val targetLanguage by viewModel.targetLanguage

    // Skill Progress data
    val speakingScore by viewModel.speakingScore
    val listeningScore by viewModel.listeningScore
    val grammarScore by viewModel.grammarScore
    val vocabularyScore by viewModel.vocabularyScore
    val pronunciationScore by viewModel.pronunciationScore

    // Monthly Progress data
    val monthlyDaysActive by viewModel.monthlyDaysActive
    val monthlyXpEarned by viewModel.monthlyXpEarned
    val monthlyLessonsCompleted by viewModel.monthlyLessonsCompleted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Profile", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Header
            ProfileHeader(
                userName = userName,
                level = proficiency,
                currentXp = xp,
                totalXp = totalXp,
                nextLevelXp = nextLevelXp,
                streak = streak,
                avatarUrl = avatarUrl,
                userInitials = userInitials
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    QuickStatsGrid(
                        practiceHours = practiceHours,
                        lessonsCompleted = lessonsCompleted,
                        recordingsCount = recordingsCount,
                        avgScore = avgScore,
                        followersCount = followersCount,
                        followingCount = followingCount
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RecentAchievements(
                        achievements = recentAchievements,
                        onNavigateToAchievements = onNavigateToAchievements
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LearningPreferences(
                        dailyGoal = dailyPracticeGoal,
                        focus = learningGoal,
                        difficulty = proficiency,
                        language = targetLanguage
                    )
                }
                1 -> ProgressTab(
                    speakingScore = speakingScore,
                    listeningScore = listeningScore,
                    grammarScore = grammarScore,
                    vocabularyScore = vocabularyScore,
                    pronunciationScore = pronunciationScore,
                    monthlyDaysActive = monthlyDaysActive,
                    monthlyXpEarned = monthlyXpEarned,
                    monthlyLessonsCompleted = monthlyLessonsCompleted
                )
                2 -> ActivityTab(recentActivities = recentActivities)
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    level: String,
    currentXp: Int,
    totalXp: Int,
    nextLevelXp: Int,
    streak: Int,
    avatarUrl: String?,
    userInitials: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Text(
                            text = userInitials,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    error = {
                        Text(
                            text = userInitials,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                )
            } else {
                Text(
                    text = userInitials,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // User Name
        Text(
            text = userName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(4.dp))

        // User Level
        Text(
            text = level,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // XP Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentXp XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$nextLevelXp XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = currentXp.toFloat() / nextLevelXp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Total XP: $totalXp",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streak
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Whatshot,
                contentDescription = "Streak",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$streak Day Streak",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun QuickStatsGrid(
    practiceHours: Float,
    lessonsCompleted: Int,
    recordingsCount: Int,
    avgScore: Float,
    followersCount: Int,
    followingCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Schedule,
                    value = String.format("%.1f", practiceHours),
                    label = "Hours",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    icon = Icons.Default.Assignment,
                    value = lessonsCompleted.toString(),
                    label = "Lessons",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Mic,
                    value = recordingsCount.toString(),
                    label = "Recordings",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    icon = Icons.Default.Star,
                    value = String.format("%.1f", avgScore),
                    label = "Avg Score",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Group,
                    value = followersCount.toString(),
                    label = "Followers",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    icon = Icons.Default.Person,
                    value = followingCount.toString(),
                    label = "Following",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentAchievements(
    achievements: List<Achievement>,
    onNavigateToAchievements: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToAchievements) {
                    Text("View All")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display real achievements or placeholder message
            if (achievements.isNotEmpty()) {
                achievements.forEach { achievement ->
                    AchievementItem(
                        icon = getIconForCategory(achievement.badge.category),
                        title = achievement.badge.name,
                        description = achievement.badge.description,
                        color = parseColor(achievement.badge.patternColor)
                    )
                }
            } else {
                // Placeholder when no achievements
                Text(
                    text = "No recent achievements yet. Keep practicing to earn your first badge!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

// Helper function to get icon based on achievement category
@Composable
fun getIconForCategory(category: String): ImageVector {
    return when (category.lowercase()) {
        "pronunciation" -> Icons.Default.RecordVoiceOver
        "grammar" -> Icons.Default.MenuBook
        "fluency" -> Icons.Default.Speed
        "vocabulary" -> Icons.Default.Translate
        "cultural" -> Icons.Default.Public
        "streak" -> Icons.Default.Whatshot
        "collaboration" -> Icons.Default.Group
        "special" -> Icons.Default.Star
        else -> Icons.Default.EmojiEvents
    }
}

// Helper function to parse color from hex string
@Composable
fun parseColor(hexColor: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (e: Exception) {
        // Fallback to primary color if parsing fails
        MaterialTheme.colorScheme.primary
    }
}

@Composable
fun AchievementItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LearningPreferences(
    dailyGoal: Int,
    focus: String,
    difficulty: String,
    language: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Learning Preferences",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            PreferenceChip("Daily Goal: $dailyGoal min", Icons.Default.Timer)
            PreferenceChip("Focus: $focus", Icons.Default.Business)
            PreferenceChip("Difficulty: $difficulty", Icons.Default.Speed)
            PreferenceChip("Language: $language", Icons.Default.Language)
        }
    }
}

@Composable
fun PreferenceChip(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun ProgressTab(
    speakingScore: Float,
    listeningScore: Float,
    grammarScore: Float,
    vocabularyScore: Float,
    pronunciationScore: Float,
    monthlyDaysActive: Int,
    monthlyXpEarned: Int,
    monthlyLessonsCompleted: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skills Progress
        SkillsProgressCard(
            speakingScore = speakingScore,
            listeningScore = listeningScore,
            grammarScore = grammarScore,
            vocabularyScore = vocabularyScore,
            pronunciationScore = pronunciationScore
        )

        // Monthly Progress
        MonthlyProgressCard(
            daysActive = monthlyDaysActive,
            xpEarned = monthlyXpEarned,
            lessonsCompleted = monthlyLessonsCompleted
        )
    }
}

@Composable
fun SkillsProgressCard(
    speakingScore: Float,
    listeningScore: Float,
    grammarScore: Float,
    vocabularyScore: Float,
    pronunciationScore: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Skills Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            SkillProgressItem("Speaking", speakingScore, MaterialTheme.colorScheme.primary)
            SkillProgressItem("Listening", listeningScore, MaterialTheme.colorScheme.secondary)
            SkillProgressItem("Grammar", grammarScore, MaterialTheme.colorScheme.tertiary)
            SkillProgressItem("Vocabulary", vocabularyScore, MaterialTheme.colorScheme.error)
            SkillProgressItem("Pronunciation", pronunciationScore, Color(0xFF4CAF50))
        }
    }
}

@Composable
fun SkillProgressItem(skill: String, progress: Float, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(skill, fontSize = 14.sp)
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MonthlyProgressCard(
    daysActive: Int,
    xpEarned: Int,
    lessonsCompleted: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "This Month",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MonthStatItem("Days Active", daysActive.toString(), Icons.Default.CalendarToday)
                MonthStatItem("XP Earned", formatNumber(xpEarned), Icons.Default.Star)
                MonthStatItem("Lessons", lessonsCompleted.toString(), Icons.Default.School)
            }
        }
    }
}

// Helper function to format numbers with commas
private fun formatNumber(number: Int): String {
    return java.text.NumberFormat.getNumberInstance().format(number)
}

@Composable
fun MonthStatItem(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActivityTab(recentActivities: List<com.example.voicevibe.data.model.Activity>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recent Activity
        RecentActivityCard(activities = recentActivities)
    }
}

@Composable
fun RecentActivityCard(activities: List<com.example.voicevibe.data.model.Activity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (activities.isEmpty()) {
                // Show placeholder when no activities
                Text(
                    text = "No recent activities yet. Start practicing to see your activity here!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                // Display real activities
                activities.forEach { activity ->
                    ActivityItem(
                        title = activity.title,
                        time = activity.relativeTime,
                        icon = getIconFromString(activity.icon),
                        color = parseColor(activity.color)
                    )
                }
            }
        }
    }
}

// Helper function to map backend icon strings to Material Icons
@Composable
fun getIconFromString(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "check_circle" -> Icons.Default.CheckCircle
        "mic" -> Icons.Default.Mic
        "emoji_events" -> Icons.Default.EmojiEvents
        "school" -> Icons.Default.School
        "leaderboard" -> Icons.Default.Leaderboard
        "star" -> Icons.Default.Star
        "grade" -> Icons.Default.Grade
        "workspace_premium" -> Icons.Default.WorkspacePremium
        else -> Icons.Default.CheckCircle // Default fallback
    }
}

@Composable
fun ActivityItem(
    title: String,
    time: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Text(
                text = time,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
