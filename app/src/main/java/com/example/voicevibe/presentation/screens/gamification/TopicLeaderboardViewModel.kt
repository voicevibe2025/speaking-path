package com.example.voicevibe.presentation.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import com.example.voicevibe.data.remote.api.SpeakingTopicDto
import com.example.voicevibe.domain.model.LeaderboardData
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicLeaderboardViewModel @Inject constructor(
    private val repository: GamificationRepository,
    private val speakingRepo: SpeakingJourneyRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val data: LeaderboardData? = null,
        val error: String? = null,
        val topics: List<SpeakingTopicDto> = emptyList(),
        val selectedTopicId: String? = null,
        val topicTitle: String = "Topic Leaderboard"
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(topicId: String, limit: Int = 50) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Fetch topic title
            val topicTitle = runCatching { 
                speakingRepo.getTopics().getOrNull()?.topics?.find { it.id == topicId }?.title 
            }.getOrNull() ?: "Topic Leaderboard"
            
            when (val res = repository.getTopicLeaderboard(topicId, limit)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, data = res.data, error = null, selectedTopicId = topicId, topicTitle = topicTitle) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.message ?: "Failed to load topic leaderboard", topicTitle = topicTitle) }
                }
                else -> {}
            }
        }
    }

    fun refresh(topicId: String) {
        load(topicId)
    }

    fun loadTopics() {
        viewModelScope.launch {
            runCatching { speakingRepo.getTopics() }
                .onSuccess { result ->
                    result.getOrNull()?.let { response ->
                        _uiState.update { it.copy(topics = response.topics) }
                    }
                }
        }
    }
}
