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
import androidx.compose.runtime.SideEffect
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
import com.wakewakeup.session.epochDayOf
import com.wakewakeup.session.isSkippingNext
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.ColonDots
import com.wakewakeup.ui.components.DayPrintRow
import com.wakewakeup.ui.components.FlipNumber
import com.wakewakeup.ui.components.FlipSize
import com.wakewakeup.ui.components.GraphiteKey
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
import com.wakewakeup.ui.components.softShadow
import com.wakewakeup.ui.components.speakerGrille
import com.wakewakeup.ui.components.taskCardLabel
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.components.weekdayName
import com.wakewakeup.ui.theme.Drawer
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.InkOff
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun AlarmListScreen(
    onNewAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
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
    val nextTrigger = remember(alarms, now) { viewModel.nextAlarmMillis(alarms, now) }

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
                AlarmList(alarms, now, viewModel, onEditAlarm)
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
private fun AlarmList(alarms: List<Alarm>, now: Long, viewModel: AlarmListViewModel, onEditAlarm: (Alarm) -> Unit) {
    // The "skip just the next day" offer only makes sense right after switching a recurring
    // alarm off, not for every recurring alarm that already happens to be off.
    var justDisabledId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                now = now,
                showSkipPrompt = alarm.id == justDisabledId,
                onToggle = {
                    justDisabledId = if (alarm.enabled && alarm.days.isNotEmpty()) alarm.id else null
                    viewModel.toggleEnabled(alarm)
                },
                onSkipNext = {
                    justDisabledId = null
                    viewModel.skipNext(alarm)
                },
                onClearSkip = { viewModel.clearSkip(alarm) },
                onOpen = { onEditAlarm(alarm) },
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    now: Long,
    showSkipPrompt: Boolean,
    onToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onClearSkip: () -> Unit,
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

        // Drawer sliding out from under the card: the day a recurring alarm is skipping, with an
        // undo, or, right after one is switched off, the offer to skip only its next day instead.
        val from = remember(now) { LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()) }
        val drawer = when {
            isSkippingNext(alarm, from) -> CardDrawerKind.SKIPPING
            !alarm.enabled && alarm.days.isNotEmpty() && showSkipPrompt -> CardDrawerKind.SKIP_PROMPT
            else -> null
        }
        // Keeps the content of a drawer that is closing, so it fades out as it was.
        var lastDrawer by remember { mutableStateOf(drawer) }
        SideEffect { if (drawer != null) lastDrawer = drawer }
        AnimatedVisibility(
            visible = drawer != null,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            val (skipDay, backDay) = remember(alarm, from) { skipAndReturnDays(alarm, from) }
            val skipName = weekdayName(skipDay, from.toLocalDate())
            val backName = weekdayName(backDay, from.toLocalDate())
            CardDrawer(top = 16.dp, end = 8.dp, bottom = 8.dp) {
                if ((drawer ?: lastDrawer) == CardDrawerKind.SKIPPING) {
                    Lamp(Vfd, size = 6.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.skipping_info, skipName, backName), style = WwuType.bodyXS, color = Ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    GraphiteKey(stringResource(R.string.undo), onClick = onClearSkip)
                } else {
                    Text(stringResource(R.string.skip_prompt, skipName, backName), style = WwuType.bodyXS, color = Ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OrangeKey(stringResource(R.string.skip_once), onClick = onSkipNext)
                }
            }
        }
    }
}

private enum class CardDrawerKind { SKIPPING, SKIP_PROMPT }

/**
 * The alarm's next occurrence (the one a skip leaves out) and the occurrence after it (when it
 * rings again), both as days. The skip itself doesn't matter: it is always the next occurrence.
 */
private fun skipAndReturnDays(alarm: Alarm, from: LocalDateTime): Pair<LocalDate, LocalDate> {
    val zone = ZoneId.systemDefault()
    val skipped = nextTriggerMillis(alarm, from)
    val back = nextTriggerMillis(alarm, LocalDateTime.ofInstant(Instant.ofEpochMilli(skipped), zone))
    return LocalDate.ofEpochDay(epochDayOf(skipped)) to LocalDate.ofEpochDay(epochDayOf(back))
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
