package com.wakewakeup.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakewakeup.ui.edit.EditAlarmScreen
import com.wakewakeup.ui.edit.EditAlarmViewModel
import com.wakewakeup.ui.list.AlarmListScreen
import com.wakewakeup.ui.permissions.PermissionsScreen
import com.wakewakeup.ui.skip.SkipConfirmScreen
import com.wakewakeup.ui.sound.RadioScreen
import com.wakewakeup.ui.sound.SoundPickerScreen
import com.wakewakeup.ui.stats.StatsScreen
import com.wakewakeup.ui.task.TaskConfigScreen

/**
 * Runs a tap's navigation only while its screen is the one showing. A second tap that lands
 * while the screen is already leaving (a double tap on Back or Save) would otherwise pop or
 * push a second time.
 */
private fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

@Composable
fun WwuNavGraph(
    openStatsOnStart: Boolean = false,
    showPermissions: Boolean = false,
    skipRequest: SkipRequest? = null,
) {
    val navController = rememberNavController()
    val editViewModel: EditAlarmViewModel = viewModel()

    LaunchedEffect(openStatsOnStart, skipRequest) {
        if (openStatsOnStart) navController.navigate(Destinations.STATS)
        if (skipRequest != null) navController.navigate(Destinations.skip(skipRequest))
    }

    val fastFade = tween<Float>(120)
    NavHost(
        navController = navController,
        startDestination = if (showPermissions) Destinations.PERMISSIONS else Destinations.LIST,
        enterTransition = { fadeIn(fastFade) },
        exitTransition = { fadeOut(fastFade) },
        popEnterTransition = { fadeIn(fastFade) },
        popExitTransition = { fadeOut(fastFade) },
    ) {
        composable(Destinations.PERMISSIONS) { entry ->
            PermissionsScreen(
                onContinue = {
                    entry.ifResumed {
                        navController.navigate(Destinations.LIST) {
                            popUpTo(Destinations.PERMISSIONS) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Destinations.LIST) { entry ->
            AlarmListScreen(
                onNewAlarm = {
                    entry.ifResumed {
                        editViewModel.startNew()
                        navController.navigate(Destinations.edit(Destinations.NEW_ALARM_ID))
                    }
                },
                onEditAlarm = { alarm ->
                    entry.ifResumed {
                        editViewModel.startEdit(alarm)
                        navController.navigate(Destinations.edit(alarm.id))
                    }
                },
                onStats = { entry.ifResumed { navController.navigate(Destinations.STATS) } },
            )
        }
        composable(
            route = Destinations.EDIT,
            arguments = listOf(navArgument(Destinations.ARG_ALARM_ID) { type = NavType.LongType }),
        ) { entry ->
            val alarmId = entry.arguments?.getLong(Destinations.ARG_ALARM_ID) ?: Destinations.NEW_ALARM_ID
            EditAlarmScreen(
                alarmId = alarmId,
                onCancel = { entry.ifResumed { navController.popBackStack() } },
                onSaved = { navController.popBackStack(Destinations.LIST, inclusive = false) },
                onDeleted = { navController.popBackStack(Destinations.LIST, inclusive = false) },
                onOpenTask = { entry.ifResumed { navController.navigate(Destinations.TASK) } },
                onOpenSound = { entry.ifResumed { navController.navigate(Destinations.SOUND) } },
                viewModel = editViewModel,
            )
        }
        composable(Destinations.TASK) { entry ->
            TaskConfigScreen(onBack = { entry.ifResumed { navController.popBackStack() } }, viewModel = editViewModel)
        }
        composable(Destinations.SOUND) { entry ->
            SoundPickerScreen(
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onOpenRadio = { entry.ifResumed { navController.navigate(Destinations.RADIO) } },
                viewModel = editViewModel,
            )
        }
        composable(Destinations.RADIO) { entry ->
            RadioScreen(onBack = { entry.ifResumed { navController.popBackStack() } }, viewModel = editViewModel)
        }
        composable(Destinations.STATS) { entry ->
            StatsScreen(onBack = { entry.ifResumed { navController.popBackStack() } })
        }
        composable(
            route = Destinations.SKIP,
            arguments = listOf(
                navArgument(Destinations.ARG_SKIP_IDS) { type = NavType.StringType },
                navArgument(Destinations.ARG_SKIP_ATS) { type = NavType.StringType },
            ),
        ) { entry ->
            val request = SkipRequest.parse(
                entry.arguments?.getString(Destinations.ARG_SKIP_IDS),
                entry.arguments?.getString(Destinations.ARG_SKIP_ATS),
            )
            SkipConfirmScreen(
                request = request,
                onDone = { entry.ifResumed { navController.popBackStack(Destinations.LIST, inclusive = false) } },
            )
        }
    }
}
