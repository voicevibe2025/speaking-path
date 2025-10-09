package com.example.voicevibe.domain.model

/**
 * Aggregated metrics for the Profile Overview tab derived from Speaking Journey models
 */
data class SpeakingOverview(
    val averagePronunciation: Float = 0f,   // 0..100
    val averageFluency: Float = 0f,         // 0..100
    val averageVocabulary: Float = 0f,      // 0..100
    val averageGrammar: Float = 0f,         // 0..100
    val improvementRate: Float = 0f,        // -100..100 (delta percentage points)
    val completedTopics: Int = 0,
    val totalPracticeMinutes: Int = 0,      // unknown from API for now; keep 0 until available
    val totalWordsLearned: Int = 0          // derived from vocabulary sets of completed topics
)
