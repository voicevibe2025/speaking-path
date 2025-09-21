package com.example.voicevibe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.voicevibe.R

// Google Fonts provider and Baloo 2 family
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val baloo2 = GoogleFont("Baloo 2")

val Baloo2FontFamily = FontFamily(
    Font(googleFont = baloo2, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = baloo2, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = baloo2, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = baloo2, fontProvider = googleFontsProvider, weight = FontWeight.Bold),
    Font(googleFont = baloo2, fontProvider = googleFontsProvider, weight = FontWeight.ExtraBold)
)

// Start from Material3 defaults and apply Baloo 2 across all styles to keep sizes/spacing consistent
private val DefaultTypography = Typography()

val Typography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Baloo2FontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Baloo2FontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Baloo2FontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Baloo2FontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Baloo2FontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Baloo2FontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Baloo2FontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Baloo2FontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Baloo2FontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = Baloo2FontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Baloo2FontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Baloo2FontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Baloo2FontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Baloo2FontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Baloo2FontFamily)
)