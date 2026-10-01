package com.wakewakeup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.wakewakeup.R
import com.wakewakeup.ui.theme.Action
import com.wakewakeup.ui.theme.ActionBase
import com.wakewakeup.ui.theme.CalcDigitDark
import com.wakewakeup.ui.theme.CalcDigitLight
import com.wakewakeup.ui.theme.CalcDigitMid
import com.wakewakeup.ui.theme.FnKeyBase
import com.wakewakeup.ui.theme.FnKeyDark
import com.wakewakeup.ui.theme.FnKeyInk
import com.wakewakeup.ui.theme.FnKeyLight
import com.wakewakeup.ui.theme.FnKeyMid
import com.wakewakeup.ui.theme.HousingDeep
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.OnAction
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

private enum class CalcKind { Digit, Function, Confirm }

private val KEY = 64.dp
private val GAP_V = 14.dp
private val GAP_H = 16.dp

/** Keys sent to the mission: digits, "C", "⌫", "−" (sign) and "OK". */
object CalcKeys {
    const val CLEAR = "C"
    const val BACKSPACE = "⌫"
    const val SIGN = "−"
    const val OK = "OK"
}

/**
 * Braun ET 66 style keypad: convex round keys in three families — graphite digits, cream
 * function keys (C, ⌫), and an orange OK pill spanning two rows; 0 spans two columns.
 */
@Composable
fun CalculatorKeypad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val digitKey: @Composable (String) -> Unit = { d -> CalcKey(d, CalcKind.Digit, onKey) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(GAP_V)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP_H)) {
            digitKey("7"); digitKey("8"); digitKey("9")
            CalcKey(CalcKeys.CLEAR, CalcKind.Function, onKey, description = stringResource(R.string.cd_key_clear))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP_H)) {
            digitKey("4"); digitKey("5"); digitKey("6")
            CalcKey(CalcKeys.BACKSPACE, CalcKind.Function, onKey, description = stringResource(R.string.cd_key_backspace))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP_H)) {
            Column(verticalArrangement = Arrangement.spacedBy(GAP_V)) {
                Row(horizontalArrangement = Arrangement.spacedBy(GAP_H)) { digitKey("1"); digitKey("2"); digitKey("3") }
                Row(horizontalArrangement = Arrangement.spacedBy(GAP_H)) {
                    CalcKey("0", CalcKind.Digit, onKey, width = KEY * 2 + GAP_H)
                    CalcKey(CalcKeys.SIGN, CalcKind.Digit, onKey, description = stringResource(R.string.cd_key_sign))
                }
            }
            CalcKey(
                CalcKeys.OK, CalcKind.Confirm, onKey,
                height = KEY * 2 + GAP_V,
                description = stringResource(R.string.confirm),
            )
        }
    }
}

@Composable
private fun CalcKey(
    key: String,
    kind: CalcKind,
    onKey: (String) -> Unit,
    width: Dp = KEY,
    height: Dp = KEY,
    description: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val shape: Shape = if (width == height) CircleShape else WwuShape.calcPill
    val base = when (kind) {
        CalcKind.Digit -> HousingDeep
        CalcKind.Function -> FnKeyBase
        CalcKind.Confirm -> ActionBase
    }
    val textStyle: TextStyle
    val textColor: Color
    when (kind) {
        CalcKind.Digit -> { textStyle = WwuType.calcDigit; textColor = Ink }
        CalcKind.Function -> { textStyle = if (key == CalcKeys.BACKSPACE) WwuType.calcDigit.copy(fontSize = WwuType.calcFn.fontSize * 1.1f) else WwuType.calcFn; textColor = FnKeyInk }
        CalcKind.Confirm -> { textStyle = WwuType.calcOk; textColor = OnAction }
    }
    val travel = if (pressed) 2.dp else 0.dp
    Box(
        Modifier
            .size(width, height)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                onKey(key)
            }
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .dropShadow(shape, Shadow(radius = if (kind == CalcKind.Confirm) 12.dp else 10.dp, color = Color.Black, offset = DpOffset(0.dp, 6.dp), alpha = if (kind == CalcKind.Confirm) 0.5f else 0.45f))
            .solidBase(shape, base, 3.dp)
            .graphicsLayer { translationY = travel.toPx() }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val brush = when (kind) {
                    CalcKind.Digit -> Brush.radialGradient(
                        0f to CalcDigitLight, 0.6f to CalcDigitMid, 1f to CalcDigitDark,
                        center = Offset(w / 2, h * 0.36f), radius = maxOf(w, h) * 0.62f,
                    )
                    CalcKind.Function -> Brush.radialGradient(
                        0f to FnKeyLight, 0.65f to FnKeyMid, 1f to FnKeyDark,
                        center = Offset(w / 2, h * 0.36f), radius = w * 0.812f,
                    )
                    CalcKind.Confirm -> Brush.radialGradient(
                        0f to Color(0xFFF07A50), 0.55f to Action, 1f to Color(0xFFCF5530),
                        center = Offset(w / 2, h * 0.30f), radius = h * 0.75f,
                    )
                }
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind { drawOutline(outline, brush) }
            }
            .topHighlight(
                shape,
                when (kind) {
                    CalcKind.Digit -> 0.10f
                    CalcKind.Function -> 0.35f
                    CalcKind.Confirm -> 0.25f
                },
                Color.White,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(key, style = textStyle, color = textColor)
    }
}
