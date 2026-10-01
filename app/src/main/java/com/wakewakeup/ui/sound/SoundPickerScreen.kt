package com.wakewakeup.ui.sound

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.R
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.Chevron
import com.wakewakeup.ui.components.Lamp
import com.wakewakeup.ui.components.PlateDivider
import com.wakewakeup.ui.components.PlayGlyph
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.SlideSwitch
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.glow
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.rememberReduceMotion
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.edit.EditAlarmViewModel
import com.wakewakeup.ui.theme.Action
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.InkSoft
import com.wakewakeup.ui.theme.Tick
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.VfdBright
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One station on the dial. A null [uri] is the system's default alarm sound. */
private data class SoundOption(val uri: String?, val name: String, val description: String? = null)

private const val PREVIEW_MS = 2500L
internal const val STREAM_PREVIEW_MS = 10_000L

internal fun String?.isStreamUrl() = this != null && (startsWith("https://") || startsWith("http://"))
private val NeedleEasing = CubicBezierEasing(0.34f, 1.3f, 0.64f, 1f)

/**
 * Tuner-style sound picker: the device's own alarm sounds (plus the system default and any
 * file picked from the phone), a dial whose needle slides to the selected station, a short
 * preview on tap, and the per-alarm "Aumentar aos poucos" switch.
 */
@Composable
fun SoundPickerScreen(onBack: () -> Unit, onOpenRadio: () -> Unit, viewModel: EditAlarmViewModel) {
    val context = LocalContext.current
    val draft = viewModel.draft
    val defaultName = stringResource(R.string.sound_default)

    val systemSounds by produceState<List<SoundOption>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadAlarmSounds(context) }
    }
    val bundled = BundledSounds.all.map { SoundOption(BundledSounds.uriOf(context, it), stringResource(it.nameRes), stringResource(it.descRes)) }
    val options = remember(systemSounds, draft.soundUri, draft.soundName, defaultName, bundled) {
        // Radio first (its row opens the station search), then the app's own sounds, then the
        // phone's; a file picked from the phone goes last.
        val selected = draft.soundUri
        val list = mutableListOf<SoundOption>()
        if (selected.isStreamUrl()) list += SoundOption(selected, draft.soundName ?: selected.orEmpty())
        list += bundled
        list += SoundOption(null, defaultName)
        list += systemSounds.orEmpty()
        if (selected != null && list.none { it.uri == selected }) {
            list += SoundOption(selected, draft.soundName ?: selected.substringAfterLast('/'))
        }
        list
    }
    val selectedIndex = options.indexOfFirst { it.uri == draft.soundUri }.coerceAtLeast(0)

    val player = rememberSoundPreview()
    fun preview(option: SoundOption) =
        player.play(option.uri, if (option.uri.isStreamUrl()) STREAM_PREVIEW_MS else PREVIEW_MS)
    fun select(option: SoundOption) {
        viewModel.setSound(option.uri, if (option.uri == null) null else option.name)
        preview(option)
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val name = displayName(context, uri)
        select(SoundOption(uri.toString(), name))
    }

    Column(Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding()) {
        BackHeader(
            title = stringResource(R.string.sound),
            backDescription = stringResource(R.string.cd_back),
            onBack = {
                player.stop()
                onBack()
            },
        )

        TunerVisor(
            name = options.getOrNull(selectedIndex)?.name ?: defaultName,
            previewing = player.isPreviewing(draft.soundUri),
            position = if (options.size <= 1) 0.5f else selectedIndex / (options.size - 1f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .faceplate(softShadow = false)
                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            ) {
                val radioSelected = draft.soundUri.isStreamUrl()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) {
                            player.stop()
                            onOpenRadio()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Lamp(Vfd, lit = radioSelected)
                    Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(R.string.radio_row),
                            style = WwuType.body.copy(fontWeight = if (radioSelected) FontWeight.SemiBold else FontWeight.Normal),
                            color = Ink,
                        )
                        // Once a station is chosen, the row names it.
                        Text(
                            if (radioSelected) draft.soundName.orEmpty() else stringResource(R.string.radio_row_desc),
                            style = WwuType.caption,
                            color = if (radioSelected) Vfd else InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Chevron(size = 14.dp) }
                }
                PlateDivider()
                options.filterNot { it.uri.isStreamUrl() }.forEach { option ->
                    val selected = option.uri == draft.soundUri
                    SoundRow(
                        option = option,
                        selected = selected,
                        playing = player.isPreviewing(option.uri),
                        onSelect = { select(option) },
                        onPlay = { preview(option) },
                    )
                    PlateDivider()
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { pickFile.launch(arrayOf("audio/*")) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Lamp(Vfd, lit = false)
                    Text(stringResource(R.string.phone_music), style = WwuType.body, color = Ink, modifier = Modifier.weight(1f))
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Chevron(size = 14.dp) }
                }
            }

            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .faceplate(softShadow = false)
                    .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.ramp_title), style = WwuType.body, color = Ink)
                    Text(stringResource(R.string.ramp_desc), style = WwuType.caption, color = InkMuted)
                }
                SlideSwitch(
                    checked = draft.rampUp,
                    onCheckedChange = viewModel::setRampUp,
                    description = stringResource(R.string.ramp_title),
                    onLabel = stringResource(R.string.switch_on),
                    offLabel = stringResource(R.string.switch_off),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TunerVisor(name: String, previewing: Boolean, position: Float, modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    val needle = remember { Animatable(0.08f + position * 0.84f) }
    LaunchedEffect(position) {
        val target = 0.08f + position * 0.84f
        if (reduce) needle.snapTo(target) else needle.animateTo(target, tween(350, easing = NeedleEasing))
    }
    Column(
        modifier
            .fillMaxWidth()
            .visor(WwuShape.visorLarge, depth = 3.dp, blurRadius = 8.dp)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VfdText(
                name.uppercase(),
                26.sp.nonScaling(),
                color = VfdBright,
                glowColor = VfdBright,
                glowRadius = 8.dp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            PrintLabel(
                stringResource(if (previewing) R.string.preview_label else R.string.selected_label),
                color = if (previewing) Action else InkMuted,
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp)) {
            val width = maxWidth
            Canvas(Modifier.fillMaxSize()) {
                val ticks = 25
                val cell = size.width / ticks
                for (i in 0 until ticks) {
                    val h = when {
                        i % 6 == 0 -> 18.dp
                        i % 3 == 0 -> 12.dp
                        else -> 7.dp
                    }.toPx()
                    val x = cell * i + cell / 2
                    drawLine(Tick, Offset(x, size.height - h), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                }
            }
            Box(
                Modifier
                    .offset(x = width * needle.value - 1.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .glow(WwuShape.segmentThin, Action, 6.dp, 0.8f)
                    .background(Action),
            )
        }
    }
}

@Composable
private fun SoundRow(option: SoundOption, selected: Boolean, playing: Boolean, onSelect: () -> Unit, onPlay: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Lamp(Vfd, lit = selected)
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                option.name,
                style = WwuType.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (selected) Ink else InkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.description != null) {
                Text(option.description, style = WwuType.caption, color = InkMuted)
            }
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            RoundButton(
                onClick = onPlay,
                size = 36.dp,
                style = RoundStyle.Graphite,
                contentDescription = stringResource(R.string.cd_play_preview, option.name),
                baseDepth = 2.dp,
            ) { PlayGlyph(9.dp, 12.dp, if (playing) Vfd else Ink, nudge = 1.dp) }
        }
    }
}

private fun loadAlarmSounds(context: Context): List<SoundOption> = runCatching {
    val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
    val cursor = manager.cursor
    val result = mutableListOf<SoundOption>()
    while (cursor.moveToNext()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
        val uri = manager.getRingtoneUri(cursor.position) ?: continue
        result += SoundOption(uri.toString(), title)
    }
    result.distinctBy { it.uri }
}.getOrDefault(emptyList())

private fun displayName(context: Context, uri: Uri): String {
    val raw = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
    return raw.substringBeforeLast('.').ifBlank { raw }
}
