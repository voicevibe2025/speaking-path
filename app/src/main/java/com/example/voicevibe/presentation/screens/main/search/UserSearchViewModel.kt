package com.example.voicevibe.presentation.screens.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
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
                _uiState.update { it.copy(results = emptyList(), error = null) }
            }
        }
    }

    fun onSubmitSearch() {
        val q = _uiState.value.query.trim()
        if (q.isNotEmpty()) search(q)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = repository.searchUsers(query)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, results = res.data ?: emptyList(), error = null) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.message ?: "Search failed", results = emptyList()) }
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
    val results: List<UserProfile> = emptyList(),
    val error: String? = null
)
