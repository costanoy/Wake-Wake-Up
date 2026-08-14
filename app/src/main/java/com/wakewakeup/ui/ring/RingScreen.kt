package com.wakewakeup.ui.ring

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.session.RingSession
import com.wakewakeup.ui.components.EqualizerBars
import com.wakewakeup.ui.theme.AccentLight
import com.wakewakeup.ui.theme.BgDeep
import com.wakewakeup.ui.theme.TextOnGradient
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.onGradientAlpha
import com.wakewakeup.ui.theme.wwuRingGradient
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RingScreen(session: RingSession, onStartMission: () -> Unit, onHoldOn: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().wwuRingGradient().statusBarsPadding().navigationBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(26.dp, 34.dp, 26.dp, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(session.alarmLabel.uppercase(), style = WwuType.ringingEyebrow, color = onGradientAlpha(0.62f))
            Text(
                "%02d:%02d".format(session.alarmHour, session.alarmMinute),
                style = WwuType.ringClock,
                color = TextOnGradient,
            )
            Text(ringDateText(), style = WwuType.listRow, color = onGradientAlpha(0.7f))
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PulsingSpeaker()
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.ringing), style = WwuType.ctaSecondary, color = TextOnGradient)
            Spacer(Modifier.height(8.dp))
            EqualizerBars(level = session.level)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.volume_pct, (40 + session.level * 20).coerceAtMost(100)),
                style = WwuType.taskDesc,
                color = onGradientAlpha(0.72f),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(26.dp, 0.dp, 26.dp, 34.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(WwuShape.cta)
                    .background(BgDeep, WwuShape.cta)
                    .clickable { onStartMission() }
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.start_mission), style = WwuType.ctaPrimary, color = AccentLight)
            }

            val holdUntil = session.holdUntilMillis
            if (holdUntil != null) {
                HoldCountdown(holdUntilMillis = holdUntil)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(WwuShape.cta)
                        .border(1.dp, onGradientAlpha(0.42f), WwuShape.cta)
                        .clickable { onHoldOn() }
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.wait_5), style = WwuType.ctaSecondary, color = TextOnGradient)
                }
            }

            if (session.waits > 0) {
                Text(
                    stringResource(R.string.wait_used, session.waits),
                    style = WwuType.taskDesc,
                    color = onGradientAlpha(0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HoldCountdown(holdUntilMillis: Long) {
    var remainingMs by remember(holdUntilMillis) { mutableLongStateOf(holdUntilMillis - System.currentTimeMillis()) }
    LaunchedEffect(holdUntilMillis) {
        while (true) {
            remainingMs = holdUntilMillis - System.currentTimeMillis()
            if (remainingMs <= 0) break
            delay(200)
        }
    }
    val totalSec = (remainingMs / 1000).coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WwuShape.cta)
            .border(1.dp, onGradientAlpha(0.42f), WwuShape.cta)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "%d:%02d".format(totalSec / 60, totalSec % 60),
            style = WwuType.ctaSecondary,
            color = TextOnGradient,
        )
    }
}

@Composable
private fun PulsingSpeaker() {
    val transition = rememberInfiniteTransition(label = "wwuPulse")
    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        PulseRing(transition, delayMs = 0)
        PulseRing(transition, delayMs = 1100)
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(Color(0xFF141017).copy(alpha = 0.35f), CircleShape)
                .border(1.dp, onGradientAlpha(0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.cd_speaker), tint = TextOnGradient)
        }
    }
}

@Composable
private fun PulseRing(transition: InfiniteTransition, delayMs: Int) {
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delayMs, StartOffsetType.FastForward),
        ),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delayMs, StartOffsetType.FastForward),
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .size(74.dp)
            .scale(scale)
            .border(1.dp, onGradientAlpha(alpha), CircleShape),
    )
}

private fun ringDateText(): String {
    val locale = Locale.getDefault()
    val pattern = if (locale.language == "pt") "EEEE, d 'de' MMMM" else "EEEE, MMMM d"
    return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern, locale))
}
