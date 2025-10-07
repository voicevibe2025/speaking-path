package com.example.voicevibe.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceManager @Inject constructor() {

    enum class Cause { Maintenance, Network }

    data class MaintenanceState(
        val active: Boolean = false,
        val message: String = "",
        val retryAfterSeconds: Long? = null,
        val cause: Cause = Cause.Maintenance,
        val version: Long = System.currentTimeMillis()
    )

    private val _state = MutableStateFlow(MaintenanceState(active = false))
    val state: StateFlow<MaintenanceState> = _state

    fun setMaintenance(message: String? = null, retryAfterSeconds: Long? = null) {
        _state.value = MaintenanceState(
            active = true,
            message = message ?: "Server maintenance in progress. Some features may be unavailable. Please try again later.",
            retryAfterSeconds = retryAfterSeconds,
            cause = Cause.Maintenance,
            version = System.currentTimeMillis()
        )
    }

    fun setNetworkIssue(message: String? = null) {
        _state.value = MaintenanceState(
            active = true,
            message = message ?: "We are having trouble reaching the server. This may be due to a reset or maintenance. Please try again shortly.",
            retryAfterSeconds = null,
            cause = Cause.Network,
            version = System.currentTimeMillis()
        )
    }

    fun clearIfResolved() {
        // Only clear if it was previously active
        if (_state.value.active) {
            _state.value = MaintenanceState(active = false, message = "", version = System.currentTimeMillis())
        }
    }
}
