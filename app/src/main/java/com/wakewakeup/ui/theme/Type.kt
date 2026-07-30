package com.wakewakeup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Font files are not bundled in this scaffold. Drop Space Grotesk (300/400/500/700)
 * and Instrument Sans (400/500/600) .ttf files — both Open Font License, from Google
 * Fonts — into res/font/ and list them below; until then this falls back to the
 * platform default so the app still builds and runs.
 */
val SpaceGrotesk = FontFamily.Default
val InstrumentSans = FontFamily.Default

/** Every size here runs ~15% above the original design spec, per user request. */
object WwuType {
    val ringClock = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 106.sp, lineHeight = 106.sp, letterSpacing = (-0.04).em)
    val timePickerValue = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 84.sp, lineHeight = 88.sp, letterSpacing = (-0.03).em)
    val timePickerNeighbor = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 23.sp, lineHeight = 23.sp)
    val timeSeparator = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 68.sp, lineHeight = 68.sp)
    val cardTime = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 50.sp, lineHeight = 50.sp, letterSpacing = (-0.02).em)
    val goodMorningTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 50.sp, lineHeight = 55.sp, letterSpacing = (-0.02).em)
    val missionQuestion = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-0.02).em)
    val mathAnswerField = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 46.sp, lineHeight = 46.sp)
    val alarmsTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 39.sp, lineHeight = 43.sp)
    val taskPreview = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 37.sp, lineHeight = 37.sp)
    val missionCountdown = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 34.sp)
    val statNumber = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 30.sp, lineHeight = 30.sp)
    val numericKey = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 28.sp)
    val stepperCount = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 20.sp)
    val goodMorningStat = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 24.sp)
    val phraseTarget = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 34.sp)
    val phraseField = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp)
    val ctaPrimary = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 19.sp)
    val ctaSecondary = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 18.sp)
    val listRow = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 18.sp)
    val goodMorningCopy = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 27.sp)
    val taskName = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 16.sp)
    val wokeVsRow = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 16.sp)
    val cardLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 18.sp)
    val taskDesc = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp)
    val warningCaption = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp)
    val eyebrow = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 13.sp, letterSpacing = 0.14.em)
    val ringingEyebrow = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 13.sp, letterSpacing = 0.18.em)
    val dayChipCard = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 12.sp)
    val dayChipEdit = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 15.sp)
    val chartCaption = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 12.sp)
    val timeToWakeSub = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp)
    val statCardLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 15.sp)
}

val WwuTypography = Typography()
