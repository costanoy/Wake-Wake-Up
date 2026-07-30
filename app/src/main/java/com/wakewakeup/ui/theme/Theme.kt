package com.wakewakeup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WwuColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = BgBase,
    secondary = AccentLight,
    onSecondary = BgBase,
    error = AccentAlert,
    onError = TextPrimary,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,
    outline = TextDisabled,
)

/**
 * Dark mode is mandatory for this app (design brief: "Modo escuro é obrigatório") —
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
