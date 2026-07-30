package com.wakewakeup.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.TextPrimary

@Composable
fun WwuToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val trackColor by animateColorAsState(if (checked) AccentPrimary else TextPrimary.copy(alpha = 0.14f), label = "track")
    val knobOffset by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knob")
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .background(trackColor, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = knobOffset)
                .size(24.dp)
                .background(BgBase, CircleShape),
        )
    }
}
