@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VocabularyPracticeScreen(
    topicId: String,
    onNavigateBack: () -> Unit
) {
    val sjVM: SpeakingJourneyViewModel = hiltViewModel()
    val sjUi = sjVM.uiState.value
    val topic = sjUi.topics.firstOrNull { it.id == topicId }

    val viewModel: VocabularyPracticeViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(topic?.id) {
        topic?.let { viewModel.start(it) }
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(20.dp))
                        Text("Vocabulary Practice", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(8.dp).size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(innerPadding)
        ) {
            if (topic == null || ui.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF90CAF9), strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Preparing questions with AI…",
                            color = Color(0xFFE0E0E0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(400))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Topic Title Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Question ${ui.questionIndex + 1} of ${ui.totalQuestions}", color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        // Clue card
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                Text("Mistery Hint", color = Color(0xFFFFD700), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(ui.definition, color = Color.White, fontSize = 20.sp, lineHeight = 28.sp)
                            }
                        }

                        // Options
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ui.options.forEach { option ->
                                OptionItem(
                                    text = option,
                                    enabled = !ui.isSubmitting && !ui.revealedAnswer,
                                    selected = ui.selectedOption == option,
                                    reveal = ui.revealedAnswer,
                                    answerCorrect = ui.answerCorrect,
                                ) { viewModel.selectOption(option) }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Score/Xp footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Score: ${ui.score}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("XP +${ui.lastAwardedXp} (Total ${ui.totalXp})", color = Color(0xFF81C784), fontSize = 14.sp)
                        }
                    }
                }
            }

            // Error toast-like dialog
            ui.error?.let { msg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    confirmButton = {
                        Button(onClick = { viewModel.clearError() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))) {
                            Text("OK")
                        }
                    },
                    title = { Text("Error", color = Color.White) },
                    text = { Text(msg, color = Color(0xFFE0E0E0)) },
                    containerColor = Color(0xFF2a2d3a),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Congrats overlay
            if (ui.showCongrats) {
                CongratsOverlay(ui = ui, onContinue = {
                    viewModel.dismissCongrats()
                    onNavigateBack()
                })
            }
        }
    }
}

@Composable
private fun OptionItem(
    text: String,
    enabled: Boolean,
    selected: Boolean,
    reveal: Boolean,
    answerCorrect: Boolean?,
    onClick: () -> Unit
) {
    val defaultColor = Color(0xFF37474F)
    val correctColor = Color(0xFF2E7D32) // green
    val wrongColor = Color(0xFFC62828)   // red
    val container = when {
        reveal && selected && (answerCorrect == true) -> correctColor
        reveal && selected && (answerCorrect == false) -> wrongColor
        else -> defaultColor
    }
    val borderColor = when {
        reveal && selected && (answerCorrect == true) -> Color(0xFF66BB6A)
        reveal && selected && (answerCorrect == false) -> Color(0xFFEF5350)
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(if (borderColor == Color.Transparent) 0.dp else 2.dp, borderColor)
    ) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = text, color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CongratsOverlay(ui: VocabUiState, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Great job!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Score: ${ui.score}", color = Color(0xFFFFD700), fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("XP gained: +${ui.completionXp} (Total ${ui.totalXp})", color = Color(0xFF81C784), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Continue")
                }
            }
        }
    }
}
