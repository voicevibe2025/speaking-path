package com.example.voicevibe.utils

/**
 * Utilities for sanitizing ASR transcripts.
 *
 * collapseRepeats removes contiguous repeated 2..maxPhraseWords-gram runs, keeping only
 * the first occurrence in the original formatting. Designed to clean Whisper-style loops
 * like "hello hello hello" or repeated short phrases.
 */
object TranscriptUtils {
    @JvmStatic
    fun collapseRepeats(
        text: String?,
        maxPhraseWords: Int = 5,
        maxWordsForAuto: Int = 30,
        force: Boolean = false
    ): String {
        val s = (text ?: "").trim()
        if (s.isEmpty()) return s
        // Split by whitespace but keep original words for output
        val words = s.replace("\n", " ").split(Regex("\\s+")).toMutableList()
        val n = words.size
        if (!force && n <= 3) return words.joinToString(" ")
        if (!force && n > maxWordsForAuto) return words.joinToString(" ")

        // Build normalized tokens for comparison (lowercase, strip punctuation except apostrophe)
        val norm = words.map { w ->
            w.replace('’', '\'').replace('`', '\'')
                .lowercase()
                .replace(Regex("[^a-z0-9']+"), "")
        }
        val out = ArrayList<String>(n)
        var i = 0
        while (i < n) {
            val maxW = minOf(maxPhraseWords, n - i)
            var collapsed = false
            // Try longer chunks first
            for (w in maxW downTo 2) {
                val chunk = norm.subList(i, i + w)
                if (chunk.any { it.isEmpty() }) continue
                var repeats = 1
                while (i + (repeats * w) + w <= n) {
                    val next = norm.subList(i + repeats * w, i + (repeats + 1) * w)
                    if (next == chunk) repeats++ else break
                }
                if (repeats >= 2) {
                    // Keep only the first occurrence in original formatting
                    out.addAll(words.subList(i, i + w))
                    i += repeats * w
                    collapsed = true
                    break
                }
            }
            if (!collapsed) {
                out.add(words[i])
                i += 1
            }
        }
        return out.joinToString(" ")
    }
}
