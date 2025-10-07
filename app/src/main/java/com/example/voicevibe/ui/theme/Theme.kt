package com.example.voicevibe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = NeutralLightGray,
    surface = Surface,
    onPrimary = NeutralWhite,
    onSecondary = NeutralBlack,
    onTertiary = NeutralBlack,
    onBackground = NeutralDarkGray,
    onSurface = NeutralDarkGray,
    error = Error,
    onError = NeutralWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = BackgroundDeep,
    surface = BackgroundCard,
    onPrimary = NeutralWhite,
    onSecondary = NeutralBlack,
    onTertiary = NeutralBlack,
    onBackground = NeutralWhite,
    onSurface = NeutralWhite,
    error = Error,
    onError = NeutralWhite
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
