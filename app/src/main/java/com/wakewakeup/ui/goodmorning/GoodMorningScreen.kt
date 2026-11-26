package com.wakewakeup.ui.goodmorning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.session.FinishedInfo
import com.wakewakeup.session.Occurrence
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.CheckGlyph
import com.wakewakeup.ui.components.Chevron
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.GraphiteKey
import com.wakewakeup.ui.components.Lamp
import com.wakewakeup.ui.components.OrangeKey
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VisorStat
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.components.panelDate
import com.wakewakeup.ui.components.rememberReduceMotion
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.goodMorningSunrise
import kotlinx.coroutines.delay

private const val FLIP_DELAY_MS = 350L

@Composable
fun GoodMorningScreen(
    finished: FinishedInfo,
    onStartDay: () -> Unit,
    onSeeStats: () -> Unit,
    companions: List<Occurrence> = emptyList(),
    companionsSkipped: Boolean = false,
    onSkipCompanions: () -> Unit = {},
    onUndoCompanions: () -> Unit = {},
    /** The last week's average time to wake up, in minutes; null when there is no earlier wake-up. */
    averageMinutes: Int? = null,
    /** The history was read: say how today compares, or that it is the first. */
    comparisonReady: Boolean = false,
) {
    val minutes = (finished.durationSec + 30) / 60
    // Under a minute the card shows seconds, which is the honest unit for a quick one.
    val inSeconds = finished.durationSec < 60
    val value = if (inSeconds) finished.durationSec else minutes
    val target = "%02d".format(value)
    // The digits come in as 00 and flip once to the value; with "remove animations" they just arrive.
    val reduce = rememberReduceMotion()
    var shown by rememberSaveable(target) { mutableStateOf(if (reduce) target else "00") }
    LaunchedEffect(target, reduce) {
        if (!reduce) delay(FLIP_DELAY_MS)
        shown = target
    }
    Column(
        Modifier
            .fillMaxSize()
            .goodMorningSunrise()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PrintLabel(panelDate(), color = Ink, modifier = Modifier.alpha(0.85f))
                Text(stringResource(R.string.good_morning), style = WwuType.display, color = Ink)
            }
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.you_took), style = WwuType.bodyS, color = Ink)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlipNumber(
                        text = shown,
                        size = FlipSize.XL,
                        spokenText = if (inSeconds) {
                            pluralStringResource(R.plurals.gm_seconds_spoken, value, value)
                        } else {
                            pluralStringResource(R.plurals.minutes_spoken, minutes, minutes)
                        },
                        shadow = CardShadow(14.dp, 6.dp, 0.45f),
                    )
                    Text(
                        stringResource(if (inSeconds) R.string.second_short else R.string.min_unit).uppercase(),
                        style = WwuType.gmUnit,
                        color = Ink,
                        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                    )
                }
                Text(stringResource(R.string.to_really_wake), style = WwuType.bodyS, color = Ink)
                if (comparisonReady) {
                    // Compared with their own week, never with a score: no points, streaks or praise.
                    val diff = if (averageMinutes == null) 0 else averageMinutes - minutes
                    Text(
                        when {
                            averageMinutes == null -> stringResource(R.string.gm_first)
                            diff > 0 -> stringResource(R.string.gm_faster, diff)
                            diff < 0 -> stringResource(R.string.gm_slower, -diff)
                            else -> stringResource(R.string.gm_same)
                        },
                        style = WwuType.bodyStrong,
                        color = Ink,
                    )
                }
            }
            if (companions.isNotEmpty()) {
                // Backup alarms still to come: the mission just proved the user is up.
                Row(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp)
                        .fillMaxWidth()
                        .faceplate(WwuShape.faceplate, softShadow = false, baseDepth = 3.dp)
                        .padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (companionsSkipped) {
                        Lamp(Vfd, size = 6.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.gm_skipped), style = WwuType.bodyXS, color = Ink, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        GraphiteKey(stringResource(R.string.undo), onClick = onUndoCompanions)
                    } else {
                        val times = companions.joinToString(" · ") { formatClock(it.hour, it.minute) }
                        Text(stringResource(R.string.gm_skip_also, times), style = WwuType.bodyXS, color = Ink, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OrangeKey(stringResource(R.string.skip_once), onClick = onSkipCompanions)
                    }
                }
            }
            Spacer(Modifier.heightIn(min = 24.dp))
        }

        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                .fillMaxWidth()
                .dropShadow(WwuShape.faceplateLarge, Shadow(radius = 28.dp, color = Color(0xFF3C140A), offset = DpOffset(0.dp, 14.dp), alpha = 0.45f))
                .faceplate(WwuShape.faceplateLarge, softShadow = false, baseDepth = 3.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VisorStat(stringResource(R.string.alarm_at), formatClock(finished.alarmHour, finished.alarmMinute))
                VisorStat(stringResource(R.string.woke_at), formatClock(finished.wokeHour, finished.wokeMinute), bright = true)
                VisorStat(stringResource(R.string.waits_count), stringResource(R.string.times, finished.waits))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onSeeStats),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.see_stats), style = WwuType.bodyS, color = InkMuted)
                    Chevron(color = InkMuted, size = 12.dp)
                }
                // CONCLUIR and its ✓ are one target, and one stop for TalkBack: a thumb that lands
                // on the caption starts the day too.
                val done = stringResource(R.string.done)
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onStartDay)
                        .clearAndSetSemantics { contentDescription = done },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrintLabel(done, color = Ink)
                    RoundButton(
                        onClick = onStartDay,
                        size = 60.dp,
                        style = RoundStyle.Action,
                        contentDescription = null,
                        baseDepth = 3.dp,
                    ) { CheckGlyph(60.dp) }
                }
            }
        }
    }
}
