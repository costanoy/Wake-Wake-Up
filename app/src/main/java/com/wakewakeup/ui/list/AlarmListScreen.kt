package com.wakewakeup.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.ui.components.DayChipRow
import com.wakewakeup.ui.components.WwuToggle
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.components.nextAlarmInText
import com.wakewakeup.ui.components.taskCardLabel
import com.wakewakeup.ui.theme.AccentLight
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.BgCard
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextSecondary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.accentAlpha
import java.time.LocalDate

@Composable
fun AlarmListScreen(
    onNewAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onStats: () -> Unit,
    viewModel: AlarmListViewModel = viewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val nextTrigger = remember(alarms) { viewModel.nextAlarmMillis(alarms) }
    val listState = rememberLazyListState()

    // Enabling an alarm moves it up into the enabled group — scroll up to reveal
    // where it landed. Disabling one must NOT auto-scroll; the list stays put.
    var justEnabledId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(alarms) {
        val id = justEnabledId
        if (id != null) {
            justEnabledId = null
            val index = alarms.indexOfFirst { it.id == id }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    // Turning off a recurring alarm asks whether to skip just the next ring or disable it outright.
    var confirmDisable by remember { mutableStateOf<Alarm?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(BgBase).statusBarsPadding().navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(22.dp, 24.dp, 22.dp, 30.dp)) {
                Text(
                    stringResource(R.string.alarms),
                    style = WwuType.alarmsTitle.copy(fontSize = 44.sp),
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(54.dp)
                        .clip(WwuShape.textField)
                        .border(1.dp, TextPrimary.copy(alpha = 0.14f), WwuShape.textField)
                        .clickable { onStats() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.BarChart,
                        contentDescription = stringResource(R.string.cd_stats_icon),
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // Always reserved, even with nothing to show, so toggling an alarm on/off
            // never shifts the list up or down by adding/removing this banner.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 16.dp)
                    .background(accentAlpha(0.08f), WwuShape.banner)
                    .border(1.dp, accentAlpha(0.18f), WwuShape.banner)
                    .padding(14.dp, 12.dp)
                    .alpha(if (nextTrigger != null) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(7.dp).background(AccentPrimary, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (nextTrigger != null) nextAlarmInText(System.currentTimeMillis(), nextTrigger) else "",
                    style = WwuType.cardLabel,
                    color = AccentLight,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = {
                            if (!alarm.enabled) {
                                justEnabledId = alarm.id
                                viewModel.toggleEnabled(alarm)
                            } else if (alarm.days.isNotEmpty()) {
                                confirmDisable = alarm
                            } else {
                                viewModel.toggleEnabled(alarm)
                            }
                        },
                        onCancelSkip = { viewModel.toggleSkipNext(alarm) },
                        onOpen = { onEditAlarm(alarm.id) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 44.dp)
                .size(62.dp)
                .clip(WwuShape.fab)
                .background(AccentPrimary, WwuShape.fab)
                .clickable { onNewAlarm() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_alarm), tint = BgBase)
        }

        confirmDisable?.let { alarm ->
            val dayShort = stringArrayResource(R.array.day_short)
            val dayIndex = remember(alarm) { isoDayIndexOf(LocalDate.ofEpochDay(nextTriggerEpochDay(alarm))) }
            AlertDialog(
                onDismissRequest = { confirmDisable = null },
                title = { Text(stringResource(R.string.disable_alarm_title)) },
                text = { Text(stringResource(R.string.disable_alarm_body, dayShort.getOrElse(dayIndex) { "" })) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.toggleSkipNext(alarm)
                        confirmDisable = null
                    }) { Text(stringResource(R.string.disable_skip_once)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.toggleEnabled(alarm)
                        confirmDisable = null
                    }) { Text(stringResource(R.string.disable_completely)) }
                },
            )
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onCancelSkip: () -> Unit, onOpen: () -> Unit) {
    val dim = if (alarm.enabled) 1f else 0.42f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WwuShape.alarmCard)
            .background(BgCard, WwuShape.alarmCard)
            .border(1.dp, TextPrimary.copy(alpha = 0.10f), WwuShape.alarmCard)
            .clickable { onOpen() }
            .padding(15.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.alpha(dim)) {
                Text(formatClock(alarm.hour, alarm.minute), style = WwuType.cardTime, color = TextPrimary)
                Text(alarm.label, style = WwuType.cardLabel, color = TextSecondary)
            }
            WwuToggle(checked = alarm.enabled, onCheckedChange = { onToggle() })
        }
        Row(
            modifier = Modifier.alpha(dim),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DayChipRow(activeDays = alarm.days)
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(TextPrimary.copy(alpha = 0.12f)))
            Box(
                modifier = Modifier
                    .background(TextPrimary.copy(alpha = 0.06f), WwuShape.chip)
                    .padding(9.dp, 5.dp),
            ) {
                Text(taskCardLabel(alarm.taskType, alarm.difficulty), style = WwuType.dayChipCard, color = TextSecondary)
            }
        }
        val skipDate = alarm.skipDate
        if (skipDate != null) {
            val dayShort = stringArrayResource(R.array.day_short)
            val dayIndex = remember(skipDate) { isoDayIndexOf(LocalDate.ofEpochDay(skipDate)) }
            Box(
                modifier = Modifier
                    .clip(WwuShape.chip)
                    .background(accentAlpha(0.16f), WwuShape.chip)
                    .clickable { onCancelSkip() }
                    .padding(9.dp, 5.dp),
            ) {
                Text(
                    stringResource(R.string.skip_next_on, dayShort.getOrElse(dayIndex) { "" }),
                    style = WwuType.dayChipCard,
                    color = AccentPrimary,
                )
            }
        }
    }
}
