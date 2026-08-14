package com.wakewakeup.ui.edit

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.wakewakeup.ui.theme.AccentAlert
import com.wakewakeup.ui.theme.AccentPrimary
import com.wakewakeup.ui.theme.BgBase
import com.wakewakeup.ui.theme.TextPrimary
import com.wakewakeup.ui.theme.TextSecondary
import com.wakewakeup.ui.theme.TextTertiary
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
    val context = LocalContext.current

    val soundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        @Suppress("DEPRECATION")
        val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (picked == null || picked == defaultUri) {
            viewModel.setSound(null, null)
        } else {
            val name = runCatching { RingtoneManager.getRingtone(context, picked)?.getTitle(context) }.getOrNull()
            viewModel.setSound(picked.toString(), name)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBase).statusBarsPadding().navigationBarsPadding()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(22.dp, 34.dp, 22.dp, 18.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cancel),
                tint = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 12.dp)
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        ) {
            Spacer(Modifier.height(22.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(WwuShape.configBlock)
                    .background(TextPrimary.copy(alpha = 0.03f), WwuShape.configBlock)
                    .border(1.dp, TextPrimary.copy(alpha = 0.10f), WwuShape.configBlock),
            ) {
                OptionRow(label = stringResource(R.string.label)) {
                    val placeholder = stringResource(R.string.new_alarm)
                    Box(
                        modifier = Modifier
                            .background(TextPrimary.copy(alpha = 0.07f), WwuShape.textField)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (draft.label.isEmpty()) {
                            Text(placeholder, style = WwuType.listRow, color = TextSecondary.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                        }
                        BasicTextField(
                            value = draft.label,
                            onValueChange = viewModel::setLabel,
                            singleLine = true,
                            textStyle = WwuType.listRow.copy(color = TextSecondary, textAlign = TextAlign.Center),
                            cursorBrush = SolidColor(AccentPrimary),
                            modifier = Modifier.widthIn(max = 150.dp),
                        )
                    }
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
                OptionDivider()
                OptionRow(
                    label = stringResource(R.string.sound),
                    onClick = {
                        val existing = draft.soundUri?.let { Uri.parse(it) }
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                        }
                        soundPickerLauncher.launch(intent)
                    },
                ) {
                    Text(draft.soundName ?: stringResource(R.string.sound_default), style = WwuType.listRow, color = TextSecondary)
                }
                OptionDivider()
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelColumn(value = draft.hour, range = 0..23, onValueChange = viewModel::setHour)
                Text(":", style = WwuType.timeSeparator, color = TextSecondary.copy(alpha = 0.5f))
                WheelColumn(value = draft.minute, range = 0..59, onValueChange = viewModel::setMinute)
            }

            Spacer(Modifier.height(90.dp))
        }
    }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 34.dp, bottom = 28.dp)
                .size(62.dp)
                .clip(WwuShape.fab)
                .background(AccentAlert, WwuShape.fab)
                .clickable { viewModel.deleteCurrent(onDeleted) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete_alarm), tint = BgBase)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 34.dp, bottom = 28.dp)
                .size(62.dp)
                .clip(WwuShape.fab)
                .background(AccentPrimary, WwuShape.fab)
                .clickable { viewModel.save(onSaved) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save), tint = BgBase)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            value()
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun OptionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(TextPrimary.copy(alpha = 0.08f)),
    )
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
 * The "centered" item is found geometrically (whichever visible row's own
 * center is closest to the viewport's center) — the same calculation
 * [WheelItem] uses to decide which row to draw at full size — rather than
 * assumed from `firstVisibleItemIndex`, since the fling's actual snap
 * alignment shouldn't be taken for granted.
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
    val currentValue = rememberUpdatedState(value)

    fun centeredVirtualIndex(): Int {
        val layoutInfo = listState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val closest = layoutInfo.visibleItemsInfo.minByOrNull { info ->
            abs((info.offset + info.size / 2f) - viewportCenter)
        }
        return closest?.index ?: listState.firstVisibleItemIndex
    }

    // Report the centered value once a drag/fling settles.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val centered = values[centeredVirtualIndex().mod(values.size)]
                    if (centered != currentValue.value) onValueChange(centered)
                }
            }
    }

    // External changes (e.g. loading a saved alarm) jump the wheel if the user isn't dragging it,
    // staying near the current virtual position so a future scroll keeps looping smoothly.
    LaunchedEffect(value) {
        if (!listState.isScrollInProgress) {
            val current = centeredVirtualIndex()
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
            WheelItem(
                text = "%02d".format(v),
                index = index,
                listState = listState,
                itemHeightPx = itemHeightPx,
                onClick = { coroutineScope.launch { listState.animateScrollToItem(index) } },
            )
        }
    }
}

@Composable
private fun WheelItem(
    text: String,
    index: Int,
    listState: LazyListState,
    itemHeightPx: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(WHEEL_ITEM_HEIGHT)
            .fillMaxWidth()
            .clickable { onClick() }
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
