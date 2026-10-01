package com.wakewakeup.ui.ring

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.R
import com.wakewakeup.session.RAMP_SECONDS
import com.wakewakeup.session.RingSession
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.ColonDots
import com.wakewakeup.ui.components.EspereBar
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.LabeledRoundButton
import com.wakewakeup.ui.components.LedMeter
import com.wakewakeup.ui.components.PlayGlyph
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.panelDate
import com.wakewakeup.ui.components.rememberReduceMotion
import com.wakewakeup.ui.components.softShadow
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.OnAction
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.espereSunrise
import com.wakewakeup.ui.theme.ringSunrise
import kotlinx.coroutines.delay
import kotlin.math.ceil

private const val VOLUME_SEGMENTS = 16

@Composable
fun RingScreen(session: RingSession, onStartMission: () -> Unit, onHoldOn: () -> Unit) {
    val holdUntil = session.holdUntilMillis
    if (holdUntil != null) {
        EspereActive(session, holdUntil, onStartMission)
    } else {
        Ringing(session, onStartMission, onHoldOn)
    }
}

@Composable
private fun Ringing(session: RingSession, onStartMission: () -> Unit, onHoldOn: () -> Unit) {
    val reduce = rememberReduceMotion()

    // The sunrise grows to full over the first minute of ringing.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.ringingSinceMillis, reduce) {
        if (reduce) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            if (now - session.ringingSinceMillis > RAMP_SECONDS * 1000L) break
            delay(250)
        }
    }
    val grow = if (reduce) 1f else 0.8f + 0.2f * ((now - session.ringingSinceMillis) / (RAMP_SECONDS * 1000f)).coerceIn(0f, 1f)

    // The display pulses like a VFD (100 → 86%, 1.6 s).
    val pulse = rememberInfiniteTransition(label = "vfdPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (reduce) 1f else 0.86f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )

    val time = formatClock(session.alarmHour, session.alarmMinute)
    val label = listOfNotNull(session.alarmLabel.takeIf { it.isNotBlank() }?.uppercase(), panelDate()).joinToString(" · ")
    val litSegments = ceil(session.volume * VOLUME_SEGMENTS).toInt().coerceIn(1, VOLUME_SEGMENTS)

    Column(
        Modifier
            .fillMaxSize()
            .ringSunrise { grow }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        EspereBar(
            active = false,
            title = stringResource(R.string.espere_title),
            subtitle = stringResource(R.string.espere_sub),
            activeTitle = stringResource(R.string.espere_active),
            onClick = onHoldOn,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            PrintLabel(label, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(14.dp))
            FlipNumber(
                text = time.replace(":", ""),
                size = FlipSize.XL,
                spokenText = time,
                shadow = CardShadow(10.dp, 4.dp, 0.7f),
                colon = { ColonDots(8.dp, 14.dp, Vfd, glow = true) },
                modifier = Modifier.graphicsLayer { alpha = pulseAlpha },
            )

            Column(
                Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 40.dp)
                    .fillMaxWidth()
                    .visor(WwuShape.visor, lip = false)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PrintLabel(stringResource(R.string.volume))
                    VfdText(
                        stringResource(if (session.volume < 1f) R.string.volume_rising else R.string.volume_max).uppercase(),
                        14.sp.nonScaling(),
                        glowAlpha = 0.5f,
                    )
                }
                LedMeter(
                    count = VOLUME_SEGMENTS,
                    isLit = { it < litSegments },
                    isHot = { it >= 13 },
                    segmentHeight = 14.dp,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        LabeledRoundButton(
            label = stringResource(R.string.start_mission),
            onClick = onStartMission,
            size = 96.dp,
            style = RoundStyle.Action,
            baseDepth = 5.dp,
            gap = 10.dp,
            labelColor = OnAction,
            labelStyle = WwuType.labelWide,
            softShadow = softShadow(24.dp, 14.dp, 0.45f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 36.dp),
        ) { PlayGlyph(24.dp, 30.dp, nudge = 3.dp) }
    }
}

@Composable
private fun EspereActive(session: RingSession, holdUntilMillis: Long, onStartMission: () -> Unit) {
    var remainingMs by remember(holdUntilMillis) { mutableLongStateOf(holdUntilMillis - System.currentTimeMillis()) }
    LaunchedEffect(holdUntilMillis) {
        while (true) {
            remainingMs = holdUntilMillis - System.currentTimeMillis()
            if (remainingMs <= 0) break
            delay(200)
        }
    }
    val totalSec = ((remainingMs + 999) / 1000).coerceAtLeast(0).toInt()
    val mmss = "%02d%02d".format(totalSec / 60, totalSec % 60)
    val spoken = pluralStringResource(R.plurals.minutes_spoken, totalSec / 60, totalSec / 60) + " %02d s".format(totalSec % 60)

    Column(
        Modifier
            .fillMaxSize()
            .espereSunrise()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        EspereBar(
            active = true,
            title = stringResource(R.string.espere_title),
            subtitle = stringResource(R.string.espere_sub),
            activeTitle = stringResource(R.string.espere_active),
            onClick = {},
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Text(stringResource(R.string.espere_returns_in), style = WwuType.bodyXS, color = InkMuted)
            Spacer(Modifier.height(16.dp))
            FlipNumber(
                text = mmss,
                size = FlipSize.L,
                spokenText = spoken,
                colon = { ColonDots(8.dp, 14.dp, InkMuted) },
            )
            Spacer(Modifier.height(36.dp))
            Row(
                Modifier
                    .visor(WwuShape.visorSmall, lip = false)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrintLabel(stringResource(R.string.espere_used))
                VfdText(stringResource(R.string.times, session.waits), 24.sp.nonScaling())
            }
            Spacer(Modifier.height(24.dp))
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LabeledRoundButton(
                label = stringResource(R.string.awake_start_mission),
                onClick = onStartMission,
                size = 84.dp,
                style = RoundStyle.Action,
                gap = 10.dp,
                labelColor = Ink,
                softShadow = softShadow(20.dp, 12.dp, 0.5f),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
            ) { PlayGlyph(20.dp, 26.dp, nudge = 2.5.dp) }
        }
    }
}
