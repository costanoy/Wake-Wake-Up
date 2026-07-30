package com.wakewakeup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reproduces the "Good morning" sunrise glyph faithfully: a 3.5px horizon,
 * a five-ray burst (3.2px, rgba(255,244,222,.85)), and a semicircle sun with
 * a vertical #FFF4DE → #F7D46B gradient. Drawn on a 100x100 logical grid,
 * matching the source SVG, then scaled to [size].
 */
@Composable
fun SunIcon(size: Dp = 72.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 100f
        fun p(x: Float, y: Float) = Offset(x * scale, y * scale)

        val rayColor = Color(0xFFFFF4DE).copy(alpha = 0.85f)
        val rayWidth = 3.2f * scale
        val rays = listOf(
            p(50f, 36f) to p(50f, 25f),
            p(37f, 41f) to p(31f, 33f),
            p(63f, 41f) to p(69f, 33f),
            p(26f, 51f) to p(18f, 46f),
            p(74f, 51f) to p(82f, 46f),
        )
        rays.forEach { (start, end) ->
            drawLine(rayColor, start, end, strokeWidth = rayWidth, cap = StrokeCap.Round)
        }

        drawLine(
            color = Color(0xFFFFF4DE).copy(alpha = 0.55f),
            start = p(20f, 63f),
            end = p(80f, 63f),
            strokeWidth = 3.5f * scale,
            cap = StrokeCap.Round,
        )

        val sunTop = p(50f, 44f)
        val sunBottom = p(50f, 63f)
        val sunBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF4DE), Color(0xFFF7D46B)),
            startY = sunTop.y,
            endY = sunBottom.y,
        )
        val radius = 19f * scale
        val topLeft = Offset(50f * scale - radius, sunBottom.y - radius)
        drawArc(
            brush = sunBrush,
            startAngle = 0f,
            sweepAngle = -180f,
            useCenter = true,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
        )
    }
}
