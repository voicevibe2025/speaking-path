package com.example.voicevibe.presentation.screens.scenarios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CulturalScenario(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val estimatedTime: Int,
    val xpReward: Int,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulturalScenariosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScenario: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Business", "Travel", "Social", "Academic", "Daily Life")

    val scenarios = remember { generateMockScenarios() }
    val filteredScenarios = if (selectedCategory == "All") {
        scenarios
    } else {
        scenarios.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cultural Scenarios",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                // Category Filter
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = if (selectedCategory == category) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            item {
                // Stats Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.CheckCircle,
                            value = "${scenarios.count { it.isCompleted }}",
                            label = "Completed"
                        )
                        StatItem(
                            icon = Icons.Default.Star,
                            value = "${scenarios.sumOf { if (it.isCompleted) it.xpReward else 0 }}",
                            label = "XP Earned"
                        )
                        StatItem(
                            icon = Icons.Default.TrendingUp,
                            value = "${scenarios.count { !it.isCompleted && !it.isLocked }}",
                            label = "Available"
                        )
                    }
                }
            }

            items(filteredScenarios) { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    onClick = {
                        if (!scenario.isLocked) {
                            onNavigateToScenario(scenario.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
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
private fun ScenarioCard(
    scenario: CulturalScenario,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(enabled = !scenario.isLocked) { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            // Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (scenario.isLocked) {
                                listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.1f))
                            } else {
                                scenario.gradientColors
                            }
                        )
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(scenario.category) },
                            modifier = Modifier.height(24.dp)
                        )
                        AssistChip(
                            onClick = { },
                            label = { Text(scenario.difficulty) },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = scenario.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "${scenario.estimatedTime} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "${scenario.xpReward} XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }

                Icon(
                    if (scenario.isLocked) Icons.Default.Lock
                    else if (scenario.isCompleted) Icons.Default.CheckCircle
                    else scenario.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(8.dp),
                    tint = Color.White
                )
            }
        }
    }
}

private fun generateMockScenarios() = listOf(
    CulturalScenario(
        id = "1",
        title = "Job Interview",
        description = "Practice professional English for interviews",
        category = "Business",
        difficulty = "Intermediate",
        estimatedTime = 20,
        xpReward = 150,
        icon = Icons.Default.BusinessCenter,
        gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
        isCompleted = true
    ),
    CulturalScenario(
        id = "2",
        title = "Airport Check-in",
        description = "Navigate airport procedures confidently",
        category = "Travel",
        difficulty = "Beginner",
        estimatedTime = 15,
        xpReward = 100,
        icon = Icons.Default.Flight,
        gradientColors = listOf(Color(0xFF00b4db), Color(0xFF0083b0))
    ),
    CulturalScenario(
        id = "3",
        title = "Restaurant Ordering",
        description = "Order food and interact with staff",
        category = "Daily Life",
        difficulty = "Beginner",
        estimatedTime = 10,
        xpReward = 80,
        icon = Icons.Default.Restaurant,
        gradientColors = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
    ),
    CulturalScenario(
        id = "4",
        title = "Business Meeting",
        description = "Lead and participate in meetings",
        category = "Business",
        difficulty = "Advanced",
        estimatedTime = 25,
        xpReward = 200,
        icon = Icons.Default.Groups,
        gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
        isLocked = true
    ),
    CulturalScenario(
        id = "5",
        title = "Making Friends",
        description = "Social interactions and small talk",
        category = "Social",
        difficulty = "Intermediate",
        estimatedTime = 15,
        xpReward = 120,
        icon = Icons.Default.People,
        gradientColors = listOf(Color(0xFFfa709a), Color(0xFFfee140))
    )
)
