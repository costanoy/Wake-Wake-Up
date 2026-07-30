package com.wakewakeup.ui.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.MissionState
import com.wakewakeup.ui.components.NumericKeypad
import com.wakewakeup.ui.components.QwertyKeyboard
import com.wakewakeup.ui.theme.AccentAlert
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.MutedIcon
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextSecondary
import com.wakewakeup.ui.theme.TextTertiary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.accentAlpha

@Composable
fun MissionScreen(
    mission: MissionState,
    onGiveUp: () -> Unit,
    onKey: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val urgent = mission.secondsLeft <= 30
    val accentColor = if (urgent) AccentAlert else AccentPrimary
    val hasInput = mission.typed.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(22.dp, 16.dp, 22.dp, 22.dp),
    ) {
        Text(
            stringResource(R.string.cant_solve),
            style = WwuType.listRow,
            color = MutedIcon,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onGiveUp() }
                .padding(16.dp, 9.dp),
        )

        Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "%d:%02d".format(mission.secondsLeft / 60, mission.secondsLeft % 60),
                    style = WwuType.missionCountdown,
                    color = accentColor,
                )
                Text(
                    stringResource(R.string.step_of, (mission.index + 1).coerceAtMost(mission.count), mission.count),
                    style = WwuType.taskDesc,
                    color = TextTertiary,
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(TextPrimary.copy(alpha = 0.10f), WwuShape.dayChip)) {
                val fraction = (mission.secondsLeft / 120f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(accentColor, WwuShape.dayChip),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, tint = TextTertiary, modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.sound_paused), style = WwuType.taskDesc, color = TextTertiary)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mission.type) {
                TaskType.PHRASE -> PhraseBody(mission, onKey)
                else -> MathBody(mission, onKey)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (hasInput) AccentPrimary else accentAlpha(0.22f), WwuShape.cta)
                .clickable(enabled = hasInput) { onConfirm() }
                .padding(19.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.confirm),
                style = WwuType.ctaPrimary,
                color = if (hasInput) BgBase else TextPrimary.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun MathBody(mission: MissionState, onKey: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(mission.question, style = WwuType.missionQuestion, color = TextPrimary)
            Spacer(Modifier.height(20.dp))
            Text(
                mission.typed,
                style = WwuType.mathAnswerField,
                color = if (mission.wrong) AccentAlert else TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            )
        }
        NumericKeypad(onKey = onKey)
    }
}

@Composable
private fun PhraseBody(mission: MissionState, onKey: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.type_phrase), style = WwuType.eyebrow, color = TextTertiary)
            Spacer(Modifier.height(18.dp))
            Text(mission.phrase, style = WwuType.phraseTarget, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TextPrimary.copy(alpha = 0.05f), WwuShape.textField)
                    .padding(14.dp),
            ) {
                Text(
                    mission.typed,
                    style = WwuType.phraseField,
                    color = if (mission.wrong) AccentAlert else TextPrimary,
                )
            }
        }
        QwertyKeyboard(
            onKey = onKey,
            onSpace = { onKey(" ") },
            onBackspace = { onKey("⌫") },
            spaceLabel = stringResource(R.string.space),
        )
    }
}
