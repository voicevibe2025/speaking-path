package com.example.voicevibe.presentation.screens.main.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.presentation.components.OnlineStatusIndicator
import com.example.voicevibe.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchResultsScreen(
    onNavigateBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeutralWhite)
                    }
                },
                title = {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search users", color = NeutralWhite.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NeutralWhite,
                            unfocusedTextColor = NeutralWhite,
                            focusedPlaceholderColor = NeutralWhite.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = NeutralWhite.copy(alpha = 0.7f),
                            focusedBorderColor = NeutralWhite.copy(alpha = 0.5f),
                            unfocusedBorderColor = NeutralWhite.copy(alpha = 0.3f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = NeutralWhite
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.onSubmitSearch()
                        })
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandIndigo,
                    titleContentColor = NeutralWhite
                )
            )
        },
        containerColor = Color.Transparent
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.Transparent)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.results.isEmpty() && uiState.query.length < 2 && uiState.error == null) {
                EmptyState()
            } else if (uiState.error != null) {
                ErrorState(uiState.error!!)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.results) { user ->
                        UserRow(user = user, onClick = { onOpenUserProfile(user.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: UserProfile, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = NeutralWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(BrandIndigo.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = user.avatarUrl
                    if (!avatarUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = BrandIndigo)
                    }
                }
                
                // Online status indicator
                OnlineStatusIndicator(
                    isOnline = user.isOnline,
                    size = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.Bold, color = NeutralDarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${user.username}", fontSize = 12.sp, color = AccentBlueGray)
            }
            Icon(Icons.Filled.Person, contentDescription = null, tint = AccentBlueGray)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Find friends", fontWeight = FontWeight.Bold, color = NeutralWhite, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text("Search by name or username", color = NeutralWhite.copy(alpha = 0.9f))
    }
}

@Composable
private fun ErrorState(msg: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Something went wrong", fontWeight = FontWeight.Bold, color = BrandFuchsia)
        Spacer(Modifier.height(4.dp))
        Text(msg, color = BrandFuchsia)
    }
}
