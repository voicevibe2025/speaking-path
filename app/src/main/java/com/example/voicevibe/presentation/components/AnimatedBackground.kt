package com.example.voicevibe.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedBackground(animatedOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Create animated gradient mesh
        val colors = listOf(
            Color(0xFF6C63FF),
            Color(0xFF00D9FF),
            Color(0xFFFF006E),
            Color(0xFFFFBE0B),
            Color(0xFF8338EC)
        )

        // Draw multiple gradient circles
        colors.forEachIndexed { index, color ->
            val angle = animatedOffset + (index * 72f)
            val radius = width * 0.6f
            val x = width / 2 + cos(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.3f
            val y = height / 2 + sin(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.3f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = radius
                ),
                center = Offset(x, y),
                radius = radius
            )
        }

        // Dark overlay for better contrast
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.7f),
                    Color.Black.copy(alpha = 0.6f)
                )
            )
        )
    }
}
