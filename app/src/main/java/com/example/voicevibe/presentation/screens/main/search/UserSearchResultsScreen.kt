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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.Material
import com.example.voicevibe.presentation.components.OnlineStatusIndicator
import com.example.voicevibe.presentation.screens.main.search.SearchTab
import com.example.voicevibe.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchResultsScreen(
    onNavigateBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenGroup: (Int) -> Unit = {},
    onOpenMaterial: (String) -> Unit = {},
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
                        placeholder = { Text("Search users, groups, materials", color = NeutralWhite.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

            // Tabs
            if (uiState.query.length >= 2) {
                SearchTabs(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::onTabChange,
                    userCount = uiState.users.size,
                    groupCount = uiState.groups.size,
                    materialCount = uiState.materials.size
                )
            }

            if (uiState.query.length < 2 && uiState.error == null) {
                EmptyState()
            } else if (uiState.error != null) {
                ErrorState(uiState.error!!)
            } else {
                SearchResults(
                    selectedTab = uiState.selectedTab,
                    users = uiState.users,
                    groups = uiState.groups,
                    materials = uiState.materials,
                    onOpenUserProfile = onOpenUserProfile,
                    onOpenGroup = onOpenGroup,
                    onOpenMaterial = onOpenMaterial
                )
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
private fun SearchTabs(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    userCount: Int,
    groupCount: Int,
    materialCount: Int
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = NeutralWhite,
        edgePadding = 12.dp,
        divider = {}
    ) {
        Tab(
            selected = selectedTab == SearchTab.ALL,
            onClick = { onTabSelected(SearchTab.ALL) },
            text = { Text("All (${userCount + groupCount + materialCount})") }
        )
        Tab(
            selected = selectedTab == SearchTab.USERS,
            onClick = { onTabSelected(SearchTab.USERS) },
            text = { Text("Users ($userCount)") }
        )
        Tab(
            selected = selectedTab == SearchTab.GROUPS,
            onClick = { onTabSelected(SearchTab.GROUPS) },
            text = { Text("Groups ($groupCount)") }
        )
        Tab(
            selected = selectedTab == SearchTab.MATERIALS,
            onClick = { onTabSelected(SearchTab.MATERIALS) },
            text = { Text("Materials ($materialCount)") }
        )
    }
}

@Composable
private fun SearchResults(
    selectedTab: SearchTab,
    users: List<UserProfile>,
    groups: List<Group>,
    materials: List<Material>,
    onOpenUserProfile: (String) -> Unit,
    onOpenGroup: (Int) -> Unit,
    onOpenMaterial: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        when (selectedTab) {
            SearchTab.ALL -> {
                // Show all results
                if (users.isNotEmpty()) {
                    item {
                        Text(
                            "Users",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = NeutralWhite
                        )
                    }
                    items(users) { user ->
                        UserRow(user = user, onClick = { onOpenUserProfile(user.id) })
                    }
                }
                if (groups.isNotEmpty()) {
                    item {
                        Text(
                            "Groups",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = NeutralWhite
                        )
                    }
                    items(groups) { group ->
                        GroupRow(group = group, onClick = { onOpenGroup(group.id) })
                    }
                }
                if (materials.isNotEmpty()) {
                    item {
                        Text(
                            "Materials",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = NeutralWhite
                        )
                    }
                    items(materials) { material ->
                        MaterialRow(material = material, onClick = { onOpenMaterial(material.id) })
                    }
                }
            }
            SearchTab.USERS -> {
                items(users) { user ->
                    UserRow(user = user, onClick = { onOpenUserProfile(user.id) })
                }
            }
            SearchTab.GROUPS -> {
                items(groups) { group ->
                    GroupRow(group = group, onClick = { onOpenGroup(group.id) })
                }
            }
            SearchTab.MATERIALS -> {
                items(materials) { material ->
                    MaterialRow(material = material, onClick = { onOpenMaterial(material.id) })
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: Group, onClick: () -> Unit) {
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
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = BrandIndigo)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(group.displayName, fontWeight = FontWeight.Bold, color = NeutralDarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${group.memberCount} members", fontSize = 12.sp, color = AccentBlueGray)
            }
            Icon(Icons.Filled.Groups, contentDescription = null, tint = AccentBlueGray)
        }
    }
}

@Composable
private fun MaterialRow(material: Material, onClick: () -> Unit) {
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
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = BrandCyan)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(material.title, fontWeight = FontWeight.Bold, color = NeutralDarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (material.description.isNotBlank()) {
                    Text(material.description, fontSize = 12.sp, color = AccentBlueGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = AccentBlueGray)
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
        Text("Search for users, groups, and materials", fontWeight = FontWeight.Bold, color = NeutralWhite, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text("Type at least 2 characters to start searching", color = NeutralWhite.copy(alpha = 0.9f))
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
