package com.example.voicevibe.data.remote.dto.auth

import com.google.gson.annotations.SerializedName

// Request DTOs

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("password_confirm")
    val passwordConfirm: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("native_language")
    val nativeLanguage: String = "Indonesian",
    @SerializedName("target_language")
    val targetLanguage: String = "English",
    @SerializedName("proficiency_level")
    val proficiencyLevel: String = "A1"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refresh: String
)

data class VerifyEmailRequest(
    val token: String
)

data class ResendVerificationRequest(
    val email: String
)

data class PasswordResetRequest(
    val email: String
)

data class PasswordResetConfirmRequest(
    val token: String,
    @SerializedName("new_password")
    val newPassword: String
)

// Response DTOs

data class AuthResponse(
    val access: String,
    val refresh: String,
    val user: UserDto
)

data class TokenResponse(
    val access: String,
    val refresh: String? = null
)

data class MessageResponse(
    val message: String,
    val success: Boolean = true
)

data class UserDto(
    val id: Int,
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_verified")
    val isVerified: Boolean,
    @SerializedName("date_joined")
    val dateJoined: String,
    val profile: UserProfileDto?
)

data class UserProfileDto(
    val id: Int,
    @SerializedName("native_language")
    val nativeLanguage: String,
    @SerializedName("target_language")
    val targetLanguage: String,
    @SerializedName("proficiency_level")
    val proficiencyLevel: String,
    @SerializedName("learning_goals")
    val learningGoals: List<String>,
    @SerializedName("daily_practice_goal")
    val dailyPracticeGoal: Int,
    @SerializedName("preferred_topics")
    val preferredTopics: List<String>,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("streak_count")
    val streakCount: Int,
    @SerializedName("total_points")
    val totalPoints: Int,
    @SerializedName("current_level")
    val currentLevel: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
