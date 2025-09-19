package com.example.voicevibe.presentation.screens.practice.speaking

import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.SpeakingPracticeRepository
import com.example.voicevibe.domain.model.PracticePrompt
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Speaking Practice screen
 */
@HiltViewModel
class SpeakingPracticeViewModel @Inject constructor(
    private val repository: SpeakingPracticeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakingPracticeUiState())
    val uiState: StateFlow<SpeakingPracticeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SpeakingPracticeEvent>()
    val events: SharedFlow<SpeakingPracticeEvent> = _events.asSharedFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    init {
        loadPracticePrompt()
    }

    private fun loadPracticePrompt() {
        viewModelScope.launch {
            repository.getRandomPrompt().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentPrompt = resource.data
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun startRecording(outputFile: File) {
        try {
            audioFile = outputFile

            @Suppress("DEPRECATION")
            val mr = MediaRecorder()
            mediaRecorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(128000)
            mr.setOutputFile(outputFile.absolutePath)
            mr.prepare()
            mr.start()

            _uiState.update {
                it.copy(
                    recordingState = RecordingState.RECORDING,
                    recordingDuration = 0
                )
            }

            // Start duration timer
            startDurationTimer()

        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Failed to start recording: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            _uiState.update {
                it.copy(
                    recordingState = RecordingState.STOPPED,
                    audioFilePath = audioFile?.absolutePath
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Failed to stop recording: ${e.message}")
            }
        }
    }

    fun pauseRecording() {
        try {
            mediaRecorder?.pause()
            _uiState.update {
                it.copy(recordingState = RecordingState.PAUSED)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Failed to pause recording: ${e.message}")
            }
        }
    }

    fun resumeRecording() {
        try {
            mediaRecorder?.resume()
            _uiState.update {
                it.copy(recordingState = RecordingState.RECORDING)
            }
            startDurationTimer()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Failed to resume recording: ${e.message}")
            }
        }
    }

    fun submitRecording() {
        val filePath = _uiState.value.audioFilePath ?: return
        val promptId = _uiState.value.currentPrompt?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            repository.submitRecording(
                promptId = promptId,
                audioFilePath = filePath
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isSubmitting = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submissionResult = resource.data
                            )
                        }
                        val id = resource.data?.sessionId
                        if (!id.isNullOrBlank()) {
                            _events.emit(SpeakingPracticeEvent.NavigateToResults(id))
                        } else {
                            _uiState.update { it.copy(error = "Missing session id from submission") }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun retryRecording() {
        audioFile?.delete()
        _uiState.update {
            it.copy(
                recordingState = RecordingState.IDLE,
                audioFilePath = null,
                recordingDuration = 0
            )
        }
    }

    fun loadNewPrompt() {
        loadPracticePrompt()
    }

    fun skipPrompt() {
        loadPracticePrompt()
    }

    private fun startDurationTimer() {
        viewModelScope.launch {
            while (_uiState.value.recordingState == RecordingState.RECORDING) {
                kotlinx.coroutines.delay(1000)
                _uiState.update {
                    it.copy(recordingDuration = it.recordingDuration + 1)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}

/**
 * UI State for Speaking Practice
 */
data class SpeakingPracticeUiState(
    val isLoading: Boolean = false,
    val currentPrompt: PracticePrompt? = null,
    val recordingState: RecordingState = RecordingState.IDLE,
    val recordingDuration: Int = 0, // in seconds
    val audioFilePath: String? = null,
    val isSubmitting: Boolean = false,
    val submissionResult: SubmissionResult? = null,
    val error: String? = null,
    val amplitudes: List<Float> = emptyList() // for waveform visualization
)

/**
 * Recording states
 */
enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

/**
 * Events from Speaking Practice
 */
sealed class SpeakingPracticeEvent {
    data class NavigateToResults(val sessionId: String) : SpeakingPracticeEvent()
    object ShowPermissionRequest : SpeakingPracticeEvent()
}

/**
 * Submission result data
 */
data class SubmissionResult(
    val sessionId: String,
    val score: Float,
    val feedback: String
)
