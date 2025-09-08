package com.example.voicevibe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandIndigo,
    secondary = BrandFuchsia,
    tertiary = BrandCyan,
    background = BrandNavyDark,
    surface = BrandNavy,
    onPrimary = NeutralWhite,
    onSecondary = NeutralWhite,
    onTertiary = NeutralBlack,
    onBackground = NeutralWhite,
    onSurface = NeutralWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandIndigo,
    secondary = BrandFuchsia,
    tertiary = BrandCyan,
    background = BrandNavyDark,
    surface = BrandNavy,
    onPrimary = NeutralWhite,
    onSecondary = NeutralWhite,
    onTertiary = NeutralBlack,
    onBackground = NeutralWhite,
    onSurface = NeutralWhite
)

@Composable
fun VoiceVibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color disabled for consistent brand theme
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
