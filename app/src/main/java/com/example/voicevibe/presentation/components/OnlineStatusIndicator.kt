package com.example.voicevibe.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small green dot indicator to show online status
 * 
 * @param isOnline Whether the user is online
 * @param size Size of the indicator dot (default 10.dp)
 * @param modifier Modifier for positioning the indicator
 */
@Composable
fun OnlineStatusIndicator(
    isOnline: Boolean,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    if (isOnline) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)) // Green color for online
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}
