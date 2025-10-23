package com.example.voicevibe.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Post-registration/login screen for selecting English Level
 * Only shown if user hasn't set their level yet (backward compatible)
 */
@Composable
fun SelectEnglishLevelScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToGroupSelection: () -> Unit,
    viewModel: SelectEnglishLevelViewModel = hiltViewModel()
) {
    val levels = listOf(
        LevelOption("BEGINNER", "Beginner", "Just starting to learn English"),
        LevelOption("INTERMEDIATE", "Intermediate", "Comfortable with basic conversations"),
        LevelOption("ADVANCED", "Advanced", "Fluent and confident in English")
    )
    
    var selectedLevel by remember { mutableStateOf("INTERMEDIATE") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Observe navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            isLoading = false
            when (event) {
                is NavigationEvent.NavigateToHome -> onNavigateToHome()
                is NavigationEvent.NavigateToGroupSelection -> onNavigateToGroupSelection()
            }
        }
    }
    
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Select Your English Level",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "This helps us provide topics matched to your skill level",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Level options
                levels.forEach { level ->
                    LevelOptionCard(
                        level = level,
                        isSelected = selectedLevel == level.value,
                        onClick = { selectedLevel = level.value }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Bottom button
            Button(
                onClick = {
                    isLoading = true
                    viewModel.saveEnglishLevel(selectedLevel)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelOptionCard(
    level: LevelOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else 
                Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = level.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private data class LevelOption(
    val value: String,
    val displayName: String,
    val description: String
)

@HiltViewModel
class SelectEnglishLevelViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val speakingRepo: SpeakingJourneyRepository,
    private val userRepository: com.example.voicevibe.data.repository.UserRepository
) : ViewModel() {
    
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()
    
    fun saveEnglishLevel(level: String) {
        viewModelScope.launch {
            try {
                // Save to DataStore first
                tokenManager.setEnglishLevel(level)
                
                // Save to server (MUST complete before navigation)
                speakingRepo.updateEnglishLevel(level)
                
                // Check if user already has a group
                userRepository.getCurrentUser().collect { resource ->
                    when (resource) {
                        is com.example.voicevibe.domain.model.Resource.Success -> {
                            val hasGroup = resource.data?.hasGroup ?: false
                            if (hasGroup) {
                                _navigationEvent.emit(NavigationEvent.NavigateToHome)
                            } else {
                                _navigationEvent.emit(NavigationEvent.NavigateToGroupSelection)
                            }
                        }
                        is com.example.voicevibe.domain.model.Resource.Error -> {
                            // If we can't check, default to group selection
                            _navigationEvent.emit(NavigationEvent.NavigateToGroupSelection)
                        }
                        else -> { /* Loading */ }
                    }
                }
            } catch (e: Exception) {
                // If server save fails, still navigate but with risk
                _navigationEvent.emit(NavigationEvent.NavigateToGroupSelection)
            }
        }
    }
}

sealed class NavigationEvent {
    object NavigateToHome : NavigationEvent()
    object NavigateToGroupSelection : NavigationEvent()
}
