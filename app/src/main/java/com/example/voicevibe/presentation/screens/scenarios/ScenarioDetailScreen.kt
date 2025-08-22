package com.example.voicevibe.presentation.screens.scenarios

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class ScenarioStep(
    val id: String,
    val type: StepType,
    val speaker: String,
    val content: String,
    val expectedResponse: String? = null,
    val hints: List<String> = emptyList(),
    val culturalNote: String? = null
)

enum class StepType {
    CONTEXT, DIALOGUE, USER_RESPONSE, CULTURAL_TIP, COMPLETION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioDetailScreen(
    scenarioId: String,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    var userResponse by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var showHints by remember { mutableStateOf(false) }
    var xpEarned by remember { mutableStateOf(0) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val scenarioSteps = remember { generateMockScenarioSteps(scenarioId) }
    val currentStep = scenarioSteps.getOrNull(currentStepIndex)

    LaunchedEffect(currentStep) {
        if (currentStep?.type == StepType.COMPLETION) {
            delay(500)
            showCompletionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Job Interview Practice",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    LinearProgressIndicator(
                        progress = (currentStepIndex + 1).toFloat() / scenarioSteps.size,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        currentStep?.let { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                slideInHorizontally { it } with slideOutHorizontally { -it }
                            }
                        ) { animatedStep ->
                            when (animatedStep.type) {
                                StepType.CONTEXT -> ContextCard(animatedStep)
                                StepType.DIALOGUE -> DialogueCard(animatedStep)
                                StepType.USER_RESPONSE -> UserResponseCard(
                                    step = animatedStep,
                                    userResponse = userResponse,
                                    onResponseChange = { userResponse = it },
                                    isRecording = isRecording,
                                    onToggleRecording = { isRecording = !isRecording },
                                    showHints = showHints,
                                    onToggleHints = { showHints = !showHints }
                                )
                                StepType.CULTURAL_TIP -> CulturalTipCard(animatedStep)
                                StepType.COMPLETION -> CompletionCard(xpEarned)
                            }
                        }
                    }

                    if (step.type != StepType.COMPLETION) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentStepIndex > 0) {
                                    OutlinedButton(
                                        onClick = {
                                            currentStepIndex--
                                            showHints = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Previous")
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (step.type == StepType.USER_RESPONSE && userResponse.isNotBlank()) {
                                            xpEarned += 50
                                            userResponse = ""
                                        }
                                        currentStepIndex++
                                        showHints = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = step.type != StepType.USER_RESPONSE || userResponse.isNotBlank()
                                ) {
                                    Text(if (currentStepIndex < scenarioSteps.size - 1) "Next" else "Complete")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = onComplete) {
                    Text("Continue")
                }
            },
            icon = {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Scenario Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Great job practicing this scenario!",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "You earned $xpEarned XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@Composable
private fun ContextCard(step: ScenarioStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Scenario Context",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = step.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun DialogueCard(step: ScenarioStep) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.speaker.first().toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = step.speaker,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = step.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun UserResponseCard(
    step: ScenarioStep,
    userResponse: String,
    onResponseChange: (String) -> Unit,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    showHints: Boolean,
    onToggleHints: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Your Response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = step.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = userResponse,
                onValueChange = onResponseChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type or speak your response...") },
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = onToggleRecording,
                    modifier = Modifier.weight(1f),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                    )
                }

                OutlinedButton(
                    onClick = onToggleHints,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hints")
                }
            }

            AnimatedVisibility(visible = showHints && step.hints.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Helpful phrases:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        step.hints.forEach { hint ->
                            Text(
                                text = "• $hint",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CulturalTipCard(step: ScenarioStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.TipsAndUpdates,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Cultural Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = step.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CompletionCard(xpEarned: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Excellent Work!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "You've successfully completed this cultural scenario practice.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun generateMockScenarioSteps(scenarioId: String) = listOf(
    ScenarioStep(
        id = "1",
        type = StepType.CONTEXT,
        speaker = "System",
        content = "You're at a job interview for a software developer position at a tech company. The interviewer has just welcomed you into their office."
    ),
    ScenarioStep(
        id = "2",
        type = StepType.DIALOGUE,
        speaker = "Interviewer",
        content = "Good morning! Please have a seat. How are you doing today?"
    ),
    ScenarioStep(
        id = "3",
        type = StepType.USER_RESPONSE,
        speaker = "You",
        content = "Respond to the interviewer's greeting professionally",
        hints = listOf(
            "Good morning! I'm doing very well, thank you.",
            "Thank you for taking the time to meet with me today.",
            "I'm excited about this opportunity."
        )
    ),
    ScenarioStep(
        id = "4",
        type = StepType.CULTURAL_TIP,
        speaker = "System",
        content = "In professional American settings, maintain eye contact and offer a firm handshake. Keep your initial response brief but enthusiastic."
    ),
    ScenarioStep(
        id = "5",
        type = StepType.DIALOGUE,
        speaker = "Interviewer",
        content = "Could you tell me a bit about yourself and why you're interested in this position?"
    ),
    ScenarioStep(
        id = "6",
        type = StepType.USER_RESPONSE,
        speaker = "You",
        content = "Introduce yourself and explain your interest in the position",
        hints = listOf(
            "I have X years of experience in...",
            "I'm particularly interested in this role because...",
            "My background in... makes me a good fit for..."
        )
    ),
    ScenarioStep(
        id = "7",
        type = StepType.COMPLETION,
        speaker = "System",
        content = "Scenario completed successfully!"
    )
)
