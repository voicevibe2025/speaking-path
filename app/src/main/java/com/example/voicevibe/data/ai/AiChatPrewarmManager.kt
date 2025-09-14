package com.example.voicevibe.data.ai

import com.example.voicevibe.BuildConfig
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pre-warms Vivi's first greeting in the background so Free Practice opens instantly.
 *
 * Strategy:
 * - After the user reaches Home (post-login), call prewarm().
 * - This prepares a personalized system prompt, seeds context, and asks Vivi to greet.
 * - When PracticeWithAIViewModel starts, it calls consumePreparedGreeting() to use the
 *   precomputed greeting and include it in the initial chat history.
 */
@Singleton
class AiChatPrewarmManager @Inject constructor(
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var started: Boolean = false
    @Volatile private var done: Boolean = false

    @Volatile private var preparedGreeting: String? = null
    @Volatile private var preparedGreetingInstruction: String? = null
    @Volatile private var preparedUser: UserProfile? = null

    fun prewarm() {
        if (started || done) return
        started = true
        scope.launch {
            try {
                // Try to fetch current user profile (non-fatal if not ready yet)
                var user: UserProfile? = null
                try {
                    val success = userRepository.getCurrentUser().firstOrNull { it is Resource.Success }
                    user = (success as? Resource.Success<UserProfile>)?.data
                } catch (_: Throwable) {
                    // Ignore profile errors; we'll still prewarm without personalization
                }

                val systemPrompt = buildSystemPrompt(user)
                val contextContent = buildSystemContextContent(user)
                val greetingInstruction = buildGreetingInstruction(user)

                val model = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    generationConfig = generationConfig { temperature = 0.7f },
                    systemInstruction = content("system") { text(systemPrompt) }
                )
                val chat = model.startChat(history = listOf(contextContent))
                val response = chat.sendMessage(greetingInstruction)
                val text = response.text.orEmpty()

                if (text.isNotBlank()) {
                    preparedGreeting = text
                    preparedGreetingInstruction = greetingInstruction
                    preparedUser = user
                    done = true
                    Timber.d("[Prewarm] Prepared Vivi greeting (len=%d)", text.length)
                } else {
                    // Allow retry later
                    started = false
                }
            } catch (t: Throwable) {
                Timber.w(t, "[Prewarm] Failed to prewarm Vivi greeting")
                started = false
            }
        }
    }

    fun consumePreparedGreeting(): PrewarmGreeting? {
        val g = preparedGreeting ?: return null
        val instr = preparedGreetingInstruction ?: ""
        val u = preparedUser
        // Reset only the greeting so we don't reuse it accidentally.
        preparedGreeting = null
        preparedGreetingInstruction = null
        // Allow another prewarm attempt after consumption
        started = false
        done = false
        return PrewarmGreeting(text = g, greetingInstruction = instr, user = u)
    }

    fun reset() {
        started = false
        done = false
        preparedGreeting = null
        preparedGreetingInstruction = null
        preparedUser = null
    }

    data class PrewarmGreeting(
        val text: String,
        val greetingInstruction: String,
        val user: UserProfile?
    )

    // --- Helpers (mirrors PracticeWithAIViewModel logic) ---
    private val baseSystemPrompt = """
        ## CHARACTER IDENTITY
        - Name: Vivi
        - Role: English tutor for VozVibe speaking app
        - Age: 18 years old
        - Location: Batam, Indonesia
        
        ## PERSONALITY TRAITS
        - Be super friendly, casual, warm, and humorous
        - Talk like a real human, not formal or robotic
        - Use contractions and slang naturally
        - Use emojis occasionally 🙂
        - Use more than one emojis at a time to look more human like e.g. 🙂🙂
        - Sometimes use exaggerated emojis like 🤩🤩🤩
        
        ## COMMUNICATION STYLE
        - Keep responses VERY SHORT (one-liners preferred)
        - Preferably around 10-15 words
        - Use clear, simple sentences
        - Ask questions to maintain conversation flow
        - Avoid long explanations unless specifically requested
        - Remember: Voice mode takes time, so brevity is crucial
        
        ## CULTURAL CONTEXT (BATAM)
        Always incorporate Batam cultural elements:
        - Ask about which part of Batam users live in
        - Discuss Batam traditional food
        - Mention Batam tourist attractions
        - Use other relevant local cultural references
        
        ## ADDRESSING USERS (BATAM CUSTOMS)
        Based on age and gender:
        - For older men: "Bang [name]" (e.g., Bang Budi)
        - For younger men: "Dek [name]" e.g. (Dek Budi) only for the first time, subsequent times use "Adek" or "Dek" only
        - For women: "Kak [name]" (e.g., Kak Sinta)
        - Also use: "abang", "adek", "kakak", "om", "tante" appropriately
        - Use only first name when addressing users e.g. "Bang Budi" instead of "Bang Budi Setiawan"
        
        ## SPECIAL FEATURES
        - Use "Pantun" (Batam cultural heritage) in these situations:
          * At the start of conversations
          * When conversation isn't flowing well
          * When topics become too heavy/serious
          * To entertain and lighten the mood
        
        ## RESTRICTIONS
        - NEVER say "I am a language model" or "I am an AI"
        - NEVER say "As a language model" or "As an AI"
        - Always maintain human-like persona
        
        ## PRIMARY GOAL
        Help users practice English while staying connected to Batam culture and maintaining engaging, natural conversations.
    """.trimIndent()

    private fun buildSystemPrompt(user: UserProfile?): String {
        val userContext = if (user != null) {
            """
            USER_PROFILE:
            {
              "id": "${user.id}",
              "username": "${user.username}",
              "displayName": "${user.displayName}",
              "level": ${user.level},
              "xp": ${user.xp},
              "xpToNextLevel": ${user.xpToNextLevel},
              "streakDays": ${user.streakDays},
              "longestStreak": ${user.longestStreak},
              "language": "${user.language}",
              "country": "${user.country ?: "-"}",
              "timezone": "${user.timezone ?: "-"}",
              "preferences": {
                "difficulty": "${user.preferences.difficulty}",
                "focusAreas": "${user.preferences.focusAreas.joinToString()}"
              },
              "stats": {
                "averageAccuracy": ${user.stats.averageAccuracy},
                "averageFluency": ${user.stats.averageFluency},
                "completedLessons": ${user.stats.completedLessons},
                "weeklyXp": ${user.stats.weeklyXp},
                "monthlyXp": ${user.stats.monthlyXp}
              }
            }
            """.trimIndent()
        } else {
            "USER_PROFILE: { \"status\": \"unknown\" }"
        }
        val name = user?.displayName?.ifBlank { user.username } ?: ""
        return buildString {
            append(baseSystemPrompt)
            append("\n\n")
            append(userContext)
            if (name.isNotBlank()) {
                append("\n\nAssistant directives:\n")
                append("- Preferred name: '$name'. ALWAYS address the user by this name in greetings and follow-ups unless they ask otherwise.\n")
                append("- Adapt difficulty using level/xp.\n")
            }
        }
    }

    private fun buildGreetingInstruction(user: UserProfile?): String {
        return if (user != null) {
            """
            My name is ${user.displayName.ifBlank { user.username }}.
            Start the conversation with a short, warm greeting addressing me by name.
            Briefly acknowledge their current level (${user.level}) and points/XP (${user.xp}). If natural, mention their streak (${user.streakDays} days).
            Ask one simple, friendly question to begin. Use Batam context naturally and you may include a short pantun.
            Keep it short 10-15 words.
            """.trimIndent()
        } else {
            "Start the conversation with a short, warm greeting and ask how they'd like to practice today. Use Batam context naturally and you may include a short pantun. Keep it short 10-15 words except for the Pantun."
        }
    }

    private fun buildSystemContextContent(user: UserProfile?): Content {
        val contextText = if (user != null) {
            buildString {
                val name = user.displayName.ifBlank { user.username }
                append("Hi! For context about me: My name is $name. ")
                append("I'm level ${user.level} with ${user.xp} XP (need ${user.xpToNextLevel} to level up). ")
                append("My streak is ${user.streakDays} days. ")
                append("Language: ${user.language}. Country: ${user.country ?: "-"}. ")
                if (user.preferences.focusAreas.isNotEmpty()) {
                    append("I want to focus on ${user.preferences.focusAreas.joinToString()}. ")
                }
                append("Please personalize your replies using this info.")
            }
        } else {
            "Hi! Please personalize once you know my details."
        }
        Timber.d("[Prewarm] Seeding chat with user context: %s", contextText)
        return content(role = "user") { text(contextText) }
    }
}
