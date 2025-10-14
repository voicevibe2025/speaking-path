package com.example.voicevibe.presentation.screens.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.Material
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab {
    ALL,
    USERS,
    GROUPS,
    MATERIALS
}

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSearchUiState())
    val uiState: StateFlow<UserSearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        // Debounce search for better UX
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(300)
            if (query.trim().length >= 2) {
                search(query.trim())
            } else {
                _uiState.update { it.copy(
                    users = emptyList(),
                    groups = emptyList(),
                    materials = emptyList(),
                    error = null
                ) }
            }
        }
    }

    fun onSubmitSearch() {
        val q = _uiState.value.query.trim()
        if (q.isNotEmpty()) search(q)
    }

    fun onTabChange(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = repository.unifiedSearch(query)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        users = res.data?.users ?: emptyList(),
                        groups = res.data?.groups ?: emptyList(),
                        materials = res.data?.materials ?: emptyList(),
                        error = null
                    ) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = res.message ?: "Search failed",
                        users = emptyList(),
                        groups = emptyList(),
                        materials = emptyList()
                    ) }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}

data class UserSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val selectedTab: SearchTab = SearchTab.ALL,
    val users: List<UserProfile> = emptyList(),
    val groups: List<Group> = emptyList(),
    val materials: List<Material> = emptyList(),
    val error: String? = null
)
