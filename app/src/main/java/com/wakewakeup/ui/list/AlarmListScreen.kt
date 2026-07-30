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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
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

@Composable
fun AlarmListScreen(
    onNewAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onStats: () -> Unit,
    viewModel: AlarmListViewModel = viewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val nextTrigger = remember(alarms) { viewModel.nextAlarmMillis(alarms) }

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

            if (nextTrigger != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 16.dp)
                        .background(accentAlpha(0.08f), WwuShape.banner)
                        .border(1.dp, accentAlpha(0.18f), WwuShape.banner)
                        .padding(14.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(7.dp).background(AccentPrimary, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        nextAlarmInText(System.currentTimeMillis(), nextTrigger),
                        style = WwuType.cardLabel,
                        color = AccentLight,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(alarm = alarm, onToggle = { viewModel.toggleEnabled(alarm) }, onOpen = { onEditAlarm(alarm.id) })
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 44.dp)
                .size(62.dp)
                .background(AccentPrimary, WwuShape.fab)
                .clickable { onNewAlarm() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_alarm), tint = BgBase)
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onOpen: () -> Unit) {
    val dim = if (alarm.enabled) 1f else 0.42f
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
    }
}
