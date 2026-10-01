package com.wakewakeup.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

/** Alarm ringing: radial-gradient(130% 62% at 50% 112%, #F7D46B 0%, #E8663C 28%, #6B2433 58%, #141017 88%). */
private val RingStops = arrayOf(
    0.00f to Color(0xFFF7D46B),
    0.28f to Color(0xFFE8663C),
    0.58f to Color(0xFF6B2433),
    0.88f to Housing,
)

/** Espere active: radial-gradient(130% 42% at 50% 112%, #8A3A2C 0%, #3A1A22 50%, #141017 90%). */
private val EspereStops = arrayOf(
    0.00f to Color(0xFF8A3A2C),
    0.50f to Color(0xFF3A1A22),
    0.90f to Housing,
)

/** Bom dia: linear-gradient(180deg, #3A1622 0%, #8A2E34 30%, #E8663C 62%, #F7D46B 100%). */
private val GoodMorningStops = arrayOf(
    0.00f to Color(0xFF3A1622),
    0.30f to Color(0xFF8A2E34),
    0.62f to Color(0xFFE8663C),
    1.00f to Color(0xFFF7D46B),
)

/**
 * CSS `radial-gradient(rx ry at cx cy, …)` as an ellipse. Compose's radial brush is a
 * circle, so it's drawn with radius rx inside a vertically squashed (ry/rx) space.
 * [grow] scales the ellipse (the sunrise "rising" over the first minute of ringing).
 */
private fun Modifier.ellipticalSunrise(
    stops: Array<Pair<Float, Color>>,
    rxFraction: Float,
    ryFraction: Float,
    grow: () -> Float,
): Modifier = drawWithCache {
    val g = grow().coerceAtLeast(0.01f)
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.5f, h * 1.12f)
    val rx = (w * rxFraction * g).coerceAtLeast(1f)
    val ry = (h * ryFraction * g).coerceAtLeast(1f)
    val k = ry / rx
    val brush = Brush.radialGradient(colorStops = stops, center = center, radius = rx)
    val top = center.y - center.y / k
    val bottom = center.y + (h - center.y) / k
    onDrawBehind {
        drawRect(Housing)
        scale(scaleX = 1f, scaleY = k, pivot = center) {
            drawRect(brush, topLeft = Offset(0f, top), size = Size(w, bottom - top))
        }
    }
}

fun Modifier.ringSunrise(grow: () -> Float = { 1f }): Modifier =
    ellipticalSunrise(RingStops, rxFraction = 1.3f, ryFraction = 0.62f, grow = grow)

fun Modifier.espereSunrise(): Modifier =
    ellipticalSunrise(EspereStops, rxFraction = 1.3f, ryFraction = 0.42f, grow = { 1f })

fun Modifier.goodMorningSunrise(): Modifier = drawWithCache {
    val brush = Brush.verticalGradient(colorStops = GoodMorningStops)
    onDrawBehind { drawRect(brush) }
}
