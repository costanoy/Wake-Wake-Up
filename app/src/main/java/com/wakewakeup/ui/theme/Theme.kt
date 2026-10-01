package com.wakewakeup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WwuColorScheme = darkColorScheme(
    primary = Action,
    onPrimary = OnAction,
    secondary = Vfd,
    onSecondary = Housing,
    error = ErrorDisplay,
    onError = Ink,
    background = Housing,
    onBackground = Ink,
    surface = PlateBottom,
    onSurface = Ink,
    surfaceVariant = PlateTop,
    onSurfaceVariant = InkMuted,
    outline = InkOff,
)

/**
 * Dark mode is mandatory for this app (design brief: "todas no modo escuro, é obrigatório") —
 * there is no light color scheme, regardless of the system setting.
 */
@Composable
fun WakeWakeUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WwuColorScheme,
        typography = WwuTypography,
        content = content,
    )
}
