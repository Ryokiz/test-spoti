package com.example.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val LocalAppColors = staticCompositionLocalOf { DarkColors }
val LocalAppTypography = staticCompositionLocalOf { DefaultTypography }

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val type: AppTypography @Composable get() = LocalAppTypography.current
}

@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    // Material 3 scheme is mapped from our roles so stock components (switch, slider, sheet) pick them up.
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceRaised, onSurfaceVariant = c.textMuted,
        outline = c.borderInput, outlineVariant = c.border, error = c.danger, inverseSurface = c.surfaceRaised, inverseOnSurface = c.text, inversePrimary = c.accent
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceRaised, onSurfaceVariant = c.textMuted,
        outline = c.borderInput, outlineVariant = c.border, error = c.danger, inverseSurface = c.surfaceRaised, inverseOnSurface = c.text, inversePrimary = c.accent
    )
    CompositionLocalProvider(LocalAppColors provides c, LocalAppTypography provides DefaultTypography) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
