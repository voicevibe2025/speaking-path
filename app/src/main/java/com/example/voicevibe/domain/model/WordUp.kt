package com.example.voicevibe.domain.model

import java.time.Instant

/**
 * Domain models for WordUp vocabulary feature
 */

data class VocabularyWord(
    val id: Int,
    val word: String,
    val definition: String,
    val difficulty: WordDifficulty,
    val exampleSentence: String,
    val partOfSpeech: String,
    val ipaPronunciation: String = ""
)

enum class WordDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    companion object {
        fun from(value: String): WordDifficulty {
            return when (value.lowercase()) {
                "beginner" -> BEGINNER
                "intermediate" -> INTERMEDIATE
                "advanced" -> ADVANCED
                else -> BEGINNER
            }
        }
    }
}

data class WordProgress(
    val id: Int,
    val word: VocabularyWord,
    val isMastered: Boolean,
    val attempts: Int,
    val userExampleSentence: String,
    val firstAttemptedAt: Instant,
    val masteredAt: Instant?,
    val lastPracticedAt: Instant
)

data class WordWithProgress(
    val word: VocabularyWord,
    val progress: WordProgress?
)

data class EvaluationResult(
    val isAcceptable: Boolean,
    val feedback: String,
    val wordId: Int,
    val isMastered: Boolean
)

data class WordUpStats(
    val totalWords: Int,
    val masteredCount: Int,
    val inProgressCount: Int,
    val completionPercentage: Double
)

data class PronunciationResult(
    val isCorrect: Boolean,
    val transcribedText: String,
    val feedback: String,
    val confidence: Float
)
