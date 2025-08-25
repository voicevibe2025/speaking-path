package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_email")
    val userEmail: String,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("current_proficiency")
    val currentProficiency: String?,
    @SerializedName("current_level")
    val currentLevel: Int?,
    @SerializedName("experience_points")
    val experiencePoints: Int?,
    @SerializedName("streak_days")
    val streakDays: Int?,
    @SerializedName("total_practice_hours")
    val totalPracticeHours: Float?,
    @SerializedName("lessons_completed")
    val lessonsCompleted: Int?,
    @SerializedName("recordings_count")
    val recordingsCount: Int?,
    @SerializedName("avg_score")
    val avgScore: Float?,
    @SerializedName("recent_achievements")
    val recentAchievements: List<Achievement>?,
    @SerializedName("daily_practice_goal")
    val dailyPracticeGoal: Int?,
    @SerializedName("learning_goal")
    val learningGoal: String?,
    @SerializedName("target_language")
    val targetLanguage: String?
)
