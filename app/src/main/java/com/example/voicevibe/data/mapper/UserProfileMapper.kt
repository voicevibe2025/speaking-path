package com.example.voicevibe.data.mapper

import com.example.voicevibe.data.model.UserProfile as DataUserProfile
import com.example.voicevibe.domain.model.UserProfile as DomainUserProfile
import com.example.voicevibe.domain.model.UserStats
import com.example.voicevibe.domain.model.UserPreferences
import com.example.voicevibe.domain.model.PrivacySettings
import com.example.voicevibe.domain.model.ProfileVisibility
import com.example.voicevibe.domain.model.DifficultyLevel
import com.example.voicevibe.domain.model.UserBadge
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.example.voicevibe.data.model.Achievement as DataAchievement
import com.example.voicevibe.data.model.Activity as DataActivity

fun DataUserProfile.toDomain(): DomainUserProfile {
    // Map recent achievements' badges (from data model) into domain UserBadge list for profile tabs
    val mappedBadges: List<UserBadge> = (this.recentAchievements ?: emptyList()).mapNotNull { ach ->
        val b = ach.badge
        val id = b.badgeId.ifBlank { return@mapNotNull null }
        val colorHex = normalizeColorHex(b.patternColor)
        UserBadge(
            id = id,
            name = b.name,
            icon = b.icon,
            color = colorHex
        )
    }.distinctBy { it.id }

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
        totalXp = this.totalPointsEarned ?: (this.experiencePoints ?: 0),
        xpToNextLevel = this.xpToNextLevel ?: 100,
        streakDays = this.streakDays ?: 0,
        longestStreak = this.streakDays ?: 0,
        joinedDate = parseToLocalDateTime(this.joinedDate),
        lastActiveDate = parseToLocalDateTime(this.lastActiveDate),
        country = null, // Not in data model
        countryCode = null, // Not in data model
        language = this.targetLanguage ?: "English",
        timezone = null, // Not in data model
        isVerified = false, // Not in data model
        isPremium = this.membershipStatus == "premium",
        isOnline = false, // Not in data model
        isFollowing = this.isFollowing == true,
        isFollower = this.isFollower == true,
        isBlocked = false, // Not in data model
        stats = UserStats(
            totalPracticeSessions = this.practiceCount ?: 0,
            totalPracticeMinutes = (this.totalPracticeHours?.times(60))?.toInt() ?: 0,
            averageAccuracy = this.avgScore ?: 0f,
            averageFluency = 0f, // Placeholder
            completedLessons = this.lessonsCompleted ?: 0,
            achievementsUnlocked = this.recentAchievements?.size ?: 0,
            followersCount = this.followersCount ?: 0,
            followingCount = this.followingCount ?: 0,
            globalRank = null, // Not in data model
            weeklyXp = 0, // Placeholder
            monthlyXp = this.monthlyXpEarned ?: 0,
            totalWords = this.wordsLearned ?: 0,
            improvementRate = 0f // Placeholder
        ),
        badges = mappedBadges,
        preferences = UserPreferences(
            dailyGoalMinutes = this.dailyPracticeGoal ?: 15,
            privacy = PrivacySettings(
                profileVisibility = ProfileVisibility.PUBLIC
            ),
            difficulty = DifficultyLevel.INTERMEDIATE // Placeholder
        ),
        socialLinks = null, // Not in data model
        proficiency = this.currentProficiency ?: "",
        // Per-skill scores from backend Speaking Journey aggregations
        speakingScore = this.speakingScore ?: 0f,
        pronunciationScore = this.pronunciationScore ?: 0f,
        fluencyScore = this.fluencyScore ?: 0f,
        vocabularyScore = this.vocabularyScore ?: 0f,
        listeningScore = this.listeningScore ?: 0f,
        grammarScore = this.grammarScore ?: 0f
    )
}

private fun parseToLocalDateTime(input: String?): LocalDateTime {
    if (input.isNullOrBlank()) return LocalDateTime.now()
    return try {
        // Try ISO instant (e.g., 2025-09-22T13:00:00Z)
        val ins = Instant.parse(input)
        LocalDateTime.ofInstant(ins, ZoneOffset.UTC)
    } catch (_: Exception) {
        try {
            // Try offset datetime (e.g., 2025-09-22T13:00:00+00:00)
            val odt = OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            odt.toLocalDateTime()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)
            } catch (_: Exception) {
                LocalDateTime.now()
            }
        }
    }
}

/**
 * Normalize color hex into 8-digit ARGB string without leading '#', expected by BadgeItem parser.
 */
private fun normalizeColorHex(input: String): String {
    var hex = input.trim()
    if (hex.startsWith("#")) hex = hex.substring(1)
    // If only RGB provided, prefix with full alpha
    if (hex.length == 6) return "FF$hex"
    // If already ARGB (8), return as-is
    return when (hex.length) {
        8 -> hex
        else -> "FF6200EE" // fallback to a primary-like color
    }
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
        totalPointsEarned = null, // Domain model doesn't track lifetime XP; let server compute
        joinedDate = null, // read-only on server
        lastActiveDate = null, // read-only on server
        xpToNextLevel = this.xpToNextLevel,
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
        fluencyScore = null, // Not in domain model
        listeningScore = null, // Not in domain model
        grammarScore = null, // Not in domain model
        vocabularyScore = null, // Not in domain model
        pronunciationScore = null, // Not in domain model
        practiceCount = null, // Not in domain model
        wordsLearned = null, // Not in domain model
        monthlyDaysActive = null, // Not in domain model
        monthlyXpEarned = this.stats.monthlyXp,
        monthlyLessonsCompleted = null, // Not in domain model
        recentActivities = emptyList(), // Complex mapping, handle separately if needed
        membershipStatus = if (this.isPremium) "premium" else "free"
    )
}
