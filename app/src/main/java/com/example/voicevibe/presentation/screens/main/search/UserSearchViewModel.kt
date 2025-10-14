package com.example.voicevibe.presentation.screens.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.SearchFilter
import com.example.voicevibe.domain.model.SearchResultItem
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.SearchMaterial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnifiedSearchUiState())
    val uiState: StateFlow<UnifiedSearchUiState> = _uiState.asStateFlow()

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
    
    fun onFilterChange(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val type = when (_uiState.value.selectedFilter) {
                SearchFilter.ALL -> "all"
                SearchFilter.USERS -> "users"
                SearchFilter.GROUPS -> "groups"
                SearchFilter.MATERIALS -> "materials"
            }
            
            when (val res = repository.unifiedSearch(query, type)) {
                is Resource.Success -> {
                    val data = res.data
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            users = data?.users ?: emptyList(),
                            groups = data?.groups ?: emptyList(),
                            materials = data?.materials ?: emptyList(),
                            error = null
                        ) 
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = res.message ?: "Search failed",
                            users = emptyList(),
                            groups = emptyList(),
                            materials = emptyList()
                        ) 
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}

data class UnifiedSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val users: List<UserProfile> = emptyList(),
    val groups: List<Group> = emptyList(),
    val materials: List<SearchMaterial> = emptyList(),
    val error: String? = null
)
