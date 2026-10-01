package com.wakewakeup.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.ColonDots
import com.wakewakeup.ui.components.DayPrintRow
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.LabeledRoundButton
import com.wakewakeup.ui.components.Lamp
import com.wakewakeup.ui.components.OrangeKey
import com.wakewakeup.ui.components.PlusGlyph
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.SlideSwitch
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.formatClock
import com.wakewakeup.ui.components.nextAlarmInText
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.relativeDayName
import com.wakewakeup.ui.components.softShadow
import com.wakewakeup.ui.components.speakerGrille
import com.wakewakeup.ui.components.taskCardLabel
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.Drawer
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.InkOff
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun AlarmListScreen(
    onNewAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onStats: () -> Unit,
    viewModel: AlarmListViewModel = viewModel(),
) {
    val loaded by viewModel.alarms.collectAsStateWithLifecycle()
    val alarms = loaded
    if (alarms == null) {
        Box(Modifier.fillMaxSize().background(Housing))
        return
    }

    // The PRÓXIMO display counts down, so refresh "now" every 30 s while on screen.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val nextTrigger = remember(alarms, now) { viewModel.nextAlarmMillis(alarms) }

    Box(modifier = Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ListHeader(showStats = alarms.isNotEmpty(), onStats = onStats)
            NextStrip(
                text = if (nextTrigger != null) nextAlarmInText(now, nextTrigger) else null,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            if (alarms.isEmpty()) {
                EmptyState(onNewAlarm)
            } else {
                AlarmList(alarms, viewModel, onEditAlarm)
            }
        }

        if (alarms.isNotEmpty()) {
            LabeledRoundButton(
                label = stringResource(R.string.new_short),
                onClick = onNewAlarm,
                size = 68.dp,
                style = RoundStyle.Action,
                labelColor = InkMuted,
                softShadow = softShadow(18.dp, 10.dp, 0.55f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 28.dp),
            ) { PlusGlyph(22.dp, 4.dp) }
        }
    }
}

@Composable
private fun ListHeader(showStats: Boolean, onStats: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.alarms), style = WwuType.titleL, color = Ink)
        if (showStats) {
            RoundButton(
                onClick = onStats,
                size = 48.dp,
                style = RoundStyle.Graphite,
                contentDescription = stringResource(R.string.cd_stats_icon),
                baseDepth = 3.dp,
                softShadow = softShadow(10.dp, 6.dp, 0.45f),
            ) { StatsBarsGlyph() }
        }
    }
}

/** Three amber bars (8, 14, 11 dp) sitting 16 dp above the bottom of the 48 dp button. */
@Composable
private fun StatsBarsGlyph() {
    Canvas(Modifier.size(48.dp)) {
        val w = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val heights = listOf(8.dp, 14.dp, 11.dp).map { it.toPx() }
        var x = (size.width - (3 * w + 2 * gap)) / 2
        heights.forEach { h ->
            drawRoundRect(Vfd, Offset(x, bottom - h), Size(w, h), CornerRadius(1.dp.toPx()))
            x += w + gap
        }
    }
}

/** The speaker-grille strip with its PRÓXIMO visor — the only place the grille texture appears. */
@Composable
private fun NextStrip(text: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(64.dp).speakerGrille(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.visor(WwuShape.field, lip = false).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrintLabel(stringResource(R.string.next_label))
            if (text != null) {
                VfdText(text.uppercase(), 20.sp.nonScaling(), tracking = 0.04f, maxLines = 1)
            } else {
                VfdText(
                    stringResource(R.string.next_none).uppercase(),
                    20.sp.nonScaling(),
                    tracking = 0.04f,
                    modifier = Modifier.alpha(0.35f),
                    glowAlpha = 0f,
                )
            }
        }
    }
}

@Composable
private fun AlarmList(alarms: List<Alarm>, viewModel: AlarmListViewModel, onEditAlarm: (Long) -> Unit) {
    val listState = rememberLazyListState()

    // Enabling an alarm moves it up into the enabled group — scroll up to reveal
    // where it landed. Disabling one must NOT auto-scroll; the list stays put.
    var justEnabledId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(alarms) {
        val id = justEnabledId
        if (id != null) {
            justEnabledId = null
            val index = alarms.indexOfFirst { it.id == id }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    // The "turn on again for <day>" drawer only makes sense right after disabling a
    // recurring alarm — not for every recurring alarm that already happens to be off.
    var justDisabledId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                showResumePrompt = alarm.id == justDisabledId,
                onToggle = {
                    if (!alarm.enabled) {
                        justEnabledId = alarm.id
                        if (justDisabledId == alarm.id) justDisabledId = null
                    } else if (alarm.days.isNotEmpty()) {
                        justDisabledId = alarm.id
                    }
                    viewModel.toggleEnabled(alarm)
                },
                onScheduleResume = {
                    justDisabledId = null
                    viewModel.scheduleResume(alarm)
                },
                onCancelResume = { viewModel.cancelResume(alarm) },
                onOpen = { onEditAlarm(alarm.id) },
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    showResumePrompt: Boolean,
    onToggle: () -> Unit,
    onScheduleResume: () -> Unit,
    onCancelResume: () -> Unit,
    onOpen: () -> Unit,
) {
    val name = alarm.label.ifBlank { stringResource(R.string.untitled_alarm) }
    val time = formatClock(alarm.hour, alarm.minute)
    Column {
        Column(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxWidth()
                .faceplate()
                .clip(WwuShape.faceplate)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.edit_alarm)) { onOpen() }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .alpha(if (alarm.enabled) 1f else 0.4f)
                        .visor(WwuShape.visorSmall)
                        .padding(7.dp),
                ) {
                    FlipNumber(
                        text = time.replace(":", ""),
                        size = FlipSize.S,
                        spokenText = time,
                        shadow = CardShadow(3.dp, 2.dp, 0.6f),
                        colon = { ColonDots(4.dp, 6.dp, InkMuted, horizontalPadding = 2.dp) },
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(name, style = WwuType.bodyStrong, color = if (alarm.enabled) Ink else InkOff, maxLines = 2)
                    PrintLabel(taskCardLabel(alarm.taskType))
                }
                SlideSwitch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() },
                    description = stringResource(R.string.cd_alarm_switch, time),
                    onLabel = stringResource(R.string.switch_on),
                    offLabel = stringResource(R.string.switch_off),
                )
            }
            DayPrintRow(activeDays = alarm.days)
        }

        // Drawer sliding out from under the card: armed auto-resume (info), or the offer to
        // re-arm a recurring alarm you just switched off.
        val resumeDate = alarm.resumeDate
        val showInfo = !alarm.enabled && alarm.days.isNotEmpty() && resumeDate != null
        val showPrompt = !alarm.enabled && alarm.days.isNotEmpty() && resumeDate == null && showResumePrompt
        AnimatedVisibility(
            visible = showInfo || showPrompt,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            if (resumeDate != null) {
                val day = relativeDayName(LocalDate.ofEpochDay(resumeDate))
                CardDrawer(
                    modifier = Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_cancel_resume)) { onCancelResume() },
                    top = 18.dp, end = 14.dp, bottom = 10.dp,
                ) {
                    Lamp(Vfd, size = 6.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.resume_scheduled, day), style = WwuType.bodyXS, color = Ink)
                }
            } else {
                val day = relativeDayName(remember(alarm.hour, alarm.minute, alarm.days) { LocalDate.ofEpochDay(nextTriggerEpochDay(alarm)) })
                CardDrawer(top = 16.dp, end = 8.dp, bottom = 8.dp) {
                    Text(stringResource(R.string.resume_prompt, day), style = WwuType.bodyXS, color = Ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OrangeKey(stringResource(R.string.activate), onClick = onScheduleResume)
                }
            }
        }
    }
}

/** The recessed "gaveta" tucked 10 dp under the card above it. */
@Composable
private fun CardDrawer(
    modifier: Modifier = Modifier,
    top: Dp,
    end: Dp,
    bottom: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    val tuck = 10.dp
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val tuckPx = tuck.roundToPx()
                layout(placeable.width, placeable.height - tuckPx) { placeable.place(0, -tuckPx) }
            }
            .fillMaxWidth()
            .background(Drawer, WwuShape.drawer)
            .innerShadow(WwuShape.drawer, Shadow(radius = 6.dp, color = Color.Black, offset = DpOffset(0.dp, 3.dp), alpha = 0.8f))
            .then(modifier)
            .padding(start = 14.dp, top = top, end = end, bottom = bottom),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun EmptyState(onNewAlarm: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(84.dp))
        Column(
            Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(
                Modifier
                    .alpha(0.55f)
                    .visor(WwuShape.visorMedium, depth = 3.dp, blurRadius = 8.dp, lip = false)
                    .padding(10.dp),
            ) {
                FlipNumber(
                    text = "––––",
                    size = FlipSize.Empty,
                    spokenText = stringResource(R.string.empty_title),
                    color = InkOff,
                    shadow = null,
                    colon = { ColonDots(6.dp, 10.dp, Color(0xFF5A4C44), horizontalPadding = 3.dp) },
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.empty_title), style = WwuType.titleS, color = Ink, textAlign = TextAlign.Center)
                Text(stringResource(R.string.empty_body), style = WwuType.body, color = InkMuted, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.weight(1f))
        LabeledRoundButton(
            label = stringResource(R.string.create_alarm),
            onClick = onNewAlarm,
            size = 84.dp,
            style = RoundStyle.Action,
            gap = 10.dp,
            softShadow = softShadow(18.dp, 10.dp, 0.55f),
            modifier = Modifier.padding(bottom = 40.dp),
        ) { PlusGlyph(26.dp, 4.dp) }
    }
}
