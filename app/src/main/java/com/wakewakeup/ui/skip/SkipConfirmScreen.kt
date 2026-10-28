package com.wakewakeup.ui.skip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.CalculatorKeypad
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.ColonDots
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.GraphiteKey
import com.wakewakeup.ui.components.Lamp
import com.wakewakeup.ui.components.OrangeKey
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.components.glowShadow
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.components.weekdayName
import com.wakewakeup.ui.navigation.SkipRequest
import com.wakewakeup.ui.theme.ErrorDisplay
import com.wakewakeup.ui.theme.ErrorText
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.VfdBright
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.delay

/**
 * Where "Desativar" in the pre-alarm notification leads: what turning off means for each ring,
 * and a quick sum to confirm, so a groggy tap on a notification never silences an alarm.
 */
@Composable
fun SkipConfirmScreen(
    request: SkipRequest?,
    onDone: () -> Unit,
    viewModel: SkipConfirmViewModel = viewModel(),
) {
    LaunchedEffect(request) { viewModel.load(request) }
    BackHandler(onBack = onDone)
    val stage = viewModel.stage
    // Turned off: back to the list after a moment. The receipt notification keeps the undo.
    LaunchedEffect(stage) {
        if (stage == SkipStage.DONE) {
            delay(5_000)
            onDone()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Housing)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(
            title = stringResource(R.string.skip_confirm_title),
            backDescription = stringResource(R.string.cd_back),
            onBack = onDone,
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            when (stage) {
                SkipStage.LOADING -> Unit
                SkipStage.STALE -> Text(
                    stringResource(R.string.skip_confirm_stale),
                    style = WwuType.body,
                    color = Ink,
                    modifier = Modifier.padding(top = 24.dp, start = 4.dp, end = 4.dp),
                )
                SkipStage.CONFIRM, SkipStage.DONE -> {
                    viewModel.rows.forEach { SkipRowCard(it, turnedOff = stage == SkipStage.DONE) }
                    if (stage == SkipStage.CONFIRM) {
                        Gate(viewModel)
                    } else {
                        Row(
                            Modifier.padding(top = 20.dp, start = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Lamp(Vfd, size = 6.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.skip_confirm_done), style = WwuType.body, color = Ink)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (stage) {
                SkipStage.LOADING -> Unit
                SkipStage.CONFIRM -> {
                    GraphiteKey(stringResource(R.string.skip_confirm_keep), onClick = onDone, modifier = Modifier.weight(1f))
                    OrangeKey(stringResource(R.string.skip_confirm_turn_off), onClick = viewModel::confirm, modifier = Modifier.weight(1f))
                }
                SkipStage.DONE -> {
                    GraphiteKey(stringResource(R.string.undo), onClick = { viewModel.undo(onDone) }, modifier = Modifier.weight(1f))
                    OrangeKey(stringResource(R.string.cd_back), onClick = onDone, modifier = Modifier.weight(1f))
                }
                SkipStage.STALE -> OrangeKey(stringResource(R.string.cd_back), onClick = onDone, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SkipRowCard(row: SkipRow, turnedOff: Boolean) {
    val time = formatClock(row.occurrence.hour, row.occurrence.minute)
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .faceplate()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.alpha(if (turnedOff) 0.4f else 1f).visor(WwuShape.visorSmall).padding(7.dp)) {
            FlipNumber(
                text = time.replace(":", ""),
                size = FlipSize.S,
                spokenText = time,
                shadow = CardShadow(3.dp, 2.dp, 0.6f),
                colon = { ColonDots(4.dp, 6.dp, InkMuted, horizontalPadding = 2.dp) },
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                row.occurrence.label.ifBlank { stringResource(R.string.untitled_alarm) },
                style = WwuType.bodyStrong,
                color = Ink,
                maxLines = 2,
            )
            val ringsAgain = row.ringsAgainOn
            PrintLabel(
                if (ringsAgain != null) {
                    stringResource(R.string.skip_confirm_back_on, weekdayName(ringsAgain))
                } else {
                    stringResource(R.string.skip_confirm_one_off)
                },
            )
        }
    }
}

@Composable
private fun Gate(viewModel: SkipConfirmViewModel) {
    val answerColor = if (viewModel.wrong) ErrorDisplay else VfdBright
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PrintLabel(
            stringResource(R.string.skip_confirm_gate),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .visor(WwuShape.visorMedium, depth = 3.dp, blurRadius = 8.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfdText(
                "${viewModel.question.question} =",
                28.sp.nonScaling(),
                glowAlpha = 0.5f,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                viewModel.typed.replace("-", "−").ifEmpty { "_" },
                style = WwuType.flip(44.sp.nonScaling()).copy(shadow = glowShadow(answerColor, 8.dp, 0.55f)),
                color = answerColor,
                maxLines = 1,
            )
        }
        Text(
            if (viewModel.wrong) stringResource(R.string.wrong_try_again) else stringResource(R.string.enter_result),
            style = WwuType.caption,
            color = if (viewModel.wrong) ErrorText else InkMuted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(16.dp))
        CalculatorKeypad(onKey = viewModel::pressKey)
    }
}
