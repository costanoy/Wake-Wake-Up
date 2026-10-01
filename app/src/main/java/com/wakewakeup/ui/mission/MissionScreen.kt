package com.wakewakeup.ui.mission

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.R
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.MISSION_TOTAL_SECONDS
import com.wakewakeup.session.MissionGenerator
import com.wakewakeup.session.MissionState
import com.wakewakeup.ui.components.CalcKeys
import com.wakewakeup.ui.components.CalculatorKeypad
import com.wakewakeup.ui.components.CheckGlyph
import com.wakewakeup.ui.components.LedMeter
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.glowShadow
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.rememberReduceMotion
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.ErrorDisplay
import com.wakewakeup.ui.theme.ErrorText
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.VfdBright
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlin.math.ceil

private const val TIMER_SEGMENTS = 20
private const val TIMER_DANGER_SEGMENTS = 5

@Composable
fun MissionScreen(
    mission: MissionState,
    onGiveUp: () -> Unit,
    onKey: (String) -> Unit,
    onTypedChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val isPhrase = mission.type == TaskType.PHRASE
    Column(
        Modifier
            .fillMaxSize()
            .background(Housing)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrintLabel(stringResource(R.string.mission_seconds, mission.secondsLeft))
            VfdText(
                if (isPhrase) {
                    stringResource(R.string.type_phrase_short).uppercase()
                } else {
                    stringResource(R.string.problem_of, (mission.index + 1).coerceAtMost(mission.count), mission.count).uppercase()
                },
                18.sp.nonScaling(),
                glowAlpha = 0.5f,
            )
        }

        // Timer: 20 LED segments turning off right → left, one per step; the first 5 are the danger zone.
        val lit = ceil(mission.secondsLeft.toFloat() / MISSION_TOTAL_SECONDS * TIMER_SEGMENTS).toInt()
        LedMeter(
            count = TIMER_SEGMENTS,
            isLit = { it < lit },
            isHot = { it < TIMER_DANGER_SEGMENTS },
            segmentHeight = 8.dp,
            shape = WwuShape.segmentThin,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp),
        )

        if (isPhrase) {
            PhraseBody(mission, onTypedChange, onConfirm)
        } else {
            MathBody(mission, onKey, onConfirm, onGiveUp)
        }
    }
}

@Composable
private fun rememberShake(trigger: Boolean): Animatable<Float, *> {
    val shake = remember { Animatable(0f) }
    val reduce = rememberReduceMotion()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.Reject)
        if (reduce) return@LaunchedEffect
        // 3 oscillations, 8 → 0 dp, ~320 ms.
        listOf(-8f, 8f, -6f, 6f, -3f, 3f, 0f).forEach { shake.animateTo(it, tween(45)) }
    }
    return shake
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.MathBody(
    mission: MissionState,
    onKey: (String) -> Unit,
    onConfirm: () -> Unit,
    onGiveUp: () -> Unit,
) {
    val shake = rememberShake(mission.wrong)
    val (message, messageColor) = when {
        mission.wrong -> stringResource(R.string.wrong_try_again) to ErrorText
        mission.justCorrect -> stringResource(R.string.correct_next) to VfdBright
        else -> stringResource(R.string.enter_result) to InkMuted
    }
    val answerColor = if (mission.wrong) ErrorDisplay else VfdBright

    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .fillMaxWidth()
                .height(150.dp)
                .visor(WwuShape.visorMedium, depth = 3.dp, blurRadius = 8.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            VfdText("${mission.question} =", 34.sp.nonScaling(), glowRadius = 8.dp, glowAlpha = 0.5f, maxLines = 1)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    message,
                    style = WwuType.caption,
                    color = messageColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                val size = 64.sp.nonScaling()
                Text(
                    mission.typed.replace("-", "−").ifEmpty { "_" },
                    style = WwuType.flip(size).copy(shadow = glowShadow(answerColor, 10.dp, 0.55f)),
                    color = answerColor,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        CalculatorKeypad(onKey = { key -> if (key == CalcKeys.OK) onConfirm() else onKey(key) })

        Spacer(Modifier.height(16.dp))
        PrintLabel(
            stringResource(R.string.cant_solve),
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onGiveUp)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PhraseBody(
    mission: MissionState,
    onTypedChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val shake = rememberShake(mission.wrong)

    val phrase = mission.phrase
    val matched = remember(phrase, mission.typed) { MissionGenerator.matchedPrefixLength(phrase, mission.typed) }
    val brightGlow = glowShadow(VfdBright, 8.dp, 0.55f)
    val display = buildAnnotatedString {
        withStyle(SpanStyle(color = VfdBright, shadow = brightGlow)) { append(phrase.take(matched)) }
        withStyle(SpanStyle(color = Vfd.copy(alpha = 0.45f))) { append(phrase.drop(matched)) }
    }

    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Box(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
                .fillMaxWidth()
                .visor(WwuShape.visorMedium, depth = 3.dp, blurRadius = 8.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .semantics { contentDescription = phrase },
        ) {
            Text(display, style = WwuType.vfd(28.sp).copy(lineHeight = 34.sp))
        }

        Row(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .graphicsLayer { translationX = shake.value.dp.toPx() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .visor(WwuShape.visorSmall, lip = false)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = mission.typed,
                    onValueChange = onTypedChange,
                    textStyle = WwuType.bodyInput.copy(color = if (mission.wrong) ErrorDisplay else Ink),
                    cursorBrush = SolidColor(Vfd),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            RoundButton(
                onClick = onConfirm,
                size = 52.dp,
                style = RoundStyle.Action,
                contentDescription = stringResource(R.string.confirm),
                baseDepth = 3.dp,
            ) { CheckGlyph(52.dp) }
        }

        Text(
            if (mission.wrong) stringResource(R.string.wrong_try_again) else stringResource(R.string.chars_of, matched, phrase.length),
            style = WwuType.caption,
            color = if (mission.wrong) ErrorText else InkMuted,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
