package com.wakewakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wakewakeup.ui.theme.MutedIcon
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

private val numericKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "C")

@Composable
fun NumericKeypad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier,
    ) {
        items(numericKeys) { key ->
            val isAccessory = key == "⌫" || key == "C"
            Box(
                modifier = Modifier
                    .height(58.dp)
                    .clip(WwuShape.numericKey)
                    .background(TextPrimary.copy(alpha = 0.06f), WwuShape.numericKey)
                    .clickable { onKey(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(key, style = WwuType.numericKey, color = if (isAccessory) MutedIcon else TextPrimary)
            }
        }
    }
}
