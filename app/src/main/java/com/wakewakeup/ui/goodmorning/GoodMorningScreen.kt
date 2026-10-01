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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.session.FinishedInfo
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.CheckGlyph
import com.wakewakeup.ui.components.Chevron
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VisorStat
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.components.panelDate
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.goodMorningSunrise

@Composable
fun GoodMorningScreen(finished: FinishedInfo, onStartDay: () -> Unit, onSeeStats: () -> Unit) {
    val minutes = (finished.durationSec + 30) / 60
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
                        text = "%02d".format(minutes),
                        size = FlipSize.XL,
                        spokenText = pluralStringResource(R.plurals.minutes_spoken, minutes, minutes),
                        shadow = CardShadow(14.dp, 6.dp, 0.45f),
                    )
                    Text(
                        stringResource(R.string.min_unit).uppercase(),
                        style = WwuType.gmUnit,
                        color = Ink,
                        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                    )
                }
                Text(stringResource(R.string.to_really_wake), style = WwuType.bodyS, color = Ink)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrintLabel(stringResource(R.string.done), color = Ink)
                    RoundButton(
                        onClick = onStartDay,
                        size = 60.dp,
                        style = RoundStyle.Action,
                        contentDescription = stringResource(R.string.done),
                        baseDepth = 3.dp,
                    ) { CheckGlyph(60.dp) }
                }
            }
        }
    }
}
