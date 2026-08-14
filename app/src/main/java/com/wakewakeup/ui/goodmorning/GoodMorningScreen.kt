package com.wakewakeup.ui.goodmorning

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.session.FinishedInfo
import com.wakewakeup.ui.components.SunIcon
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.theme.AccentLight
import com.wakewakeup.ui.theme.BgDeep
import com.wakewakeup.ui.theme.TextOnGradient
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.onGradientAlpha
import com.wakewakeup.ui.theme.wwuGoodMorningGradient

@Composable
fun GoodMorningScreen(finished: FinishedInfo, onStartDay: () -> Unit, onSeeStats: () -> Unit) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, animationSpec = tween(600)) }

    Column(modifier = Modifier.fillMaxSize().wwuGoodMorningGradient().statusBarsPadding().navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(26.dp, 32.dp)
                .graphicsLayer {
                    alpha = rise.value
                    translationY = (1f - rise.value) * 14.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SunIcon()
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.good_morning), style = WwuType.goodMorningTitle, color = TextOnGradient)
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.woke_in, finished.tookText),
                style = WwuType.goodMorningCopy,
                color = onGradientAlpha(0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 250.dp),
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(22.dp, 0.dp, 22.dp, 26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GmStatCard(formatClock(finished.alarmHour, finished.alarmMinute), stringResource(R.string.alarm_at))
                GmStatCard(formatClock(finished.wokeHour, finished.wokeMinute), stringResource(R.string.woke_at))
                GmStatCard("${finished.waits}×", stringResource(R.string.waits_count))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(WwuShape.cta)
                    .background(BgDeep, WwuShape.cta)
                    .clickable { onStartDay() }
                    .padding(19.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.start_day), style = WwuType.ctaPrimary, color = AccentLight)
            }
            Text(
                stringResource(R.string.see_stats),
                style = WwuType.ctaSecondary,
                color = onGradientAlpha(0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeeStats() }
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun RowScope.GmStatCard(value: String, label: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(Color(0xFF141017).copy(alpha = 0.34f), WwuShape.textField)
            .border(1.dp, onGradientAlpha(0.16f), WwuShape.textField)
            .padding(12.dp, 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(value, style = WwuType.goodMorningStat, color = TextOnGradient)
        Text(label, style = WwuType.chartCaption, color = onGradientAlpha(0.65f), textAlign = TextAlign.Center)
    }
}
