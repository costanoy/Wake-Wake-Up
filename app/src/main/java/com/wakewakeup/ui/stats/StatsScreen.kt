package com.wakewakeup.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.ui.theme.AccentLight
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.BgCard
import com.wakewakeup.ui.theme.StatWarn
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextQuaternary
import com.wakewakeup.ui.theme.TextSecondary
import com.wakewakeup.ui.theme.TextTertiary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayShort = stringArrayResource(R.array.day_short)

    Column(modifier = Modifier.fillMaxSize().background(BgBase).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp, 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = TextSecondary,
                modifier = Modifier.clickable { onBack() },
            )
            Text(stringResource(R.string.stats), style = WwuType.taskName, color = TextPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ChartCard(bars = state.bars, dayShort = dayShort)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(value = state.todayText, label = stringResource(R.string.today_time), color = AccentLight)
                SummaryCard(value = "${state.waitsToday}×", label = stringResource(R.string.waits_today), color = TextPrimary)
                SummaryCard(value = state.avg7Text, label = stringResource(R.string.avg_7), color = TextPrimary)
            }

            WokeVsAlarmCard(rows = state.vsRows, dayShort = dayShort)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ChartCard(bars: List<BarPoint>, dayShort: Array<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, WwuShape.chartCard)
            .border(1.dp, TextPrimary.copy(alpha = 0.10f), WwuShape.chartCard)
            .padding(18.dp, 20.dp, 18.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text(stringResource(R.string.time_to_wake), style = WwuType.taskName, color = TextPrimary)
            Text(stringResource(R.string.time_to_wake_sub), style = WwuType.timeToWakeSub, color = TextTertiary)
        }
        if (bars.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.stats_empty),
                    style = WwuType.timeToWakeSub,
                    color = TextTertiary,
                )
            }
        } else {
            val maxValue = (bars.maxOfOrNull { it.minutesValue } ?: 1.0).coerceAtLeast(1.0)
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    val heightFraction = (bar.minutesValue / maxValue).toFloat().coerceIn(0.08f, 1f)
                    val color = if (bar.isToday) AccentLight else TextPrimary.copy(alpha = 0.13f)
                    val captionColor = if (bar.isToday) AccentLight else TextQuaternary
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(bar.label, style = WwuType.chartCaption, color = captionColor)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((150 * heightFraction).dp)
                                .background(color, WwuShape.dayChip),
                        )
                        Text(dayShort.getOrElse(bar.dayIndex) { "" }, style = WwuType.chartCaption, color = captionColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SummaryCard(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .border(1.dp, TextPrimary.copy(alpha = 0.10f), WwuShape.statCard)
            .padding(12.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(value, style = WwuType.statNumber, color = color)
        Text(label, style = WwuType.statCardLabel, color = TextTertiary)
    }
}

@Composable
private fun WokeVsAlarmCard(rows: List<VsRow>, dayShort: Array<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TextPrimary.copy(alpha = 0.10f), WwuShape.statCard)
            .padding(18.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.woke_vs), style = WwuType.eyebrow, color = TextTertiary)
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(dayShort.getOrElse(row.dayIndex) { "" }, style = WwuType.wokeVsRow, color = TextSecondary, modifier = Modifier.width(52.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.alarm, style = WwuType.wokeVsRow, color = TextTertiary)
                    Text("  →  ", style = WwuType.wokeVsRow, color = TextQuaternary)
                    Text(row.woke, style = WwuType.wokeVsRow, color = TextPrimary)
                    Text(
                        "  +${row.deltaMinutes}",
                        style = WwuType.wokeVsRow,
                        color = if (row.deltaMinutes >= 10) StatWarn else TextQuaternary,
                        modifier = Modifier.width(44.dp),
                    )
                }
            }
        }
    }
}
