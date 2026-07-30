package com.wakewakeup.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.ui.components.taskTypeDesc
import com.wakewakeup.ui.components.taskTypeName
import com.wakewakeup.ui.edit.EditAlarmViewModel
import com.wakewakeup.ui.theme.AccentLight
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.TextDisabled2
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextSecondary
import com.wakewakeup.ui.theme.TextTertiary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import com.wakewakeup.ui.theme.accentAlpha

@Composable
fun TaskConfigScreen(onBack: () -> Unit, viewModel: EditAlarmViewModel) {
    val draft = viewModel.draft

    Column(modifier = Modifier.fillMaxSize().background(BgBase).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp, 34.dp, 22.dp, 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = TextSecondary,
                modifier = Modifier.size(28.dp).clickable { onBack() },
            )
            Text(stringResource(R.string.task_setup_title), style = WwuType.taskName, color = TextPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(TaskType.MATH, TaskType.PHRASE, TaskType.MEMORY, TaskType.SHAKE).forEach { type ->
                    TaskOptionCard(
                        selected = draft.taskType == type,
                        name = taskTypeName(type),
                        desc = taskTypeDesc(type),
                        onClick = { viewModel.setTaskType(type) },
                    )
                }
            }

            if (draft.taskType == TaskType.MATH) {
                DifficultyBlock(
                    difficulty = draft.difficulty,
                    count = draft.taskCount,
                    onDifficulty = viewModel::setDifficulty,
                    onCountDelta = viewModel::changeTaskCount,
                )
            }
        }
    }
}

@Composable
private fun TaskOptionCard(selected: Boolean, name: String, desc: String, onClick: () -> Unit) {
    val borderColor = if (selected) accentAlpha(0.45f) else TextPrimary.copy(alpha = 0.10f)
    val bg = if (selected) accentAlpha(0.07f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, WwuShape.taskCard)
            .border(1.dp, borderColor, WwuShape.taskCard)
            .clickable { onClick() }
            .padding(16.dp, 15.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) AccentPrimary else TextDisabled2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(modifier = Modifier.size(9.dp).background(AccentPrimary, CircleShape))
            }
        }
        Column {
            Text(name, style = WwuType.taskName, color = TextPrimary)
            Text(desc, style = WwuType.taskDesc, color = TextTertiary)
        }
    }
}

@Composable
private fun DifficultyBlock(difficulty: Difficulty, count: Int, onDifficulty: (Difficulty) -> Unit, onCountDelta: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentAlpha(0.06f), WwuShape.configBlock)
            .border(1.dp, accentAlpha(0.16f), WwuShape.configBlock)
            .padding(18.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.difficulty), style = WwuType.taskName, color = AccentLight)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(Difficulty.EASY to R.string.easy, Difficulty.MEDIUM to R.string.medium, Difficulty.HARD to R.string.hard).forEach { (diff, res) ->
                val active = difficulty == diff
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (active) AccentPrimary else TextPrimary.copy(alpha = 0.06f), WwuShape.chip)
                        .clickable { onDifficulty(diff) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(res), style = WwuType.ctaSecondary, color = if (active) BgBase else TextSecondary)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.how_many), style = WwuType.listRow, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StepperButton("−") { onCountDelta(-1) }
                Text("$count", style = WwuType.stepperCount, color = TextPrimary)
                StepperButton("+") { onCountDelta(1) }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgBase, WwuShape.textField)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.preview), style = WwuType.eyebrow, color = TextTertiary)
            Spacer(Modifier.height(10.dp))
            Text(previewQuestion(difficulty), style = WwuType.taskPreview, color = TextPrimary)
        }
    }
}

/** Fixed illustrative examples from the design — not live-generated questions. */
private fun previewQuestion(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> "7 + 8"
    Difficulty.MEDIUM -> "47 + 68"
    Difficulty.HARD -> "38 × 14"
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(TextPrimary.copy(alpha = 0.08f), WwuShape.smallButton)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = WwuType.numericKey, color = TextPrimary)
    }
}
