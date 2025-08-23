package com.example.voicevibe.presentation.screens.learning

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import coil.compose.AsyncImage
import com.example.voicevibe.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathCard(
    path: LearningPath,
    onNavigateToPath: () -> Unit,
    onEnroll: () -> Unit,
    isEnrolling: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onNavigateToPath,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail with badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (path.thumbnailUrl != null) {
                    AsyncImage(
                        model = path.thumbnailUrl,
                        contentDescription = path.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Difficulty Badge
                    AssistChip(
                        onClick = {},
                        label = { Text(path.difficulty.name.lowercase().capitalize()) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = getDifficultyColor(path.difficulty).copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    // Premium/Certificate badges
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (path.isPremium) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Premium",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFFFD700).copy(alpha = 0.9f)
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        if (path.certificateAvailable) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Icon(
                                        Icons.Default.CardGiftcard,
                                        contentDescription = "Certificate",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                // Progress bar if enrolled
                if (path.isEnrolled) {
                    LinearProgressIndicator(
                        progress = path.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title and Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = path.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = getCategoryDisplayName(path.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (path.isRecommended) {
                        Icon(
                            Icons.Default.Recommend,
                            contentDescription = "Recommended",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Description
                Text(
                    text = path.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${path.duration}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Lessons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${path.totalLessons} lessons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Rating
                    if (path.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFB800)
                            )
                            Text(
                                text = String.format("%.1f", path.rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Instructor
                path.instructor?.let { instructor ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AsyncImage(
                            model = instructor.avatarUrl,
                            contentDescription = instructor.name,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = instructor.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Button
                Button(
                    onClick = onEnroll,
                    enabled = !isEnrolling,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (path.isEnrolled) {
                        ButtonDefaults.outlinedButtonColors()
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    if (isEnrolling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (path.isEnrolled) {
                                "Continue Learning (${(path.progress * 100).toInt()}%)"
                            } else {
                                "Enroll Now"
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendedPathCard(
    recommendation: PathRecommendation,
    onNavigateToPath: (String) -> Unit,
    onEnroll: () -> Unit,
    isEnrolling: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onNavigateToPath(recommendation.path.id) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Recommendation reason
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Path details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (recommendation.path.thumbnailUrl != null) {
                        AsyncImage(
                            model = recommendation.path.thumbnailUrl,
                            contentDescription = recommendation.path.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = recommendation.path.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = recommendation.path.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Match score
                        Text(
                            text = "${(recommendation.matchScore * 100).toInt()}% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        // Difficulty
                        Text(
                            text = recommendation.path.difficulty.name.lowercase().capitalize(),
                            style = MaterialTheme.typography.labelSmall,
                            color = getDifficultyColor(recommendation.path.difficulty)
                        )

                        // Duration
                        Text(
                            text = "${recommendation.path.duration}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Enroll button
            FilledTonalButton(
                onClick = onEnroll,
                enabled = !isEnrolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (isEnrolling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Start Learning")
                }
            }
        }
    }
}

@Composable
fun EmptyPathsState(
    searchQuery: String,
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) {
                "No paths found for \"$searchQuery\""
            } else if (hasFilters) {
                "No paths match your filters"
            } else {
                "No learning paths available"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasFilters) {
                "Try adjusting your filters to see more results"
            } else {
                "Check back later for new learning opportunities"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (hasFilters) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

// Helper Functions
fun getCategoryDisplayName(category: PathCategory): String {
    return when (category) {
        PathCategory.BEGINNER_BASICS -> "Beginner Basics"
        PathCategory.PRONUNCIATION -> "Pronunciation"
        PathCategory.GRAMMAR -> "Grammar"
        PathCategory.VOCABULARY -> "Vocabulary"
        PathCategory.CONVERSATION -> "Conversation"
        PathCategory.BUSINESS_ENGLISH -> "Business English"
        PathCategory.ACADEMIC_ENGLISH -> "Academic English"
        PathCategory.CULTURAL_FLUENCY -> "Cultural Fluency"
        PathCategory.ACCENT_TRAINING -> "Accent Training"
        PathCategory.PUBLIC_SPEAKING -> "Public Speaking"
        PathCategory.INTERVIEW_PREP -> "Interview Prep"
        PathCategory.TOEFL_PREP -> "TOEFL Prep"
        PathCategory.IELTS_PREP -> "IELTS Prep"
    }
}

fun getSortOptionDisplayName(option: PathSortOption): String {
    return when (option) {
        PathSortOption.RECOMMENDED -> "Recommended"
        PathSortOption.POPULARITY -> "Most Popular"
        PathSortOption.RATING -> "Highest Rated"
        PathSortOption.NEWEST -> "Newest"
        PathSortOption.DURATION_SHORT -> "Shortest"
        PathSortOption.DURATION_LONG -> "Longest"
        PathSortOption.DIFFICULTY_LOW -> "Easiest"
        PathSortOption.DIFFICULTY_HIGH -> "Hardest"
    }
}

fun getDifficultyColor(difficulty: DifficultyLevel): Color {
    return when (difficulty) {
        DifficultyLevel.BEGINNER -> Color(0xFF4CAF50)
        DifficultyLevel.INTERMEDIATE -> Color(0xFFFFC107)
        DifficultyLevel.UPPER_INTERMEDIATE -> Color(0xFFFF9800)
        DifficultyLevel.ADVANCED -> Color(0xFFFF5722)
        DifficultyLevel.EXPERT -> Color(0xFFE91E63)
    }
}
