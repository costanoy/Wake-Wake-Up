package com.wakewakeup.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object WwuRadius {
    val badge = 5.dp
    val dayChip = 8.dp
    val alphaKey = 9.dp
    val smallButton = 10.dp
    val chip = 12.dp
    val swatch = 13.dp
    val textField = 14.dp
    val numericKey = 16.dp
    val taskCard = 16.dp
    val banner = 16.dp
    val statCard = 18.dp
    val cta = 20.dp
    val alarmCard = 20.dp
    val configBlock = 20.dp
    val fab = 22.dp
    val chartCard = 22.dp
}

object WwuShape {
    val dayChip = RoundedCornerShape(WwuRadius.dayChip)
    val alphaKey = RoundedCornerShape(WwuRadius.alphaKey)
    val smallButton = RoundedCornerShape(WwuRadius.smallButton)
    val chip = RoundedCornerShape(WwuRadius.chip)
    val textField = RoundedCornerShape(WwuRadius.textField)
    val numericKey = RoundedCornerShape(WwuRadius.numericKey)
    val taskCard = RoundedCornerShape(WwuRadius.taskCard)
    val banner = RoundedCornerShape(WwuRadius.banner)
    val statCard = RoundedCornerShape(WwuRadius.statCard)
    val cta = RoundedCornerShape(WwuRadius.cta)
    val alarmCard = RoundedCornerShape(WwuRadius.alarmCard)
    val configBlock = RoundedCornerShape(WwuRadius.configBlock)
    val fab = RoundedCornerShape(WwuRadius.fab)
    val chartCard = RoundedCornerShape(WwuRadius.chartCard)
    val circle = CircleShape
}
