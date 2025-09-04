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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
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
    var voiceInput by remember { mutableStateOf("") }

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
                userEmail = viewModel.userEmail.value,
                membershipStatus = viewModel.membershipStatus.value,
                userInitials = viewModel.userInitials.value
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
                    onCheckedChange = { viewModel.onToggleSpeakingOnly(it) }
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
                        voiceInput = viewModel.ttsVoiceId.value ?: ""
                        showVoiceDialog = true
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Speech Speed",
                    subtitle = "Normal",
                    onClick = { /* Show speed selection dialog */ }
                )
                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = "Voice Accent",
                    subtitle = "American English",
                    onClick = { /* Show accent selection dialog */ }
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Daily Goal",
                    subtitle = "30 minutes per day",
                    onClick = { /* Show goal setting dialog */ }
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

    // Preferred TTS Voice Dialog
    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Preferred TTS Voice", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter an ElevenLabs voice ID. Leave empty to use default.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = voiceInput,
                        onValueChange = { voiceInput = it },
                        singleLine = true,
                        label = { Text("Voice ID") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setPreferredTtsVoice(voiceInput.ifBlank { null })
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

@Composable
fun UserProfileSection(
    userName: String,
    userEmail: String,
    membershipStatus: String,
    userInitials: String
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
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
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = membershipStatus,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Edit Icon
            IconButton(onClick = { /* Navigate to edit profile */ }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile",
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
    onCheckedChange: (Boolean) -> Unit
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
