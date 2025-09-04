package com.example.voicevibe.data.mapper

import com.example.voicevibe.data.model.UserProfile as DataUserProfile
import com.example.voicevibe.domain.model.UserProfile as DomainUserProfile
import com.example.voicevibe.domain.model.UserStats
import com.example.voicevibe.domain.model.UserPreferences
import com.example.voicevibe.domain.model.PrivacySettings
import com.example.voicevibe.domain.model.ProfileVisibility
import com.example.voicevibe.domain.model.DifficultyLevel
import java.time.LocalDateTime
import com.example.voicevibe.data.model.Achievement as DataAchievement
import com.example.voicevibe.data.model.Activity as DataActivity

fun DataUserProfile.toDomain(): DomainUserProfile {
    return DomainUserProfile(
        id = this.userName,
        username = this.userName,
        email = this.userEmail,
        displayName = "${this.firstName ?: ""} ${this.lastName ?: ""}".trim(),
        bio = null, // Not available in data model
        avatarUrl = this.avatarUrl,
        coverImageUrl = null, // Not available in data model
        level = this.currentLevel ?: 1,
        xp = this.experiencePoints ?: 0,
        xpToNextLevel = 1000, // Placeholder, not in data model
        streakDays = this.streakDays ?: 0,
        longestStreak = 0, // Not in data model
        joinedDate = LocalDateTime.now(), // Placeholder
        lastActiveDate = LocalDateTime.now(), // Placeholder
        country = null, // Not in data model
        countryCode = null, // Not in data model
        language = this.targetLanguage ?: "English",
        timezone = null, // Not in data model
        isVerified = false, // Not in data model
        isPremium = this.membershipStatus == "premium",
        isOnline = false, // Not in data model
        isFollowing = false, // Not in data model
        isFollower = false, // Not in data model
        isBlocked = false, // Not in data model
        stats = UserStats(
            totalPracticeSessions = 0, // Placeholder
            totalPracticeMinutes = (this.totalPracticeHours?.times(60))?.toInt() ?: 0,
            averageAccuracy = this.avgScore ?: 0f,
            averageFluency = 0f, // Placeholder
            completedLessons = this.lessonsCompleted ?: 0,
            achievementsUnlocked = this.recentAchievements?.size ?: 0,
            followersCount = 0, // Not in data model
            followingCount = 0, // Not in data model
            globalRank = null, // Not in data model
            weeklyXp = 0, // Placeholder
            monthlyXp = this.monthlyXpEarned ?: 0,
            totalWords = 0, // Placeholder
            improvementRate = 0f // Placeholder
        ),
        badges = emptyList(), // Not in data model
        preferences = UserPreferences(
            dailyGoalMinutes = this.dailyPracticeGoal ?: 15,
            privacy = PrivacySettings(
                profileVisibility = ProfileVisibility.PUBLIC
            ),
            difficulty = DifficultyLevel.INTERMEDIATE // Placeholder
        ),
        socialLinks = null // Not in data model
    )
}

fun DomainUserProfile.toData(): DataUserProfile {
    val nameParts = this.displayName.split(" ", limit = 2)
    val firstName = nameParts.getOrNull(0)
    val lastName = nameParts.getOrNull(1)

    return DataUserProfile(
        userName = this.username,
        userEmail = this.email,
        avatarUrl = this.avatarUrl,
        firstName = firstName,
        lastName = lastName,
        currentProficiency = null, // Not in domain model
        currentLevel = this.level,
        experiencePoints = this.xp,
        streakDays = this.streakDays,
        totalPracticeHours = this.stats.totalPracticeMinutes / 60f,
        lessonsCompleted = this.stats.completedLessons,
        recordingsCount = null, // Not in domain model
        avgScore = this.stats.averageAccuracy,
        recentAchievements = emptyList(), // Complex mapping, handle separately if needed
        dailyPracticeGoal = this.preferences.dailyGoalMinutes,
        learningGoal = null, // Not in domain model
        targetLanguage = this.language,
        speakingScore = null, // Not in domain model
        listeningScore = null, // Not in domain model
        grammarScore = null, // Not in domain model
        vocabularyScore = null, // Not in domain model
        pronunciationScore = null, // Not in domain model
        monthlyDaysActive = null, // Not in domain model
        monthlyXpEarned = this.stats.monthlyXp,
        monthlyLessonsCompleted = null, // Not in domain model
        recentActivities = emptyList(), // Complex mapping, handle separately if needed
        membershipStatus = if (this.isPremium) "premium" else "free"
    )
}
