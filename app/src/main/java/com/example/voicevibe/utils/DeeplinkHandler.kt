package com.example.voicevibe.utils

import android.net.Uri
import android.util.Log
import com.example.voicevibe.presentation.navigation.Screen

/**
 * Utility for parsing AI Coach deeplinks and converting them to navigation routes.
 * 
 * Deeplink format: app://voicevibe/speaking/topic/{topicId}/{mode}
 * 
 * Supported modes:
 * - master → TopicMaster screen (pronunciation overview)
 * - conversation → Conversation practice
 * - vocab → Vocabulary practice
 * - listening → Listening comprehension
 * - grammar → Grammar practice
 */
object DeeplinkHandler {
    
    private const val TAG = "DeeplinkHandler"
    
    /**
     * Parse a Coach deeplink and return the corresponding navigation route.
     * 
     * @param deeplink The deeplink string (e.g., "app://voicevibe/speaking/topic/uuid-here/grammar")
     * @return Navigation route string, or null if parsing fails
     */
    fun parseCoachDeeplink(deeplink: String): String? {
        return try {
            Log.d(TAG, "Parsing deeplink: $deeplink")
            val uri = Uri.parse(deeplink)
            
            // Validate scheme and host
            if (uri.scheme != "app" || uri.host != "voicevibe") {
                Log.e(TAG, "Invalid scheme or host: ${uri.scheme}://${uri.host}")
                return null
            }
            
            // Expected path: /speaking/topic/{topicId}/{mode}
            val pathSegments = uri.pathSegments
            Log.d(TAG, "Path segments: $pathSegments")
            
            if (pathSegments.size < 3 || pathSegments[0] != "speaking" || pathSegments[1] != "topic") {
                Log.e(TAG, "Invalid path structure. Expected /speaking/topic/{id}/{mode}, got: ${uri.path}")
                return null
            }
            
            val topicId = pathSegments[2]
            val mode = pathSegments.getOrNull(3) ?: "master"
            Log.d(TAG, "TopicId: $topicId, Mode: $mode")
            
            // Map mode to navigation route
            val route = when (mode.lowercase()) {
                "master" -> Screen.TopicMaster.createRoute(topicId)
                "conversation" -> Screen.ConversationPractice.createRoute(topicId)
                "vocab" -> Screen.VocabularyLesson.createRoute(topicId)
                "listening" -> Screen.ListeningPractice.createRoute(topicId)
                "grammar" -> Screen.GrammarPractice.createRoute(topicId)
                else -> Screen.TopicMaster.createRoute(topicId) // fallback
            }
            
            Log.d(TAG, "Generated route: $route")
            return route
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing deeplink: ${e.message}", e)
            null
        }
    }
}
