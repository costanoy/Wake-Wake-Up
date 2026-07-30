package com.wakewakeup.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds
val BgBase = Color(0xFF141017)
val BgCard = Color(0xFF1A1519)
val BgDeep = Color(0xFF1A1014)
val BgDeepHover = Color(0xFF241419)
val BrandIconBg = Color(0xFF1E1913)

// Accents
val AccentPrimary = Color(0xFFF2A93B)
val AccentLight = Color(0xFFF7D46B)
val AccentAlert = Color(0xFFE8663C)

// Text
val TextPrimary = Color(0xFFF6ECE0)
val TextOnGradient = Color(0xFFFFF4DE)
val TextSecondary = Color(0xFFB49E88)
val TextTertiary = Color(0xFF7E6E60)
val TextQuaternary = Color(0xFF6E5F52)
val TextDisabled = Color(0xFF5F5349)
val TextDisabled2 = Color(0xFF4E433B)

// Status
val DangerMuted = Color(0xFF8E5A4A)
val StatWarn = Color(0xFFC88A4A)

// Muted icon/label glyphs (mission screen escape + keypad accessory keys)
val MutedIcon = Color(0xFF8A7969)

// Gradient stops — "alarm ringing"
val RingGradientStops = listOf(
    0.00f to Color(0xFFF7D46B),
    0.26f to Color(0xFFEF8F3C),
    0.44f to Color(0xFFE8663C),
    0.64f to Color(0xFF8C3340),
    0.82f to Color(0xFF331A24),
    1.00f to Color(0xFF141017),
)

// Gradient stops — "good morning"
val GoodMorningGradientStops = listOf(
    0.00f to Color(0xFFF7D46B),
    0.22f to Color(0xFFEF9A46),
    0.40f to Color(0xFFE8663C),
    0.60f to Color(0xFF7E3340),
    0.80f to Color(0xFF2A1720),
    1.00f to Color(0xFF141017),
)

// Recurring alphas over TextPrimary (#F6ECE0)
fun Color.onLight(alpha: Float) = TextPrimary.copy(alpha = alpha)

// Recurring alphas over AccentPrimary (#F2A93B)
fun accentAlpha(alpha: Float) = AccentPrimary.copy(alpha = alpha)

// Recurring alphas over TextOnGradient (#FFF4DE)
fun onGradientAlpha(alpha: Float) = TextOnGradient.copy(alpha = alpha)
