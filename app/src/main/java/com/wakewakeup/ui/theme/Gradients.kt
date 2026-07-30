package com.wakewakeup.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * The reference design uses a radial-gradient centered below the bottom edge
 * (`50% 104%`), which reads as a sunrise rising from the footer. Compose's
 * radial brush is circular rather than elliptical, so the radius is derived
 * from the taller screen dimension to keep the same "rises from the bottom"
 * silhouette without visibly clipping the corners.
 */
private fun sunriseBrush(stops: List<Pair<Float, androidx.compose.ui.graphics.Color>>, size: androidx.compose.ui.geometry.Size): Brush {
    val center = Offset(size.width * 0.5f, size.height * 1.04f)
    val radius = (size.height.coerceAtLeast(size.width) * 1.3f).coerceAtLeast(1f)
    return Brush.radialGradient(
        colorStops = stops.toTypedArray(),
        center = center,
        radius = radius,
    )
}

fun Modifier.wwuRingGradient(): Modifier = drawWithCache {
    val brush = sunriseBrush(RingGradientStops, size)
    onDrawBehind { drawRect(brush) }
}

fun Modifier.wwuGoodMorningGradient(): Modifier = drawWithCache {
    val brush = sunriseBrush(GoodMorningGradientStops, size)
    onDrawBehind { drawRect(brush) }
}
