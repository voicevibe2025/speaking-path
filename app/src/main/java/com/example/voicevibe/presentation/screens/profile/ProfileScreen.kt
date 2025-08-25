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
import androidx.compose.ui.draw.shadow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Progress", "Activity")
    val userName by viewModel.userName

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
                userLevel = "Advanced",
                userXP = 4850,
                nextLevelXP = 5000,
                streak = 15
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
                0 -> OverviewTab(onNavigateToAchievements)
                1 -> ProgressTab()
                2 -> ActivityTab()
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    userLevel: String,
    userXP: Int,
    nextLevelXP: Int,
    streak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
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
                Text(
                    text = userName.split(" ").map { it.first() }.joinToString(""),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Name and Level
            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = userLevel,
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
                        text = "$userXP XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$nextLevelXP XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = userXP.toFloat() / nextLevelXP,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streak Badge
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
                    Icons.Default.LocalFireDepartment,
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
}

@Composable
fun OverviewTab(onNavigateToAchievements: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats
        QuickStatsGrid()

        // Recent Achievements
        RecentAchievements(onNavigateToAchievements)

        // Learning Preferences
        LearningPreferences()
    }
}

@Composable
fun QuickStatsGrid() {
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
                    value = "125",
                    label = "Hours",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    icon = Icons.Default.Assignment,
                    value = "45",
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
                    value = "892",
                    label = "Recordings",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    icon = Icons.Default.Star,
                    value = "4.8",
                    label = "Avg Score",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
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
fun RecentAchievements(onNavigateToAchievements: () -> Unit) {
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

            // Achievement items
            AchievementItem(
                icon = Icons.Default.EmojiEvents,
                title = "Master Speaker",
                description = "Completed 50 speaking sessions",
                color = Color(0xFFFFD700)
            )
            AchievementItem(
                icon = Icons.Default.Speed,
                title = "Speed Learner",
                description = "Finished 5 lessons in one day",
                color = Color(0xFF00CED1)
            )
            AchievementItem(
                icon = Icons.Default.TrendingUp,
                title = "Consistent Performer",
                description = "Maintained 90% accuracy for a week",
                color = Color(0xFF32CD32)
            )
        }
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
fun LearningPreferences() {
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

            PreferenceChip("Daily Goal: 30 min", Icons.Default.Timer)
            PreferenceChip("Focus: Business English", Icons.Default.Business)
            PreferenceChip("Difficulty: Intermediate", Icons.Default.Speed)
            PreferenceChip("Accent: American", Icons.Default.Language)
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
fun ProgressTab() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skills Progress
        SkillsProgressCard()

        // Monthly Progress
        MonthlyProgressCard()
    }
}

@Composable
fun SkillsProgressCard() {
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

            SkillProgressItem("Speaking", 0.85f, MaterialTheme.colorScheme.primary)
            SkillProgressItem("Listening", 0.72f, MaterialTheme.colorScheme.secondary)
            SkillProgressItem("Grammar", 0.90f, MaterialTheme.colorScheme.tertiary)
            SkillProgressItem("Vocabulary", 0.68f, MaterialTheme.colorScheme.error)
            SkillProgressItem("Pronunciation", 0.78f, Color(0xFF4CAF50))
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
fun MonthlyProgressCard() {
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
                MonthStatItem("Days Active", "22", Icons.Default.CalendarToday)
                MonthStatItem("XP Earned", "1,250", Icons.Default.Star)
                MonthStatItem("Lessons", "38", Icons.Default.School)
            }
        }
    }
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
fun ActivityTab() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recent Activity
        RecentActivityCard()
    }
}

@Composable
fun RecentActivityCard() {
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

            ActivityItem(
                title = "Completed Business Presentation Module",
                time = "2 hours ago",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            ActivityItem(
                title = "Practiced Interview Scenarios",
                time = "Yesterday",
                icon = Icons.Default.Mic,
                color = MaterialTheme.colorScheme.primary
            )
            ActivityItem(
                title = "Earned 'Perfect Week' badge",
                time = "3 days ago",
                icon = Icons.Default.EmojiEvents,
                color = Color(0xFFFFD700)
            )
            ActivityItem(
                title = "Joined weekly leaderboard",
                time = "5 days ago",
                icon = Icons.Default.Leaderboard,
                color = MaterialTheme.colorScheme.secondary
            )
            ActivityItem(
                title = "Started Advanced Grammar path",
                time = "1 week ago",
                icon = Icons.Default.School,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
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
