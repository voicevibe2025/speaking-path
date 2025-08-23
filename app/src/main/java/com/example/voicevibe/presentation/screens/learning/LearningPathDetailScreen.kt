package com.example.voicevibe.presentation.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.domain.model.LearningModule
import com.example.voicevibe.domain.model.Lesson
import com.example.voicevibe.domain.model.PathInstructor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathDetailScreen(
    pathId: String,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (moduleId: String, lessonId: String) -> Unit,
    viewModel: LearningPathDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Load path details when the screen is first composed
    LaunchedEffect(pathId) {
        viewModel.loadPathDetails(pathId)
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError()
            }
        }
    }
    
    // Show success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSuccessMessage()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.learningPath?.title ?: "Learning Path",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleBookmark() }
                    ) {
                        Icon(
                            imageVector = if (uiState.isBookmarked) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkBorder
                            },
                            contentDescription = if (uiState.isBookmarked) {
                                "Remove from bookmarks"
                            } else {
                                "Add to bookmarks"
                            }
                        )
                    }
                    IconButton(
                        onClick = { viewModel.sharePathDetails() }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.learningPath != null -> {
                LearningPathContent(
                    learningPath = uiState.learningPath!!,
                    reviews = uiState.reviews,
                    isEnrollmentInProgress = uiState.enrollmentInProgress,
                    onEnrollClick = {
                        if (uiState.learningPath!!.isEnrolled) {
                            viewModel.unenrollFromPath()
                        } else {
                            viewModel.enrollInPath()
                        }
                    },
                    onContinueLearning = {
                        // Navigate to the next lesson
                        uiState.learningPath!!.nextLesson?.let { nextLesson ->
                            val moduleId = uiState.learningPath!!.modules
                                .firstOrNull { module ->
                                    module.lessons.any { it.id == nextLesson.lessonId }
                                }?.id ?: ""
                            onNavigateToLesson(moduleId, nextLesson.lessonId)
                        }
                    },
                    onLessonClick = { moduleId, lessonId ->
                        onNavigateToLesson(moduleId, lessonId)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Failed to load learning path",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = { viewModel.loadPathDetails(pathId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningPathContent(
    learningPath: LearningPath,
    reviews: List<PathReview>,
    isEnrollmentInProgress: Boolean,
    onEnrollClick: () -> Unit,
    onContinueLearning: () -> Unit,
    onLessonClick: (moduleId: String, lessonId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Path Header Card
        PathHeaderCard(
            learningPath = learningPath,
            onEnrollClick = onEnrollClick,
            onContinueClick = onContinueLearning
        )

        // Tab Row
        TabRow(
            selectedTabIndex = 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = true,
                onClick = { },
                text = { Text("Overview") }
            )
            Tab(
                selected = false,
                onClick = { },
                text = { Text("Modules") },
                enabled = learningPath.isEnrolled
            )
            Tab(
                selected = false,
                onClick = { },
                text = { Text("Reviews") }
            )
        }

        // Tab Content
        when {
            learningPath.isEnrolled -> {
                ModulesTab(
                    modules = learningPath.modules,
                    onLessonClick = onLessonClick
                )
            }
            else -> {
                OverviewTab(path = learningPath)
            }
        }
    }
}

@Composable
private fun PathHeaderCard(
    learningPath: LearningPath,
    onEnrollClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = learningPath.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(getCategoryDisplayName(learningPath.category)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        AssistChip(
                            onClick = { },
                            label = { Text(learningPath.difficulty.name.replace("_", " ")) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = getDifficultyColor(learningPath.difficulty).copy(alpha = 0.2f)
                            )
                        )

                        if (learningPath.isPremium) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Premium") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFD700)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }

                learningPath.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Timer,
                    value = "${learningPath.duration}h",
                    label = "Duration"
                )
                StatItem(
                    icon = Icons.Default.Book,
                    value = "${learningPath.totalLessons}",
                    label = "Lessons"
                )
                StatItem(
                    icon = Icons.Default.People,
                    value = "${learningPath.enrolledCount}",
                    label = "Students"
                )
                StatItem(
                    icon = Icons.Default.Star,
                    value = "${learningPath.rating}",
                    label = "Rating"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar (if enrolled)
            if (learningPath.isEnrolled) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${(learningPath.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = learningPath.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${learningPath.completedLessons} of ${learningPath.totalLessons} lessons completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (learningPath.isEnrolled) {
                    Button(
                        onClick = onContinueClick,
                        modifier = Modifier.weight(1f),
                        enabled = learningPath.nextLesson != null
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue Learning")
                    }

                    OutlinedButton(
                        onClick = onEnrollClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unenroll")
                    }
                } else {
                    Button(
                        onClick = onEnrollClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enroll Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
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
private fun ModulesTab(
    modules: List<LearningModule>,
    onLessonClick: (moduleId: String, lessonId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(modules) { module ->
            ModuleCard(
                module = module,
                onLessonClick = onLessonClick
            )
        }
    }
}

@Composable
private fun ModuleCard(
    module: LearningModule,
    onLessonClick: (moduleId: String, lessonId: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = module.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(module.lessons) { lesson ->
                    LessonItem(
                        lesson = lesson,
                        onLessonClick = { onLessonClick(module.id, lesson.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonItem(
    lesson: Lesson,
    onLessonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLessonClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = lesson.title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
