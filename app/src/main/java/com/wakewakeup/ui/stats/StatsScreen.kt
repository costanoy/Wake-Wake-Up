package com.wakewakeup.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.LedSegment
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.Action
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.VfdBright
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.BarlowCondensed
import java.text.NumberFormat
import java.util.Locale

private const val CHART_SEGMENTS = 12
private const val CHART_HOT_FROM = 8
private const val MINUTES_PER_SEGMENT = 2f
private const val LATE_MINUTES = 15

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayShort = stringArrayResource(R.array.day_short)
    val todayLabel = stringResource(R.string.today).uppercase()
    fun dayLabel(index: Int, isToday: Boolean) = if (isToday) todayLabel else dayShort.getOrElse(index) { "" }.uppercase()
    val dash = "–"

    Column(Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding()) {
        BackHeader(title = stringResource(R.string.stats), backDescription = stringResource(R.string.cd_back), onBack = onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryPlate(
                    label = stringResource(R.string.today),
                    value = state.todayMinutes?.toString() ?: dash,
                    bright = true,
                    caption = state.todayWoke?.let { stringResource(R.string.woke_at_time, it) } ?: "",
                )
                SummaryPlate(
                    label = stringResource(R.string.avg_7),
                    value = state.avgMinutes?.toString() ?: dash,
                    bright = false,
                    caption = state.avgWaits?.let { stringResource(R.string.waits_per_wake, oneDecimal(it)) } ?: "",
                )
            }

            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .faceplate()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PrintLabel(stringResource(R.string.minutes_to_wake))
                    PrintLabel(stringResource(R.string.seven_days))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .visor(WwuShape.visorSmall, lip = false)
                        .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.days.forEach { day ->
                        ChartColumn(day, dayLabel(day.dayIndex, day.isToday), dash)
                    }
                }
                if (state.loaded && !state.hasData) {
                    Text(stringResource(R.string.stats_empty), style = WwuType.bodyXS, color = InkMuted)
                }
            }

            if (state.vsRows.isNotEmpty()) {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .fillMaxWidth()
                        .faceplate(softShadow = false)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(60.dp))
                        PrintLabel(stringResource(R.string.col_alarm), modifier = Modifier.weight(1f))
                        PrintLabel(stringResource(R.string.col_woke), modifier = Modifier.weight(1f))
                        PrintLabel(stringResource(R.string.col_plus_min), modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                    val rowStyle = WwuType.vfd(19.sp.nonScaling()).copy(fontFamily = BarlowCondensed)
                    state.vsRows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PrintLabel(dayLabel(row.dayIndex, row.isToday), modifier = Modifier.width(60.dp))
                            Text(row.alarm, style = rowStyle, color = Ink, modifier = Modifier.weight(1f))
                            VfdText(row.woke, 19.sp.nonScaling(), glowAlpha = 0.45f, modifier = Modifier.weight(1f))
                            Text(
                                "+${row.deltaMinutes}",
                                style = rowStyle,
                                color = if (row.deltaMinutes > LATE_MINUTES) Action else Ink,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(48.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.SummaryPlate(label: String, value: String, bright: Boolean, caption: String) {
    Column(
        Modifier
            .weight(1f)
            .faceplate(WwuShape.faceplateSmall, softShadow = false)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PrintLabel(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VfdText(
                value,
                40.sp.nonScaling(),
                color = if (bright) VfdBright else Vfd,
                glowColor = if (bright) VfdBright else Vfd,
                glowRadius = 8.dp,
                style = WwuType.vfd(40.sp.nonScaling()).copy(lineHeight = 40.sp.nonScaling()),
            )
            Text(stringResource(R.string.min_unit), style = WwuType.caption, color = InkMuted, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(caption, style = WwuType.caption, color = InkMuted)
    }
}

/** One day: value on top, 12 stacked LED segments (≈ 2 min each, top 4 coral), day below. */
@Composable
private fun RowScope.ChartColumn(day: DayColumn, label: String, dash: String) {
    val lit = day.minutes?.let { (it / MINUTES_PER_SEGMENT).let { v -> Math.round(v).coerceIn(1, CHART_SEGMENTS) } } ?: 0
    Column(
        Modifier
            .weight(1f)
            .clearAndSetSemantics { contentDescription = "$label ${day.minutes ?: dash}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            day.minutes?.toString() ?: dash,
            style = WwuType.vfd(14.sp.nonScaling()),
            color = if (day.isToday) VfdBright else InkMuted,
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Drawn top → bottom; segment i from the bottom is (CHART_SEGMENTS − 1 − row).
            for (row in 0 until CHART_SEGMENTS) {
                val fromBottom = CHART_SEGMENTS - 1 - row
                LedSegment(
                    lit = fromBottom < lit,
                    hot = fromBottom >= CHART_HOT_FROM,
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    shape = WwuShape.segmentThin,
                )
            }
        }
        Text(label, style = WwuType.label.copy(letterSpacing = 0.04.em), color = if (day.isToday) Ink else InkMuted, maxLines = 1, softWrap = false)
    }
}

private fun oneDecimal(value: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value)
