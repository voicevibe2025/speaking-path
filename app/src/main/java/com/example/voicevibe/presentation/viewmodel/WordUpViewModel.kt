package com.example.voicevibe.presentation.viewmodel

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.WordUpRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for WordUp feature
 */
@HiltViewModel
class WordUpViewModel @Inject constructor(
    private val repository: WordUpRepository
) : ViewModel() {

    private val tag = "WordUpViewModel"

    // UI State
    private val _uiState = MutableStateFlow(WordUpUiState())
    val uiState: StateFlow<WordUpUiState> = _uiState.asStateFlow()

    // Stats
    private val _stats = MutableStateFlow<WordUpStats?>(null)
    val stats: StateFlow<WordUpStats?> = _stats.asStateFlow()

    // Mastered words
    private val _masteredWords = MutableStateFlow<List<WordProgress>>(emptyList())
    val masteredWords: StateFlow<List<WordProgress>> = _masteredWords.asStateFlow()

    // Recording state
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    
    // TTS playback
    private var mediaPlayer: MediaPlayer? = null

    init {
        loadStats()
    }

    fun loadNewWord() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.getRandomWord().fold(
                onSuccess = { wordWithProgress ->
                    _uiState.value = _uiState.value.copy(
                        currentWord = wordWithProgress.word,
                        progress = wordWithProgress.progress,
                        isLoading = false,
                        showDefinition = false,
                        exampleSentence = "",
                        evaluationResult = null
                    )
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to load word", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load word"
                    )
                }
            )
        }
    }

    fun showDefinition() {
        _uiState.value = _uiState.value.copy(showDefinition = true)
    }

    fun updateExampleSentence(sentence: String) {
        _uiState.value = _uiState.value.copy(exampleSentence = sentence)
    }

    fun startRecording(file: File) {
        try {
            recordingFile = file
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            _uiState.value = _uiState.value.copy(isRecording = true)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start recording", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to start recording: ${e.message}"
            )
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // Check if audio file was actually created
            val audioFile = recordingFile
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    hasAudioRecording = true
                )
                // Auto-submit the recording for evaluation
                evaluateExample()
            } else {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    hasAudioRecording = false,
                    error = "Recording failed - no audio captured"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop recording", e)
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                hasAudioRecording = false,
                error = "Failed to stop recording: ${e.message}"
            )
        }
    }

    fun evaluateExample() {
        val currentWord = _uiState.value.currentWord ?: return
        val sentence = _uiState.value.exampleSentence.trim()
        val hasAudio = _uiState.value.hasAudioRecording
        val audioFile = recordingFile
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEvaluating = true, error = null)
            
            // Convert audio to base64 if recording was used
            val audioBase64 = audioFile?.let { file ->
                if (file.exists()) {
                    try {
                        val bytes = file.readBytes()
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to read audio file", e)
                        null
                    }
                } else null
            }
            
            repository.evaluateExample(
                wordId = currentWord.id,
                exampleSentence = if (sentence.isNotEmpty() && !hasAudio) sentence else null,
                audioBase64 = audioBase64
            ).fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        isEvaluating = false,
                        evaluationResult = result
                    )
                    
                    // Reload stats if word was mastered
                    if (result.isMastered) {
                        loadStats()
                    }
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to evaluate example", error)
                    _uiState.value = _uiState.value.copy(
                        isEvaluating = false,
                        error = error.message ?: "Failed to evaluate example"
                    )
                }
            )
            
            // Clean up recording file
            audioFile?.delete()
            recordingFile = null
            _uiState.value = _uiState.value.copy(hasAudioRecording = false)
        }
    }

    fun clearEvaluation() {
        _uiState.value = _uiState.value.copy(evaluationResult = null)
    }

    fun loadStats() {
        viewModelScope.launch {
            repository.getStats().fold(
                onSuccess = { stats ->
                    _stats.value = stats
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to load stats", error)
                }
            )
        }
    }

    fun loadMasteredWords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getMasteredWords().fold(
                onSuccess = { words ->
                    _masteredWords.value = words
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to load mastered words", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load mastered words"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun playWordPronunciation(word: String) {
        viewModelScope.launch {
            try {
                // Stop any existing playback
                mediaPlayer?.release()
                mediaPlayer = null
                
                // Fetch pronunciation audio from backend
                repository.getWordPronunciation(word).fold(
                    onSuccess = { audioBytes ->
                        // Save to temp file
                        val tempFile = File.createTempFile("word_tts_", ".mp3")
                        tempFile.writeBytes(audioBytes)
                        
                        // Play audio
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(tempFile.absolutePath)
                            setOnCompletionListener {
                                release()
                                mediaPlayer = null
                                tempFile.delete()
                            }
                            setOnErrorListener { _, _, _ ->
                                release()
                                mediaPlayer = null
                                tempFile.delete()
                                true
                            }
                            prepare()
                            start()
                        }
                    },
                    onFailure = { error ->
                        Log.e(tag, "Failed to play pronunciation", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error playing pronunciation", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
        recordingFile?.delete()
    }
}

data class WordUpUiState(
    val currentWord: VocabularyWord? = null,
    val progress: WordProgress? = null,
    val showDefinition: Boolean = false,
    val exampleSentence: String = "",
    val isRecording: Boolean = false,
    val hasAudioRecording: Boolean = false,
    val isLoading: Boolean = false,
    val isEvaluating: Boolean = false,
    val evaluationResult: EvaluationResult? = null,
    val error: String? = null,
    val isPlayingTts: Boolean = false
)
