package com.wakewakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakewakeup.ui.theme.MutedIcon
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

private val numericKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "C")
private val qwertyRows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

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
                    .background(TextPrimary.copy(alpha = 0.06f), WwuShape.numericKey)
                    .clickable { onKey(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(key, style = WwuType.numericKey, color = if (isAccessory) MutedIcon else TextPrimary)
            }
        }
    }
}

@Composable
fun QwertyKeyboard(onKey: (String) -> Unit, onSpace: () -> Unit, onBackspace: () -> Unit, spaceLabel: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        qwertyRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                row.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 36.dp)
                            .height(44.dp)
                            .background(TextPrimary.copy(alpha = 0.07f), WwuShape.alphaKey)
                            .clickable { onKey(ch.toString()) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ch.toString(), style = WwuType.listRow, color = TextPrimary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(TextPrimary.copy(alpha = 0.07f), WwuShape.alphaKey)
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center,
            ) {
                Text(spaceLabel, style = WwuType.taskDesc, color = MutedIcon)
            }
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(44.dp)
                    .background(TextPrimary.copy(alpha = 0.07f), WwuShape.alphaKey)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center,
            ) {
                Text("⌫", style = WwuType.listRow, color = TextPrimary)
            }
        }
    }
}
