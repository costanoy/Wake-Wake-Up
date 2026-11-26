package com.wakewakeup.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wakewakeup.R
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.CheckGlyph
import com.wakewakeup.ui.components.Chevron
import com.wakewakeup.ui.components.ColonDots
import com.wakewakeup.ui.components.CrossGlyph
import com.wakewakeup.ui.components.DayKeys
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.GraphiteKey
import com.wakewakeup.ui.components.LabeledRoundButton
import com.wakewakeup.ui.components.OrangeKey
import com.wakewakeup.ui.components.PlateDivider
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.bottomFade
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.softShadow
import com.wakewakeup.ui.components.taskSummary
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.ErrorText
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.VfdBright
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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

    // ‹ and the system Back leave the same way: straight out when nothing changed, otherwise
    // after asking, so a stray Back never throws edits away unseen.
    var askingToLeave by rememberSaveable { mutableStateOf(false) }
    fun discardAndLeave() {
        askingToLeave = false
        viewModel.discardUnsaved()
        onCancel()
    }
    fun leave() {
        if (viewModel.isDirty) askingToLeave = true else discardAndLeave()
    }
    BackHandler { leave() }
    if (askingToLeave) {
        LeaveDialog(
            isNew = isNew,
            onDismiss = { askingToLeave = false },
            onDiscard = { discardAndLeave() },
            onSave = {
                askingToLeave = false
                viewModel.save(onSaved)
            },
        )
    }

    // Delete needs two taps: the first arms it ("TOQUE DE NOVO"), which disarms after 3 s.
    var deleteArmed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(deleteArmed) {
        if (deleteArmed) {
            delay(3000)
            deleteArmed = false
        }
    }

    // On a short phone the foot bar gives up some height so MISSÃO and SOM stay above it.
    val compact = LocalConfiguration.current.screenHeightDp < 700
    val saveSize = if (compact) 64.dp else 76.dp
    val deleteSize = if (compact) 52.dp else 60.dp

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
            onBack = { leave() },
        )

        val scroll = rememberScrollState()
        Column(
            Modifier
                .weight(1f)
                .bottomFade(scroll)
                .verticalScroll(scroll),
        ) {
            TimePickerVisor(
                hour = draft.hour,
                minute = draft.minute,
                onHour = viewModel::setHour,
                onMinute = viewModel::setMinute,
                onHourStep = viewModel::stepHour,
                onMinuteStep = viewModel::stepMinute,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            )

            // The days right under the time: together they say when it rings.
            PrintLabel(
                stringResource(R.string.days),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            )
            DayKeys(
                activeDays = draft.days,
                onToggle = viewModel::toggleDay,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            if (draft.days.isEmpty()) {
                Text(
                    stringResource(R.string.days_none_hint),
                    style = WwuType.caption,
                    color = InkMuted,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                )
            }

            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .fillMaxWidth()
                    .faceplate()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                PlateRow(label = stringResource(R.string.task), onClick = onOpenTask) {
                    Text(
                        taskSummary(draft.taskType, draft.taskCount),
                        style = WwuType.body,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
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
                            Text(stringResource(R.string.name_hint), style = WwuType.body, color = InkMuted.copy(alpha = 0.8f), maxLines = 1)
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
            }
            Spacer(Modifier.height(24.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, bottom = if (compact) 16.dp else 32.dp, top = 8.dp),
            // A new alarm has nothing to delete yet (‹ abandons it), and SALVAR keeps its place.
            horizontalArrangement = if (isNew) Arrangement.End else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!isNew) {
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
                    size = deleteSize,
                    style = RoundStyle.Danger,
                    // Measured after SALVAR: at the largest font sizes its captions wrap instead
                    // of squeezing the save key.
                    modifier = Modifier.weight(1f, fill = false),
                    baseDepth = 3.dp,
                    labelColor = if (deleteArmed) ErrorText else InkMuted,
                    softShadow = softShadow(14.dp, 8.dp, 0.5f),
                    // Room for the longer caption either way, so the key stays under the confirming tap.
                    reserveLabel = stringResource(if (deleteArmed) R.string.delete else R.string.delete_confirm),
                ) { CrossGlyph(deleteSize) }
            }
            LabeledRoundButton(
                label = stringResource(R.string.save),
                onClick = { viewModel.save(onSaved) },
                size = saveSize,
                style = RoundStyle.Action,
                softShadow = softShadow(18.dp, 10.dp, 0.55f),
            ) { CheckGlyph(saveSize) }
        }
    }
}

/** Asked when leaving with something unsaved: DESCARTAR goes without it, SALVAR keeps it. */
@Composable
private fun LeaveDialog(isNew: Boolean, onDismiss: () -> Unit, onDiscard: () -> Unit, onSave: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        // No soft shadow: the dialog's window ends at the plate and would cut it off, and the
        // dimmed screen behind already sets the plate apart. The padding keeps its base in view.
        Column(
            Modifier
                .padding(bottom = 2.dp)
                .faceplate(WwuShape.faceplateLarge, softShadow = false)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (isNew) R.string.leave_new_title else R.string.leave_edit_title),
                style = WwuType.titleS,
                color = Ink,
            )
            Text(
                stringResource(if (isNew) R.string.leave_new_body else R.string.leave_edit_body),
                style = WwuType.bodyS,
                color = InkMuted,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                GraphiteKey(stringResource(R.string.discard), onClick = onDiscard)
                OrangeKey(stringResource(R.string.save), onClick = onSave)
            }
        }
    }
}

/** The hour typed as four digits ("0645"), or null while it isn't a time that exists. */
private fun parseTypedTime(digits: String): Pair<Int, Int>? {
    if (digits.length != 4) return null
    val hour = digits.take(2).toInt()
    val minute = digits.drop(2).toInt()
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}

/** Typing the time instead of turning the drums: four digits in a visor, then CONFIRMAR. */
@Composable
private fun TimeEntryDialog(onDismiss: () -> Unit, onConfirm: (hour: Int, minute: Int) -> Unit) {
    val field = rememberTextFieldState()
    val digits = field.text.toString()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val parsed = parseTypedTime(digits)
    val invalid = digits.length == 4 && parsed == null
    fun confirm() {
        parsed?.let { (h, m) -> onConfirm(h, m) }
    }
    val shown = digits.padEnd(4, '_').let { "${it.take(2)}:${it.drop(2)}" }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(bottom = 2.dp)
                .faceplate(WwuShape.faceplateLarge, softShadow = false)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.type_time_title), style = WwuType.titleS, color = Ink)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .visor(WwuShape.visorSmall, lip = false),
                contentAlignment = Alignment.Center,
            ) {
                VfdText(shown, 36.sp.nonScaling(), color = VfdBright)
                // The real field lies invisible over the readout, so the keyboard types into it.
                BasicTextField(
                    state = field,
                    inputTransformation = {
                        // Only digits, four at most: anything else is taken back.
                        val text = asCharSequence().toString()
                        if (text.length > 4 || !text.all(Char::isDigit)) revertAllChanges()
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    onKeyboardAction = { confirm() },
                    textStyle = WwuType.body.copy(color = Color.Transparent),
                    cursorBrush = SolidColor(Color.Unspecified),
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0f)
                        .focusRequester(focus),
                )
            }
            Text(
                stringResource(if (invalid) R.string.type_time_invalid else R.string.type_time_hint),
                style = WwuType.bodyS,
                color = if (invalid) ErrorText else InkMuted,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                GraphiteKey(stringResource(R.string.cancel), onClick = onDismiss)
                OrangeKey(stringResource(R.string.confirm), onClick = { confirm() })
            }
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
    // Sized by the label's own font size, so the column grows with the label at any font scale
    // (a fixed 64 sp barely grows from Android 14 on, where large text scales non-linearly).
    val labelWidth = with(LocalDensity.current) { WwuType.label.fontSize.toDp() } * 5.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrintLabel(label, modifier = Modifier.width(labelWidth), maxLines = 1, softWrap = false)
        value()
        if (onClick != null) Chevron(color = InkMuted, size = 14.dp)
    }
}

/**
 * HH : MM as flip drums in a recessed visor. The neighbours above and below step −1 / +1 on tap;
 * dragging a drum turns it card by card with a haptic tick per step, and a flick spins it on.
 * TalkBack adjusts each drum like a slider, and the minutes ten at a time too.
 */
@Composable
private fun TimePickerVisor(
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
    onHourStep: (Int) -> Unit,
    onMinuteStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val plusTen = stringResource(R.string.cd_minutes_plus_10)
    val minusTen = stringResource(R.string.cd_minutes_minus_10)
    var typing by rememberSaveable { mutableStateOf(false) }
    if (typing) {
        TimeEntryDialog(
            onDismiss = { typing = false },
            onConfirm = { h, m ->
                onHour(h)
                onMinute(m)
                typing = false
            },
        )
    }
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
            onSet = onHour,
            onStep = onHourStep,
            dragStep = 28.dp,
            friction = 2.5f,
            onType = { typing = true },
        )
        ColonDots(6.dp, 10.dp, InkMuted, horizontalPadding = 0.dp, modifier = Modifier.offset(y = (-9).dp))
        Drum(
            value = minute,
            modulo = 60,
            label = stringResource(R.string.minute),
            downDescription = stringResource(R.string.cd_minute_down),
            upDescription = stringResource(R.string.cd_minute_up),
            onSet = onMinute,
            onStep = onMinuteStep,
            // A minute is a short pull and a longer glide: 30 minutes are one or two swipes.
            dragStep = 16.dp,
            friction = 1.5f,
            onType = { typing = true },
            actions = listOf(
                CustomAccessibilityAction(plusTen) { onMinuteStep(10); true },
                CustomAccessibilityAction(minusTen) { onMinuteStep(-10); true },
            ),
        )
    }
}

/** Lifting the finger faster than this (per second) sets the drum spinning on its own. */
private val FLING_SPEED = 600.dp

@Composable
private fun Drum(
    value: Int,
    modulo: Int,
    label: String,
    downDescription: String,
    upDescription: String,
    onSet: (Int) -> Unit,
    onStep: (Int) -> Unit,
    dragStep: Dp,
    friction: Float,
    onType: () -> Unit,
    actions: List<CustomAccessibilityAction> = emptyList(),
) {
    val haptic = LocalHapticFeedback.current
    val currentOnSet by rememberUpdatedState(onSet)
    val currentOnStep by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    var spin by remember { mutableStateOf<Job?>(null) }
    fun step(delta: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        currentOnStep(delta)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Neighbour(Math.floorMod(value - 1, modulo), downDescription) {
            spin?.cancel()
            step(-1)
        }
        Box(
            Modifier
                .semantics(mergeDescendants = true) {
                    // Adjustable: TalkBack's swipe up or down turns the drum one card.
                    progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..(modulo - 1).toFloat(), steps = modulo - 2)
                    // Read as "07", not as a percentage of the range.
                    stateDescription = "%02d".format(value)
                    setProgress { target ->
                        currentOnSet(target.roundToInt().coerceIn(0, modulo - 1))
                        true
                    }
                    if (actions.isNotEmpty()) customActions = actions
                }
                // A tap on the drum itself opens the keypad entry; a drag cancels the click.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = stringResource(R.string.cd_type_time),
                ) {
                    spin?.cancel()
                    onType()
                }
                .pointerInput(dragStep, friction) {
                    val stepPx = dragStep.toPx()
                    val flingPx = FLING_SPEED.toPx()
                    val tracker = VelocityTracker()
                    var accumulated = 0f
                    // Dragging down pulls the previous card over, like turning a drum.
                    fun turn(delta: Float) {
                        accumulated += delta
                        while (abs(accumulated) >= stepPx) {
                            val dir = if (accumulated > 0) -1 else 1
                            step(dir)
                            accumulated += dir * stepPx
                        }
                    }
                    detectVerticalDragGestures(
                        onDragStart = {
                            spin?.cancel()
                            accumulated = 0f
                            tracker.resetTracking()
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().y
                            if (abs(velocity) > flingPx) {
                                spin = scope.launch {
                                    // Short of a full turn, so a flick never lands back where it began.
                                    val reach = (modulo - 1) * stepPx
                                    val wheel = Animatable(0f)
                                    wheel.updateBounds(-reach, reach)
                                    var last = 0f
                                    wheel.animateDecay(velocity, exponentialDecay(frictionMultiplier = friction)) {
                                        turn(wheel.value - last)
                                        last = wheel.value
                                    }
                                }
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        turn(dragAmount)
                    }
                },
        ) {
            FlipNumber(
                text = "%02d".format(value),
                size = FlipSize.M,
                spokenText = label,
            )
        }
        Neighbour(Math.floorMod(value + 1, modulo), upDescription) {
            spin?.cancel()
            step(1)
        }
        // The drum already says its name to TalkBack; the printed caption would be a second stop.
        PrintLabel(label, modifier = Modifier.clearAndSetSemantics {})
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
