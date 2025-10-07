package com.example.voicevibe.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Color Palette
val Primary = Color(0xFF6366F1)        // color-1: Indigo
val Secondary = Color(0xFFF2A864)      // color-2: Orange/Peach
val Tertiary = Color(0xFFADF264)       // color-3: Lime Green
val Surface = Color(0xFFC9C9F3)        // color-4: Light Lavender
val Accent = Color(0xFF1317DD)         // color-5: Deep Blue

// Neutral palette
val NeutralWhite = Color(0xFFFFFFFF)
val NeutralLightGray = Color(0xFFF5F5F5)
val NeutralGray = Color(0xFFE0E0E0)
val NeutralDarkGray = Color(0xFF424242)
val NeutralBlack = Color(0xFF000000)

// Background variations
val BackgroundDeep = Color(0xFF0A0B1E)
val BackgroundCard = Color(0xFF1A1B2E)

// Semantic colors derived from palette
val Success = Tertiary                 // Lime Green for success states
val Warning = Secondary                // Orange for warnings
val Error = Color(0xFFE53935)         // Red for errors
val Info = Primary                     // Indigo for info

// Legacy color mappings for backward compatibility
val BrandIndigo = Primary
val BrandFuchsia = Color(0xFFC026D3)
val BrandCyan = Color(0xFF06B6D4)  // Cyan accent
val BrandNavy = BackgroundCard
val BrandNavyDark = BackgroundDeep

// Practice screen colors
val PracticeBlue = Primary
val PracticePurple = Color(0xFF9333EA)
val PracticePink = BrandFuchsia
val PracticeLightBlue = Color(0xFF38BDF8)
val PracticeCyan = BrandCyan
val PracticeGreen = Tertiary
val PracticeRed = Error
val PracticeLightRed = Color(0xFFEF4444)

// Practice gradient colors
val PracticeGradientDarkBlue1 = Color(0xFF1E3A8A)
val PracticeGradientDarkBlue2 = Color(0xFF1E40AF)
val PracticeGradientDarkBlue3 = Color(0xFF2563EB)
val PracticeGradientDarkBlue4 = Color(0xFF3B82F6)

// Other legacy colors
val AccentBlueGray = Color(0xFF90A4AE)
val PracticeAccuracyGreen = Tertiary
val PracticeXPYellow = Secondary
val PracticeCardBackground = BackgroundCard
