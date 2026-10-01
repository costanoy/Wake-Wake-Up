package com.wakewakeup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wakewakeup.R

/** Numbers: flip digits and VFD readouts. OFL, bundled in res/font. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
)

/** Everything else. OFL, bundled in res/font (static instances of the variable font). */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

private val noFontPadding = PlatformTextStyle(includeFontPadding = false)
private val centeredLine = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

object WwuType {
    // Instrument Sans
    val display = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.01).em)
    val titleL = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp)
    val titleM = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp)
    val titleS = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)
    val bodyStrong = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodyInput = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val bodyS = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)
    val bodyXS = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val caption = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp)
    val dayKey = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
    val keyLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp)

    /** Screen-print labels: 12/600, ALL CAPS, +12% tracking. */
    val label = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.em)
    val labelWide = label.copy(letterSpacing = 0.2.em)
    val espereLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.22.em)
    val espereSub = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.14.em)
    val pill = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.14.em)
    val gmUnit = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = 0.14.em)

    // Calculator keys
    val calcDigit = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp, platformStyle = noFontPadding)
    val calcFn = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp, platformStyle = noFontPadding)
    val calcOk = calcFn.copy(letterSpacing = 0.08.em)

    // Barlow Condensed VFD readouts (500)
    fun vfd(size: TextUnit, tracking: Float = 0f) = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = size,
        lineHeight = size * 1.15f,
        letterSpacing = tracking.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centeredLine,
    )

    /** Flip digits (600). Size is fixed in dp by the card, so callers pass a non-scaling unit. */
    fun flip(size: TextUnit) = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = size,
        lineHeight = size,
        platformStyle = noFontPadding,
        lineHeightStyle = centeredLine,
    )
}

val WwuTypography = Typography(
    bodyLarge = WwuType.body,
    bodyMedium = WwuType.bodyS,
    labelLarge = WwuType.label,
)
