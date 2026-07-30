package com.wakewakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val baseHeights = listOf(7, 12, 18, 20, 14, 9, 15)

/**
 * Mirrors the ringing screen's escalation: bar heights scale by
 * `0.45 + 0.18 * level`, and `2 + level * 2` of the 7 bars are lit.
 */
@Composable
fun EqualizerBars(level: Int, modifier: Modifier = Modifier) {
    val litCount = 2 + level * 2
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        baseHeights.forEachIndexed { index, base ->
            val heightDp = (base * (0.45f + 0.18f * level)).dp
            val color = if (index < litCount) Color(0xFFFFF4DE) else Color(0xFFFFF4DE).copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heightDp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}
