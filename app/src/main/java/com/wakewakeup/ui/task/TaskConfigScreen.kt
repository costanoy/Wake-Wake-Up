package com.wakewakeup.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.R
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.MissionGenerator
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.CheckGlyph
import com.wakewakeup.ui.components.KeyLed
import com.wakewakeup.ui.components.LabeledRoundButton
import com.wakewakeup.ui.components.LatchKey
import com.wakewakeup.ui.components.MinusGlyph
import com.wakewakeup.ui.components.PlateDivider
import com.wakewakeup.ui.components.PlusGlyph
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.difficultyName
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.softShadow
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.edit.EditAlarmViewModel
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.VfdBright
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun TaskConfigScreen(onBack: () -> Unit, viewModel: EditAlarmViewModel) {
    val draft = viewModel.draft

    Column(modifier = Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding()) {
        BackHeader(
            title = stringResource(R.string.task_setup_title),
            backDescription = stringResource(R.string.cd_back),
            onBack = onBack,
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Mode selector: two latching keys in one well.
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .visor(WwuShape.visorMedium, lip = false)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(TaskType.MATH to R.string.mode_math, TaskType.PHRASE to R.string.mode_phrase).forEach { (type, res) ->
                    val label = stringResource(res)
                    LatchKey(
                        down = draft.taskType == type,
                        onClick = { viewModel.setTaskType(type) },
                        modifier = Modifier.weight(1f).height(72.dp),
                        shape = RoundedCornerShape(7.dp),
                        description = label,
                    ) { down ->
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            KeyLed(on = down, width = 16.dp)
                            PrintLabel(label, color = if (down) Ink else InkMuted)
                        }
                    }
                }
            }

            if (draft.taskType == TaskType.MATH) {
                MathPlate(
                    difficulty = draft.difficulty,
                    count = draft.taskCount,
                    onDifficulty = viewModel::setDifficulty,
                    onCountDelta = viewModel::changeTaskCount,
                )
            } else {
                PhrasePlate()
            }
        }

        LabeledRoundButton(
            label = stringResource(R.string.save),
            onClick = onBack,
            size = 76.dp,
            style = RoundStyle.Action,
            softShadow = softShadow(18.dp, 10.dp, 0.55f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 32.dp),
        ) { CheckGlyph(76.dp) }
    }
}

@Composable
private fun MathPlate(
    difficulty: Difficulty,
    count: Int,
    onDifficulty: (Difficulty) -> Unit,
    onCountDelta: (Int) -> Unit,
) {
    // A real problem from each level's generator, so the preview never promises something else.
    val examples = remember { Difficulty.entries.associateWith { MissionGenerator.mathQuestion(it).question } }

    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 18.dp)
            .fillMaxWidth()
            .faceplate()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrintLabel(stringResource(R.string.difficulty))
        Row(
            Modifier
                .fillMaxWidth()
                .visor(WwuShape.visor, lip = false)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Difficulty.entries.forEach { level ->
                val name = difficultyName(level)
                LatchKey(
                    down = difficulty == level,
                    onClick = { onDifficulty(level) },
                    modifier = Modifier.weight(1f).height(76.dp),
                    shape = WwuShape.key,
                    description = name,
                ) { down ->
                    Column(
                        Modifier.fillMaxSize().padding(top = 8.dp, bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        KeyLed(on = down)
                        Text(name, style = WwuType.keyLabel, color = if (down) Ink else InkMuted, maxLines = 1)
                        VfdText(examples.getValue(level), 15.sp.nonScaling(), glowAlpha = 0.45f, maxLines = 1)
                    }
                }
            }
        }
        PlateDivider(Modifier.padding(vertical = 6.dp))
        PrintLabel(stringResource(R.string.how_many))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(
                onClick = { onCountDelta(-1) },
                size = 52.dp,
                style = RoundStyle.Graphite,
                contentDescription = stringResource(R.string.cd_fewer),
                baseDepth = 3.dp,
                softShadow = softShadow(10.dp, 6.dp, 0.45f),
            ) { MinusGlyph(16.dp, 3.dp) }
            Box(
                Modifier
                    .size(width = 120.dp, height = 60.dp)
                    .visor(WwuShape.visorSmall, lip = false)
                    .clearAndSetSemantics { contentDescription = "$count" },
                contentAlignment = Alignment.Center,
            ) {
                VfdText("$count", 44.sp.nonScaling(), color = VfdBright, glowRadius = 8.dp, glowAlpha = 0.6f)
            }
            RoundButton(
                onClick = { onCountDelta(1) },
                size = 52.dp,
                style = RoundStyle.Graphite,
                contentDescription = stringResource(R.string.cd_more),
                baseDepth = 3.dp,
                softShadow = softShadow(10.dp, 6.dp, 0.45f),
            ) { PlusGlyph(16.dp, 3.dp, Ink) }
        }
    }
}

@Composable
private fun PhrasePlate() {
    val phrase = remember { MissionGenerator.phraseOfTheDay() }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 18.dp)
            .fillMaxWidth()
            .faceplate()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrintLabel(stringResource(R.string.phrase_today))
        Box(
            Modifier
                .fillMaxWidth()
                .visor(WwuShape.visorSmall, lip = false)
                .padding(14.dp),
        ) {
            VfdText(
                phrase,
                22.sp,
                style = WwuType.vfd(22.sp).copy(lineHeight = 27.sp),
                color = Vfd,
                glowAlpha = 0.45f,
            )
        }
        Text(stringResource(R.string.phrase_note), style = WwuType.bodyXS, color = InkMuted)
    }
}
