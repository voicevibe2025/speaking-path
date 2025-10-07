package com.example.voicevibe.presentation.system

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.voicevibe.data.system.MaintenanceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val maintenanceManager: MaintenanceManager
) : ViewModel() {

    val state: StateFlow<MaintenanceManager.MaintenanceState> =
        maintenanceManager.state.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            maintenanceManager.state.value
        )

    fun dismiss() {
        maintenanceManager.clearIfResolved()
    }
}
