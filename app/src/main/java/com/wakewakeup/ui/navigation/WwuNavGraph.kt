package com.wakewakeup.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakewakeup.ui.edit.EditAlarmScreen
import com.wakewakeup.ui.edit.EditAlarmViewModel
import com.wakewakeup.ui.list.AlarmListScreen
import com.wakewakeup.ui.permissions.PermissionsScreen
import com.wakewakeup.ui.sound.RadioScreen
import com.wakewakeup.ui.sound.SoundPickerScreen
import com.wakewakeup.ui.stats.StatsScreen
import com.wakewakeup.ui.task.TaskConfigScreen

@Composable
fun WwuNavGraph(openStatsOnStart: Boolean = false, showPermissions: Boolean = false) {
    val navController = rememberNavController()
    val editViewModel: EditAlarmViewModel = viewModel()

    LaunchedEffect(openStatsOnStart) {
        if (openStatsOnStart) navController.navigate(Destinations.STATS)
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
        composable(Destinations.PERMISSIONS) {
            PermissionsScreen(
                onContinue = {
                    navController.navigate(Destinations.LIST) {
                        popUpTo(Destinations.PERMISSIONS) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.LIST) {
            AlarmListScreen(
                onNewAlarm = {
                    editViewModel.startNew()
                    navController.navigate(Destinations.edit(Destinations.NEW_ALARM_ID))
                },
                onEditAlarm = { id -> navController.navigate(Destinations.edit(id)) },
                onStats = { navController.navigate(Destinations.STATS) },
            )
        }
        composable(
            route = Destinations.EDIT,
            arguments = listOf(navArgument(Destinations.ARG_ALARM_ID) { type = NavType.LongType }),
        ) { entry ->
            val alarmId = entry.arguments?.getLong(Destinations.ARG_ALARM_ID) ?: Destinations.NEW_ALARM_ID
            EditAlarmScreen(
                alarmId = alarmId,
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Destinations.LIST, inclusive = false) },
                onDeleted = { navController.popBackStack(Destinations.LIST, inclusive = false) },
                onOpenTask = { navController.navigate(Destinations.TASK) },
                onOpenSound = { navController.navigate(Destinations.SOUND) },
                viewModel = editViewModel,
            )
        }
        composable(Destinations.TASK) {
            TaskConfigScreen(onBack = { navController.popBackStack() }, viewModel = editViewModel)
        }
        composable(Destinations.SOUND) {
            SoundPickerScreen(
                onBack = { navController.popBackStack() },
                onOpenRadio = { navController.navigate(Destinations.RADIO) },
                viewModel = editViewModel,
            )
        }
        composable(Destinations.RADIO) {
            RadioScreen(onBack = { navController.popBackStack() }, viewModel = editViewModel)
        }
        composable(Destinations.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
    }
}
