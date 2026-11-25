package com.hb.offlinechat.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * HB Offline Chat theme: premium dark theme with neon cyan and green accents.
 */
@Composable
fun HbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        content = content
    )
}

private val DarkColorScheme = ColorScheme(
    primary = Color(0xFF00E5FF),        // Neon cyan
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00B8D4),
    onPrimaryContainer = Color(0xFF000000),
    inversePrimary = Color(0xFF00E5FF),
    secondary = Color(0xFF00FF9D),      // Neon green
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF00E67B),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFFB388FF),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFB388FF),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFF0A0A0C),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141419),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E26),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFF00E5FF),
    inverseSurface = Color(0xFF2A2A33),
    inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFBABA),
    onErrorContainer = Color(0xFF000000),
    outline = Color(0xFF2A2A33),
    outlineVariant = Color(0xFF3A3A44),
    scrim = Color(0xFF000000),
)

private val LightColorScheme = ColorScheme(
    primary = Color(0xFF0099CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00E5FF),
    onPrimaryContainer = Color(0xFF000000),
    inversePrimary = Color(0xFF00E5FF),
    secondary = Color(0xFF00C875),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF00FF9D),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DEF8),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFFFAFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF2F3F7),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Color(0xFF0099CC),
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFF4F4),
    onErrorContainer = Color(0xFF000000),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE7E0EC),
    scrim = Color(0xFF000000),
)

/** Dimension constants used across the HB Offline Chat UI. */
object AppDimensions {
    val cardRadius: Dp = 16.dp
    val cardRadiusLarge: Dp = 24.dp
    val spacingXs: Dp = 4.dp
    val spacingS: Dp = 8.dp
    val spacingM: Dp = 16.dp
    val spacingL: Dp = 24.dp
    val spacingXl: Dp = 32.dp
}
