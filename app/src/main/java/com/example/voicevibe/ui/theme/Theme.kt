package com.example.voicevibe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Charming Seaside Dark Theme - Deep ocean inspired
private val DarkColorScheme = darkColorScheme(
    primary = Ocean_Blue,
    secondary = Mint_Green,
    tertiary = Soft_Aqua,
    background = Charcoal_Blue,
    surface = Deep_Teal,
    onPrimary = Charcoal_Blue,
    onSecondary = Charcoal_Blue,
    onTertiary = Charcoal_Blue,
    onBackground = Pure_White,
    onSurface = Pure_White,
    surfaceVariant = Medium_Teal,
    onSurfaceVariant = Pure_White,
    outline = Light_Teal,
    outlineVariant = Ocean_Blue
)

// Charming Seaside Light Theme - Bright seaside inspired
private val LightColorScheme = lightColorScheme(
    primary = Ocean_Blue,
    secondary = Mint_Green,
    tertiary = Soft_Aqua,
    background = Sea_Foam,
    surface = Pure_White,
    onPrimary = Pure_White,
    onSecondary = Deep_Teal,
    onTertiary = Deep_Teal,
    onBackground = Deep_Teal,
    onSurface = Deep_Teal,
    surfaceVariant = Soft_Gray,
    onSurfaceVariant = Deep_Teal,
    outline = Ocean_Blue,
    outlineVariant = Soft_Aqua
)

@Composable
fun VoiceVibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color disabled for consistent seaside theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
