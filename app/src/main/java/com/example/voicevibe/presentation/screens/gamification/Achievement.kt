package com.example.voicevibe.presentation.screens.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.domain.model.ActivityType
import com.example.voicevibe.domain.model.UserActivity
import com.example.voicevibe.domain.model.Resource
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.util.Log

@HiltViewModel
class AchievementsSimpleViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val tokenManager: TokenManager,
    private val speakingJourneyRepository: SpeakingJourneyRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    var uiState by mutableStateOf(AchievementsSimpleUiState())
        private set

    private var lastKnownProficiency: String? = null
    private var lastKnownLevel: Int? = null
    private var currentUserId: String? = null

    init {
        loadAndRefresh()
    }

    private fun loadAndRefresh() {
        viewModelScope.launch {
            // First, load existing history and last-known values (MUST complete first)
            currentUserId = tokenManager.getUserIdFlow().first()?.trim()?.takeIf { it.isNotEmpty() }
            Log.d("AchievementsVM", "[loadAndRefresh] currentUserId=$currentUserId")
            
            // Only load if we have a valid user ID to prevent cross-user data contamination
            if (currentUserId != null) {
                val history = tokenManager.achievementHistoryFlow(currentUserId!!).first()
                lastKnownProficiency = tokenManager.lastProficiencyFlow(currentUserId!!).first()
                lastKnownLevel = tokenManager.lastLevelFlow(currentUserId!!).first()
                Log.d("AchievementsVM", "[loadAndRefresh] Loaded ${history.size} cached achievements for user $currentUserId")
                uiState = uiState.copy(items = history.sortedByDescending { it.timestamp })
            } else {
                // No user ID = fresh state, don't load any cached data
                Log.d("AchievementsVM", "[loadAndRefresh] No userId, returning empty state")
                uiState = uiState.copy(items = emptyList())
                lastKnownProficiency = null
                lastKnownLevel = null
            }
            
            // Then check for new achievements
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                // 1) Try server-side persistent feed first
                runCatching {
                    val res = gamificationRepository.getAchievementEvents(limit = 100)
                    Log.d("AchievementsVM", "[load] Backend response: ${if (res is Resource.Success) "${res.data?.size ?: 0} events" else "error: ${(res as? Resource.Error)?.message}"}")
                    if (res is Resource.Success) {
                        val dtos = res.data ?: emptyList()
                        val mapped = dtos.mapNotNull { dto ->
                            val t = parseTimestamp(dto.timestamp)
                            when (dto.event_type.uppercase()) {
                                "LEVEL_UP" -> AchievementFeedItem(
                                    type = AchievementItemType.LEVEL,
                                    title = dto.title,
                                    timestamp = t,
                                    timeAgo = formatRelativeTime(t)
                                )
                                "PROFICIENCY_TIER" -> AchievementFeedItem(
                                    type = AchievementItemType.PROFICIENCY,
                                    title = dto.title,
                                    timestamp = t,
                                    timeAgo = formatRelativeTime(t)
                                )
                                else -> null // Ignore other event types for now (e.g., TOPIC_COMPLETED)
                            }
                        }.sortedByDescending { it.timestamp }

                        if (mapped.isNotEmpty() && currentUserId != null) {
                            Log.d("AchievementsVM", "[load] Saving ${mapped.size} achievements from backend for user $currentUserId")
                            tokenManager.saveAchievementHistory(currentUserId!!, mapped)
                            val itemsWithUpdatedTime = mapped.map { it.copy(timeAgo = formatRelativeTime(it.timestamp)) }
                            uiState = uiState.copy(isLoading = false, items = itemsWithUpdatedTime)
                            return@launch
                        } else {
                            Log.d("AchievementsVM", "[load] Backend returned ${mapped.size} achievements but currentUserId=$currentUserId, not saving")
                        }
                    }
                }

                // 2) Fallback: infer from profile + activities (legacy local logic)
                val profile = profileRepository.getProfile()
                val newAchievements = mutableListOf<AchievementFeedItem>()
                val now = LocalDateTime.now()
                // Determine if this is an initial load (after login or cleared storage)
                val isInitialProficiency = (lastKnownProficiency == null)
                val isInitialLevel = (lastKnownLevel == null)
                // Opportunistically fetch activities once if we need to seed from history
                var activities: List<UserActivity> = emptyList()
                if ((isInitialLevel && (profile.currentLevel ?: 0) > 0) || (!profile.currentProficiency.isNullOrBlank() && isInitialProficiency)) {
                    val res = speakingJourneyRepository.getActivities(limit = 100)
                    activities = res.getOrNull() ?: emptyList()
                }

                // Check for new proficiency achievement
                val prof = profile.currentProficiency?.trim()
                if (!prof.isNullOrBlank() && prof != lastKnownProficiency) {
                    val isInitial = isInitialProficiency
                    val profTitle = "Achieved ${prof.replaceFirstChar { it.uppercase() }} proficiency"
                    // Create a feed item using historical timestamp if initial and available; otherwise only on subsequent changes
                    if (isInitial) {
                        // Try to backfill from activities (ACHIEVEMENT_UNLOCKED/BADGE_EARNED containing 'proficiency' and the target prof)
                        val profEvent = activities
                            .filter { it.type == ActivityType.ACHIEVEMENT_UNLOCKED || it.type == ActivityType.BADGE_EARNED }
                            .filter { it.title.contains("proficiency", ignoreCase = true) && it.title.contains(prof, ignoreCase = true) }
                            .maxByOrNull { it.timestamp }
                        if (profEvent != null) {
                            if (uiState.items.none { it.title == profTitle }) {
                                val t = profEvent.timestamp
                                newAchievements.add(
                                    AchievementFeedItem(
                                        type = AchievementItemType.PROFICIENCY,
                                        title = profTitle,
                                        timestamp = t,
                                        timeAgo = formatRelativeTime(t)
                                    )
                                )
                            }
                        } else {
                            // Fallback: attempt backfill from profile.recentAchievements (earned_at)
                            val fromRecent = (profile.recentAchievements ?: emptyList())
                                .filter { it.badge.category.equals("proficiency", ignoreCase = true) || it.badge.name.contains("proficiency", ignoreCase = true) }
                                .filter { (it.badge.tierDisplay?.equals(prof, ignoreCase = true) == true) || it.badge.name.contains(prof, ignoreCase = true) }
                                .mapNotNull { ach ->
                                    val t = try {
                                        LocalDateTime.parse(ach.earnedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    } catch (_: Exception) {
                                        runCatching { OffsetDateTime.parse(ach.earnedAt).toLocalDateTime() }.getOrNull()
                                    }
                                    t?.let { it to ach }
                                }
                                .maxByOrNull { it.first }
                            fromRecent?.let { pair ->
                                if (uiState.items.none { it.title == profTitle }) {
                                    val t = pair.first
                                    newAchievements.add(
                                        AchievementFeedItem(
                                            type = AchievementItemType.PROFICIENCY,
                                            title = profTitle,
                                            timestamp = t,
                                            timeAgo = formatRelativeTime(t)
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Only add if this exact achievement doesn't already exist
                        val existingAchievement = uiState.items.find { it.title == profTitle }
                        if (existingAchievement == null) {
                            val achievement = AchievementFeedItem(
                                type = AchievementItemType.PROFICIENCY,
                                title = profTitle,
                                timestamp = now,
                                timeAgo = "Just now"
                            )
                            newAchievements.add(achievement)
                        }
                    }
                    lastKnownProficiency = prof
                    if (currentUserId != null) {
                        tokenManager.setLastProficiency(currentUserId!!, prof)
                    }
                }

                // Check for new level achievement
                val level = profile.currentLevel
                if (level != null && level > 0 && level != lastKnownLevel) {
                    val initialLevel = isInitialLevel
                    val levelTitle = "Reached level $level"
                    // Create a feed item using historical timestamp if initial and available; otherwise only on subsequent changes
                    if (initialLevel) {
                        // Prefer the activity explicitly matching this level; fallback to latest LEVEL_UP
                        val specific = activities
                            .filter { it.type == ActivityType.LEVEL_UP && it.title.contains("level $level", ignoreCase = true) }
                            .maxByOrNull { it.timestamp }
                        val chosen = specific ?: activities
                            .filter { it.type == ActivityType.LEVEL_UP }
                            .maxByOrNull { it.timestamp }
                        if (chosen != null) {
                            if (uiState.items.none { it.title == levelTitle }) {
                                val t = chosen.timestamp
                                newAchievements.add(
                                    AchievementFeedItem(
                                        type = AchievementItemType.LEVEL,
                                        title = levelTitle,
                                        timestamp = t,
                                        timeAgo = formatRelativeTime(t)
                                    )
                                )
                            }
                        } else {
                            // Fallback: attempt backfill from profile.recentAchievements (earned_at)
                            val fromRecent = (profile.recentAchievements ?: emptyList())
                                .filter { it.badge.category.equals("level", ignoreCase = true) || it.badge.name.contains("level", ignoreCase = true) }
                                .filter { (it.badge.tier == level) || it.badge.name.contains("level $level", ignoreCase = true) }
                                .mapNotNull { ach ->
                                    val t = try {
                                        LocalDateTime.parse(ach.earnedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    } catch (_: Exception) {
                                        runCatching { OffsetDateTime.parse(ach.earnedAt).toLocalDateTime() }.getOrNull()
                                    }
                                    t?.let { it to ach }
                                }
                                .maxByOrNull { it.first }
                            fromRecent?.let { pair ->
                                if (uiState.items.none { it.title == levelTitle }) {
                                    val t = pair.first
                                    newAchievements.add(
                                        AchievementFeedItem(
                                            type = AchievementItemType.LEVEL,
                                            title = levelTitle,
                                            timestamp = t,
                                            timeAgo = formatRelativeTime(t)
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Only add if this exact achievement doesn't already exist
                        val existingAchievement = uiState.items.find { it.title == levelTitle }
                        if (existingAchievement == null) {
                            val achievement = AchievementFeedItem(
                                type = AchievementItemType.LEVEL,
                                title = levelTitle,
                                timestamp = now,
                                timeAgo = "Just now"
                            )
                            newAchievements.add(achievement)
                        }
                    }
                    lastKnownLevel = level
                    if (currentUserId != null) {
                        tokenManager.setLastLevel(currentUserId!!, level)
                    }
                }

                // Add new achievements to existing list and save (only if we have a user ID)
                if (newAchievements.isNotEmpty() && currentUserId != null) {
                    val updatedList = (newAchievements + uiState.items)
                        .sortedByDescending { it.timestamp }
                    tokenManager.saveAchievementHistory(currentUserId!!, updatedList)
                    uiState = uiState.copy(items = updatedList)
                }

                // Update relative times for all items
                val itemsWithUpdatedTime = uiState.items.map { item ->
                    item.copy(timeAgo = formatRelativeTime(item.timestamp))
                }
                uiState = uiState.copy(isLoading = false, items = itemsWithUpdatedTime)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Failed to load achievements")
            }
        }
    }

    private fun formatRelativeTime(timestamp: LocalDateTime): String {
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(timestamp, now)
        val hours = ChronoUnit.HOURS.between(timestamp, now)
        val days = ChronoUnit.DAYS.between(timestamp, now)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            hours < 24 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            days < 7 -> "$days ${if (days == 1L) "day" else "days"} ago"
            days < 30 -> "${days / 7} ${if (days / 7 == 1L) "week" else "weeks"} ago"
            days < 365 -> "${days / 30} ${if (days / 30 == 1L) "month" else "months"} ago"
            else -> "${days / 365} ${if (days / 365 == 1L) "year" else "years"} ago"
        }
    }

    private fun parseTimestamp(raw: String): LocalDateTime {
        return try {
            // Try Offset first (e.g., 2025-10-16T06:44:23.45Z), then fall back to local
            OffsetDateTime.parse(raw).toLocalDateTime()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                LocalDateTime.now()
            }
        }
    }
}

data class AchievementsSimpleUiState(
    val isLoading: Boolean = false,
    val items: List<AchievementFeedItem> = emptyList(),
    val error: String? = null
)

enum class AchievementItemType { PROFICIENCY, LEVEL }

data class AchievementFeedItem(
    val type: AchievementItemType,
    val title: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val timeAgo: String = "Just now"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsSimpleViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Achievements",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack, 
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when {
                state.isLoading -> {
                    LoadingState(modifier = Modifier.padding(padding))
                }
                state.error != null -> {
                    SimpleError(
                        message = state.error,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.padding(padding)
                    )
                }
                else -> {
                    if (state.items.isEmpty()) {
                        EmptyAchievements(modifier = Modifier.padding(padding))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    InfoCard()
                                }
                            }
                            itemsIndexed(state.items) { index, item ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = index * 100)) + 
                                            slideInHorizontally(animationSpec = tween(300, delayMillis = index * 100))
                                ) {
                                    AchievementRow(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading achievements...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementFeedItem) {
    val (icon, gradient) = when (item.type) {
        AchievementItemType.PROFICIENCY -> Icons.Filled.WorkspacePremium to listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
        AchievementItemType.LEVEL -> Icons.Filled.School to listOf(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    }
    
    val infiniteTransition = rememberInfiniteTransition()
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = gradient[0].copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Background gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = gradient.map { it.copy(alpha = 0.1f * shimmer) }
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(gradient)
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.timeAgo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Achieved",
                    tint = gradient[0],
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.Stars,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Achievement Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Track your progress through proficiency levels and milestones as you advance in your learning journey.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}


@Composable
private fun SimpleError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) { 
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyAchievements(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                Icons.Outlined.EmojiEvents,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = floatAnimation.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Your Journey Begins Here",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Start practicing to unlock amazing achievements and showcase your progress!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = { /* Navigation handled elsewhere */ },
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Practicing", fontWeight = FontWeight.SemiBold)
        }
    }
}