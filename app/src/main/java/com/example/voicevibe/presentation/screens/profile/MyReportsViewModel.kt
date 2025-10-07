package com.example.voicevibe.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.remote.api.ReportItem
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<Resource<List<ReportItem>>>(Resource.Loading())
    val reports: StateFlow<Resource<List<ReportItem>>> = _reports

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _reports.value = Resource.Loading()
            _reports.value = userRepository.getMyReports()
        }
    }
}
