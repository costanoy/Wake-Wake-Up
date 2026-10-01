package com.wakewakeup.ui.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.R
import com.wakewakeup.data.RadioBrowser
import com.wakewakeup.data.RadioStation
import com.wakewakeup.ui.components.BackHeader
import com.wakewakeup.ui.components.Lamp
import com.wakewakeup.ui.components.PlateDivider
import com.wakewakeup.ui.components.PlayGlyph
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.RoundButton
import com.wakewakeup.ui.components.RoundStyle
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.edit.EditAlarmViewModel
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.InkSoft
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale

private sealed interface RadioResults {
    data object Loading : RadioResults
    data object Failed : RadioResults
    data class Loaded(val stations: List<RadioStation>) : RadioResults
}

/**
 * Pick an internet radio station to wake up to instead of an alarm sound. With nothing
 * typed it lists the most listened stations in the phone's country; typing searches by name.
 * Tapping a station selects it and plays a few seconds so you know the stream works.
 */
@Composable
fun RadioScreen(onBack: () -> Unit, viewModel: EditAlarmViewModel) {
    val draft = viewModel.draft
    val player = rememberSoundPreview()
    val keyboard = LocalSoftwareKeyboardController.current

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<RadioResults>(RadioResults.Loading) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(query, attempt) {
        results = RadioResults.Loading
        if (query.isNotBlank()) delay(500) // wait for the typing to pause
        results = try {
            RadioResults.Loaded(RadioBrowser.search(query, Locale.getDefault().country.ifBlank { "BR" }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RadioResults.Failed
        }
    }

    fun select(station: RadioStation) {
        keyboard?.hide()
        viewModel.setSound(station.streamUrl, station.name)
        player.play(station.streamUrl, STREAM_PREVIEW_MS)
    }

    Column(Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding().imePadding()) {
        BackHeader(
            title = stringResource(R.string.radio_title),
            backDescription = stringResource(R.string.cd_back),
            onBack = {
                player.stop()
                onBack()
            },
        )

        Box(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .visor(WwuShape.visorSmall, lip = false)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.radio_search_hint), style = WwuType.bodyInput, color = InkMuted.copy(alpha = 0.6f), maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = WwuType.bodyInput.copy(color = Ink),
                cursorBrush = SolidColor(Vfd),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            stringResource(R.string.radio_note),
            style = WwuType.caption,
            color = InkMuted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        )
        PrintLabel(
            stringResource(if (query.isBlank()) R.string.radio_popular else R.string.radio_results),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )

        when (val state = results) {
            RadioResults.Loading -> StatusVisor(stringResource(R.string.radio_loading))
            RadioResults.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusVisor(stringResource(R.string.radio_offline))
                Text(
                    stringResource(R.string.radio_error),
                    style = WwuType.bodyXS,
                    color = InkMuted,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                )
                PrintLabel(
                    stringResource(R.string.radio_retry),
                    color = Ink,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(role = Role.Button) { attempt++ }
                        .padding(16.dp),
                )
            }
            is RadioResults.Loaded -> if (state.stations.isEmpty()) {
                StatusVisor(stringResource(R.string.radio_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                ) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .faceplate(softShadow = false)
                                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            state.stations.forEachIndexed { index, station ->
                                StationRow(
                                    station = station,
                                    selected = station.streamUrl == draft.soundUri,
                                    playing = player.isPreviewing(station.streamUrl),
                                    onSelect = { select(station) },
                                )
                                if (index < state.stations.lastIndex) PlateDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusVisor(text: String) {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .visor(WwuShape.visorSmall, lip = false)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        VfdText(text.uppercase(), 20.sp.nonScaling(), tracking = 0.04f)
    }
}

@Composable
private fun StationRow(station: RadioStation, selected: Boolean, playing: Boolean, onSelect: () -> Unit) {
    val detail = listOf(station.country, station.tags).filter { it.isNotBlank() }.joinToString(" · ")
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
                station.name,
                style = WwuType.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (selected) Ink else InkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotEmpty()) {
                Text(detail, style = WwuType.caption, color = InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            RoundButton(
                onClick = onSelect,
                size = 36.dp,
                style = RoundStyle.Graphite,
                contentDescription = stringResource(R.string.cd_play_preview, station.name),
                baseDepth = 2.dp,
            ) { PlayGlyph(9.dp, 12.dp, if (playing) Vfd else Ink, nudge = 1.dp) }
        }
    }
}
