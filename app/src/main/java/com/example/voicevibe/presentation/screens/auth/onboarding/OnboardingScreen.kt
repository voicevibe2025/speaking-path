package com.example.voicevibe.presentation.screens.auth.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.di.dataStore
import kotlinx.coroutines.launch

/**
 * Onboarding screen with feature showcase
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context.applicationContext) }
    
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.RecordVoiceOver,
            title = "AI-Powered Speaking Practice",
            description = "Practice English speaking with advanced AI that provides real-time feedback on pronunciation, fluency, and grammar",
            backgroundColor = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            icon = Icons.Filled.Psychology,
            title = "Personalized Learning Path",
            description = "Adaptive learning system that adjusts to your skill level and learning pace, ensuring optimal progress",
            backgroundColor = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            icon = Icons.Filled.EmojiEvents,
            title = "Cultural Gamification",
            description = "Engage with Indonesian cultural elements like Wayang characters and Batik patterns while earning achievements",
            backgroundColor = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            icon = Icons.Filled.Groups,
            title = "Gotong Royong Community",
            description = "Learn together with peers through collaborative challenges and mentor-student relationships",
            backgroundColor = Color(0xFF4CAF50)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val levelOptions = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")
    var selectedLevel by remember { mutableStateOf("INTERMEDIATE") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            tokenManager.setOnboardingCompleted()
                            onComplete()
                        }
                    }
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom section with indicators and button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // English Level selection on the last page
                if (pagerState.currentPage == pages.size - 1) {
                    Text(
                        text = "Choose your English level",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    levelOptions.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedLevel = level },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLevel == level,
                                onClick = { selectedLevel = level }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (level) {
                                    "BEGINNER" -> "Beginner"
                                    "INTERMEDIATE" -> "Intermediate"
                                    "ADVANCED" -> "Advanced"
                                    else -> level
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            label = "indicator_width"
                        )

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Next/Get Started button
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage == pages.size - 1) {
                                // Persist English Level and finish
                                runCatching { tokenManager.setEnglishLevel(selectedLevel) }
                                tokenManager.setOnboardingCompleted()
                                onComplete()
                            } else {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1)
                            "Get Started"
                        else
                            "Next",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with gradient background
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            page.backgroundColor.copy(alpha = 0.3f),
                            page.backgroundColor.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = page.backgroundColor
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val backgroundColor: Color
)
