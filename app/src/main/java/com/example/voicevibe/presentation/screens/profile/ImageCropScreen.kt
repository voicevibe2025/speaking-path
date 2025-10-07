package com.example.voicevibe.presentation.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr0xf00.easycrop.CropError
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.crop
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.ui.ImageCropperDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    imageUri: String,
    onNavigateBack: () -> Unit,
    onCropComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val imageCropper = rememberImageCropper()
    
    var isProcessing by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Decode URI and start cropping
    val cropState = imageCropper.cropState
    
    LaunchedEffect(imageUri) {
        try {
            val uri = Uri.parse(Uri.decode(imageUri))
            
            // Persist the picked content URI to a temporary file first (IO thread)
            val tempSrcFile = withContext(Dispatchers.IO) {
                val dst = File.createTempFile("vv_crop_src_", ".jpg", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dst.outputStream().use { out ->
                        input.copyTo(out)
                    }
                } ?: run {
                    // If we couldn't open the stream, signal failure by deleting and returning null
                    dst.delete()
                    null
                }
                dst
            }

            if (tempSrcFile == null || !tempSrcFile.exists()) {
                errorMessage = "Failed to load image"
                showError = true
                return@LaunchedEffect
            }

            // Start crop with File overload (suspends until user accepts/cancels)
            val result = imageCropper.crop(tempSrcFile)

            // Handle the crop result
            when (result) {
                is CropResult.Success -> {
                    isProcessing = true
                    try {
                        // Convert ImageBitmap to android.graphics.Bitmap
                        val androidBitmap = result.bitmap.asAndroidBitmap()
                        
                        // Convert to byte array in IO thread
                        val bytes = withContext(Dispatchers.IO) {
                            val outputStream = ByteArrayOutputStream()
                            androidBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.toByteArray()
                        }
                        
                        // Upload avatar
                        viewModel.uploadAvatar(bytes)
                        
                        // Navigate back to settings
                        onCropComplete()
                    } catch (e: Exception) {
                        errorMessage = "Failed to process image: ${e.message}"
                        showError = true
                        isProcessing = false
                    }
                }
                CropResult.Cancelled -> {
                    // User cancelled, go back
                    onNavigateBack()
                }
                CropError.LoadingError -> {
                    errorMessage = "Failed to load image"
                    showError = true
                }
                CropError.SavingError -> {
                    errorMessage = "Failed to save cropped image"
                    showError = true
                }
                null -> {
                    errorMessage = "Failed to load image"
                    showError = true
                }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load image: ${e.message}"
            showError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
            // Show the crop dialog
            if (cropState != null) {
                ImageCropperDialog(
                    state = cropState
                )
            }
            
            // Loading indicator
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Uploading avatar...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Instructions overlay at bottom
            if (cropState != null && !isProcessing) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "How to crop:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• Pinch to zoom in/out",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• Drag to reposition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• Tap ✓ when ready to save",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    
    // Error dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showError = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
