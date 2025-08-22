package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.LearningApiService
import com.example.voicevibe.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningRepository @Inject constructor(
    private val apiService: LearningApiService
) {

    suspend fun getAllLearningPaths(): List<LearningPath> {
        // In a real app, this would call the API
        // For now, return mock data
        delay(1000) // Simulate network delay
        return generateMockLearningPaths()
    }

    suspend fun getEnrolledPaths(): List<LearningPath> {
        delay(500)
        return generateMockLearningPaths().filter { it.isEnrolled }
    }

    suspend fun getRecommendedPaths(): List<PathRecommendation> {
        delay(500)
        val paths = generateMockLearningPaths()
        return listOf(
            PathRecommendation(
                path = paths[0],
                reason = "Based on your pronunciation practice",
                matchScore = 0.92f,
                basedOn = RecommendationType.SKILL_BASED
            ),
            PathRecommendation(
                path = paths[2],
                reason = "Popular among learners at your level",
                matchScore = 0.85f,
                basedOn = RecommendationType.POPULAR
            )
        )
    }

    suspend fun getLearningStreak(): LearningStreak {
        delay(200)
        return LearningStreak(
            currentStreak = 7,
            longestStreak = 15,
            lastActivityDate = LocalDateTime.now(),
            streakHistory = listOf(
                StreakDay(LocalDateTime.now(), 3, 45, 150),
                StreakDay(LocalDateTime.now().minusDays(1), 2, 30, 100),
                StreakDay(LocalDateTime.now().minusDays(2), 4, 60, 200)
            )
        )
    }

    suspend fun getContinueLesson(): LessonInfo? {
        delay(200)
        return LessonInfo(
            lessonId = "lesson_1",
            moduleId = "module_1",
            title = "Introduction to Pronunciation",
            moduleTitle = "Pronunciation Basics"
        )
    }

    suspend fun enrollInPath(pathId: String) {
        // API call to enroll in path
        delay(1000)
    }

    suspend fun unenrollFromPath(pathId: String) {
        // API call to unenroll from path
        delay(1000)
    }

    suspend fun markLessonComplete(lessonId: String) {
        // API call to mark lesson as complete
        delay(500)
    }

    suspend fun bookmarkLesson(lessonId: String) {
        // API call to bookmark lesson
        delay(300)
    }

    suspend fun getPathById(pathId: String): LearningPath? {
        delay(500)
        return generateMockLearningPaths().find { it.id == pathId }
    }

    suspend fun getLessonById(lessonId: String): Lesson? {
        delay(300)
        return generateMockLessons().find { it.id == lessonId }
    }

    private fun generateMockLearningPaths(): List<LearningPath> {
        return listOf(
            LearningPath(
                id = "path_1",
                title = "Master English Pronunciation",
                description = "Perfect your accent and sound like a native speaker with comprehensive pronunciation training",
                category = PathCategory.PRONUNCIATION,
                difficulty = DifficultyLevel.INTERMEDIATE,
                thumbnailUrl = null,
                duration = 20,
                modules = generateMockModules("path_1"),
                totalLessons = 45,
                completedLessons = 12,
                progress = 0.27f,
                isEnrolled = true,
                enrolledCount = 1250,
                rating = 4.8f,
                ratingCount = 324,
                instructor = PathInstructor(
                    id = "instructor_1",
                    name = "Dr. Sarah Johnson",
                    avatarUrl = null,
                    title = "Linguistics Professor",
                    bio = "20+ years of teaching experience",
                    rating = 4.9f,
                    studentsCount = 5000
                ),
                tags = listOf("pronunciation", "accent", "speaking"),
                estimatedCompletionDays = 30,
                prerequisites = listOf("Basic English knowledge"),
                skillsToGain = listOf("Clear pronunciation", "Accent reduction", "Intonation patterns"),
                certificateAvailable = true,
                isPremium = false,
                isRecommended = true,
                lastAccessedAt = LocalDateTime.now().minusHours(2),
                completedAt = null,
                nextLesson = LessonInfo("lesson_1", "module_1", "Vowel Sounds", "Foundation Module")
            ),
            LearningPath(
                id = "path_2",
                title = "Business English Excellence",
                description = "Master professional communication for the global workplace",
                category = PathCategory.BUSINESS_ENGLISH,
                difficulty = DifficultyLevel.UPPER_INTERMEDIATE,
                thumbnailUrl = null,
                duration = 40,
                modules = generateMockModules("path_2"),
                totalLessons = 60,
                completedLessons = 0,
                progress = 0f,
                isEnrolled = false,
                enrolledCount = 892,
                rating = 4.7f,
                ratingCount = 156,
                instructor = PathInstructor(
                    id = "instructor_2",
                    name = "Michael Chen",
                    avatarUrl = null,
                    title = "Business Communication Expert",
                    bio = "Former Fortune 500 executive",
                    rating = 4.8f,
                    studentsCount = 3200
                ),
                tags = listOf("business", "professional", "communication"),
                estimatedCompletionDays = 60,
                prerequisites = listOf("Intermediate English", "Basic business knowledge"),
                skillsToGain = listOf("Email writing", "Presentations", "Negotiations"),
                certificateAvailable = true,
                isPremium = true,
                isRecommended = false,
                lastAccessedAt = null,
                completedAt = null,
                nextLesson = null
            ),
            LearningPath(
                id = "path_3",
                title = "Conversational Fluency",
                description = "Build confidence in everyday English conversations",
                category = PathCategory.CONVERSATION,
                difficulty = DifficultyLevel.INTERMEDIATE,
                thumbnailUrl = null,
                duration = 25,
                modules = generateMockModules("path_3"),
                totalLessons = 40,
                completedLessons = 5,
                progress = 0.125f,
                isEnrolled = true,
                enrolledCount = 2100,
                rating = 4.9f,
                ratingCount = 450,
                instructor = null,
                tags = listOf("conversation", "speaking", "fluency"),
                estimatedCompletionDays = 45,
                prerequisites = listOf("Elementary English"),
                skillsToGain = listOf("Natural conversation", "Idioms", "Small talk"),
                certificateAvailable = false,
                isPremium = false,
                isRecommended = true,
                lastAccessedAt = LocalDateTime.now().minusDays(1),
                completedAt = null,
                nextLesson = LessonInfo("lesson_6", "module_2", "Daily Routines", "Everyday Topics")
            ),
            LearningPath(
                id = "path_4",
                title = "IELTS Preparation Complete",
                description = "Comprehensive preparation for IELTS Academic and General Training",
                category = PathCategory.IELTS_PREP,
                difficulty = DifficultyLevel.ADVANCED,
                thumbnailUrl = null,
                duration = 50,
                modules = generateMockModules("path_4"),
                totalLessons = 80,
                completedLessons = 0,
                progress = 0f,
                isEnrolled = false,
                enrolledCount = 1560,
                rating = 4.6f,
                ratingCount = 289,
                instructor = PathInstructor(
                    id = "instructor_3",
                    name = "Emma Williams",
                    avatarUrl = null,
                    title = "IELTS Examiner",
                    bio = "Official IELTS examiner with 15 years experience",
                    rating = 4.7f,
                    studentsCount = 4500
                ),
                tags = listOf("IELTS", "exam", "academic"),
                estimatedCompletionDays = 90,
                prerequisites = listOf("Upper-intermediate English"),
                skillsToGain = listOf("Test strategies", "Academic writing", "Critical thinking"),
                certificateAvailable = true,
                isPremium = true,
                isRecommended = false,
                lastAccessedAt = null,
                completedAt = null,
                nextLesson = null
            ),
            LearningPath(
                id = "path_5",
                title = "Grammar Fundamentals",
                description = "Build a solid foundation in English grammar",
                category = PathCategory.GRAMMAR,
                difficulty = DifficultyLevel.BEGINNER,
                thumbnailUrl = null,
                duration = 15,
                modules = generateMockModules("path_5"),
                totalLessons = 30,
                completedLessons = 30,
                progress = 1.0f,
                isEnrolled = true,
                enrolledCount = 3200,
                rating = 4.5f,
                ratingCount = 567,
                instructor = null,
                tags = listOf("grammar", "basics", "foundation"),
                estimatedCompletionDays = 30,
                prerequisites = emptyList(),
                skillsToGain = listOf("Grammar rules", "Sentence structure", "Tenses"),
                certificateAvailable = true,
                isPremium = false,
                isRecommended = false,
                lastAccessedAt = LocalDateTime.now().minusDays(7),
                completedAt = LocalDateTime.now().minusDays(3),
                nextLesson = null
            )
        )
    }

    private fun generateMockModules(pathId: String): List<LearningModule> {
        return listOf(
            LearningModule(
                id = "module_1",
                pathId = pathId,
                title = "Foundation Module",
                description = "Essential basics to get you started",
                orderIndex = 1,
                lessons = generateMockLessons().take(5),
                totalLessons = 5,
                completedLessons = 2,
                progress = 0.4f,
                duration = 150,
                isLocked = false,
                unlockRequirements = null,
                moduleType = ModuleType.THEORY
            ),
            LearningModule(
                id = "module_2",
                pathId = pathId,
                title = "Practice Module",
                description = "Apply what you've learned",
                orderIndex = 2,
                lessons = generateMockLessons().drop(5).take(5),
                totalLessons = 5,
                completedLessons = 0,
                progress = 0f,
                duration = 180,
                isLocked = false,
                unlockRequirements = "Complete Foundation Module",
                moduleType = ModuleType.PRACTICE
            ),
            LearningModule(
                id = "module_3",
                pathId = pathId,
                title = "Assessment Module",
                description = "Test your knowledge",
                orderIndex = 3,
                lessons = generateMockLessons().drop(10).take(3),
                totalLessons = 3,
                completedLessons = 0,
                progress = 0f,
                duration = 90,
                isLocked = true,
                unlockRequirements = "Complete Practice Module",
                moduleType = ModuleType.ASSESSMENT
            )
        )
    }

    private fun generateMockLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_1",
                moduleId = "module_1",
                title = "Introduction to the Topic",
                description = "Get familiar with the basic concepts",
                orderIndex = 1,
                type = LessonType.VIDEO,
                duration = 15,
                content = LessonContent(
                    videoUrl = "https://example.com/video1.mp4",
                    audioUrl = null,
                    textContent = "Welcome to this lesson...",
                    interactiveElements = null,
                    quizQuestions = null
                ),
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(2),
                score = 95,
                attempts = 1,
                isLocked = false,
                unlockRequirements = null,
                xpReward = 50,
                resources = listOf(
                    LessonResource(
                        id = "resource_1",
                        title = "Lesson Notes",
                        type = ResourceType.PDF,
                        url = "https://example.com/notes.pdf",
                        description = "Downloadable lesson notes"
                    )
                ),
                practicePrompts = null
            ),
            Lesson(
                id = "lesson_2",
                moduleId = "module_1",
                title = "Key Concepts",
                description = "Understanding the fundamentals",
                orderIndex = 2,
                type = LessonType.READING,
                duration = 20,
                content = LessonContent(
                    videoUrl = null,
                    audioUrl = null,
                    textContent = "In this lesson, we'll explore...",
                    interactiveElements = listOf(
                        InteractiveElement(
                            id = "interactive_1",
                            type = InteractiveType.FILL_IN_BLANK,
                            content = "The capital of France is ___",
                            correctResponse = "Paris",
                            hints = listOf("It's known as the City of Light")
                        )
                    ),
                    quizQuestions = null
                ),
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(1),
                score = 88,
                attempts = 2,
                isLocked = false,
                unlockRequirements = null,
                xpReward = 60,
                resources = emptyList(),
                practicePrompts = null
            )
            // Add more lessons as needed
        )
    }
}
