package com.wakewakeup.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.CheckGlyph
import com.wakewakeup.ui.components.Chevron
import com.wakewakeup.ui.components.ColonDots
import com.wakewakeup.ui.components.CrossGlyph
import com.wakewakeup.ui.components.DayKeys
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.LabeledRoundButton
import com.wakewakeup.ui.components.PlateDivider
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.softShadow
import com.wakewakeup.ui.components.taskSummary
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.ErrorText
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun EditAlarmScreen(
    alarmId: Long,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onOpenTask: () -> Unit,
    onOpenSound: () -> Unit,
    viewModel: EditAlarmViewModel,
) {
    LaunchedEffect(alarmId) { viewModel.load(alarmId) }
    val draft = viewModel.draft
    val isNew = viewModel.isNew

    // Delete needs two taps: the first arms it ("TOQUE DE NOVO"), which disarms after 3 s.
    var deleteArmed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(deleteArmed) {
        if (deleteArmed) {
            delay(3000)
            deleteArmed = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Housing)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        BackHeader(
            title = stringResource(if (isNew) R.string.new_alarm else R.string.edit_alarm),
            backDescription = stringResource(R.string.cd_back),
            onBack = {
                viewModel.discardUnsaved()
                onCancel()
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            TimePickerVisor(
                hour = draft.hour,
                minute = draft.minute,
                onHour = viewModel::setHour,
                onMinute = viewModel::setMinute,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            )

            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .fillMaxWidth()
                    .faceplate()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                PlateRow(label = stringResource(R.string.label), minHeight = 56.dp) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .visor(WwuShape.field, depth = 2.dp, blurRadius = 4.dp, lip = false)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.label.isEmpty()) {
                            Text(stringResource(R.string.name_hint), style = WwuType.body, color = InkMuted.copy(alpha = 0.6f), maxLines = 1)
                        }
                        BasicTextField(
                            value = draft.label,
                            onValueChange = viewModel::setLabel,
                            singleLine = true,
                            textStyle = WwuType.body.copy(color = Ink),
                            cursorBrush = SolidColor(Vfd),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                PlateDivider()
                PlateRow(label = stringResource(R.string.sound), onClick = onOpenSound) {
                    Text(
                        draft.soundName ?: stringResource(R.string.sound_default),
                        style = WwuType.body,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                PlateDivider()
                PlateRow(label = stringResource(R.string.task), onClick = onOpenTask) {
                    Text(
                        taskSummary(draft.taskType, draft.taskCount),
                        style = WwuType.body,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            PrintLabel(
                stringResource(R.string.days),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
            )
            DayKeys(
                activeDays = draft.days,
                onToggle = viewModel::toggleDay,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(24.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, bottom = 32.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            LabeledRoundButton(
                label = stringResource(if (deleteArmed) R.string.delete_confirm else R.string.delete),
                onClick = {
                    if (deleteArmed) {
                        deleteArmed = false
                        viewModel.deleteCurrent(onDeleted)
                    } else {
                        deleteArmed = true
                    }
                },
                size = 60.dp,
                style = RoundStyle.Danger,
                baseDepth = 3.dp,
                labelColor = if (deleteArmed) ErrorText else InkMuted,
                softShadow = softShadow(14.dp, 8.dp, 0.5f),
            ) { CrossGlyph(60.dp) }
            LabeledRoundButton(
                label = stringResource(R.string.save),
                onClick = { viewModel.save(onSaved) },
                size = 76.dp,
                style = RoundStyle.Action,
                softShadow = softShadow(18.dp, 10.dp, 0.55f),
            ) { CheckGlyph(76.dp) }
        }
    }
}

@Composable
private fun PlateRow(
    label: String,
    minHeight: Dp = 52.dp,
    onClick: (() -> Unit)? = null,
    value: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrintLabel(label, modifier = Modifier.width(62.dp))
        value()
        if (onClick != null) Chevron(color = InkMuted, size = 14.dp)
    }
}

/**
 * HH : MM as flip drums in a recessed visor. The neighbours above and below step −1 / +1 on tap;
 * dragging a drum turns it card by card with a haptic tick per step.
 */
@Composable
private fun TimePickerVisor(
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .visor(WwuShape.visorLarge, depth = 3.dp, blurRadius = 8.dp)
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Drum(
            value = hour,
            modulo = 24,
            label = stringResource(R.string.hour),
            downDescription = stringResource(R.string.cd_hour_down),
            upDescription = stringResource(R.string.cd_hour_up),
            onChange = onHour,
        )
        ColonDots(6.dp, 10.dp, InkMuted, horizontalPadding = 0.dp, modifier = Modifier.offset(y = (-9).dp))
        Drum(
            value = minute,
            modulo = 60,
            label = stringResource(R.string.minute),
            downDescription = stringResource(R.string.cd_minute_down),
            upDescription = stringResource(R.string.cd_minute_up),
            onChange = onMinute,
        )
    }
}

private val DRAG_STEP = 28.dp

@Composable
private fun Drum(
    value: Int,
    modulo: Int,
    label: String,
    downDescription: String,
    upDescription: String,
    onChange: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val stepPx = with(LocalDensity.current) { DRAG_STEP.toPx() }
    fun step(delta: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        change(Math.floorMod(current + delta, modulo))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Neighbour(Math.floorMod(value - 1, modulo), downDescription) { step(-1) }
        Box(
            Modifier.pointerInput(Unit) {
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulated = 0f },
                ) { change, dragAmount ->
                    change.consume()
                    accumulated += dragAmount
                    // Dragging down pulls the previous card over, like turning a drum.
                    while (abs(accumulated) >= stepPx) {
                        val dir = if (accumulated > 0) -1 else 1
                        step(dir)
                        accumulated += dir * stepPx
                    }
                }
            },
        ) {
            FlipNumber(
                text = "%02d".format(value),
                size = FlipSize.M,
                spokenText = "$label %02d".format(value),
            )
        }
        Neighbour(Math.floorMod(value + 1, modulo), upDescription) { step(1) }
        PrintLabel(label)
    }
}

@Composable
private fun Neighbour(value: Int, description: String, onClick: () -> Unit) {
    val size = with(LocalDensity.current) { 26.dp.toSp() }
    Text(
        "%02d".format(value),
        style = WwuType.flip(size),
        color = Ink,
        modifier = Modifier
            .alpha(0.5f)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 20.dp, vertical = 11.dp),
    )
}
