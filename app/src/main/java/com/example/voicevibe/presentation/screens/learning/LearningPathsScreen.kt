package com.example.voicevibe.presentation.screens.learning

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.voicevibe.domain.model.*
import com.example.voicevibe.presentation.components.LoadingScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onNavigateToLesson: (String, String) -> Unit,
    viewModel: LearningPathsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showFilters by viewModel.showFilters.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            LearningPathsTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onNavigateBack = onNavigateBack,
                onToggleFilters = viewModel::toggleFilters
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Continue Learning Card
            uiState.continueLesson?.let { lesson ->
                ContinueLearningCard(
                    lesson = lesson,
                    streak = uiState.learningStreak,
                    onContinue = { onNavigateToLesson(lesson.moduleId, lesson.lessonId) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Category Chips
            CategoryChipsRow(
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::selectCategory,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Filter Bar
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterBar(
                    selectedDifficulty = selectedDifficulty,
                    sortOption = sortOption,
                    activeFilters = uiState.activeFilters,
                    onDifficultySelected = viewModel::selectDifficulty,
                    onSortOptionSelected = viewModel::updateSortOption,
                    onApplyFilters = viewModel::applyFilters,
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
                }
                uiState.filteredPaths.isEmpty() -> {
                    EmptyPathsState(
                        searchQuery = searchQuery,
                        hasFilters = selectedCategory != null || selectedDifficulty != null,
                        onClearFilters = viewModel::clearFilters,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Recommended Paths Section
                        if (uiState.recommendedPaths.isNotEmpty() && searchQuery.isBlank()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "Recommended for You",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(
                                items = uiState.recommendedPaths.take(2),
                                span = { GridItemSpan(1) }
                            ) { recommendation ->
                                RecommendedPathCard(
                                    recommendation = recommendation,
                                    onNavigateToPath = onNavigateToPath,
                                    onEnroll = { viewModel.enrollInPath(recommendation.path.id) },
                                    isEnrolling = uiState.enrollingPathId == recommendation.path.id
                                )
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "All Learning Paths",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // All Paths
                        items(
                            items = uiState.filteredPaths,
                            key = { it.id }
                        ) { path ->
                            LearningPathCard(
                                path = path,
                                onNavigateToPath = { onNavigateToPath(path.id) },
                                onEnroll = {
                                    if (path.isEnrolled) {
                                        viewModel.unenrollFromPath(path.id)
                                    } else {
                                        viewModel.enrollInPath(path.id)
                                    }
                                },
                                isEnrolling = uiState.enrollingPathId == path.id
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningPathsTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onToggleFilters: () -> Unit
) {
    TopAppBar(
        title = {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search learning paths...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {}
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onToggleFilters) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }
    )
}

@Composable
private fun ContinueLearningCard(
    lesson: LessonInfo,
    streak: LearningStreak?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onContinue,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue Learning",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = lesson.moduleTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                streak?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${it.currentStreak} day streak",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Continue")
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipsRow(
    selectedCategory: PathCategory?,
    onCategorySelected: (PathCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                leadingIcon = if (selectedCategory == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }

        items(PathCategory.values()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                label = { Text(getCategoryDisplayName(category)) },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    selectedDifficulty: DifficultyLevel?,
    sortOption: PathSortOption,
    activeFilters: PathFilters,
    onDifficultySelected: (DifficultyLevel?) -> Unit,
    onSortOptionSelected: (PathSortOption) -> Unit,
    onApplyFilters: (PathFilters) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Difficulty Filter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Difficulty:",
                    style = MaterialTheme.typography.labelLarge
                )
                DifficultyLevel.values().forEach { level ->
                    FilterChip(
                        selected = selectedDifficulty == level,
                        onClick = {
                            onDifficultySelected(if (selectedDifficulty == level) null else level)
                        },
                        label = { Text(level.name.lowercase().capitalize()) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Sort Options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sort by:",
                    style = MaterialTheme.typography.labelLarge
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = getSortOptionDisplayName(sortOption),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .width(200.dp)
                            .height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        PathSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(getSortOptionDisplayName(option)) },
                                onClick = {
                                    onSortOptionSelected(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Clear Filters Button
            if (selectedDifficulty != null || activeFilters != PathFilters()) {
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Filters")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterAndSortBar(
    selectedFilters: PathFilters,
    sortOption: PathSortOption,
    onFiltersChanged: (PathFilters) -> Unit,
    onSortChanged: (PathSortOption) -> Unit
) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SortDropdown() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = getSortOptionDisplayName(sortOption),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .width(200.dp)
                        .height(48.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PathSortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(getSortOptionDisplayName(option)) },
                            onClick = {
                                onSortChanged(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    SortDropdown()
}
