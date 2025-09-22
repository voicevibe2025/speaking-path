package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.remote.api.CoachAnalysisDto
import com.example.voicevibe.data.repository.CoachRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val repo: CoachRepository
) : ViewModel() {

    data class UiState(
        val analysis: CoachAnalysisDto? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _ui = mutableStateOf(UiState(isLoading = true))
    val ui: State<UiState> get() = _ui

    init {
        refreshIfNeeded()
    }

    fun refreshIfNeeded(force: Boolean = false) {
        val has = _ui.value.analysis != null
        if (has && !force) return
        loadAnalysis(force)
    }

    fun loadAnalysis(force: Boolean = false) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            val res = if (force) repo.refreshAnalysis() else repo.getAnalysis()
            res.fold(
                onSuccess = { dto ->
                    _ui.value = UiState(analysis = dto, isLoading = false, error = null)
                },
                onFailure = { e ->
                    _ui.value = UiState(analysis = null, isLoading = false, error = e.message ?: "Failed to load coach analysis")
                }
            )
        }
    }
}
