package com.martinsterentjevs.cacheit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = CacheItAccentPrimary,
    onPrimary = Neutral0,
    secondary = CacheItAccentHover,
    tertiary = CacheItAccentDark,
    background = Black,
    onBackground = Neutral0,
    surface = Neutral700,
    onSurface = Neutral0,
    surfaceVariant = Neutral600,
    onSurfaceVariant = Neutral400,
    outline = Neutral600,
    error = StatusErrorDark,
    onError = Neutral0,
)

private val LightColorScheme = lightColorScheme(
    primary = CacheItAccentPrimary,
    onPrimary = Neutral0,
    secondary = CacheItAccentHover,
    tertiary = CacheItAccentDark,
    background = Neutral50,
    onBackground = Neutral800,
    surface = Neutral0,
    onSurface = Neutral800,
    surfaceVariant = Neutral50,
    onSurfaceVariant = Neutral300,
    outline = Neutral200,
    error = StatusErrorLight,
    onError = Neutral0,
)

@Composable
fun CacheItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color (Material You) is deliberately not offered here, unlike the wizard default.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalCacheItExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CacheItTypography,
            content = content,
        )
    }
}

/** Access point for tokens Material3's ColorScheme has no slot for — e.g. `CacheItThemeExtras.extendedColors.navText`. */
object CacheItThemeExtras {
    val extendedColors: CacheItExtendedColors
        @Composable
        get() = LocalCacheItExtendedColors.current
}