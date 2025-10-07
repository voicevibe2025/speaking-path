package com.example.voicevibe.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage

@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    contentDescription: String? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val density = LocalDensity.current
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }

            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                val adjustedPan = if (scale == 1f && newScale > 1f) Offset.Zero else panChange
                val desiredOffset = offset + adjustedPan
                scale = newScale
                offset = constrainOffset(desiredOffset, newScale, containerWidthPx, containerHeightPx)
            }

            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(transformableState)
                    .pointerInput(onDismiss) {
                        detectTapGestures(
                            onTap = {
                                if (scale <= 1.05f) {
                                    onDismiss()
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    val targetScale = 2f
                                    val relativeTap = Offset(
                                        x = tapOffset.x - containerWidthPx / 2f,
                                        y = tapOffset.y - containerHeightPx / 2f
                                    )
                                    scale = targetScale
                                    offset = constrainOffset(-relativeTap * (targetScale - 1f), targetScale, containerWidthPx, containerHeightPx)
                                }
                            }
                        )
                    },
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

private fun constrainOffset(
    desiredOffset: Offset,
    scale: Float,
    containerWidth: Float,
    containerHeight: Float
): Offset {
    if (scale <= 1f) return Offset.Zero

    val maxTranslationX = (scale - 1f) * containerWidth / 2f
    val maxTranslationY = (scale - 1f) * containerHeight / 2f

    val constrainedX = desiredOffset.x.coerceIn(-maxTranslationX, maxTranslationX)
    val constrainedY = desiredOffset.y.coerceIn(-maxTranslationY, maxTranslationY)

    return Offset(constrainedX, constrainedY)
}
