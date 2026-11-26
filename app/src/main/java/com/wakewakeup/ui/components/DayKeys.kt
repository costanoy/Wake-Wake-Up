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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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

private val WEEKDAYS = setOf(0, 1, 2, 3, 4)
private val WEEKEND = setOf(5, 6)

/**
 * Seven "piano keys" in one shared recessed well, printed DOM SEG TER like the alarm cards (single
 * letters would give three S keys). Each key's touch area extends into the gaps between keys so
 * it stays ≥ 48 dp wide on a 360 dp screen.
 */
@Composable
fun DayKeys(activeDays: Set<Int>, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val short = stringArrayResource(R.array.day_short)
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
                    Text(
                        short.getOrElse(index) { "" }.uppercase(),
                        style = WwuType.dayKey,
                        color = if (down) Ink else InkMuted,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * Read-only day row printed along the bottom of an alarm card. [dimmed] tones it down for a
 * switched-off alarm, so its days don't read as armed. TalkBack hears one phrase, not seven days.
 */
@Composable
fun DayPrintRow(activeDays: Set<Int>, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val short = stringArrayResource(R.array.day_short)
    val spoken = spokenDays(activeDays)
    val activeColor = if (dimmed) InkMuted else Ink
    val restColor = if (dimmed) InkOff.copy(alpha = 0.5f) else InkOff
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clearAndSetSemantics { if (spoken.isNotEmpty()) contentDescription = spoken },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DAY_DISPLAY_ORDER.forEach { index ->
            Text(
                short.getOrElse(index) { "" }.uppercase(),
                style = WwuType.label,
                color = if (index in activeDays) activeColor else restColor,
            )
        }
    }
}

/**
 * The days as said aloud: "todos os dias", "segunda a sexta", "sábado e domingo", or the days in
 * the panel's order ("segunda, quarta e sexta"). Empty when there are none.
 */
@Composable
fun spokenDays(days: Set<Int>): String {
    val names = stringArrayResource(R.array.day_full)
    val listed = DAY_DISPLAY_ORDER.filter { it in days }.map { names.getOrElse(it) { "" } }
    return when {
        listed.size == DAY_DISPLAY_ORDER.size -> stringResource(R.string.days_every_day)
        days == WEEKDAYS -> stringResource(R.string.days_weekdays)
        days == WEEKEND -> stringResource(R.string.days_weekend)
        listed.size <= 1 -> listed.firstOrNull().orEmpty()
        else -> stringResource(R.string.days_and, listed.dropLast(1).joinToString(", "), listed.last())
    }
}
