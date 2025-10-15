package com.example.voicevibe.data.mapper

import com.example.voicevibe.data.model.*
import com.example.voicevibe.domain.model.*
import java.time.Instant

/**
 * Mappers for WordUp data models to domain models
 */

/**
 * Parse ISO 8601 timestamp from Django (which includes microseconds and timezone offsets)
 * Android's j$.time.Instant.parse() has issues with timezone offsets, so we use OffsetDateTime
 */
private fun parseInstant(timestamp: String): Instant {
    // Django format: 2025-10-15T18:23:28.996515+07:00
    // Trim microseconds to milliseconds (3 digits)
    val normalized = if (timestamp.contains('.')) {
        val parts = timestamp.split('.')
        val beforeDecimal = parts[0]
        val afterDecimal = parts[1]
        
        // Find where the timezone starts (+ or -)
        val tzIndex = afterDecimal.indexOfFirst { it == '+' || it == '-' }
        if (tzIndex > 0) {
            val fractional = afterDecimal.substring(0, tzIndex)
            val timezone = afterDecimal.substring(tzIndex)
            // Keep only first 3 digits (milliseconds)
            val millis = fractional.take(3).padEnd(3, '0')
            "$beforeDecimal.$millis$timezone"
        } else {
            timestamp
        }
    } else {
        timestamp
    }
    
    // Parse using OffsetDateTime which handles timezone offsets properly
    return java.time.OffsetDateTime.parse(normalized).toInstant()
}

fun VocabularyWordDto.toDomain(): VocabularyWord {
    return VocabularyWord(
        id = id,
        word = word,
        definition = definition,
        difficulty = WordDifficulty.from(difficulty),
        exampleSentence = exampleSentence,
        partOfSpeech = partOfSpeech
    )
}

fun WordProgressDto.toDomain(): WordProgress {
    return WordProgress(
        id = id,
        word = word.toDomain(),
        isMastered = isMastered,
        attempts = attempts,
        userExampleSentence = userExampleSentence,
        firstAttemptedAt = parseInstant(firstAttemptedAt),
        masteredAt = masteredAt?.let { parseInstant(it) },
        lastPracticedAt = parseInstant(lastPracticedAt)
    )
}

fun RandomWordResponse.toDomain(): WordWithProgress {
    return WordWithProgress(
        word = word.toDomain(),
        progress = progress.toDomain()
    )
}

fun EvaluationResultDto.toDomain(): EvaluationResult {
    return EvaluationResult(
        isAcceptable = isAcceptable,
        feedback = feedback,
        wordId = wordId,
        isMastered = isMastered
    )
}

fun WordUpStatsDto.toDomain(): WordUpStats {
    return WordUpStats(
        totalWords = totalWords,
        masteredCount = masteredCount,
        inProgressCount = inProgressCount,
        completionPercentage = completionPercentage
    )
}
