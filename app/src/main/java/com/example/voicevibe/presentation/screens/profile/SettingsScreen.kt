package com.example.voicevibe.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.SubcomposeAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToQA: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTermsOfService: () -> Unit,
    onNavigateToCommunityGuidelines: () -> Unit,
    onNavigateToImageCrop: (String) -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var autoPlayEnabled by remember { mutableStateOf(true) }
    var offlineMode by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var selectedVoiceName: String? by remember { mutableStateOf(null) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var selectedAccent: String? by remember { mutableStateOf(null) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Navigate to crop screen with encoded URI
            val encodedUri = android.net.Uri.encode(uri.toString())
            onNavigateToImageCrop(encodedUri)
        }
    }

    // Refresh profile when screen resumes (e.g., returning from image crop)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1128),
            Color(0xFF1E2761),
            Color(0xFF0A1128)
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // User Profile Section
            UserProfileSection(
                userName = viewModel.userName.value,
                userEmail = if (viewModel.showEmailOnProfile.value) viewModel.userEmail.value else "",
                membershipStatus = viewModel.membershipStatus.value,
                userInitials = viewModel.userInitials.value,
                avatarUrl = viewModel.avatarUrl.value,
                isUploadingAvatar = viewModel.isUploadingAvatar.value,
                onChangeAvatar = { imagePickerLauncher.launch("image/*") }
            )

            // General
            SettingsSection(title = "General") {
                SettingsItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Update your profile information",
                    onClick = onNavigateToEditProfile
                )
            }

            // Account Settings
            SettingsSection(title = "Account Settings") {
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Change Email",
                    subtitle = "Update your email address",
                    onClick = onNavigateToAccountSettings
                )
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your password",
                    onClick = onNavigateToAccountSettings
                )
            }

            // Learning Preferences
            SettingsSection(title = "Learning Preferences") {
                // Feature flag: Speaking-only Journey (beta)
                SettingsToggleItem(
                    icon = Icons.Default.Mic,
                    title = "Speaking-only Journey",
                    subtitle = "Enable the new speaking-only flow",
                    checked = viewModel.speakingOnlyEnabled.value,
                    onCheckedChange = { viewModel.onToggleSpeakingOnly(it) },
                    enabled = !com.example.voicevibe.utils.Constants.LOCK_SPEAKING_ONLY_ON
                )
                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = "Preferred Voice",
                    subtitle = viewModel.ttsVoiceId.value ?: "Default",
                    onClick = {
                        selectedVoiceName = viewModel.ttsVoiceId.value
                        showVoiceDialog = true
                    }
                )
                SettingsItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Voice Accent",
                    subtitle = viewModel.voiceAccent.value ?: "Default",
                    onClick = {
                        selectedAccent = viewModel.voiceAccent.value
                        showAccentDialog = true
                    }
                )
            }

            // Privacy & Safety
            SettingsSection(title = "Privacy & Safety") {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Privacy Settings",
                    subtitle = "Control your privacy and visibility",
                    onClick = onNavigateToPrivacySettings
                )
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Blocked Users",
                    subtitle = "Manage blocked users",
                    onClick = onNavigateToBlockedUsers
                )
                SettingsItem(
                    icon = Icons.Default.Report,
                    title = "My Reports",
                    subtitle = "View your submitted reports",
                    onClick = onNavigateToMyReports
                )
                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = onNavigateToPrivacyPolicy
                )
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "View terms and conditions",
                    onClick = onNavigateToTermsOfService
                )
                SettingsItem(
                    icon = Icons.Default.Article,
                    title = "Community Guidelines",
                    subtitle = "Learn about our community standards",
                    onClick = onNavigateToCommunityGuidelines
                )
            }

            // Support & About
            SettingsSection(title = "Support & About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "VozVibe v1.0",
                    onClick = onNavigateToAbout
                )
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Q & A",
                    subtitle = "Frequently asked questions",
                    onClick = onNavigateToQA
                )
            }

            // Logout Button
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontSize = 16.sp)
            }

                // Bottom spacing
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Voice Accent Dialog
    if (showAccentDialog) {
        val accentOptions: List<Pair<String, String?>> = listOf(
            "Default (Current)" to null,
            "American Accent" to "American",
            "British Accent" to "British",
            "Australian Accent" to "Australian",
            "Indian Accent" to "Indian",
            "Singaporean Accent" to "Singaporean"
        )

        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Voice Accent", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Choose the accent Vivi should use when speaking in Live sessions.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    accentOptions.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedAccent = value }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedAccent == value) || (value == null && selectedAccent == null),
                                onClick = { selectedAccent = value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setVoiceAccent(selectedAccent)
                        showAccentDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAccentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Preferred TTS Voice Dialog (Gemini voices)
    if (showVoiceDialog) {
        val voiceOptions: List<Pair<String, String?>> = listOf(
            "Default (Current)" to null,
            "Zephyr — Bright" to "Zephyr",
            "Puck — Upbeat" to "Puck",
            "Charon — Informative" to "Charon",
            "Kore — Firm" to "Kore",
            "Fenrir — Excitable" to "Fenrir",
            "Leda — Youthful" to "Leda",
            "Orus — Firm" to "Orus",
            "Aoede — Breezy" to "Aoede",
            "Callirrhoe — Easy-going" to "Callirrhoe",
            "Autonoe — Bright" to "Autonoe",
            "Enceladus — Breathy" to "Enceladus",
            "Iapetus — Clear" to "Iapetus",
            "Umbriel — Easy-going" to "Umbriel",
            "Algieba — Smooth" to "Algieba",
            "Despina — Smooth" to "Despina",
            "Erinome — Clear" to "Erinome",
            "Algenib — Gravelly" to "Algenib",
            "Rasalgethi — Informative" to "Rasalgethi",
            "Laomedeia — Upbeat" to "Laomedeia",
            "Achernar — Soft" to "Achernar",
            "Alnilam — Firm" to "Alnilam",
            "Schedar — Even" to "Schedar",
            "Gacrux — Mature" to "Gacrux",
            "Pulcherrima — Forward" to "Pulcherrima",
            "Achird — Friendly" to "Achird",
            "Zubenelgenubi — Casual" to "Zubenelgenubi",
            "Vindemiatrix — Gentle" to "Vindemiatrix",
            "Sadachbia — Lively" to "Sadachbia",
            "Sadaltager — Knowledgeable" to "Sadaltager",
            "Sulafat — Warm" to "Sulafat"
        )

        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Preferred TTS Voice", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Choose a Gemini voice for both TTS and Live API. You can restore the current default by selecting ‘Default (Current)’.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    voiceOptions.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedVoiceName = value }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedVoiceName == value) || (value == null && selectedVoiceName == null),
                                onClick = { selectedVoiceName = value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setPreferredTtsVoice(selectedVoiceName)
                        showVoiceDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onNavigateBack: () -> Unit
) {
    // Reuse EditProfileViewModel for email/password update logic
    val editProfileViewModel: EditProfileViewModel = hiltViewModel()
    val scrollState = rememberScrollState()
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Change Email (no wrapper section)
            EmailUpdateSection(viewModel = editProfileViewModel)

            Divider()

            // Change Password (no wrapper section)
            PasswordUpdateSection(viewModel = editProfileViewModel)

            Divider()

            // Delete Account Section
            DeleteAccountSection(onDeleteClick = { showDeleteAccountDialog = true })

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            viewModel = editProfileViewModel,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirmDelete = {
                editProfileViewModel.deleteAccount()
                // Note: Navigation to login will be handled by the calling screen
                // when it observes the deletion success state
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("About", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Logo/Name Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VozVibe",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "v1.0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Description Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "About VozVibe",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "VozVibe is your AI-powered companion for mastering English speaking skills. Our platform combines cutting-edge speech recognition, real-time pronunciation feedback, and interactive conversations to help you speak English with confidence.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Features:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeatureBullet("AI-powered pronunciation analysis")
                        FeatureBullet("Interactive conversation practice")
                        FeatureBullet("Vocabulary lessons with real-time feedback")
                        FeatureBullet("Progress tracking and gamification")
                        FeatureBullet("Social learning community")
                    }
                }
            }

            // Contact Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Contact Us",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "VozVibe Team 2025",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "vozvibe2025@gmail.com",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }


            // Copyright
            Text(
                text = "© 2025 VozVibe. All rights reserved.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QAScreen(onNavigateBack: () -> Unit) {
    var expandedItems by remember { mutableStateOf(setOf<Int>()) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Q & A", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Frequently Asked Questions",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Find answers to common questions about VozVibe. Tap any question to expand the answer.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Getting Started
            Text(
                text = "Getting Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            QAItem(
                index = 0,
                question = "What is VozVibe?",
                answer = "VozVibe is an AI-powered English learning platform designed to help you master speaking skills. We combine cutting-edge speech recognition, real-time pronunciation feedback, fluency analysis, and interactive conversations to help you speak English with confidence.",
                isExpanded = 0 in expandedItems,
                onToggle = { expandedItems = if (0 in expandedItems) expandedItems - 0 else expandedItems + 0 }
            )
            
            QAItem(
                index = 1,
                question = "How do I get started?",
                answer = "After creating your account, you'll see the Speaking Journey with various topics. Start with the first unlocked topic and practice pronunciation, fluency, and vocabulary. Complete all practice modes to unlock new topics and progress through your learning journey.",
                isExpanded = 1 in expandedItems,
                onToggle = { expandedItems = if (1 in expandedItems) expandedItems - 1 else expandedItems + 1 }
            )
            
            QAItem(
                index = 2,
                question = "What are the system requirements?",
                answer = "VozVibe requires Android 7.0 (Nougat) or higher. You'll also need:\n• Stable internet connection for AI features\n• Microphone access for speech practice\n• At least 200 MB of free storage\n• Recommended: Headphones for best audio experience",
                isExpanded = 2 in expandedItems,
                onToggle = { expandedItems = if (2 in expandedItems) expandedItems - 2 else expandedItems + 2 }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Learning Features
            Text(
                text = "Learning Features",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            QAItem(
                index = 3,
                question = "What practice modes are available?",
                answer = "Each topic includes five practice modes:\n\n• Pronunciation Practice: Record and get AI feedback on your pronunciation accuracy\n• Fluency Practice: Speak freely and receive analysis on your speaking speed, pauses, and natural flow\n• Vocabulary Practice: Interactive AI-generated quizzes to learn topic-specific words\n• Conversation Practice: Engage in realistic dialogues with TTS audio\n• Vocabulary Lesson: Get detailed AI explanations with pronunciation guides",
                isExpanded = 3 in expandedItems,
                onToggle = { expandedItems = if (3 in expandedItems) expandedItems - 3 else expandedItems + 3 }
            )
            
            QAItem(
                index = 4,
                question = "How does the Speaking Journey work?",
                answer = "The Speaking Journey is a structured learning path with 60+ topics organized by difficulty. Topics unlock sequentially as you complete all practice modes with at least 75% average score. Track your progress on each topic and see your overall proficiency grow from Chaucerite to The Bard Eternal!",
                isExpanded = 4 in expandedItems,
                onToggle = { expandedItems = if (4 in expandedItems) expandedItems - 4 else expandedItems + 4 }
            )
            
            QAItem(
                index = 5,
                question = "How accurate is the pronunciation feedback?",
                answer = "VozVibe uses state-of-the-art AI models (OpenAI Whisper for transcription and SpeechBrain for phoneme analysis) combined with Google Gemini 2.5 to provide detailed, context-aware feedback. You'll get:\n• Overall accuracy percentage\n• Word-by-word pronunciation analysis\n• Specific tips on weak sounds\n• Comparison to native pronunciation",
                isExpanded = 5 in expandedItems,
                onToggle = { expandedItems = if (5 in expandedItems) expandedItems - 5 else expandedItems + 5 }
            )
            
            QAItem(
                index = 6,
                question = "Can I customize the AI voice?",
                answer = "Yes! Go to Settings > Learning Preferences to choose from 30+ Gemini TTS voices (like Zephyr, Puck, Aoede) with different tones and characteristics. You can also select accent preferences (American, British, Australian, Indian, Singaporean) for Live sessions.",
                isExpanded = 6 in expandedItems,
                onToggle = { expandedItems = if (6 in expandedItems) expandedItems - 6 else expandedItems + 6 }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Gamification & Progress
            Text(
                text = "Gamification & Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            QAItem(
                index = 7,
                question = "How does the XP and leveling system work?",
                answer = "Earn XP by completing practice activities:\n• Pronunciation: Up to 20 XP per phrase (based on accuracy)\n• Fluency: 10 XP per prompt with 80%+ score\n• Vocabulary: 5 XP per correct answer + 20 XP completion bonus\n• Topic Completion: 50 XP bonus\n\nYour level increases as you accumulate XP, unlocking new proficiency tiers and badges.",
                isExpanded = 7 in expandedItems,
                onToggle = { expandedItems = if (7 in expandedItems) expandedItems - 7 else expandedItems + 7 }
            )
            
            QAItem(
                index = 8,
                question = "What are proficiency tiers?",
                answer = "Your proficiency tier reflects your overall progress:\n\n🌱 Chaucerite (0-10 topics)\n🎭 Shakespire (11-20 topics)\n🔥 Miltonarch (21-30 topics)\n💫 Austennova (31-40 topics)\n📚 Dickenlord (41-50 topics)\n🌀 Joycemancer (51-60 topics)\n👑 The Bard Eternal (61+ topics)\n\nComplete topics to advance through these literary-inspired ranks!",
                isExpanded = 8 in expandedItems,
                onToggle = { expandedItems = if (8 in expandedItems) expandedItems - 8 else expandedItems + 8 }
            )
            
            QAItem(
                index = 9,
                question = "How do Leaderboards work?",
                answer = "Compete with other learners on three leaderboards:\n• Daily: XP earned today (resets at midnight)\n• Weekly: XP earned this week (Monday-Sunday)\n• Monthly: XP earned this month\n\nYour rank updates in real-time, and you can view top performers plus your own position. Use leaderboards for motivation and healthy competition!",
                isExpanded = 9 in expandedItems,
                onToggle = { expandedItems = if (9 in expandedItems) expandedItems - 9 else expandedItems + 9 }
            )
            
            QAItem(
                index = 10,
                question = "What is a streak and how do I maintain it?",
                answer = "A streak counts consecutive days you've practiced. Open VozVibe and complete any practice activity (pronunciation, fluency, or vocabulary) each day to keep your streak alive. Streaks are displayed on your profile and contribute to your achievements!",
                isExpanded = 10 in expandedItems,
                onToggle = { expandedItems = if (10 in expandedItems) expandedItems - 10 else expandedItems + 10 }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Social Features
            Text(
                text = "Social Features",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            QAItem(
                index = 11,
                question = "Can I interact with other learners?",
                answer = "Yes! VozVibe includes a social learning community where you can:\n• Create posts and share your progress\n• Like and comment on others' posts\n• Follow users to see their activities\n• View other users' profiles and learning journey\n• Receive notifications for interactions\n• Send direct messages (coming soon)",
                isExpanded = 11 in expandedItems,
                onToggle = { expandedItems = if (11 in expandedItems) expandedItems - 11 else expandedItems + 11 }
            )
            
            QAItem(
                index = 12,
                question = "How do I find other users?",
                answer = "Tap the search icon in the Home screen to find users by username or display name. You can also discover users through the social feed when they post content or appear on the leaderboard.",
                isExpanded = 12 in expandedItems,
                onToggle = { expandedItems = if (12 in expandedItems) expandedItems - 12 else expandedItems + 12 }
            )
            
            QAItem(
                index = 13,
                question = "What are the privacy options?",
                answer = "Control your privacy in Settings > Privacy & Safety:\n• Privacy Settings: Manage who can see your profile and activities\n• Blocked Users: View and manage users you've blocked\n• Report inappropriate content or users\n• Hide email visibility on profile\n\nReview our Privacy Policy and Community Guidelines for more details.",
                isExpanded = 13 in expandedItems,
                onToggle = { expandedItems = if (13 in expandedItems) expandedItems - 13 else expandedItems + 13 }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Account & Technical
            Text(
                text = "Account & Technical",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            QAItem(
                index = 14,
                question = "How do I change my email or password?",
                answer = "Go to Settings > Account Settings to update your email or password. For email changes, enter your new email and save. For password changes, you'll be prompted to enter your current password and a new one. If you forgot your password, use the 'Forgot Password' link on the login screen.",
                isExpanded = 14 in expandedItems,
                onToggle = { expandedItems = if (14 in expandedItems) expandedItems - 14 else expandedItems + 14 }
            )
            
            QAItem(
                index = 15,
                question = "Can I use VozVibe offline?",
                answer = "Most features require an internet connection because they rely on cloud-based AI processing (Whisper for transcription, Gemini for feedback and TTS). However, you can view your past progress, statistics, and profile data offline. We recommend using WiFi for best experience and to save mobile data.",
                isExpanded = 15 in expandedItems,
                onToggle = { expandedItems = if (15 in expandedItems) expandedItems - 15 else expandedItems + 15 }
            )
            
            QAItem(
                index = 16,
                question = "How do I update my profile picture (avatar)?",
                answer = "Tap your avatar in Settings or Profile screen, select 'Change Avatar', choose an image from your device, crop it as desired, and save. Your new avatar will sync across all your activities and be visible to other users (unless you hide it in Privacy Settings).",
                isExpanded = 16 in expandedItems,
                onToggle = { expandedItems = if (16 in expandedItems) expandedItems - 16 else expandedItems + 16 }
            )
            
            QAItem(
                index = 17,
                question = "What happens if I delete my account?",
                answer = "Account deletion is permanent and irreversible. All your data will be deleted, including:\n• Profile information\n• Learning progress and scores\n• Recordings and evaluations\n• Social connections and posts\n\nTo delete your account, go to Settings > Account Settings > Delete Account and follow the confirmation steps.",
                isExpanded = 17 in expandedItems,
                onToggle = { expandedItems = if (17 in expandedItems) expandedItems - 17 else expandedItems + 17 }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Troubleshooting
            Text(
                text = "Troubleshooting",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            QAItem(
                index = 18,
                question = "The app isn't recognizing my speech. What should I do?",
                answer = "Try these solutions:\n• Check microphone permissions in device settings\n• Ensure you're in a quiet environment\n• Speak clearly and at moderate volume\n• Check your internet connection\n• Try closing and reopening the app\n• If using headphones, ensure mic is working\n• Restart your device if issues persist",
                isExpanded = 18 in expandedItems,
                onToggle = { expandedItems = if (18 in expandedItems) expandedItems - 18 else expandedItems + 18 }
            )
            
            QAItem(
                index = 19,
                question = "Why is the app slow or laggy?",
                answer = "AI processing can take a few seconds, especially for transcription and feedback generation. To improve performance:\n• Ensure stable WiFi connection (4G/5G works but may be slower)\n• Close other apps running in background\n• Clear app cache in device settings\n• Update to the latest VozVibe version\n• The first transcription after opening may be slower (model warmup)",
                isExpanded = 19 in expandedItems,
                onToggle = { expandedItems = if (19 in expandedItems) expandedItems - 19 else expandedItems + 19 }
            )
            
            QAItem(
                index = 20,
                question = "I found a bug or have a feature request. Where can I report it?",
                answer = "We'd love to hear from you! Contact the VozVibe Team at:\n\nEmail: vozvibe2025@gmail.com\n\nPlease include:\n• Your device model and Android version\n• Detailed description of the issue\n• Steps to reproduce (for bugs)\n• Screenshots if applicable\n\nWe review all feedback and continuously work to improve VozVibe!",
                isExpanded = 20 in expandedItems,
                onToggle = { expandedItems = if (20 in expandedItems) expandedItems - 20 else expandedItems + 20 }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Still have questions?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Contact us at vozvibe2025@gmail.com and we'll be happy to help!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QAItem(
    index: Int,
    question: String,
    answer: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer 
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            if (isExpanded) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Text(
                    text = answer,
                    fontSize = 14.sp,
                    color = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer 
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    var showDisplayNameDialog by remember { mutableStateOf(false) }
    var showAboutMeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Show Email Toggle
            SettingsToggleItem(
                icon = Icons.Default.Email,
                title = "Show Email",
                subtitle = "Display your email on Profile Screen",
                checked = settingsViewModel.showEmailOnProfile.value,
                onCheckedChange = { settingsViewModel.setShowEmailOnProfile(it) }
            )

            // Change Display Name
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Change Display Name",
                subtitle = viewModel.currentDisplayName.value.ifEmpty { "Not set" },
                onClick = { showDisplayNameDialog = true }
            )

            // About Me
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About Me",
                subtitle = viewModel.aboutMe.value.ifEmpty { "Tell others about yourself" }.take(50) + if (viewModel.aboutMe.value.length > 50) "..." else "",
                onClick = { showAboutMeDialog = true }
            )
        }
    }

    // Display Name Dialog
    if (showDisplayNameDialog) {
        DisplayNameDialog(
            viewModel = viewModel,
            onDismiss = { showDisplayNameDialog = false }
        )
    }

    // About Me Dialog
    if (showAboutMeDialog) {
        AboutMeDialog(
            viewModel = viewModel,
            onDismiss = { showAboutMeDialog = false }
        )
    }
}

@Composable
private fun EmailUpdateSection(viewModel: EditProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Change Email",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Current: ${viewModel.currentEmail.value}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = viewModel.newEmail.value,
            onValueChange = { viewModel.onEmailChanged(it) },
            label = { Text("New Email") },
            placeholder = { Text("Enter new email address") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = viewModel.emailUpdateError.value != null,
            supportingText = {
                viewModel.emailUpdateError.value?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        if (viewModel.emailUpdateSuccess.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Email updated successfully",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = { viewModel.updateEmail() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isUpdatingEmail.value && viewModel.newEmail.value.isNotBlank(),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (viewModel.isUpdatingEmail.value) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Update Email")
            }
        }
    }
}

@Composable
private fun DisplayNameUpdateSection(viewModel: EditProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Change Display Name",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Current: ${viewModel.currentDisplayName.value.ifEmpty { "Not set" }}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = viewModel.newDisplayName.value,
            onValueChange = { viewModel.onDisplayNameChanged(it) },
            label = { Text("New Display Name") },
            placeholder = { Text("Enter new display name") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = "Display Name")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = viewModel.displayNameUpdateError.value != null,
            supportingText = {
                viewModel.displayNameUpdateError.value?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        if (viewModel.displayNameUpdateSuccess.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Display name updated successfully",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = { viewModel.updateDisplayName() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isUpdatingDisplayName.value && viewModel.newDisplayName.value.isNotBlank(),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (viewModel.isUpdatingDisplayName.value) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Update Display Name")
            }
        }
    }
}

@Composable
private fun PasswordUpdateSection(viewModel: EditProfileViewModel) {
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Change Password",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = viewModel.currentPassword.value,
            onValueChange = { viewModel.onCurrentPasswordChanged(it) },
            label = { Text("Current Password") },
            placeholder = { Text("Enter current password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            },
            trailingIcon = {
                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                    Icon(
                        if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = viewModel.newPassword.value,
            onValueChange = { viewModel.onNewPasswordChanged(it) },
            label = { Text("New Password") },
            placeholder = { Text("Enter new password (min. 6 characters)") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            },
            trailingIcon = {
                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(
                        if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = viewModel.confirmPassword.value,
            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
            label = { Text("Confirm New Password") },
            placeholder = { Text("Re-enter new password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = viewModel.passwordUpdateError.value != null,
            supportingText = {
                viewModel.passwordUpdateError.value?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        if (viewModel.passwordUpdateSuccess.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Password changed successfully",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = { viewModel.updatePassword() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isUpdatingPassword.value &&
                    viewModel.currentPassword.value.isNotBlank() &&
                    viewModel.newPassword.value.isNotBlank() &&
                    viewModel.confirmPassword.value.isNotBlank(),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (viewModel.isUpdatingPassword.value) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Change Password")
            }
        }
    }
}

@Composable
fun UserProfileSection(
    userName: String,
    userEmail: String,
    membershipStatus: String,
    userInitials: String,
    avatarUrl: String?,
    isUploadingAvatar: Boolean,
    onChangeAvatar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onChangeAvatar() },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Text(
                                text = userInitials,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        },
                        error = {
                            Text(
                                text = userInitials,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    )
                } else {
                    Text(
                        text = userInitials,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                if (isUploadingAvatar) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (userEmail.isNotBlank()) {
                    Text(
                        text = userEmail,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = membershipStatus,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Edit Icon
            IconButton(onClick = { onChangeAvatar() }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change Avatar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun DisplayNameDialog(
    viewModel: EditProfileViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Display Name", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current: ${viewModel.currentDisplayName.value.ifEmpty { "Not set" }}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = viewModel.newDisplayName.value,
                    onValueChange = { viewModel.onDisplayNameChanged(it) },
                    label = { Text("New Display Name") },
                    placeholder = { Text("Enter new display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = viewModel.displayNameUpdateError.value != null,
                    supportingText = {
                        viewModel.displayNameUpdateError.value?.let { error ->
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                if (viewModel.displayNameUpdateSuccess.value) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Display name updated successfully",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    viewModel.updateDisplayName()
                },
                enabled = !viewModel.isUpdatingDisplayName.value && viewModel.newDisplayName.value.isNotBlank()
            ) {
                if (viewModel.isUpdatingDisplayName.value) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AboutMeDialog(
    viewModel: EditProfileViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("About Me", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tell others about yourself",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = viewModel.aboutMe.value,
                    onValueChange = { viewModel.onAboutMeChanged(it) },
                    label = { Text("About Me") },
                    placeholder = { Text("Share something about yourself...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 6,
                    supportingText = {
                        Text("${viewModel.aboutMe.value.length}/500")
                    }
                )

                if (viewModel.aboutMeUpdateSuccess.value) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About me updated successfully",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    viewModel.updateAboutMe()
                },
                enabled = !viewModel.isUpdatingAboutMe.value
            ) {
                if (viewModel.isUpdatingAboutMe.value) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountSection(
    onDeleteClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Delete Account",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Permanently delete your account and all associated data. This action cannot be undone.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onDeleteClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Account",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete My Account")
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    viewModel: EditProfileViewModel,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    val requiredText = "DELETE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = " Warning: This action is permanent and cannot be undone.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "All your data including:\n• Profile information\n• Learning progress\n• Recordings and evaluations\n• Social connections\n\nwill be permanently deleted.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Type \"$requiredText\" to confirm:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text("Confirmation") },
                    placeholder = { Text(requiredText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = MaterialTheme.colorScheme.error
                    )
                )

                if (viewModel.deleteAccountError.value != null) {
                    Text(
                        text = viewModel.deleteAccountError.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = confirmationText == requiredText && !viewModel.isDeletingAccount.value,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                if (viewModel.isDeletingAccount.value) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete Account")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Bind to ViewModel state
    val hideAvatar = viewModel.hideAvatar.value
    val hideOnlineStatus = viewModel.hideOnlineStatus.value
    val allowMessagesFromStrangers = viewModel.allowMessagesFromStrangers.value
    val saving = viewModel.isSavingPrivacy.value
    val loading = viewModel.privacyLoading.value
    val error = viewModel.privacyError.value

    LaunchedEffect(Unit) {
        viewModel.loadPrivacySettings()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Privacy Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Control your visibility and privacy",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Hide Avatar",
                        subtitle = "Other users won't see your profile picture",
                        checked = hideAvatar,
                        onCheckedChange = { viewModel.setHideAvatar(it) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Default.Visibility,
                        title = "Hide Online Status",
                        subtitle = "Don't show when you're active",
                        checked = hideOnlineStatus,
                        onCheckedChange = { viewModel.setHideOnlineStatus(it) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Default.Message,
                        title = "Allow Messages from Anyone",
                        subtitle = "If off, only followers can message you",
                        checked = allowMessagesFromStrangers,
                        onCheckedChange = { viewModel.setAllowMessagesFromStrangers(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.savePrivacySettings(onSuccess = { onNavigateBack() })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val blockedUsers = viewModel.blockedUsers.value
    val loading = viewModel.blockedUsersLoading.value
    val error = viewModel.blockedUsersError.value
    var showUnblockDialog by remember { mutableStateOf<com.example.voicevibe.data.remote.api.BlockedUser?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadBlockedUsers()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Blocked Users", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (loading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            // Error state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.loadBlockedUsers() }) {
                    Text("Retry")
                }
            }
        } else if (blockedUsers.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Blocked Users",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Users you block will appear here",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // List of blocked users
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "You have blocked ${blockedUsers.size} user${if (blockedUsers.size != 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                blockedUsers.forEach { user ->
                    BlockedUserItem(
                        user = user,
                        onUnblock = { showUnblockDialog = user }
                    )
                }
            }
        }
    }

    // Block confirmation dialog
    showUnblockDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showUnblockDialog = null },
            title = { Text("Unblock User", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to unblock ${user.displayName ?: user.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unblockUser(user.userId) {
                            showUnblockDialog = null
                        }
                    }
                ) {
                    Text("Unblock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnblockDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
private fun BlockedUserItem(
    user: com.example.voicevibe.data.remote.api.BlockedUser,
    onUnblock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user.displayName ?: user.username).take(2).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName ?: user.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Blocked",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onUnblock,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Unblock")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "VozVibe Privacy Policy",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Last updated: ${java.time.LocalDate.now()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            PrivacySection(
                title = "1. Information We Collect",
                content = "We collect information you provide directly to us, including:\n• Account information (email, username)\n• Profile information (name, bio, avatar)\n• Learning progress and practice recordings\n• Social interactions (posts, comments, follows)"
            )
            
            PrivacySection(
                title = "2. How We Use Your Information",
                content = "We use the information we collect to:\n• Provide and improve our services\n• Personalize your learning experience\n• Communicate with you\n• Ensure platform safety and security"
            )
            
            PrivacySection(
                title = "3. Data Sharing",
                content = "We do not sell your personal information. We may share your information only in these circumstances:\n• With your consent\n• To comply with legal obligations\n• To protect our rights and safety"
            )
            
            PrivacySection(
                title = "4. Data Security",
                content = "We implement appropriate security measures to protect your personal information from unauthorized access, alteration, disclosure, or destruction."
            )
            
            PrivacySection(
                title = "5. Your Rights",
                content = "You have the right to:\n• Access your personal data\n• Correct inaccurate data\n• Delete your account and data\n• Control your privacy settings\n• Export your data"
            )
            
            PrivacySection(
                title = "6. Contact Us",
                content = "If you have questions about this Privacy Policy, please contact us at privacy@vozvibe.com"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityGuidelinesScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Community Guidelines", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "VozVibe Community Guidelines",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Our community guidelines help create a safe, respectful, and supportive learning environment for everyone.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            GuidelineSection(
                title = "Be Respectful",
                content = "Treat all community members with respect and kindness. Harassment, hate speech, or discriminatory behavior will not be tolerated."
            )
            
            GuidelineSection(
                title = "No Spam or Self-Promotion",
                content = "Don't post spam, excessive self-promotion, or repetitive content. Share helpful resources and engage authentically."
            )
            
            GuidelineSection(
                title = "Keep Content Appropriate",
                content = "Ensure all content is appropriate for a learning platform. Explicit, violent, or offensive material is prohibited."
            )
            
            GuidelineSection(
                title = "Protect Privacy",
                content = "Respect others' privacy. Don't share personal information about others without their consent."
            )
            
            GuidelineSection(
                title = "Be Supportive",
                content = "We're all here to learn. Encourage others, celebrate progress, and help create a positive learning environment."
            )
            
            GuidelineSection(
                title = "Report Violations",
                content = "If you see content that violates these guidelines, please report it using the report button. Our moderation team will review it promptly."
            )
            
            Divider()
            
            Text(
                text = "Consequences of Violations",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Violations of these guidelines may result in:\n• Content removal\n• Temporary suspension\n• Permanent account termination\n\nThe severity of the action depends on the nature and frequency of the violation.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "VozVibe Terms of Service",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Last updated: ${java.time.LocalDate.now()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            TermsSection(
                title = "1. Acceptance of Terms",
                content = "By accessing and using VozVibe, you agree to be bound by these Terms of Service and all applicable laws and regulations. If you do not agree with any of these terms, you are prohibited from using or accessing this application."
            )
            
            TermsSection(
                title = "2. Description of Service",
                content = "VozVibe provides an AI-powered English learning platform that includes:\n• Speech recognition and pronunciation analysis\n• Interactive conversation practice\n• Vocabulary lessons and exercises\n• Progress tracking and gamification\n• Social learning features\n\nThe service is provided \"as is\" and we reserve the right to modify or discontinue any feature at any time."
            )
            
            TermsSection(
                title = "3. User Accounts",
                content = "You are responsible for:\n• Maintaining the confidentiality of your account credentials\n• All activities that occur under your account\n• Notifying us immediately of any unauthorized use\n\nWe reserve the right to terminate accounts that violate these terms or engage in fraudulent activity."
            )
            
            TermsSection(
                title = "4. User Content",
                content = "You retain ownership of content you create (recordings, posts, comments). By using VozVibe, you grant us a license to:\n• Store and process your content to provide services\n• Use anonymized data to improve our AI models\n• Display your content to other users as part of social features\n\nYou are responsible for ensuring your content does not violate any laws or third-party rights."
            )
            
            TermsSection(
                title = "5. Acceptable Use",
                content = "You agree NOT to:\n• Harass, abuse, or harm other users\n• Post spam, malware, or malicious content\n• Attempt to hack or compromise the platform\n• Use the service for any illegal purpose\n• Impersonate others or create fake accounts\n• Scrape or copy content without permission\n\nViolations may result in immediate account termination."
            )
            
            TermsSection(
                title = "6. Intellectual Property",
                content = "VozVibe and its original content, features, and functionality are owned by VozVibe Team and are protected by international copyright, trademark, patent, trade secret, and other intellectual property laws.\n\nYou may not copy, modify, distribute, sell, or lease any part of our services or software without express written permission."
            )
            
            TermsSection(
                title = "7. Privacy",
                content = "Your use of VozVibe is also governed by our Privacy Policy. We collect and process data as described in the Privacy Policy to provide and improve our services.\n\nBy using VozVibe, you consent to data processing as outlined in our Privacy Policy."
            )
            
            TermsSection(
                title = "8. Limitation of Liability",
                content = "VozVibe is provided for educational purposes. We make no guarantees about learning outcomes.\n\nTo the maximum extent permitted by law, VozVibe shall not be liable for any indirect, incidental, special, consequential, or punitive damages, or any loss of profits or revenues, whether incurred directly or indirectly."
            )
            
            TermsSection(
                title = "9. Subscription and Payments",
                content = "VozVibe currently offers free access to all features. If we introduce paid features in the future:\n• Pricing will be clearly displayed\n• Subscriptions may auto-renew unless cancelled\n• Refund policies will be specified\n• You are responsible for all charges incurred"
            )
            
            TermsSection(
                title = "10. Termination",
                content = "We reserve the right to terminate or suspend your account and access to VozVibe at our sole discretion, without notice, for conduct that we believe:\n• Violates these Terms of Service\n• Violates Community Guidelines\n• Is harmful to other users or VozVibe\n• Exposes us to legal liability"
            )
            
            TermsSection(
                title = "11. Changes to Terms",
                content = "We reserve the right to modify these terms at any time. We will notify users of material changes via email or in-app notification.\n\nYour continued use of VozVibe after changes constitutes acceptance of the modified terms."
            )
            
            TermsSection(
                title = "12. Governing Law",
                content = "These Terms shall be governed by and construed in accordance with applicable international laws, without regard to conflict of law provisions.\n\nAny disputes arising from these terms will be resolved through binding arbitration."
            )
            
            TermsSection(
                title = "13. Contact Information",
                content = "If you have questions about these Terms of Service, please contact us at:\n\nVozVibe Team 2025\nvozvibe2025@gmail.com"
            )
            
            Divider()
            
            Text(
                text = "By using VozVibe, you acknowledge that you have read, understood, and agree to be bound by these Terms of Service.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun GuidelineSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}
