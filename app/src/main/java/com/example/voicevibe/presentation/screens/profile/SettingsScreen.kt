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
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToCommunityGuidelines: () -> Unit,
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
                    subtitle = "Version 1.0.0",
                    onClick = onNavigateToAbout
                )
                SettingsItem(
                    icon = Icons.Default.ContactMail,
                    title = "Contact",
                    subtitle = "Get in touch with us",
                    onClick = { /* TODO: Open contact form */ }
                )
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Q & A",
                    subtitle = "Frequently asked questions",
                    onClick = { /* TODO: Open FAQ */ }
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
    var hideAvatar by remember { mutableStateOf(false) }
    var hideOnlineStatus by remember { mutableStateOf(false) }
    var allowMessagesFromStrangers by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Privacy Settings", fontWeight = FontWeight.Bold) },
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
                text = "Control your visibility and privacy",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        onCheckedChange = { hideAvatar = it }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Default.Visibility,
                        title = "Hide Online Status",
                        subtitle = "Don't show when you're active",
                        checked = hideOnlineStatus,
                        onCheckedChange = { hideOnlineStatus = it }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Default.Message,
                        title = "Allow Messages from Anyone",
                        subtitle = "If off, only followers can message you",
                        checked = allowMessagesFromStrangers,
                        onCheckedChange = { allowMessagesFromStrangers = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // TODO: Save privacy settings via viewModel
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes", fontSize = 16.sp)
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
