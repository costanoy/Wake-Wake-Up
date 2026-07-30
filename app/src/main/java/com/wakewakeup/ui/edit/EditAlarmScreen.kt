package com.wakewakeup.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.R
import com.wakewakeup.ui.components.DaySelectorRow
import com.wakewakeup.ui.components.taskCardLabel
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.DangerMuted
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextSecondary
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun EditAlarmScreen(
    alarmId: Long,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onOpenTask: () -> Unit,
    viewModel: EditAlarmViewModel,
) {
    LaunchedEffect(alarmId) { viewModel.load(alarmId) }
    val draft = viewModel.draft
    val isNew = viewModel.isNew

    Column(modifier = Modifier.fillMaxSize().background(BgBase).statusBarsPadding().navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().padding(22.dp, 18.dp)) {
            Text(
                stringResource(R.string.cancel),
                style = WwuType.listRow,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 12.dp, y = 10.dp)
                    .clickable {
                        viewModel.discardUnsaved()
                        onCancel()
                    },
            )
            Text(
                stringResource(if (isNew) R.string.new_alarm else R.string.edit_alarm),
                style = WwuType.taskName.copy(fontSize = 21.sp),
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                stringResource(R.string.save),
                style = WwuType.listRow.copy(fontWeight = FontWeight.SemiBold),
                color = AccentPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-12).dp, y = 10.dp)
                    .clickable { viewModel.save(onSaved) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TextPrimary.copy(alpha = 0.10f), WwuShape.configBlock),
            ) {
                OptionRow(label = stringResource(R.string.label)) {
                    val placeholder = stringResource(R.string.new_alarm)
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (draft.label.isEmpty()) {
                            Text(placeholder, style = WwuType.listRow, color = TextSecondary.copy(alpha = 0.5f))
                        }
                        BasicTextField(
                            value = draft.label,
                            onValueChange = viewModel::setLabel,
                            singleLine = true,
                            textStyle = WwuType.listRow.copy(color = TextSecondary, textAlign = TextAlign.End),
                            cursorBrush = SolidColor(AccentPrimary),
                            modifier = Modifier.widthIn(max = 180.dp),
                        )
                    }
                }
                OptionRow(label = stringResource(R.string.sound)) {
                    Text(stringResource(R.string.sound_name), style = WwuType.listRow, color = TextSecondary)
                }
                OptionRow(label = stringResource(R.string.task), onClick = onOpenTask) {
                    Text(
                        taskCardLabel(draft.taskType, draft.difficulty),
                        style = WwuType.listRow.copy(fontWeight = FontWeight.Medium),
                        color = AccentPrimary,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            DaySelectorRow(activeDays = draft.days, onToggle = viewModel::toggleDay)

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelColumn(value = draft.hour, range = 0..23, onValueChange = viewModel::setHour)
                Text(":", style = WwuType.timeSeparator, color = TextSecondary.copy(alpha = 0.5f))
                WheelColumn(value = draft.minute, range = 0..59, onValueChange = viewModel::setMinute)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.delete_alarm),
                style = WwuType.listRow,
                color = DangerMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.deleteCurrent(onDeleted) }
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OptionRow(label: String, onClick: (() -> Unit)? = null, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp, 17.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = WwuType.listRow, color = TextPrimary)
        value()
    }
}

private const val WHEEL_VISIBLE_ITEMS = 3
private val WHEEL_ITEM_HEIGHT = 78.dp
private val WHEEL_COLUMN_WIDTH = 122.dp

private const val WHEEL_VIRTUAL_COUNT = Int.MAX_VALUE

/**
 * A draggable, snapping, infinitely-looping number wheel — like a native
 * time picker. Built on a [LazyColumn] with a virtually unbounded item count
 * mapped back onto [range] with modulo, so scrolling past the last value
 * wraps to the first (and vice-versa) instead of stopping.
 *
 * Equal-height rows plus a top/bottom content padding of one half-viewport
 * mean that whenever the list settles at a zero offset, the item at
 * `firstVisibleItemIndex` sits exactly centered.
 */
@Composable
private fun WheelColumn(value: Int, range: IntRange, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val values = remember(range) { range.toList() }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { WHEEL_ITEM_HEIGHT.toPx() }
    val sidePadding = WHEEL_ITEM_HEIGHT * (WHEEL_VISIBLE_ITEMS / 2)

    val anchor = remember(values) { (WHEEL_VIRTUAL_COUNT / 2).let { it - it.mod(values.size) } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = anchor + values.indexOf(value).coerceAtLeast(0))
    val coroutineScope = rememberCoroutineScope()

    // Report the centered value once a drag/fling settles.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val centered = values[listState.firstVisibleItemIndex.mod(values.size)]
                    if (centered != value) onValueChange(centered)
                }
            }
    }

    // External changes (e.g. loading a saved alarm) jump the wheel if the user isn't dragging it,
    // staying near the current virtual position so a future scroll keeps looping smoothly.
    LaunchedEffect(value) {
        if (!listState.isScrollInProgress) {
            val current = listState.firstVisibleItemIndex
            if (values[current.mod(values.size)] != value) {
                val base = current - current.mod(values.size)
                coroutineScope.launch { listState.scrollToItem(base + values.indexOf(value)) }
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        modifier = modifier
            .width(WHEEL_COLUMN_WIDTH)
            .height(WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE_ITEMS),
        contentPadding = PaddingValues(vertical = sidePadding),
    ) {
        items(count = WHEEL_VIRTUAL_COUNT) { index ->
            val v = values[index.mod(values.size)]
            WheelItem(text = "%02d".format(v), index = index, listState = listState, itemHeightPx = itemHeightPx)
        }
    }
}

@Composable
private fun WheelItem(
    text: String,
    index: Int,
    listState: LazyListState,
    itemHeightPx: Float,
) {
    Box(
        modifier = Modifier
            .height(WHEEL_ITEM_HEIGHT)
            .fillMaxWidth()
            .graphicsLayer {
                val layoutInfo = listState.layoutInfo
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                val distance = if (itemInfo != null) {
                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                    (itemCenter - viewportCenter) / itemHeightPx
                } else {
                    2f
                }
                val closeness = (1f - (abs(distance) / (WHEEL_VISIBLE_ITEMS / 2f + 0.5f))).coerceIn(0f, 1f)
                scaleX = 0.55f + 0.45f * closeness
                scaleY = scaleX
                alpha = 0.35f + 0.65f * closeness
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = WwuType.timePickerValue, color = TextPrimary, maxLines = 1)
    }
}
