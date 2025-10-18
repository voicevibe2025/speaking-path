package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for WordUp API
 */

data class VocabularyWordDto(
    @SerializedName("id") val id: Int,
    @SerializedName("word") val word: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("example_sentence") val exampleSentence: String,
    @SerializedName("part_of_speech") val partOfSpeech: String,
    @SerializedName("ipa_pronunciation") val ipaPronunciation: String = ""
)

data class WordProgressDto(
    @SerializedName("id") val id: Int,
    @SerializedName("word") val word: VocabularyWordDto,
    @SerializedName("is_mastered") val isMastered: Boolean,
    @SerializedName("attempts") val attempts: Int,
    @SerializedName("user_example_sentence") val userExampleSentence: String,
    @SerializedName("first_attempted_at") val firstAttemptedAt: String,
    @SerializedName("mastered_at") val masteredAt: String?,
    @SerializedName("last_practiced_at") val lastPracticedAt: String
)

data class RandomWordResponse(
    @SerializedName("word") val word: VocabularyWordDto,
    @SerializedName("progress") val progress: WordProgressDto
)

data class EvaluateExampleRequest(
    @SerializedName("word_id") val wordId: Int,
    @SerializedName("example_sentence") val exampleSentence: String? = null,
    @SerializedName("audio_base64") val audioBase64: String? = null
)

data class EvaluationResultDto(
    @SerializedName("is_acceptable") val isAcceptable: Boolean,
    @SerializedName("feedback") val feedback: String,
    @SerializedName("word_id") val wordId: Int,
    @SerializedName("is_mastered") val isMastered: Boolean
)

data class MasteredWordsResponse(
    @SerializedName("mastered_words") val masteredWords: List<WordProgressDto>,
    @SerializedName("total_mastered") val totalMastered: Int
)

data class WordUpStatsDto(
    @SerializedName("total_words") val totalWords: Int,
    @SerializedName("mastered_count") val masteredCount: Int,
    @SerializedName("in_progress_count") val inProgressCount: Int,
    @SerializedName("completion_percentage") val completionPercentage: Double
)

data class EvaluatePronunciationRequest(
    @SerializedName("word_id") val wordId: Int,
    @SerializedName("audio_base64") val audioBase64: String
)

data class PronunciationResultDto(
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("transcribed_text") val transcribedText: String,
    @SerializedName("feedback") val feedback: String,
    @SerializedName("confidence") val confidence: Float
)
