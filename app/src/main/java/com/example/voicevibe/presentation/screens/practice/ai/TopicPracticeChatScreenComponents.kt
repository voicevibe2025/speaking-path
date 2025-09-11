package com.example.voicevibe.presentation.screens.practice.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionCard(onRoleSelected: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2a2d3a)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose Your Role", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Select which speaker you want to be:", color = Color(0xFFB0BEC5), fontSize = 14.sp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { onRoleSelected("A") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Speaker A", tint = Color(0xFF64B5F6))
                        Text("Speaker A", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
                
                Surface(
                    onClick = { onRoleSelected("B") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Face, contentDescription = "Speaker B", tint = Color(0xFF4CAF50))
                        Text("Speaker B", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeTurnCard(
    text: String,
    speaker: String,
    isUserTurn: Boolean,
    currentlyPlayingId: String?,
    onPlay: () -> Unit
) {
    val isActive = currentlyPlayingId == text
    
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isUserTurn) Color(0xFF1B4332) else Color(0xFF2a2d3a)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) Color.White.copy(alpha = 0.06f) else Color.Transparent, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f)) {
                Text(
                    speaker,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(text, color = Color.White, modifier = Modifier.weight(1f))
            if (!isUserTurn) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Play", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingPromptCard(
    expectedText: String,
    isProcessing: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Mic, contentDescription = "Record", tint = Color(0xFF4CAF50))
                Text("Your Turn", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            
            Text("Say this phrase:", color = Color(0xFFB0BEC5), fontSize = 14.sp)
            Text("\"$expectedText\"", color = Color.White, fontStyle = FontStyle.Italic)
            
            Surface(
                onClick = {
                    if (isProcessing) return@Surface
                    isRecording = !isRecording
                    if (isRecording) {
                        onStartRecording()
                    } else {
                        onStopRecording()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isProcessing) Color(0xFF8D6E63) else if (isRecording) Color(0xFFE53935) else Color(0xFF4CAF50)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Mic, 
                        contentDescription = if (isProcessing) "Processing" else if (isRecording) "Stop Recording" else "Start Recording", 
                        tint = Color.White
                    )
                    Text(
                        when {
                            isProcessing -> "Transcribing..."
                            isRecording -> "Stop Recording"
                            else -> "Start Recording"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeHintCard(hint: String, expectedText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8E24AA).copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.Lightbulb, contentDescription = "Hint", tint = Color(0xFFFFB74D))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hint:", color = Color(0xFFFFB74D), fontWeight = FontWeight.SemiBold)
                Text(hint, color = Color.White)
                Text("Expected: \"$expectedText\"", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
fun RevealAnswerCard(correctText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Answer", tint = Color(0xFF4CAF50))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Correct Answer:", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                Text("\"$correctText\"", color = Color.White, fontWeight = FontWeight.Medium)
                Text("Try saying this phrase next time.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
            }
        }
    }
}
