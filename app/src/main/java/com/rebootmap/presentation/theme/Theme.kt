package com.rebootmap.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = CardLight,
    primaryContainer = Color(0xFFB8D4E3),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = AccentCoral,
    onSecondary = TextPrimary,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = CardLight,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE8EEF2),
    onSurfaceVariant = TextSecondary,
    error = WarningRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BA3C0),
    onPrimary = SurfaceDark,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color(0xFFB8D4E3),
    secondary = AccentCoral,
    onSecondary = SurfaceDark,
    background = SurfaceDark,
    onBackground = Color(0xFFE2E8F0),
    surface = CardDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF243044),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = WarningRed,
)

@Composable
fun RebootMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        content = content,
    )
}
