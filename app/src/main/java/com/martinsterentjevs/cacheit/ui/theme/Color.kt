package com.martinsterentjevs.cacheit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Neutral scale
val Neutral0 = Color(0xFFFFFFFF)
val Neutral50 = Color(0xFFF9FAFB)
val Neutral100 = Color(0xFFF9F9FB)
val Neutral200 = Color(0xFFE5E7EB)
val Neutral300 = Color(0xFF9CA3AF)
val Neutral400 = Color(0xFFB3B3B3)
val Neutral500 = Color(0xFF374151)
val Neutral600 = Color(0xFF2D2D2D)
val Neutral700 = Color(0xFF1A1A1A)
val Neutral800 = Color(0xFF111827)
val Neutral900 = Color(0xFF0D0D0D)
val Black = Color(0XFF000000)

// Status
val StatusSuccessLight = Color(0xFF16A34A)
val StatusSuccessDark = Color(0xFF4ADE80)
val StatusWarningLight = Color(0xFFD97706)
val StatusWarningDark = Color(0xFFF88F24)
val StatusErrorLight = Color(0xFFB91C1C)
val StatusErrorDark = Color(0xFFEF4444)
val StatusInfoLight = Color(0xFF2563EB)
val StatusInfoDark = Color(0xFF60A5FA)

// Overlay (backdrop scrims — dialogs, modals)
val OverlayLight = Color(0x66000000) // #00000066
val OverlayDark = Color(0xCC000000) // #000000cc

val CacheItAccentPrimary = Color(0xFF3A8C42)
val CacheItAccentHover = Color(0xFF4CAF50)
val CacheItAccentDark = Color(0xFF235C2A)
val CacheItAccentLight = Color(0xFF1E3A28) // tinted bg on dark surface
val CacheItDanger = StatusErrorDark
val CacheItDangerLight = Color(0x1AEF4444) // error at ~10% opacity

/**
 * Tokens Material3's ColorScheme has no dedicated slot for — nav active state, raised surfaces
 * distinct from `surfaceVariant`, input backgrounds, tinted accent backgrounds. Resolved per
 * light/dark the same way the standard ColorScheme is; access via `CacheItThemeExtras.extendedColors`.
 */
data class CacheItExtendedColors(
    val surfaceRaised: Color,
    val borderSubtle: Color,
    val navActiveBackground: Color,
    val navActiveText: Color,
    val navText: Color,
    val overlay: Color,
    val inputBackground: Color,
    val accentLight: Color,
    val danger: Color,
    val dangerLight: Color,
)

val DarkExtendedColors = CacheItExtendedColors(
    surfaceRaised = Neutral600,
    borderSubtle = Neutral500,
    navActiveBackground = Neutral600,
    navActiveText = Neutral0,
    navText = Neutral400,
    overlay = OverlayDark,
    inputBackground = Neutral500,
    accentLight = CacheItAccentLight,
    danger = CacheItDanger,
    dangerLight = CacheItDangerLight,
)

val LightExtendedColors = CacheItExtendedColors(
    surfaceRaised = Neutral50,
    borderSubtle = Neutral100,
    navActiveBackground = Neutral200,
    navActiveText = Neutral800,
    navText = Neutral300,
    overlay = OverlayLight,
    inputBackground = Neutral200,
    accentLight = Color(0xFFE8F5E9),
    danger = StatusErrorLight,
    dangerLight = Color(0x1AB91C1C),
)

val LocalCacheItExtendedColors = staticCompositionLocalOf { DarkExtendedColors }