package com.example.voicevibe.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    Box(modifier = Modifier.fillMaxSize()) {
        repeat(15) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = -0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (15000..25000).random(),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "particle$index"
            )

            val offsetX by infiniteTransition.animateFloat(
                initialValue = -0.1f,
                targetValue = 0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (5000..8000).random(),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "particleX$index"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(
                        x = (offsetX * 100).dp,
                        y = with(LocalDensity.current) { (offsetY * 1200).dp }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((index * 73) % 350).dp,
                            y = ((index * 47) % 200).dp
                        )
                        .size((2..6).random().dp)
                        .clip(CircleShape)
                        .background(
                            Color.White.copy(alpha = Random.nextFloat() * 0.4f + 0.3f)
                        )
                )
            }
        }
    }
}
