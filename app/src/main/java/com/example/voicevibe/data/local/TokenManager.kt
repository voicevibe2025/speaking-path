package com.example.voicevibe.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.voicevibe.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.voicevibe.presentation.screens.gamification.AchievementFeedItem
import com.example.voicevibe.presentation.screens.gamification.AchievementItemType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Extension property to get DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

/**
 * Manages authentication tokens and user session data using DataStore
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Preference keys
    private val accessTokenKey = stringPreferencesKey(Constants.ACCESS_TOKEN_KEY)
    private val refreshTokenKey = stringPreferencesKey(Constants.REFRESH_TOKEN_KEY)
    private val userIdKey = stringPreferencesKey(Constants.USER_ID_KEY)
    private val userEmailKey = stringPreferencesKey(Constants.USER_EMAIL_KEY)
    private val userNameKey = stringPreferencesKey(Constants.USER_NAME_KEY)
    private val isLoggedInKey = booleanPreferencesKey(Constants.IS_LOGGED_IN_KEY)
    private val onboardingCompletedKey = booleanPreferencesKey(Constants.ONBOARDING_COMPLETED_KEY)
    private val speakingOnlyFlowKey = booleanPreferencesKey(Constants.SPEAKING_ONLY_FLOW_KEY)
    private val ttsVoiceIdKey = stringPreferencesKey(Constants.TTS_VOICE_ID_KEY)
    private val voiceAccentKey = stringPreferencesKey(Constants.VOICE_ACCENT_KEY)
    private val micPermissionAskedKey = booleanPreferencesKey("mic_permission_asked")
    private val showEmailOnProfileKey = booleanPreferencesKey(Constants.SHOW_EMAIL_ON_PROFILE_KEY)
    private val achievementHistoryKey = stringPreferencesKey("achievement_history")
    private val lastProficiencyKey = stringPreferencesKey("last_proficiency")
    private val lastLevelKey = intPreferencesKey("last_level")
    private val celebratedTopicsKey = stringPreferencesKey(Constants.CELEBRATED_TOPICS_KEY)
    
    private val gson = Gson()

    /**
     * Save authentication tokens
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
            preferences[isLoggedInKey] = true
        }
    }

    /**
     * Save user data
     */
    suspend fun saveUserData(userId: String, email: String, name: String) {
        dataStore.edit { preferences ->
            preferences[userIdKey] = userId
            preferences[userEmailKey] = email
            preferences[userNameKey] = name
        }
    }

    /**
     * Get access token synchronously (for interceptor)
     */
    fun getAccessToken(): String? = runBlocking {
        dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }.first()
    }

    /**
     * Get refresh token as Flow
     */
    fun getRefreshTokenFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[refreshTokenKey]
    }

    /**
     * Get refresh token synchronously
     */
    suspend fun getRefreshToken(): String? = dataStore.data.map { preferences ->
        preferences[refreshTokenKey]
    }.first()

    /**
     * Update access token
     */
    suspend fun updateAccessToken(newAccessToken: String) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = newAccessToken
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedInFlow(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[isLoggedInKey] ?: false
    }

    /**
     * Check if user is logged in synchronously
     */
    suspend fun isLoggedIn(): Boolean = dataStore.data.map { preferences ->
        preferences[isLoggedInKey] ?: false
    }.first()

    /**
     * Get user ID
     */
    fun getUserIdFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[userIdKey]
    }

    /**
     * Get user email
     */
    fun getUserEmailFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[userEmailKey]
    }

    /**
     * Get user name
     */
    fun getUserNameFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[userNameKey]
    }

    /**
     * Check if onboarding is completed
     */
    fun isOnboardingCompletedFlow(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[onboardingCompletedKey] ?: false
    }

    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = true
        }
    }

    /**
     * Clear all data (logout)
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Mic permission prompt tracking
     */
    fun micPermissionAskedFlow(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[micPermissionAskedKey] ?: false
    }

    fun wasMicPermissionAsked(): Boolean = runBlocking {
        dataStore.data.map { preferences ->
            preferences[micPermissionAskedKey] ?: false
        }.first()
    }

    suspend fun setMicPermissionAsked(value: Boolean = true) {
        dataStore.edit { preferences ->
            preferences[micPermissionAskedKey] = value
        }
    }

    /**
     * Speaking-only flow feature flag
     */
    fun speakingOnlyFlowEnabledFlow(): Flow<Boolean> =
        if (Constants.LOCK_SPEAKING_ONLY_ON) {
            // Hard-lock to ON while feature is in beta
            flowOf(true)
        } else {
            dataStore.data.map { preferences ->
                preferences[speakingOnlyFlowKey] ?: false
            }
        }

    suspend fun setSpeakingOnlyFlowEnabled(enabled: Boolean) {
        if (Constants.LOCK_SPEAKING_ONLY_ON) {
            // Persist ON so when the lock is removed later, default remains enabled
            dataStore.edit { preferences ->
                preferences[speakingOnlyFlowKey] = true
            }
            return
        }
        dataStore.edit { preferences ->
            preferences[speakingOnlyFlowKey] = enabled
        }
    }

    /**
     * Preferred TTS voice id (nullable -> default voice)
     */
    fun ttsVoiceIdFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[ttsVoiceIdKey]
    }

    suspend fun setTtsVoiceId(voiceId: String?) {
        dataStore.edit { preferences ->
            if (voiceId == null) {
                preferences.remove(ttsVoiceIdKey)
            } else {
                preferences[ttsVoiceIdKey] = voiceId
            }
        }
    }

    /**
     * Preferred voice accent (nullable -> no preference)
     */
    fun voiceAccentFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[voiceAccentKey]
    }

    suspend fun setVoiceAccent(accent: String?) {
        dataStore.edit { preferences ->
            if (accent.isNullOrBlank()) {
                preferences.remove(voiceAccentKey)
            } else {
                preferences[voiceAccentKey] = accent
            }
        }
    }

    /**
     * Account preferences: show email on header/profile
     */
    fun showEmailOnProfileFlow(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[showEmailOnProfileKey] ?: true
    }

    suspend fun setShowEmailOnProfile(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[showEmailOnProfileKey] = value
        }
    }

    /**
     * Achievement history persistence
     */
    fun achievementHistoryFlow(): Flow<List<AchievementFeedItem>> = dataStore.data.map { preferences ->
        val json = preferences[achievementHistoryKey] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<AchievementHistoryDto>>() {}.type
            val dtos: List<AchievementHistoryDto> = gson.fromJson(json, type)
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAchievementHistory(items: List<AchievementFeedItem>) {
        dataStore.edit { preferences ->
            val dtos = items.map { AchievementHistoryDto.fromDomain(it) }
            val json = gson.toJson(dtos)
            preferences[achievementHistoryKey] = json
        }
    }

    /**
     * Last known proficiency level (for change detection)
     */
    fun lastProficiencyFlow(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[lastProficiencyKey]
    }

    suspend fun setLastProficiency(proficiency: String) {
        dataStore.edit { preferences ->
            preferences[lastProficiencyKey] = proficiency
        }
    }

    /**
     * Last known user level (for change detection)
     */
    fun lastLevelFlow(): Flow<Int?> = dataStore.data.map { preferences ->
        preferences[lastLevelKey]
    }

    suspend fun setLastLevel(level: Int) {
        dataStore.edit { preferences ->
            preferences[lastLevelKey] = level
        }
    }

    /**
     * Celebrated topics tracking (to prevent re-showing celebration overlay)
     */
    suspend fun getCelebratedTopics(): Set<String> {
        return dataStore.data.map { preferences ->
            val json = preferences[celebratedTopicsKey] ?: return@map emptySet<String>()
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                gson.fromJson<Set<String>>(json, type)
            } catch (e: Exception) {
                emptySet()
            }
        }.first()
    }

    suspend fun markTopicAsCelebrated(topicId: String) {
        dataStore.edit { preferences ->
            val current = try {
                val json = preferences[celebratedTopicsKey] ?: "[]"
                val type = object : TypeToken<Set<String>>() {}.type
                gson.fromJson<Set<String>>(json, type).toMutableSet()
            } catch (e: Exception) {
                mutableSetOf()
            }
            current.add(topicId)
            preferences[celebratedTopicsKey] = gson.toJson(current)
        }
    }

    suspend fun hasTopicBeenCelebrated(topicId: String): Boolean {
        return getCelebratedTopics().contains(topicId)
    }
}

/**
 * DTO for serializing AchievementFeedItem to JSON
 */
private data class AchievementHistoryDto(
    val type: String,
    val title: String,
    val timestamp: String,
    val timeAgo: String
) {
    fun toDomain(): AchievementFeedItem {
        val itemType = when (type) {
            "LEVEL" -> AchievementItemType.LEVEL
            else -> AchievementItemType.PROFICIENCY
        }
        val dateTime = try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
        return AchievementFeedItem(
            type = itemType,
            title = title,
            timestamp = dateTime,
            timeAgo = timeAgo
        )
    }

    companion object {
        fun fromDomain(item: AchievementFeedItem): AchievementHistoryDto {
            return AchievementHistoryDto(
                type = item.type.name,
                title = item.title,
                timestamp = item.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                timeAgo = item.timeAgo
            )
        }
    }
}
