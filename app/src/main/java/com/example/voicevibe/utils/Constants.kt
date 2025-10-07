package com.example.voicevibe.utils

/**
 * App-wide constants
 */
object Constants {
    // API Configuration
    //const val BASE_URL = "http://10.0.2.2:8000/api/v1/" // For Android emulator
    //const val BASE_URL = "http://10.212.7.136:8000/api/v1/" // For physical device (replace with your IP)
    const val BASE_URL = "https://speaking-path-server-production.up.railway.app/api/v1/" // Production

    // WebSocket Configuration
    //const val WS_BASE_URL = "ws://10.0.2.2:8000/ws/" // For Android emulator
    //const val WS_BASE_URL = "ws://10.212.7.136:8000/ws/" // For physical device
    const val WS_BASE_URL = "wss://speaking-path-server-production.up.railway.app/ws/" // Production

    // DataStore Keys
    const val PREFERENCES_NAME = "voicevibe_preferences"
    const val ACCESS_TOKEN_KEY = "access_token"
    const val REFRESH_TOKEN_KEY = "refresh_token"
    const val USER_ID_KEY = "user_id"
    const val USER_EMAIL_KEY = "user_email"
    const val USER_NAME_KEY = "user_name"
    const val IS_LOGGED_IN_KEY = "is_logged_in"
    const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    const val SPEAKING_ONLY_FLOW_KEY = "speaking_only_flow_enabled"
    const val TTS_VOICE_ID_KEY = "tts_voice_id"
    const val VOICE_ACCENT_KEY = "voice_accent"
    const val SHOW_EMAIL_ON_PROFILE_KEY = "show_email_on_profile"

    // Temporary feature flag: lock Speaking-only Journey to ON
    const val LOCK_SPEAKING_ONLY_ON = true

    // Audio Recording
    const val AUDIO_SAMPLE_RATE = 44100
    const val AUDIO_CHANNEL_CONFIG = 1 // Mono
    const val AUDIO_FORMAT = 16 // 16-bit PCM
    const val MIN_RECORDING_DURATION_MS = 1000L
    const val MAX_RECORDING_DURATION_MS = 300000L // 5 minutes

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val INITIAL_PAGE = 1

    // Cache
    const val CACHE_VALIDITY_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Animation Durations
    const val ANIMATION_DURATION_SHORT = 300
    const val ANIMATION_DURATION_MEDIUM = 500
    const val ANIMATION_DURATION_LONG = 1000

    // Practice timers
    const val PRACTICE_SECONDS_PER_TURN = 10

    // Learning Levels
    val PROFICIENCY_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")

    // Error Messages
    const val NETWORK_ERROR = "Network error. Please check your connection."
    const val UNKNOWN_ERROR = "An unexpected error occurred."
    const val INVALID_CREDENTIALS = "Invalid email or password."
    const val SESSION_EXPIRED = "Your session has expired. Please login again."
}
