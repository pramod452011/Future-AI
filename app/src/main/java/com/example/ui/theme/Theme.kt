package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val FutureAiDarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = OnNeonBlue,
    primaryContainer = NeonBlueContainer,
    onPrimaryContainer = NeonBlueBright,
    secondary = ElectricPurple,
    onSecondary = OnElectricPurple,
    secondaryContainer = ElectricPurpleContainer,
    onSecondaryContainer = IceWhite,
    tertiary = PlasmaCyan,
    onTertiary = OnNeonBlue,
    tertiaryContainer = PlasmaCyanContainer,
    onTertiaryContainer = IceWhite,
    background = NavyBackground,
    onBackground = IceWhite,
    surface = NavySurface,
    onSurface = IceWhite,
    surfaceVariant = NavySurfaceContainer,
    onSurfaceVariant = MutedIce,
    surfaceContainerLowest = NavyBackground,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavySurfaceContainer,
    surfaceContainerHigh = NavySurfaceHigh,
    surfaceContainerHighest = NavySurfaceHigh,
    outline = NavyOutline,
    outlineVariant = NavyOutlineSubtle,
    error = ErrorRose,
    onError = IceWhite,
    errorContainer = ErrorRoseContainer,
    onErrorContainer = IceWhite,
)

val FutureAiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FutureAiTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FutureAiDarkColorScheme,
        typography = FutureAiTypography,
        shapes = FutureAiShapes,
        content = content,
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    FutureAiTheme(content = content)
}
