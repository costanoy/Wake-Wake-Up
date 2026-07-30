package com.wakewakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.TextDisabled
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextQuaternary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.accentAlpha

/** Compact, read-only day indicators used on the alarm-list card. */
@Composable
fun DayChipRow(activeDays: Set<Int>, modifier: Modifier = Modifier) {
    val letters = stringArrayResource(R.array.day_letters)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        letters.forEachIndexed { index, letter ->
            val active = activeDays.contains(index)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(if (active) accentAlpha(0.16f) else TextPrimary.copy(alpha = 0.05f), WwuShape.dayChip),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = WwuType.dayChipCard, color = if (active) AccentPrimary else TextDisabled)
            }
        }
    }
}

/** Large tappable day squares used on the Create/Edit alarm screen. */
@Composable
fun DaySelectorRow(activeDays: Set<Int>, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val letters = stringArrayResource(R.array.day_letters)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        letters.forEachIndexed { index, letter ->
            val active = activeDays.contains(index)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(if (active) accentAlpha(0.16f) else Color.Transparent, WwuShape.textField)
                    .border(1.dp, if (active) accentAlpha(0.45f) else TextPrimary.copy(alpha = 0.10f), WwuShape.textField)
                    .clickable { onToggle(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = WwuType.dayChipEdit, color = if (active) AccentPrimary else TextQuaternary)
            }
        }
    }
}
