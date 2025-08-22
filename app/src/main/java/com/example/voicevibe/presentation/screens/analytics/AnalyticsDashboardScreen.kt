package com.example.voicevibe.presentation.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

data class StatCard(
    val title: String,
    val value: String,
    val change: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val isPositive: Boolean = true
)

data class WeeklyProgress(
    val day: String,
    val minutes: Int,
    val xp: Int
)

data class SkillProgress(
    val name: String,
    val level: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTimeRange by remember { mutableStateOf("Week") }
    val timeRanges = listOf("Day", "Week", "Month", "Year")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics Dashboard",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share analytics */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // Time Range Filter
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timeRanges) { range ->
                        FilterChip(
                            selected = selectedTimeRange == range,
                            onClick = { selectedTimeRange = range },
                            label = { Text(range) }
                        )
                    }
                }
            }

            item {
                // Key Stats Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCardItem(
                        stat = StatCard(
                            title = "Study Streak",
                            value = "12",
                            change = "days",
                            icon = Icons.Default.LocalFireDepartment,
                            color = Color(0xFFFF6B6B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    StatCardItem(
                        stat = StatCard(
                            title = "Total XP",
                            value = "2,450",
                            change = "+350",
                            icon = Icons.Default.Star,
                            color = Color(0xFFFFB300)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCardItem(
                        stat = StatCard(
                            title = "Practice Time",
                            value = "4.5",
                            change = "hours",
                            icon = Icons.Default.Timer,
                            color = Color(0xFF4ECDC4)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    StatCardItem(
                        stat = StatCard(
                            title = "Accuracy",
                            value = "78%",
                            change = "+5%",
                            icon = Icons.Default.Speed,
                            color = Color(0xFF667EEA)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Weekly Activity Chart
                WeeklyActivityChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            item {
                // Skills Radar Chart
                SkillsRadarChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            item {
                // Achievement Progress
                AchievementProgressSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            item {
                // Learning Insights
                LearningInsightsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCardItem(
    stat: StatCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = stat.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    stat.icon,
                    contentDescription = null,
                    tint = stat.color,
                    modifier = Modifier.size(24.dp)
                )
                if (stat.change.startsWith("+") || stat.change.startsWith("-")) {
                    Text(
                        text = stat.change,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stat.isPositive) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = stat.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = stat.color
            )
            Text(
                text = stat.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityChart(modifier: Modifier = Modifier) {
    val weeklyData = listOf(
        WeeklyProgress("Mon", 45, 120),
        WeeklyProgress("Tue", 60, 150),
        WeeklyProgress("Wed", 30, 80),
        WeeklyProgress("Thu", 75, 200),
        WeeklyProgress("Fri", 50, 130),
        WeeklyProgress("Sat", 40, 100),
        WeeklyProgress("Sun", 65, 180)
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val barWidth = size.width / (weeklyData.size * 2)
                val maxMinutes = weeklyData.maxOf { it.minutes }

                weeklyData.forEachIndexed { index, data ->
                    val barHeight = (data.minutes.toFloat() / maxMinutes) * size.height * 0.8f
                    val xPosition = barWidth * (index * 2 + 0.5f)

                    // Draw bar
                    drawRoundRect(
                        color = Color(0xFF667EEA),
                        topLeft = Offset(xPosition, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    // Draw day label
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = Color.Gray.toArgb()
                            textSize = 12.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            data.day,
                            xPosition + barWidth / 2,
                            size.height - 4.dp.toPx(),
                            paint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weeklyData.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${data.minutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillsRadarChart(modifier: Modifier = Modifier) {
    val skills = listOf(
        SkillProgress("Speaking", 0.75f, Color(0xFFFF6B6B)),
        SkillProgress("Listening", 0.85f, Color(0xFF4ECDC4)),
        SkillProgress("Grammar", 0.60f, Color(0xFFFFB300)),
        SkillProgress("Vocabulary", 0.70f, Color(0xFF667EEA)),
        SkillProgress("Pronunciation", 0.80f, Color(0xFF95E77E))
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Skills Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = minOf(centerX, centerY) * 0.8f
                val angleStep = 360f / skills.size

                // Draw grid circles
                for (i in 1..5) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = radius * (i / 5f),
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw axes
                skills.forEachIndexed { index, _ ->
                    val angle = Math.toRadians((angleStep * index - 90).toDouble())
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(centerX, centerY),
                        end = Offset(
                            (centerX + radius * cos(angle)).toFloat(),
                            (centerY + radius * sin(angle)).toFloat()
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw skill polygon
                val path = Path()
                skills.forEachIndexed { index, skill ->
                    val angle = Math.toRadians((angleStep * index - 90).toDouble())
                    val x = (centerX + radius * skill.level * cos(angle)).toFloat()
                    val y = (centerY + radius * skill.level * sin(angle)).toFloat()

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()

                drawPath(
                    path = path,
                    color = Color(0xFF667EEA).copy(alpha = 0.3f)
                )
                drawPath(
                    path = path,
                    color = Color(0xFF667EEA),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { skill ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(skill.color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = skill.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Achievement Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { /* Navigate to achievements */ }) {
                    Text("View All")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Achievement items
            AchievementItem(
                title = "Perfect Week",
                description = "Practice 7 days in a row",
                progress = 0.85f,
                icon = Icons.Default.EmojiEvents
            )
            Spacer(modifier = Modifier.height(8.dp))
            AchievementItem(
                title = "Vocabulary Master",
                description = "Learn 500 new words",
                progress = 0.62f,
                icon = Icons.Default.MenuBook
            )
            Spacer(modifier = Modifier.height(8.dp))
            AchievementItem(
                title = "Speaking Champion",
                description = "Complete 50 speaking exercises",
                progress = 0.44f,
                icon = Icons.Default.Mic
            )
        }
    }
}

@Composable
private fun AchievementItem(
    title: String,
    description: String,
    progress: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LearningInsightsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Learning Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            InsightItem(
                icon = Icons.Default.TrendingUp,
                text = "You're 23% more active than last week",
                isPositive = true
            )
            InsightItem(
                icon = Icons.Default.Schedule,
                text = "Your best learning time is 7-9 PM",
                isPositive = true
            )
            InsightItem(
                icon = Icons.Default.Warning,
                text = "Grammar needs more practice",
                isPositive = false
            )
        }
    }
}

@Composable
private fun InsightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isPositive: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
