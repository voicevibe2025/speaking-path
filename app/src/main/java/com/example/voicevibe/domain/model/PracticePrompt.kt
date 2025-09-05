package com.example.voicevibe.domain.model

/**
 * Practice prompt for speaking exercises
 */
data class PracticePrompt(
    val id: String,
    val text: String,
    val category: String,
    val difficulty: DifficultyLevel,
    val hints: List<String> = emptyList(),
    val targetDuration: Int = 30, // in seconds
    val culturalContext: String? = null,
    val scenarioType: ScenarioType = ScenarioType.GENERAL
)

/**
 * Difficulty levels for practice prompts
 */
enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    UPPER_INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Types of practice scenarios
 */
enum class ScenarioType {
    GENERAL,
    BUSINESS,
    ACADEMIC,
    SOCIAL,
    CULTURAL,
    TECHNICAL
}

/**
 * Speaking session data
 */
data class SpeakingSession(
    val id: String,
    val userId: String,
    val promptId: String,
    val audioUrl: String,
    val transcription: String? = null,
    val evaluation: SpeakingEvaluation? = null,
    val duration: Int,
    val createdAt: String,
    val status: SessionStatus = SessionStatus.PENDING
)

/**
 * Session status
 */
enum class SessionStatus {
    PENDING,
    PROCESSING,
    EVALUATED,
    FAILED
}

/**
 * Speaking evaluation result
 */
data class SpeakingEvaluation(
    val sessionId: String,
    val overallScore: Float,
    val pronunciation: EvaluationScore,
    val fluency: EvaluationScore,
    val vocabulary: EvaluationScore,
    val grammar: EvaluationScore,
    val coherence: EvaluationScore,
    val culturalAppropriateness: EvaluationScore? = null,
    val feedback: String,
    val suggestions: List<String>,
    val phoneticErrors: List<PhoneticError>,
    val pauses: List<Float>? = null,
    val stutters: Int? = null,
    val createdAt: String
)

/**
 * Individual evaluation score
 */
data class EvaluationScore(
    val score: Float, // 0-100
    val level: PerformanceLevel,
    val feedback: String
)

/**
 * Performance levels
 */
enum class PerformanceLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    NEEDS_IMPROVEMENT
}

/**
 * Phonetic error detail
 */
data class PhoneticError(
    val word: String,
    val expected: String,
    val actual: String,
    val timestamp: Float
)
