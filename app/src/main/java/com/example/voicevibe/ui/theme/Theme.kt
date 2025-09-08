package com.example.voicevibe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = NeutralDarkGray,
    secondary = AccentBlueGray,
    tertiary = AccentWarmBeige,
    background = NeutralLightGray,
    surface = NeutralWhite,
    onPrimary = NeutralWhite,
    onSecondary = NeutralBlack,
    onTertiary = NeutralBlack,
    onBackground = NeutralDarkGray,
    onSurface = NeutralDarkGray
)

private val DarkColorScheme = darkColorScheme(
    primary = NeutralLightGray,
    secondary = AccentBlueGray,
    tertiary = AccentWarmBeige,
    background = NeutralBlack,
    surface = NeutralDarkGray,
    onPrimary = NeutralBlack,
    onSecondary = NeutralWhite,
    onTertiary = NeutralWhite,
    onBackground = NeutralLightGray,
    onSurface = NeutralLightGray
)

@Composable
fun VoiceVibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color disabled for consistent neutral theme
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
