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
import coil.compose.SubcomposeAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToLanguageSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
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
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    viewModel.uploadAvatar(bytes)
                }
            } catch (_: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
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

            // General Settings
            SettingsSection(title = "General") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account Settings",
                    subtitle = "Manage your account details",
                    onClick = onNavigateToAccountSettings
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Configure notification preferences",
                    onClick = onNavigateToNotificationSettings
                )
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language & Region",
                    subtitle = "Change app language and region",
                    onClick = onNavigateToLanguageSettings
                )
            }

            // Learning Preferences
            SettingsSection(title = "Learning Preferences") {
                // Feature flag: Speaking-only Journey (beta)
                SettingsToggleItem(
                    icon = Icons.Default.Mic,
                    title = "Speaking-only Journey (beta)",
                    subtitle = "Enable the new speaking-only flow",
                    checked = viewModel.speakingOnlyEnabled.value,
                    onCheckedChange = { viewModel.onToggleSpeakingOnly(it) },
                    enabled = !com.example.voicevibe.utils.Constants.LOCK_SPEAKING_ONLY_ON
                )
                SettingsToggleItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Auto-play Audio",
                    subtitle = "Automatically play audio in lessons",
                    checked = autoPlayEnabled,
                    onCheckedChange = { autoPlayEnabled = it }
                )
                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = "Preferred TTS Voice",
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
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Speech Speed",
                    subtitle = "Normal",
                    onClick = { /* Show speed selection dialog */ }
                )
                SettingsItem(
                    icon = Icons.Default.Hearing,
                    title = "Audio Playback",
                    subtitle = if (autoPlayEnabled) "Auto-play is On" else "Auto-play is Off",
                    onClick = { /* Navigate to audio playback settings */ }
                )
            }

            // App Settings
            SettingsSection(title = "App Settings") {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
                SettingsToggleItem(
                    icon = Icons.Default.CloudOff,
                    title = "Offline Mode",
                    subtitle = "Download content for offline use",
                    checked = offlineMode,
                    onCheckedChange = { offlineMode = it }
                )
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Storage",
                    subtitle = "Manage downloaded content",
                    onClick = { /* Show storage management */ }
                )
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup & Sync",
                    subtitle = "Last synced: 2 hours ago",
                    onClick = { /* Show backup settings */ }
                )
            }

            // Privacy & Security
            SettingsSection(title = "Privacy & Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Policy",
                    subtitle = "View our privacy policy",
                    onClick = { /* Open privacy policy */ }
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Security Settings",
                    subtitle = "Manage app security",
                    onClick = { /* Show security settings */ }
                )
                SettingsItem(
                    icon = Icons.Default.Analytics,
                    title = "Data & Analytics",
                    subtitle = "Manage data collection preferences",
                    onClick = { /* Show data settings */ }
                )
            }

            // Support & About
            SettingsSection(title = "Support & About") {
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help Center",
                    subtitle = "Get help and support",
                    onClick = { /* Open help center */ }
                )
                SettingsItem(
                    icon = Icons.Default.Feedback,
                    title = "Send Feedback",
                    subtitle = "Share your thoughts with us",
                    onClick = { /* Open feedback form */ }
                )
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = "Rate Us",
                    subtitle = "Rate our app on the store",
                    onClick = { /* Open app store rating */ }
                )
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version 1.0.0",
                    onClick = onNavigateToAbout
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
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSection(title = "Account") {
                SettingsToggleItem(
                    icon = Icons.Default.Email,
                    title = "Show email in Profile Screen",
                    subtitle = "Display your email in Profile Screen",
                    checked = viewModel.showEmailOnProfile.value,
                    onCheckedChange = { viewModel.setShowEmailOnProfile(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsSection(title = "Edit Profile") {
                SettingsItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Change email, display name, or password",
                    onClick = onNavigateToEditProfile
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Configure notification preferences.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Language & Region", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Change app language and region.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
            }
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text("VoiceVibe v1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Email Section
            EmailUpdateSection(viewModel)

            Divider()

            // Display Name Section
            DisplayNameUpdateSection(viewModel)

            Divider()

            // Password Section
            PasswordUpdateSection(viewModel)

            Spacer(modifier = Modifier.height(16.dp))
        }
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
