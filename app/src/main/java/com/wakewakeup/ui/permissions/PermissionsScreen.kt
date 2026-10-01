package com.wakewakeup.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wakewakeup.R
import com.wakewakeup.ui.components.Lamp
import com.wakewakeup.ui.components.LedSegment
import com.wakewakeup.ui.components.OrangeKey
import com.wakewakeup.ui.components.PillButton
import com.wakewakeup.ui.components.PrintLabel
import com.wakewakeup.ui.components.SunkKey
import com.wakewakeup.ui.components.VfdText
import com.wakewakeup.ui.components.faceplate
import com.wakewakeup.ui.components.nonScaling
import com.wakewakeup.ui.components.visor
import com.wakewakeup.ui.theme.Action
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

/**
 * "Antes de começar": the four system permissions an alarm needs, each with its LED (coral
 * pending / amber granted). State is re-read from the system every time the screen resumes,
 * since most of them are granted on a system settings page.
 */
@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    fun readAll() = AlarmPermission.entries.associateWith { AlarmPermissions.isGranted(context, it) }
    var granted by remember { mutableStateOf(readAll()) }
    var nag by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = readAll() }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = readAll()
    }
    fun request(permission: AlarmPermission) {
        if (permission == AlarmPermission.NOTIFICATIONS && AlarmPermissions.shouldAskNotificationsInApp(context)) {
            AlarmPermissions.markNotificationsAsked(context)
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AlarmPermissions.openSettings(context, permission)
        }
    }

    val count = granted.count { it.value }
    val total = AlarmPermission.entries.size
    val all = count == total

    Column(Modifier.fillMaxSize().background(Housing).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrintLabel(stringResource(R.string.perm_eyebrow))
                Text(stringResource(R.string.perm_title), style = WwuType.titleM, color = Ink)
                Text(stringResource(R.string.perm_body), style = WwuType.bodyS, color = InkMuted)
            }

            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .fillMaxWidth()
                    .visor(WwuShape.visorMedium, lip = false)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PrintLabel(stringResource(R.string.perm_granted_label))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AlarmPermission.entries.forEach { p ->
                            LedSegment(lit = granted[p] == true, hot = false, modifier = Modifier.size(width = 18.dp, height = 8.dp))
                        }
                    }
                    VfdText(stringResource(R.string.perm_count, count, total).uppercase(), 20.sp.nonScaling())
                }
            }

            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AlarmPermission.entries.forEach { p ->
                    val (title, desc) = when (p) {
                        AlarmPermission.NOTIFICATIONS -> R.string.perm_notifications_title to R.string.perm_notifications_desc
                        AlarmPermission.EXACT_ALARMS -> R.string.perm_exact_title to R.string.perm_exact_desc
                        AlarmPermission.FULL_SCREEN -> R.string.perm_fullscreen_title to R.string.perm_fullscreen_desc
                        AlarmPermission.BATTERY -> R.string.perm_battery_title to R.string.perm_battery_desc
                    }
                    PermissionPlate(
                        title = stringResource(title),
                        description = stringResource(desc),
                        granted = granted[p] == true,
                        onAllow = { request(p) },
                    )
                }
            }
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PillButton(
                text = stringResource(R.string.continue_label),
                enabled = all,
                onClick = {
                    // Without everything allowed, the first tap explains the risk; a second
                    // tap continues anyway (it can all be granted later in Settings).
                    if (all || nag) {
                        AlarmPermissions.markOnboarded(context)
                        onContinue()
                    } else {
                        nag = true
                    }
                },
            )
            Text(
                when {
                    all -> stringResource(R.string.perm_all_set)
                    nag -> stringResource(R.string.perm_nag)
                    else -> ""
                },
                style = WwuType.caption,
                color = InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
            )
        }
    }
}

@Composable
private fun PermissionPlate(title: String, description: String, granted: Boolean, onAllow: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .faceplate(WwuShape.faceplateSmall, softShadow = false)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Lamp(if (granted) Vfd else Action, glowAlpha = if (granted) 0.6f else 0.5f)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = WwuType.bodyStrong, color = Ink)
            Text(description, style = WwuType.caption, color = InkMuted)
        }
        if (granted) {
            SunkKey(stringResource(R.string.allowed))
        } else {
            OrangeKey(stringResource(R.string.allow), onClick = onAllow)
        }
    }
}
