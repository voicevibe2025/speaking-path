package com.example.voicevibe.presentation.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Leaderboard screen
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LeaderboardEvent>()
    val events: SharedFlow<LeaderboardEvent> = _events.asSharedFlow()

    init {
        loadLeaderboard()
        loadCompetitionStats()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val type = _uiState.value.selectedType
            val filter = _uiState.value.selectedFilter

            when (val result = repository.getLeaderboard(type, filter)) {
                is Resource.Success -> {
                    result.data?.let { data ->
                        runCatching {
                            Log.d(
                                "LeaderboardVM",
                                "Loaded ${data.type} leaderboard: entries=${data.entries.size} total=${data.totalParticipants} lastUpdated=${data.lastUpdated}"
                            )
                            data.currentUserEntry?.let { e ->
                                Log.d(
                                    "LeaderboardVM",
                                    "CurrentUser rank=${e.rank} level=${e.level} streakDays=${e.streakDays} score=${e.score}"
                                )
                            }
                            data.entries.firstOrNull()?.let { e ->
                                Log.d(
                                    "LeaderboardVM",
                                    "TopUser rank=${e.rank} level=${e.level} streakDays=${e.streakDays} score=${e.score}"
                                )
                            }
                        }.onFailure { t ->
                            Log.e("LeaderboardVM", "Logging failed: ${t.message}", t)
                        }
                    }
                    val normalizedData = result.data?.let { data ->
                        val normalizedEntries = data.entries.map { e ->
                            e.copy(avatarUrl = e.avatarUrl?.let { u -> normalizeUrl(u) })
                        }
                        val normalizedCurrent = data.currentUserEntry?.let { e ->
                            e.copy(avatarUrl = e.avatarUrl?.let { u -> normalizeUrl(u) })
                        }
                        data.copy(entries = normalizedEntries, currentUserEntry = normalizedCurrent)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            leaderboardData = normalizedData,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun loadCompetitionStats() {
        viewModelScope.launch {
            when (val result = repository.getCompetitionStats()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(competitionStats = result.data)
                    }
                }
                else -> {}
            }
        }
    }

    fun selectLeaderboardType(type: LeaderboardType) {
        if (_uiState.value.selectedType != type) {
            _uiState.update {
                it.copy(selectedType = type)
            }
            loadLeaderboard()
        }
    }

    fun selectFilter(filter: LeaderboardFilter) {
        if (_uiState.value.selectedFilter != filter) {
            _uiState.update {
                it.copy(selectedFilter = filter)
            }
            loadLeaderboard()
        }
    }

    fun viewUserProfile(userId: String) {
        viewModelScope.launch {
            _events.emit(LeaderboardEvent.NavigateToProfile(userId))
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            when (val result = repository.followUser(userId)) {
                is Resource.Success -> {
                    _events.emit(
                        LeaderboardEvent.ShowMessage("User followed successfully")
                    )
                    loadLeaderboard() // Refresh to update friend leaderboard
                }
                is Resource.Error -> {
                    _events.emit(
                        LeaderboardEvent.ShowMessage(
                            result.message ?: "Failed to follow user"
                        )
                    )
                }
                else -> {}
            }
        }
    }

    fun challengeUser(userId: String) {
        viewModelScope.launch {
            _events.emit(LeaderboardEvent.ShowChallengeDialog(userId))
        }
    }

    fun refreshLeaderboard() {
        loadLeaderboard()
        loadCompetitionStats()
    }

    fun shareRank() {
        viewModelScope.launch {
            _uiState.value.leaderboardData?.currentUserEntry?.let { entry ->
                _events.emit(
                    LeaderboardEvent.ShareRank(
                        rank = entry.rank,
                        score = entry.score,
                        league = _uiState.value.competitionStats?.currentLeague?.name ?: ""
                    )
                )
            }
        }
    }

    fun viewLeagueInfo() {
        viewModelScope.launch {
            _uiState.value.competitionStats?.currentLeague?.let { league ->
                _events.emit(LeaderboardEvent.ShowLeagueInfo(league))
            }
        }
    }

    fun scrollToCurrentUser() {
        viewModelScope.launch {
            val data = _uiState.value.leaderboardData ?: return@launch
            val entry = data.currentUserEntry ?: return@launch

            // Find the index of the current user within the visible entries list
            val entries = data.entries
            val entryIndex = entries.indexOfFirst { it.userId == entry.userId }

            if (entryIndex == -1) {
                // Current user is not present in the currently visible list
                _events.emit(
                    LeaderboardEvent.ShowMessage(
                        "You're ranked #${entry.rank} but not visible in the current list"
                    )
                )
                return@launch
            }

            val hasPodium = entries.size >= 3
            val targetIndex = if (hasPodium) {
                // Index 0 is the podium (ranks 1-3). Remaining items start from rank 4 at index 1
                if (entryIndex < 3) 0 else (entryIndex - 3) + 1
            } else {
                // No podium header, items map 1:1 with entries
                entryIndex
            }

            _events.emit(LeaderboardEvent.ScrollToPosition(targetIndex))
        }
    }
}

private fun normalizeUrl(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    val serverBase = com.example.voicevibe.utils.Constants.BASE_URL.substringBefore("/api/").trimEnd('/')
    val path = if (url.startsWith("/")) url else "/$url"
    return serverBase + path
}

/**
 * UI State for Leaderboard screen
 */
data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val leaderboardData: LeaderboardData? = null,
    val competitionStats: CompetitionStats? = null,
    val selectedType: LeaderboardType = LeaderboardType.WEEKLY,
    val selectedFilter: LeaderboardFilter = LeaderboardFilter.OVERALL_XP,
    val error: String? = null
)

/**
 * Events from Leaderboard screen
 */
sealed class LeaderboardEvent {
    data class NavigateToProfile(val userId: String) : LeaderboardEvent()
    data class ShowChallengeDialog(val userId: String) : LeaderboardEvent()
    data class ShowLeagueInfo(val league: League) : LeaderboardEvent()
    data class ShareRank(
        val rank: Int,
        val score: Int,
        val league: String
    ) : LeaderboardEvent()
    data class ShowMessage(val message: String) : LeaderboardEvent()
    data class ScrollToPosition(val position: Int) : LeaderboardEvent()
}
