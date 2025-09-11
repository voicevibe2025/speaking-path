package com.example.voicevibe.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    title: String,
    onNavigationIconClick: () -> Unit,
    navigationIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
) {
    TopAppBar(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigationIconClick,
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6C63FF).copy(alpha = 0.3f),
                                Color(0xFF00D9FF).copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                navigationIcon()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}
