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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.epochDayOf
import com.wakewakeup.session.isSkippingNext
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.ui.components.CardShadow
import com.wakewakeup.ui.components.Chevron
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
import com.wakewakeup.ui.permissions.AlarmPermissions
import com.wakewakeup.ui.theme.Action
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
import java.time.temporal.ChronoUnit

/** The radio's base plate along the bottom, which the NOVO key sits on and the cards slide under. */
private val BasePlateHeight = 136.dp

@Composable
fun AlarmListScreen(
    onNewAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onStats: () -> Unit,
    onOpenPermissions: () -> Unit,
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

    // Both change outside the app (in Settings, with the volume keys), so they're read again
    // every time the list comes back.
    val context = LocalContext.current
    var missing by remember { mutableIntStateOf(AlarmPermissions.missingRequired(context).size) }
    var volume by remember { mutableStateOf(viewModel.alarmVolume()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        missing = AlarmPermissions.missingRequired(context).size
        volume = viewModel.alarmVolume()
    }
    // A quiet phone only matters while some alarm is on (one skipping a day is on too).
    val quietVolume = volume?.takeIf { it.isLow && alarms.any { alarm -> alarm.enabled } }

    Box(modifier = Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ListHeader(showStats = alarms.isNotEmpty(), onStats = onStats)
            NextStrip(
                text = if (nextTrigger != null) nextRingText(now, nextTrigger) else null,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            Warnings(
                missingPermissions = missing,
                quietVolume = quietVolume,
                onOpenPermissions = onOpenPermissions,
                onTurnUp = {
                    viewModel.turnUpAlarmVolume()
                    volume = viewModel.alarmVolume()
                },
            )
            if (alarms.isEmpty()) {
                // A warning takes most of the gap above, so CRIAR ALARME still fits a small phone.
                EmptyState(onNewAlarm, topGap = if (missing > 0) 24.dp else 84.dp)
            } else {
                AlarmList(alarms, now, viewModel, onEditAlarm)
            }
        }

        if (alarms.isNotEmpty()) {
            // Cards slide under the base plate, so the key never sits on a card's switch. It takes
            // no touches: taps and scrolling go through to the list.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(BasePlateHeight)
                    .background(Brush.verticalGradient(0f to Housing.copy(alpha = 0f), 0.45f to Housing)),
            )
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

/**
 * What the PRÓXIMO display reads: a countdown within a day ("EM 8 H 12 MIN"), then the ring's
 * day and time ("SÁB, 09:00"), since "EM 38 H" leaves the reader working out which morning.
 * A week or more ahead it says so ("SEG QUE VEM, 06:30"), like the card drawers: the weekday
 * alone would name the nearer one, which for a weekly alarm skipping its next day is the very
 * day it skips.
 */
@Composable
private fun nextRingText(now: Long, trigger: Long): String {
    // Rounded up like the countdown, so it never reads "24 H 0 MIN".
    val minutes = (trigger - now + 59_999L) / 60_000L
    if (minutes < 24 * 60) return nextAlarmInText(now, trigger)
    val zone = ZoneId.systemDefault()
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(trigger), zone)
    val today = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone).toLocalDate()
    val short = stringArrayResource(R.array.day_short).getOrElse(isoDayIndexOf(at.toLocalDate())) { "" }
    val day = if (ChronoUnit.DAYS.between(today, at.toLocalDate()) >= 7) stringResource(R.string.day_next_week, short) else short
    return "$day, ${formatClock(at.hour, at.minute)}"
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
                // Unlit next to the glowing countdown, but still readable: it's all the strip says.
                VfdText(
                    stringResource(R.string.next_none).uppercase(),
                    20.sp.nonScaling(),
                    tracking = 0.04f,
                    modifier = Modifier.alpha(0.65f),
                    glowAlpha = 0f,
                )
            }
        }
    }
}

/**
 * Slim plates under the PRÓXIMO strip for what can keep an alarm from being heard: permissions
 * Android doesn't allow (the row opens their screen) and a low alarm volume (its key turns it up).
 */
@Composable
private fun Warnings(
    missingPermissions: Int,
    quietVolume: AlarmVolume?,
    onOpenPermissions: () -> Unit,
    onTurnUp: () -> Unit,
) {
    // Rows that are closing fade out as they were, not with a count of zero.
    val shownMissing = rememberLast(missingPermissions.takeIf { it > 0 }) ?: 0
    val shownVolume = rememberLast(quietVolume)
    // AnimatedVisibility clips to its bounds: the bottom padding keeps each plate's base inside.
    val rowSpacing = Modifier.padding(top = 12.dp, bottom = 2.dp)
    Column(Modifier.padding(horizontal = 16.dp)) {
        AnimatedVisibility(
            visible = missingPermissions > 0,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            WarningRow(
                text = pluralStringResource(R.plurals.perm_missing, shownMissing, shownMissing),
                onClick = onOpenPermissions,
                modifier = rowSpacing,
            ) { Chevron(color = InkMuted) }
        }
        AnimatedVisibility(
            visible = quietVolume != null,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            shownVolume?.let { v ->
                WarningRow(
                    text = stringResource(R.string.alarm_volume_low, v.level, v.max),
                    modifier = rowSpacing,
                ) { OrangeKey(stringResource(R.string.alarm_volume_up), onClick = onTurnUp) }
            }
        }
    }
}

/** One warning: a coral lamp, what's wrong, and on the right what fixes it. */
@Composable
private fun WarningRow(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fix: @Composable RowScope.() -> Unit,
) {
    val shape = WwuShape.faceplateSmall
    // A row that opens something is one big key; one with a key of its own hugs that key.
    val inset = if (onClick != null) 10.dp else 6.dp
    Row(
        modifier
            .fillMaxWidth()
            .faceplate(shape, softShadow = false)
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clip(shape).clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 14.dp, end = if (onClick != null) 10.dp else 8.dp, top = inset, bottom = inset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Lamp(Action, glowAlpha = 0.5f)
        Text(text, style = WwuType.bodyXS, color = Ink, modifier = Modifier.weight(1f))
        fix()
    }
}

/** [value], or while it's gone the last one it had. */
@Composable
private fun <T : Any> rememberLast(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    SideEffect { if (value != null) last = value }
    return value ?: last
}

@Composable
private fun AlarmList(alarms: List<Alarm>, now: Long, viewModel: AlarmListViewModel, onEditAlarm: (Alarm) -> Unit) {
    // The "skip just the next day" offer only makes sense right after switching a recurring
    // alarm off, not for every recurring alarm that already happens to be off.
    var justDisabledId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        // The bottom padding lets the last card scroll clear of the base plate.
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
                modifier = Modifier.animateItem(),
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
    modifier: Modifier = Modifier,
) {
    val name = alarm.label.ifBlank { stringResource(R.string.untitled_alarm) }
    val time = formatClock(alarm.hour, alarm.minute)
    val from = remember(now) { LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()) }
    Column(modifier) {
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
                    Text(
                        name,
                        style = WwuType.bodyStrong,
                        color = if (alarm.enabled) Ink else InkOff,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // One word on one line, even in the narrow column of a small phone.
                    PrintLabel(
                        taskCardLabel(alarm.taskType),
                        color = if (alarm.enabled) InkMuted else InkOff,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SlideSwitch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() },
                    description = stringResource(R.string.cd_alarm_switch, time),
                    onLabel = stringResource(R.string.switch_on),
                    offLabel = stringResource(R.string.switch_off),
                )
            }
            if (alarm.days.isEmpty()) {
                OnceLine(alarm, from)
            } else {
                DayPrintRow(activeDays = alarm.days, dimmed = !alarm.enabled)
            }
        }

        // Drawer sliding out from under the card: the day a recurring alarm is skipping, with an
        // undo, or, right after one is switched off, the offer to skip only its next day instead.
        val drawer = when {
            isSkippingNext(alarm, from) -> CardDrawerKind.SKIPPING
            !alarm.enabled && alarm.days.isNotEmpty() && showSkipPrompt -> CardDrawerKind.SKIP_PROMPT
            else -> null
        }
        // Keeps the content of a drawer that is closing, so it fades out as it was.
        var lastDrawer by remember { mutableStateOf(drawer) }
        SideEffect { if (drawer != null) lastDrawer = drawer }

        // Once the offer has slid out, scroll it into view and clear of the base plate, where the
        // NOVO key would cover its PULAR key.
        val requester = remember { BringIntoViewRequester() }
        var drawerSize by remember { mutableStateOf(IntSize.Zero) }
        val clearance = with(LocalDensity.current) { BasePlateHeight.toPx() }
        val prompting = drawer == CardDrawerKind.SKIP_PROMPT
        LaunchedEffect(prompting) {
            if (prompting) {
                delay(300)
                requester.bringIntoView(Rect(0f, 0f, drawerSize.width.toFloat(), drawerSize.height + clearance))
            }
        }

        AnimatedVisibility(
            visible = drawer != null,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            val (skipDay, backDay) = remember(alarm, from) { skipAndReturnDays(alarm, from) }
            val skipName = weekdayName(skipDay, from.toLocalDate())
            val backName = weekdayName(backDay, from.toLocalDate())
            CardDrawer(
                modifier = Modifier.bringIntoViewRequester(requester).onSizeChanged { drawerSize = it },
                top = 16.dp,
                end = 8.dp,
                bottom = 8.dp,
            ) {
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

/**
 * Where a one-time alarm's card would print seven unlit days (which reads as "never"): one line,
 * "UMA VEZ · QUARTA", with the day it rings.
 */
@Composable
private fun OnceLine(alarm: Alarm, from: LocalDateTime) {
    val day = remember(alarm, from) { LocalDate.ofEpochDay(nextTriggerEpochDay(alarm, from)) }
    val text = stringResource(R.string.once_on_day, weekdayName(day, from.toLocalDate()))
    PrintLabel(
        text,
        Modifier.padding(horizontal = 4.dp).clearAndSetSemantics { contentDescription = text },
        color = if (alarm.enabled) Ink else InkMuted,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
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
private fun EmptyState(onNewAlarm: () -> Unit, topGap: Dp) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(topGap))
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
