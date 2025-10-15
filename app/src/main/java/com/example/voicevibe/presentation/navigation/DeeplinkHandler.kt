package com.example.voicevibe.presentation.navigation

import android.net.Uri

/**
 * Utility for parsing and handling AI Coach deeplinks
 * 
 * Supported format: app://voicevibe/speaking/topic/{topicId}/{mode}
 * 
 * Modes:
 * - master: TopicMaster screen (pronunciation overview)
 * - conversation: Conversation practice
 * - vocab: Vocabulary practice
 * - listening: Listening practice
 * - grammar: Grammar practice
 */
object DeeplinkHandler {
    
    /**
     * Parse AI Coach deeplink and return navigation route
     * 
     * @param deeplink Format: app://voicevibe/speaking/topic/{topicId}/{mode}
     * @return Navigation route string, or null if invalid
     */
    fun parseCoachDeeplink(deeplink: String): String? {
        return try {
            val uri = Uri.parse(deeplink)
            
            // Validate scheme and host
            if (uri.scheme != "app" || uri.host != "voicevibe") {
                return null
            }
            
            // Parse path: /speaking/topic/{topicId}/{mode}
            val pathSegments = uri.pathSegments
            if (pathSegments.size < 4 || 
                pathSegments[0] != "speaking" || 
                pathSegments[1] != "topic") {
                return null
            }
            
            val topicId = pathSegments[2]
            val mode = pathSegments.getOrNull(3) ?: "master"
            
            // Map mode to navigation route
            when (mode.lowercase()) {
                "master" -> Screen.TopicMaster.createRoute(topicId)
                "conversation" -> Screen.ConversationPractice.createRoute(topicId)
                "vocab" -> Screen.VocabularyPractice.createRoute(topicId)
                "listening" -> Screen.ListeningPractice.createRoute(topicId)
                "grammar" -> Screen.GrammarPractice.createRoute(topicId)
                else -> Screen.TopicMaster.createRoute(topicId) // Default fallback
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create AI Coach deeplink from topic ID and mode
     * 
     * @param topicId Topic identifier
     * @param mode Practice mode (master, conversation, vocab, listening, grammar)
     * @return Deeplink string
     */
    fun createCoachDeeplink(topicId: String, mode: String = "master"): String {
        return "app://voicevibe/speaking/topic/$topicId/$mode"
    }
}
