package com.example.voicevibe.presentation.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.GroupRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.comparisons.compareByDescending
import kotlin.comparisons.thenByDescending

/**
 * ViewModel for Leaderboard screen
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: GamificationRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LeaderboardEvent>()
    val events: SharedFlow<LeaderboardEvent> = _events.asSharedFlow()

    init {
        loadLeaderboard()
        loadCompetitionStats()
        loadGroupLeaderboard()
    }

    private fun applySorting(data: LeaderboardData, filter: LeaderboardFilter): LeaderboardData {
        val sorted = when (filter) {
            LeaderboardFilter.OVERALL_XP -> data.entries.sortedByDescending { it.score }
            LeaderboardFilter.STREAK -> data.entries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.streakDays }
                    .thenByDescending { it.score }
            )
            LeaderboardFilter.ACCURACY -> data.entries // TODO: Needs backend-provided accuracy metric
            LeaderboardFilter.PRACTICE_TIME -> data.entries // TODO: Needs backend-provided practice time metric
            LeaderboardFilter.ACHIEVEMENTS -> data.entries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.achievements }
                    .thenByDescending { it.score }
            )
            LeaderboardFilter.DAILY_XP -> data.entries.sortedByDescending { it.weeklyXp } // Kept for backward compatibility if ever selected
            LeaderboardFilter.WEEKLY_XP -> data.entries.sortedByDescending { it.weeklyXp }
            LeaderboardFilter.MONTHLY_XP -> data.entries.sortedByDescending { it.monthlyXp }
            // Lingo League categories are already ranked server-side
            LeaderboardFilter.PRONUNCIATION -> data.entries
            LeaderboardFilter.FLUENCY -> data.entries
            LeaderboardFilter.VOCABULARY -> data.entries
            LeaderboardFilter.GRAMMAR -> data.entries
            LeaderboardFilter.LISTENING -> data.entries
            LeaderboardFilter.CONVERSATION -> data.entries
            LeaderboardFilter.TOPICS_COMPLETED -> data.entries
        }
        // If no change in order, keep original ranks
        if (sorted === data.entries || sorted == data.entries) return data

        // Renumber ranks for display according to the sorted order
        val renumbered = sorted.mapIndexed { index, e -> e.copy(rank = index + 1) }
        return data.copy(entries = renumbered)
    }

    private fun loadLeaderboard(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val type = _uiState.value.selectedType
            // For Lingo League, use the selected skill as the filter for the backend
            val filter = if (type == LeaderboardType.LINGO_LEAGUE) {
                _uiState.value.selectedLingoLeagueSkill
            } else {
                _uiState.value.selectedFilter
            }

            when (val result = repository.getLeaderboard(type, filter, refresh = refresh)) {
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

                    // Apply client-side sorting based on the selected filter
                    val sortedData = normalizedData?.let { applySorting(it, filter) }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            leaderboardData = sortedData,
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
            val newFilter = when (type) {
                LeaderboardType.LINGO_LEAGUE -> LeaderboardFilter.DAILY_XP // Default to Daily time filter for Lingo League
                else -> LeaderboardFilter.OVERALL_XP
            }
            _uiState.update {
                it.copy(selectedType = type, selectedFilter = newFilter)
            }
            loadLeaderboard(refresh = false)
        }
    }

    fun selectFilter(filter: LeaderboardFilter) {
        if (_uiState.value.selectedFilter != filter) {
            _uiState.update {
                it.copy(selectedFilter = filter)
            }
            loadLeaderboard(refresh = false)
        }
    }

    fun selectLingoLeagueSkill(skill: LeaderboardFilter) {
        if (_uiState.value.selectedLingoLeagueSkill != skill) {
            _uiState.update {
                it.copy(selectedLingoLeagueSkill = skill)
            }
            loadLeaderboard(refresh = false)
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
        if (_uiState.value.selectedMode == LeaderboardMode.GROUP) {
            loadGroupLeaderboard()
        } else {
            loadLeaderboard(refresh = true)
            loadCompetitionStats()
        }
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

    fun selectMode(mode: LeaderboardMode) {
        if (_uiState.value.selectedMode != mode) {
            _uiState.update {
                it.copy(selectedMode = mode)
            }
        }
    }

    private fun loadGroupLeaderboard() {
        viewModelScope.launch {
            // First get current user's group
            val groupStatusResult = groupRepository.checkGroupStatus()
            val currentUserGroupId = if (groupStatusResult is Resource.Success) {
                groupStatusResult.data?.group?.id
            } else null

            // Fetch all groups
            when (val groupsResult = groupRepository.getGroups()) {
                is Resource.Success -> {
                    val groups = groupsResult.data ?: emptyList()
                    val groupEntries = mutableListOf<GroupLeaderboardEntry>()

                    // For each group, fetch members and calculate total XP
                    groups.forEach { group ->
                        when (val membersResult = groupRepository.getGroupMembers(group.id)) {
                            is Resource.Success -> {
                                val (_, members) = membersResult.data ?: return@forEach
                                val totalXp = members.sumOf { it.xp }
                                
                                groupEntries.add(
                                    GroupLeaderboardEntry(
                                        rank = 0, // Will be assigned after sorting
                                        groupId = group.id,
                                        groupName = group.name,
                                        displayName = group.displayName,
                                        icon = group.icon,
                                        color = group.color,
                                        totalScore = totalXp,
                                        memberCount = members.size,
                                        isCurrentUserGroup = group.id == currentUserGroupId
                                    )
                                )
                            }
                            else -> {
                                Log.w("LeaderboardVM", "Failed to load members for group ${group.name}")
                            }
                        }
                    }

                    // Sort by total XP descending and assign ranks
                    val sortedEntries = groupEntries
                        .sortedByDescending { it.totalScore }
                        .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

                    _uiState.update {
                        it.copy(
                            groupLeaderboardEntries = sortedEntries,
                            currentUserGroupId = currentUserGroupId
                        )
                    }
                }
                is Resource.Error -> {
                    Log.e("LeaderboardVM", "Failed to load groups: ${groupsResult.message}")
                }
                else -> {}
            }
        }
    }

    fun refreshGroupLeaderboard() {
        loadGroupLeaderboard()
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
    val selectedMode: LeaderboardMode = LeaderboardMode.INDIVIDUAL,
    val selectedType: LeaderboardType = LeaderboardType.WEEKLY,
    val selectedFilter: LeaderboardFilter = LeaderboardFilter.OVERALL_XP,
    val selectedLingoLeagueSkill: LeaderboardFilter = LeaderboardFilter.PRONUNCIATION,
    val groupLeaderboardEntries: List<GroupLeaderboardEntry> = emptyList(),
    val currentUserGroupId: Int? = null,
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
