package com.example.voicevibe.presentation.screens.auth.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicevibe.BuildConfig
import com.example.voicevibe.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.BorderStroke
import com.example.voicevibe.ui.theme.BrandCyan
import com.example.voicevibe.ui.theme.BrandFuchsia
import com.example.voicevibe.ui.theme.BrandIndigo
import com.example.voicevibe.ui.theme.BrandNavy
import com.example.voicevibe.ui.theme.BrandNavyDark

/**
 * Register screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToGroupSelection: () -> Unit,
    onNavigateToEnglishLevelSelection: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe register events
    LaunchedEffect(Unit) {
        viewModel.registerEvent.collect { event ->
            when (event) {
                is RegisterEvent.Success -> {
                    // Deprecated - keeping for backward compatibility
                    snackbarHostState.showSnackbar(
                        message = "Registration successful! Welcome to VoiceVibe!",
                        duration = SnackbarDuration.Short
                    )
                    onNavigateToHome()
                }
                is RegisterEvent.NavigateToHome -> {
                    snackbarHostState.showSnackbar(
                        message = "Registration successful! Welcome to VoiceVibe!",
                        duration = SnackbarDuration.Short
                    )
                    onNavigateToHome()
                }
                is RegisterEvent.NavigateToGroupSelection -> {
                    snackbarHostState.showSnackbar(
                        message = "Registration successful! Please choose your group.",
                        duration = SnackbarDuration.Short
                    )
                    onNavigateToGroupSelection()
                }
                is RegisterEvent.NavigateToEnglishLevelSelection -> {
                    onNavigateToEnglishLevelSelection()
                }
                is RegisterEvent.NetworkError -> {
                    snackbarHostState.showSnackbar(
                        message = "Network error. Please check your connection.",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    // Show error messages
    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to login"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BrandNavyDark, BrandNavy)
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))


                // Logo and Title
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "VoiceVibe Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Start your language learning journey",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Name Fields Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = viewModel::onFirstNameChanged,
                        label = { Text("First Name") },
                        isError = uiState.firstNameError != null,
                        supportingText = {
                            uiState.firstNameError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = BrandIndigo,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = BrandCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = viewModel::onLastNameChanged,
                        label = { Text("Last Name") },
                        isError = uiState.lastNameError != null,
                        supportingText = {
                            uiState.lastNameError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = BrandIndigo,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = BrandCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gender Dropdown
                var genderExpanded by remember { mutableStateOf(false) }
                val genderOptions = listOf("Male", "Female")
                
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                        },
                        isError = uiState.genderError != null,
                        supportingText = {
                            uiState.genderError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = BrandIndigo,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = BrandCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onGenderChanged(option)
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Province Dropdown
                var provinceExpanded by remember { mutableStateOf(false) }
                val provinceOptions = listOf(
                    "Aceh", "Bali", "Bangka Belitung", "Banten", "Bengkulu",
                    "DKI Jakarta", "Gorontalo", "Jambi", "Jawa Barat", "Jawa Tengah",
                    "Jawa Timur", "Kalimantan Barat", "Kalimantan Selatan", "Kalimantan Tengah",
                    "Kalimantan Timur", "Kalimantan Utara", "Kepulauan Riau", "Lampung",
                    "Maluku", "Maluku Utara", "Nusa Tenggara Barat", "Nusa Tenggara Timur",
                    "Papua", "Papua Barat", "Riau", "Sulawesi Barat", "Sulawesi Selatan",
                    "Sulawesi Tengah", "Sulawesi Tenggara", "Sulawesi Utara", "Sumatera Barat",
                    "Sumatera Selatan", "Sumatera Utara", "DI Yogyakarta"
                )
                
                ExposedDropdownMenuBox(
                    expanded = provinceExpanded,
                    onExpandedChange = { provinceExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.province,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Province") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded)
                        },
                        isError = uiState.provinceError != null,
                        supportingText = {
                            uiState.provinceError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = BrandIndigo,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = BrandCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = provinceExpanded,
                        onDismissRequest = { provinceExpanded = false }
                    ) {
                        provinceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onProvinceChanged(option)
                                    provinceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = BrandCyan
                        )
                    },
                    isError = uiState.emailError != null,
                    supportingText = {
                        uiState.emailError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandIndigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = BrandIndigo,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = BrandCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = { Text("Password") },
                    placeholder = { Text("Create a strong password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BrandCyan
                        )
                    },
                    isError = uiState.passwordError != null,
                    supportingText = {
                        uiState.passwordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible)
                                    "Hide password"
                                else
                                    "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandIndigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = BrandIndigo,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = BrandCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                // Password Strength Indicator
                if (uiState.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordStrengthIndicator(strength = uiState.passwordStrength)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                var confirmPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    label = { Text("Confirm Password", color = Color.White) },
                    placeholder = { Text("Re-enter your password", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BrandCyan
                        )
                    },
                    isError = uiState.confirmPasswordError != null,
                    supportingText = {
                        uiState.confirmPasswordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                        ) {
                            Icon(
                                imageVector = if (confirmPasswordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible)
                                    "Hide password"
                                else
                                    "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandIndigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = BrandIndigo,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = BrandCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Terms and Conditions Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.agreeToTerms,
                        onCheckedChange = viewModel::onAgreeToTermsChanged,
                        colors = CheckboxDefaults.colors(checkedColor = BrandIndigo)
                    )
                    Text(
                        text = "I agree to the ",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color.White
                    )
                    Text(
                        text = "Terms and Conditions",
                        fontSize = 14.sp,
                        color = BrandCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToTerms() }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Register Button
                Button(
                    onClick = viewModel::register,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BrandCyan, BrandIndigo, BrandFuchsia)
                            )
                        ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isLoading && uiState.agreeToTerms,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Or divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                    Text(
                        text = "  or  ",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In button
                var webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (webClientId.isBlank()) {
                    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    if (resId != 0) webClientId = context.getString(resId)
                }
                val gso = remember(webClientId) {
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                }
                val googleClient = remember(webClientId) { GoogleSignIn.getClient(context, gso) }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    scope.launch {
                        try {
                            val account = task.getResult(ApiException::class.java)
                            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                            val auth = FirebaseAuth.getInstance()
                            auth.signInWithCredential(credential).await()
                            val firebaseUser = auth.currentUser
                            val tokenResult = firebaseUser?.getIdToken(true)?.await()
                            val idToken = tokenResult?.token
                            if (!idToken.isNullOrEmpty()) {
                                viewModel.loginWithGoogle(idToken)
                            }
                        } catch (_: Exception) {
                            // Snackbar already handled in Login; keep silent here to avoid duplicating UI
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                FirebaseAuth.getInstance().signOut()
                                googleClient.signOut().await()
                            } catch (_: Exception) { }
                            launcher.launch(googleClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading,
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(BrandCyan, BrandFuchsia))),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Continue with Google")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Login Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyan,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> Color.Red
            PasswordStrength.MEDIUM -> Color(0xFFFFA500) // Orange
            PasswordStrength.STRONG -> BrandCyan
        },
        label = "strength_color"
    )

    val progress by animateFloatAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> 0.33f
            PasswordStrength.MEDIUM -> 0.66f
            PasswordStrength.STRONG -> 1f
        },
        label = "strength_progress"
    )

    Column {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.2f)
        )

        Text(
            text = when (strength) {
                PasswordStrength.WEAK -> "Weak password"
                PasswordStrength.MEDIUM -> "Medium strength"
                PasswordStrength.STRONG -> "Strong password"
            },
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
