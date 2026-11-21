package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ActiPermDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ObsidianBg,
    primaryContainer = CyberCyanContainer,
    onPrimaryContainer = CyberCyan,
    secondary = CyberBlue,
    onSecondary = TextPrimary,
    secondaryContainer = SlateSurfaceHigh,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSafe,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldSafeContainer,
    onTertiaryContainer = EmeraldSafe,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceHigh,
    surfaceContainerHigh = SlateSurfaceHighest,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = CrimsonDanger,
    onError = TextPrimary,
    errorContainer = CrimsonDangerContainer,
    onErrorContainer = CrimsonDanger
)

val ActiPermShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ActiPermTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ActiPermDarkColorScheme,
        typography = ActiPermTypography,
        shapes = ActiPermShapes,
        content = content
    )
}
