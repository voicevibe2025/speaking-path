package com.example.voicevibe.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.domain.model.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Duration

// New: Flattened content builders to be used inside a parent LazyColumn
fun LazyListScope.OverviewTabContent(
    profile: UserProfile,
    overview: SpeakingOverview?,
    onViewAchievements: () -> Unit
) {
    // Spacing matches original LazyColumn(contentPadding = 16.dp, spacedBy = 16.dp)
    if (profile.badges.isNotEmpty()) {
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Badges",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onViewAchievements) {
                            Text("View All")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(profile.badges.take(5)) { badge ->
                            BadgeItem(badge)
                        }
                    }
                }
            }
        }
    }

    item {
        Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                PerformanceMetric(
                    label = "Conversation",
                    value = profile.speakingScore,
                    color = MaterialTheme.colorScheme.primary
                )
                PerformanceMetric(
                    label = "Pronunciation",
                    value = profile.pronunciationScore,
                    color = MaterialTheme.colorScheme.secondary
                )
                PerformanceMetric(
                    label = "Fluency",
                    value = profile.fluencyScore,
                    color = MaterialTheme.colorScheme.tertiary
                )
                PerformanceMetric(
                    label = "Vocabulary",
                    value = profile.vocabularyScore,
                    color = MaterialTheme.colorScheme.primary
                )
                PerformanceMetric(
                    label = "Listening",
                    value = profile.listeningScore,
                    color = MaterialTheme.colorScheme.secondary
                )
                PerformanceMetric(
                    label = "Grammar",
                    value = profile.grammarScore,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }

    item {
        Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Learning Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProgressStat(
                        icon = Icons.Default.School,
                        value = profile.stats.completedLessons.toString(),
                        label = "Lessons"
                    )
                    ProgressStat(
                        icon = Icons.Default.Timer,
                        value = profile.stats.totalPracticeSessions.toString(),
                        label = "Practice"
                    )
                    ProgressStat(
                        icon = Icons.Default.Abc,
                        value = profile.stats.totalWords.toString(),
                        label = "Words"
                    )
                }
            }
        }
    }

    item {
        Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Member Since",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile.joinedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (profile.longestStreak > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Longest Streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${'$'}{profile.longestStreak} days",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF6B35)
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.ActivityTabContent(activities: List<UserActivity>) {
    if (activities.isEmpty()) {
        item {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                EmptyStateMessage(
                    icon = Icons.Default.Timeline,
                    message = "No recent activity"
                )
            }
        }
    } else {
        // top padding
        item { Spacer(modifier = Modifier.height(16.dp)) }
        items(activities) { activity ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ActivityCard(activity)
            }
        }
        // bottom padding
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

fun LazyListScope.AchievementsTabContent(
    badges: List<UserBadge>,
    proficiency: String,
    level: Int,
    onViewAll: () -> Unit
) {
    // Summary card
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Proficiency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = proficiency.ifBlank { "—" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Level ${'$'}level",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (badges.isEmpty()) {
        item {
            EmptyStateMessage(
                icon = Icons.Default.EmojiEvents,
                message = "No achievements yet"
            )
        }
    } else {
        // Manual 3-column grid to avoid nested lazy grids
        val rows = badges.chunked(3)
        items(rows) { rowBadges ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBadges.forEach { badge ->
                    Box(modifier = Modifier.weight(1f)) {
                        BadgeItem(badge)
                    }
                }
                // Fill remaining spaces to keep 3 columns layout
                repeat(3 - rowBadges.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (badges.size >= 9) {
            item {
                Button(
                    onClick = onViewAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("View All Achievements")
                }
            }
        }
    }
}

fun LazyListScope.StatisticsTabContent(stats: UserStats) {
    // Performance Stats
    item {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Performance Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatRow("Total Sessions", stats.totalPracticeSessions.toString())
                StatRow("Practice Time", "${'$'}{stats.totalPracticeMinutes / 60}h ${'$'}{stats.totalPracticeMinutes % 60}m")
                StatRow("Words Learned", stats.totalWords.toString())
                StatRow("Lessons Completed", stats.completedLessons.toString())
            }
        }
    }

    // XP Stats
    item {
        Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Experience Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatRow("Daily XP", stats.dailyXp.toString())
                StatRow("Weekly XP", stats.weeklyXp.toString())
                StatRow("Monthly XP", stats.monthlyXp.toString())
                stats.globalRank?.let {
                    StatRow("Global Rank", "#${'$'}it")
                }
            }
        }
    }

    // Social Stats
    item {
        Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Social",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatRow("Followers", stats.followersCount.toString())
                StatRow("Following", stats.followingCount.toString())
            }
        }
    }
}
@Composable
fun OverviewTab(
    profile: UserProfile,
    overview: SpeakingOverview?,
    onViewAchievements: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recent Badges
        if (profile.badges.isNotEmpty()) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Badges",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onViewAchievements) {
                                Text("View All")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(profile.badges.take(5)) { badge ->
                                BadgeItem(badge)
                            }
                        }
                    }
                }
            }
        }

        // Performance (per-skill scores)
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Conversation (Speaking)
                    PerformanceMetric(
                        label = "Conversation",
                        value = profile.speakingScore,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Pronunciation
                    PerformanceMetric(
                        label = "Pronunciation",
                        value = profile.pronunciationScore,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    // Fluency
                    PerformanceMetric(
                        label = "Fluency",
                        value = profile.fluencyScore,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    // Vocabulary
                    PerformanceMetric(
                        label = "Vocabulary",
                        value = profile.vocabularyScore,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Listening
                    PerformanceMetric(
                        label = "Listening",
                        value = profile.listeningScore,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    // Grammar (not implemented yet)
                    PerformanceMetric(
                        label = "Grammar",
                        value = profile.grammarScore,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Learning Progress
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Learning Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProgressStat(
                            icon = Icons.Default.School,
                            value = profile.stats.completedLessons.toString(),
                            label = "Lessons"
                        )
                        ProgressStat(
                            icon = Icons.Default.Timer,
                            value = profile.stats.totalPracticeSessions.toString(),
                            label = "Practice"
                        )
                        ProgressStat(
                            icon = Icons.Default.Abc,
                            value = profile.stats.totalWords.toString(),
                            label = "Words"
                        )
                    }
                }
            }
        }

        // Member Since
        item {
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Member Since",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            profile.joinedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (profile.longestStreak > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Longest Streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${profile.longestStreak} days",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF6B35)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityTab(activities: List<UserActivity>) {
    if (activities.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.Timeline,
            message = "No recent activity"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activities) { activity ->
                ActivityCard(activity)
            }
        }
    }
}

@Composable
fun ActivityCard(activity: UserActivity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Activity Icon
            Surface(
                shape = CircleShape,
                color = getActivityColor(activity.type).copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = getActivityIcon(activity.type),
                    contentDescription = activity.type.name,
                    tint = getActivityColor(activity.type),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                )
            }

            // Activity Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    activity.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    relativeTime(activity.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // XP Earned
            activity.xpEarned?.let { xp ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "+$xp XP",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementsTab(
    badges: List<UserBadge>,
    proficiency: String,
    level: Int,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Proficiency & Level summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Proficiency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = proficiency.ifBlank { "—" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Level $level",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (badges.isEmpty()) {
            EmptyStateMessage(
                icon = Icons.Default.EmojiEvents,
                message = "No achievements yet"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(badges) { badge ->
                    BadgeItem(badge)
                }
            }

            if (badges.size >= 9) {
                Button(
                    onClick = onViewAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("View All Achievements")
                }
            }
        }
    }
}

@Composable
fun StatisticsTab(stats: UserStats) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Stats
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Performance Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatRow("Total Sessions", stats.totalPracticeSessions.toString())
                    StatRow("Practice Time", "${stats.totalPracticeMinutes / 60}h ${stats.totalPracticeMinutes % 60}m")
                    StatRow("Words Learned", stats.totalWords.toString())
                    StatRow("Lessons Completed", stats.completedLessons.toString())
                }
            }
        }

        // XP Stats
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Experience Points",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatRow("Daily XP", stats.dailyXp.toString())
                    StatRow("Weekly XP", stats.weeklyXp.toString())
                    StatRow("Monthly XP", stats.monthlyXp.toString())
                    stats.globalRank?.let {
                        StatRow("Global Rank", "#$it")
                    }
                }
            }
        }

        // Social Stats
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Social",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatRow("Followers", stats.followersCount.toString())
                    StatRow("Following", stats.followingCount.toString())
                }
            }
        }
    }
}

// Helper Composables
@Composable
fun BadgeItem(badge: UserBadge) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color(badge.color.toLongOrNull(radix = 16) ?: 0xFF6200EE),
            modifier = Modifier.size(60.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    badge.icon,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            badge.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PerformanceMetric(
    label: String,
    value: Float,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        Box(
            modifier = Modifier.weight(1.5f)
        ) {
            LinearProgressIndicator(
                progress = { value / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }

        Text(
            "${value.toInt()}%",
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProgressStat(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyStateMessage(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = message,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun relativeTime(ts: LocalDateTime): String {
    return try {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val duration = Duration.between(ts, now)
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

// Helper functions for activity colors and icons
fun getActivityColor(type: ActivityType): Color {
    return when (type) {
        ActivityType.LESSON_COMPLETED -> Color(0xFF4CAF50)
        ActivityType.ACHIEVEMENT_UNLOCKED -> Color(0xFFFFD700)
        ActivityType.LEVEL_UP -> Color(0xFF9C27B0)
        ActivityType.STREAK_MILESTONE -> Color(0xFFFF6B35)
        ActivityType.PRACTICE_SESSION -> Color(0xFF2196F3)
        ActivityType.CHALLENGE_COMPLETED -> Color(0xFFE91E63)
        ActivityType.FRIEND_ADDED -> Color(0xFF00BCD4)
        ActivityType.BADGE_EARNED -> Color(0xFFFF9800)
    }
}

fun getActivityIcon(type: ActivityType): ImageVector {
    return when (type) {
        ActivityType.LESSON_COMPLETED -> Icons.Default.School
        ActivityType.ACHIEVEMENT_UNLOCKED -> Icons.Default.EmojiEvents
        ActivityType.LEVEL_UP -> Icons.Default.TrendingUp
        ActivityType.STREAK_MILESTONE -> Icons.Default.LocalFireDepartment
        ActivityType.PRACTICE_SESSION -> Icons.Default.Mic
        ActivityType.CHALLENGE_COMPLETED -> Icons.Default.SportsEsports
        ActivityType.FRIEND_ADDED -> Icons.Default.PersonAdd
        ActivityType.BADGE_EARNED -> Icons.Default.Stars
    }
}
