package com.example.voicevibe.presentation.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.R
import com.example.voicevibe.data.repository.WordUpRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repository: WordUpRepository,
    @ApplicationContext private val context: Context
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
    private var pronunciationRecordingFile: File? = null
    
    // TTS playback
    private var mediaPlayer: MediaPlayer? = null
    
    // Sound effects
    private var soundEffectPlayer: MediaPlayer? = null

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
                        learningStep = LearningStep.INITIAL,
                        pronunciationResult = null,
                        exampleSentence = "",
                        evaluationResult = null,
                        pronunciationFailures = 0,
                        sentenceInputMode = null
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
        _uiState.value = _uiState.value.copy(learningStep = LearningStep.DEFINITION_REVEALED)
    }

    fun startPronunciationPractice(file: File) {
        try {
            pronunciationRecordingFile = file
            
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
            
            _uiState.value = _uiState.value.copy(
                isPronunciationRecording = true,
                learningStep = LearningStep.PRONUNCIATION_PRACTICE
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start pronunciation recording", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to start recording: ${e.message}"
            )
        }
    }

    fun stopPronunciationPractice() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val audioFile = pronunciationRecordingFile
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                _uiState.value = _uiState.value.copy(isPronunciationRecording = false)
                evaluatePronunciation()
            } else {
                _uiState.value = _uiState.value.copy(
                    isPronunciationRecording = false,
                    error = "Recording failed - no audio captured"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop pronunciation recording", e)
            _uiState.value = _uiState.value.copy(
                isPronunciationRecording = false,
                error = "Failed to stop recording: ${e.message}"
            )
        }
    }

    private fun evaluatePronunciation() {
        val currentWord = _uiState.value.currentWord ?: return
        val audioFile = pronunciationRecordingFile ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPronunciationEvaluating = true, error = null)
            
            val audioBase64 = try {
                val bytes = audioFile.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(tag, "Failed to read audio file", e)
                _uiState.value = _uiState.value.copy(
                    isPronunciationEvaluating = false,
                    error = "Failed to read audio: ${e.message}"
                )
                return@launch
            }
            
            repository.evaluatePronunciation(
                wordId = currentWord.id,
                audioBase64 = audioBase64
            ).fold(
                onSuccess = { result ->
                    val previousFailures = _uiState.value.pronunciationFailures

                    _uiState.value = _uiState.value.copy(
                        isPronunciationEvaluating = false,
                        pronunciationResult = result,
                        learningStep = LearningStep.PRONUNCIATION_EVALUATED,
                        pronunciationFailures = if (result.isCorrect) 0 else previousFailures + 1
                    )
                    
                    // Play sound effect for correct pronunciation
                    if (result.isCorrect) {
                        playSoundEffect(R.raw.correct)
                        
                        // Auto-advance to sentence practice
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            _uiState.value = _uiState.value.copy(
                                learningStep = LearningStep.SENTENCE_PRACTICE
                            )
                        }, 2000) // 2 second delay to show feedback
                    }
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to evaluate pronunciation", error)
                    _uiState.value = _uiState.value.copy(
                        isPronunciationEvaluating = false,
                        error = error.message ?: "Failed to evaluate pronunciation"
                    )
                }
            )
            
            // Clean up pronunciation recording file
            audioFile.delete()
            pronunciationRecordingFile = null
        }
    }

    fun retryPronunciation() {
        _uiState.value = _uiState.value.copy(
            pronunciationResult = null,
            learningStep = LearningStep.DEFINITION_REVEALED
        )
    }

    fun skipCurrentWord() {
        loadNewWord()
    }

    fun selectSentenceInputMode(mode: SentenceInputMode) {
        _uiState.value = _uiState.value.copy(sentenceInputMode = mode)
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
                    
                    // Play applause sound for correct sentence usage
                    if (result.isAcceptable) {
                        playSoundEffect(R.raw.applause)
                    }
                    
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
        _uiState.value = _uiState.value.copy(
            evaluationResult = null,
            exampleSentence = "",
            hasAudioRecording = false
        )
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

    private fun playSoundEffect(resourceId: Int) {
        try {
            // Release any existing sound effect player
            soundEffectPlayer?.release()
            
            soundEffectPlayer = MediaPlayer.create(context, resourceId)?.apply {
                setOnCompletionListener {
                    it.release()
                    soundEffectPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error playing sound effect", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
        soundEffectPlayer?.release()
        recordingFile?.delete()
    }
}

enum class LearningStep {
    INITIAL,              // Show only word and speaker icon
    DEFINITION_REVEALED,  // Show definition + mic button for pronunciation
    PRONUNCIATION_PRACTICE, // Recording pronunciation
    PRONUNCIATION_EVALUATED, // Show pronunciation result
    SENTENCE_PRACTICE     // Show text input for sentence
}

enum class SentenceInputMode {
    TEXT,
    VOICE
}

data class WordUpUiState(
    val currentWord: VocabularyWord? = null,
    val progress: WordProgress? = null,
    val learningStep: LearningStep = LearningStep.INITIAL,
    val pronunciationResult: PronunciationResult? = null,
    val pronunciationFailures: Int = 0,
    val exampleSentence: String = "",
    val isRecording: Boolean = false,
    val isPronunciationRecording: Boolean = false,
    val hasAudioRecording: Boolean = false,
    val isLoading: Boolean = false,
    val isEvaluating: Boolean = false,
    val isPronunciationEvaluating: Boolean = false,
    val evaluationResult: EvaluationResult? = null,
    val error: String? = null,
    val isPlayingTts: Boolean = false,
    val sentenceInputMode: SentenceInputMode? = null
)
