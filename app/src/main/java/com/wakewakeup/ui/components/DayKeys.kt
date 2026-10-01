package com.wakewakeup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.InkOff
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

/**
 * [com.wakewakeup.data.Alarm.days] is indexed 0 = Monday .. 6 = Sunday; the panel reads
 * Sunday first, like the design (DOM SEG TER QUA QUI SEX SÁB).
 */
val DAY_DISPLAY_ORDER = listOf(6, 0, 1, 2, 3, 4, 5)

/**
 * Seven "piano keys" in one shared recessed well. Each key's touch area extends into the
 * gaps between keys so it stays ≥ 48 dp wide on a 360 dp screen.
 */
@Composable
fun DayKeys(activeDays: Set<Int>, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val letters = stringArrayResource(R.array.day_letters)
    val names = stringArrayResource(R.array.day_full)
    Row(
        modifier
            .fillMaxWidth()
            .visor(WwuShape.visor, lip = false)
            .padding(horizontal = 4.dp, vertical = 5.dp),
    ) {
        DAY_DISPLAY_ORDER.forEach { index ->
            LatchKey(
                down = index in activeDays,
                onClick = { onToggle(index) },
                modifier = Modifier.weight(1f).height(54.dp),
                description = names.getOrElse(index) { "" },
                visualInset = 1.dp,
            ) { down ->
                Column(
                    Modifier.fillMaxSize().padding(top = 6.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    KeyLed(on = down)
                    Text(letters.getOrElse(index) { "" }, style = WwuType.dayKey, color = if (down) Ink else InkMuted)
                }
            }
        }
    }
}

/** Read-only day row printed along the bottom of an alarm card. */
@Composable
fun DayPrintRow(activeDays: Set<Int>, modifier: Modifier = Modifier) {
    val short = stringArrayResource(R.array.day_short)
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        DAY_DISPLAY_ORDER.forEach { index ->
            Text(
                short.getOrElse(index) { "" }.uppercase(),
                style = WwuType.label,
                color = if (index in activeDays) Ink else InkOff,
            )
        }
    }
}
