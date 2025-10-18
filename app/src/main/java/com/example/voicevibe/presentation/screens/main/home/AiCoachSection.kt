package com.example.voicevibe.presentation.screens.main.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.data.remote.api.CoachAnalysisDto
import com.example.voicevibe.data.remote.api.CoachScheduleItemDto
import com.example.voicevibe.data.remote.api.CoachSkillDto
import com.example.voicevibe.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.ceil

@Composable
fun AiCoachSection(
    analysis: CoachAnalysisDto?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var hoursUntilRefresh by remember { mutableStateOf<Int?>(null) }

    // Calculate hours until next refresh
    LaunchedEffect(analysis?._next_refresh_at) {
        analysis?._next_refresh_at?.let { nextRefreshAt ->
            try {
                android.util.Log.d("AiCoachSection", "Next refresh at: $nextRefreshAt")
                
                // Parse ISO timestamp (handles formats like: 2025-10-18T01:59:00.123456 or 2025-10-18T01:59:00Z)
                val cleanedTimestamp = nextRefreshAt
                    .substringBefore("+")  // Remove timezone offset if present
                    .replace("Z", "")       // Remove Z indicator
                    .substringBefore(".")   // Remove milliseconds
                
                val nextRefresh = LocalDateTime.parse(cleanedTimestamp)
                val now = LocalDateTime.now()
                val minutesUntil = ChronoUnit.MINUTES.between(now, nextRefresh)
                hoursUntilRefresh = ceil(minutesUntil / 60.0).toInt().coerceAtLeast(0)
                
                android.util.Log.d("AiCoachSection", "Hours until refresh: $hoursUntilRefresh")
            } catch (e: Exception) {
                android.util.Log.e("AiCoachSection", "Failed to parse next_refresh_at: $nextRefreshAt", e)
                hoursUntilRefresh = null
            }
        } ?: run {
            android.util.Log.w("AiCoachSection", "No _next_refresh_at in analysis data")
            hoursUntilRefresh = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    // Show countdown timer or subtitle
                    if (hoursUntilRefresh != null) {
                        Text(
                            text = "Refreshes in ${hoursUntilRefresh}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandCyan.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "GRU-Powered Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandCyan.copy(alpha = 0.8f)
                        )
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) "Hide Details" else "See Details",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterHorizontally),
                    color = BrandCyan
                )
            } else if (analysis != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // Coach Message
                Text(
                    text = analysis.coachMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 24.sp
                )

                if (expanded) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // All 5 Skills Grid
                    Text(
                        text = "Your Skills Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(360.dp)
                    ) {
                        items(analysis.skills) { skill ->
                            SkillCard(skill = skill)
                        }
                    }

                    // Strengths & Weaknesses
                    if (analysis.strengths.isNotEmpty() || analysis.weaknesses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (analysis.strengths.isNotEmpty()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "💪 Strengths",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Green.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    analysis.strengths.take(3).forEach { strength ->
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    strength.replaceFirstChar { it.titlecase(Locale.ROOT) },
                                                    fontSize = 13.sp
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = Color.Green.copy(alpha = 0.15f),
                                                labelColor = Color.White
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            
                            if (analysis.weaknesses.isNotEmpty()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🎯 Focus Areas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandFuchsia.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    analysis.weaknesses.take(3).forEach { weakness ->
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    weakness.replaceFirstChar { it.titlecase(Locale.ROOT) },
                                                    fontSize = 13.sp
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = BrandFuchsia.copy(alpha = 0.15f),
                                                labelColor = Color.White
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Practice Schedule
                    if (!analysis.schedule.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "📅 Your Practice Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            analysis.schedule.take(5).forEach { scheduleItem ->
                                ScheduleCard(item = scheduleItem)
                            }
                        }
                    }
                }

                // Next Best Actions
                Spacer(modifier = Modifier.height(16.dp))
                
                if (analysis.nextBestActions.isNotEmpty()) {
                    val primaryAction = analysis.nextBestActions.first()
                    Button(
                        onClick = { 
                            android.util.Log.d("AiCoachSection", "Button clicked! Deeplink: ${primaryAction.deeplink}")
                            onActionClick(primaryAction.deeplink) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandIndigo,
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = primaryAction.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            primaryAction.expectedGain?.let { gain ->
                                Text(
                                    text = "Expected gain: ${gain.uppercase()}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                    
                    // Show additional actions if expanded
                    if (expanded && analysis.nextBestActions.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        analysis.nextBestActions.drop(1).take(2).forEach { action ->
                            OutlinedButton(
                                onClick = { onActionClick(action.deeplink) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = action.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = action.rationale,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                action.expectedGain?.let {
                                    Badge(
                                        containerColor = when (it) {
                                            "large" -> Color.Green
                                            "medium" -> BrandCyan
                                            else -> Color.Gray
                                        }.copy(alpha = 0.3f)
                                    ) {
                                        Text(it, fontSize = 10.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unable to load AI Coach analysis. Pull to refresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SkillCard(skill: CoachSkillDto) {
    val trendIcon = when (skill.trend) {
        "up" -> Icons.Default.TrendingUp
        "down" -> Icons.Default.TrendingDown
        else -> Icons.Default.TrendingFlat
    }
    val trendColor = when (skill.trend) {
        "up" -> Color.Green
        "down" -> Color.Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Skill name and trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = trendIcon,
                    contentDescription = skill.trend,
                    tint = trendColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Mastery score
            Text(
                text = "${skill.mastery}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    skill.mastery >= 75 -> Color.Green
                    skill.mastery >= 50 -> BrandCyan
                    else -> BrandFuchsia
                }
            )

            // Progress bar
            LinearProgressIndicator(
                progress = skill.mastery / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    skill.mastery >= 75 -> Color.Green
                    skill.mastery >= 50 -> BrandCyan
                    else -> BrandFuchsia
                },
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            // Confidence
            skill.confidence?.let { conf ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Confidence: ${(conf * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(item: CoachScheduleItemDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandIndigo.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, BrandIndigo.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                val date = try {
                    LocalDate.parse(item.date)
                } catch (e: Exception) {
                    LocalDate.now()
                }
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM")),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = BrandCyan
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.focus.replaceFirstChar { it.titlecase(Locale.ROOT) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                item.microSkills?.let { skills ->
                    if (skills.isNotEmpty()) {
                        Text(
                            text = skills.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = BrandCyan.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                item.reason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
