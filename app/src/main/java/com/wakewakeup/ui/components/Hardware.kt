package com.wakewakeup.ui.components

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakewakeup.ui.theme.Action
import com.wakewakeup.ui.theme.ActionBase
import com.wakewakeup.ui.theme.ActionDark
import com.wakewakeup.ui.theme.ActionKeyBottom
import com.wakewakeup.ui.theme.ActionLight
import com.wakewakeup.ui.theme.Danger
import com.wakewakeup.ui.theme.DangerBase
import com.wakewakeup.ui.theme.DangerDark
import com.wakewakeup.ui.theme.DangerLight
import com.wakewakeup.ui.theme.EspereLabelPlate
import com.wakewakeup.ui.theme.EspereLabelPlateSunk
import com.wakewakeup.ui.theme.GraphiteDark
import com.wakewakeup.ui.theme.GraphiteLight
import com.wakewakeup.ui.theme.GrilleFace
import com.wakewakeup.ui.theme.GrilleHole
import com.wakewakeup.ui.theme.HousingDeep
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.InkMuted
import com.wakewakeup.ui.theme.InkOff
import com.wakewakeup.ui.theme.KeyFaceBottom
import com.wakewakeup.ui.theme.KeyFaceTop
import com.wakewakeup.ui.theme.KeyLedOff
import com.wakewakeup.ui.theme.KeyPressed
import com.wakewakeup.ui.theme.LedOff
import com.wakewakeup.ui.theme.OnAction
import com.wakewakeup.ui.theme.PlateBottom
import com.wakewakeup.ui.theme.PlateTop
import com.wakewakeup.ui.theme.RibDark
import com.wakewakeup.ui.theme.RibLight
import com.wakewakeup.ui.theme.RibSunkDark
import com.wakewakeup.ui.theme.RibSunkLight
import com.wakewakeup.ui.theme.SwitchTrack
import com.wakewakeup.ui.theme.Vfd
import com.wakewakeup.ui.theme.Visor
import com.wakewakeup.ui.theme.WwuShape
import com.wakewakeup.ui.theme.WwuType

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

/** Key press: 80 ms. */
internal const val PRESS_MS = 80

/** The switch's snap: 240 ms with a small overshoot. */
internal val SnapEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * "Reduzir movimento": the system's animator duration scale set to 0 (Developer options /
 * Accessibility "Remove animations"). Flips become swaps, no shake, no pulse, no rising sunrise.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
private fun pressTravel(interaction: MutableInteractionSource, travel: Dp): Dp {
    val pressed by interaction.collectIsPressedAsState()
    val value by animateDpAsState(if (pressed) travel else 0.dp, tween(PRESS_MS), label = "press")
    return value
}

// ---------------------------------------------------------------------------------------------
// Relief primitives (light from above)
// ---------------------------------------------------------------------------------------------

private fun blur(radius: Dp, y: Dp, alpha: Float, color: Color = Color.Black) =
    Shadow(radius = radius, color = color, offset = DpOffset(0.dp, y), alpha = alpha)

/** A solid, unblurred copy of [shape] [depth] below the element: the "0 3px 0 #0A080A" base. */
fun Modifier.solidBase(shape: Shape, color: Color, depth: Dp): Modifier = drawBehind {
    if (depth <= 0.dp) return@drawBehind
    val outline = shape.createOutline(size, layoutDirection, this)
    translate(top = depth.toPx()) { drawOutline(outline, color) }
}

/** CSS `inset 0 1px 0 color`: a thin highlight along the top edge. */
fun Modifier.topHighlight(shape: Shape, alpha: Float, color: Color = Ink): Modifier =
    innerShadow(shape, blur(1.dp, 1.dp, alpha, color))

/** CSS `0 0 Npx color`: a soft glow around a lit LED / segment. */
fun Modifier.glow(shape: Shape, color: Color, radius: Dp = 6.dp, alpha: Float = 0.55f): Modifier =
    dropShadow(shape, Shadow(radius = radius, color = color, alpha = alpha))

/** elevated.faceplate: plate gradient + top highlight + 2 dp solid base + soft drop shadow. */
fun Modifier.faceplate(
    shape: Shape = WwuShape.faceplate,
    softShadow: Boolean = true,
    baseDepth: Dp = 2.dp,
): Modifier = this
    .then(if (softShadow) Modifier.dropShadow(shape, blur(18.dp, 10.dp, 0.45f)) else Modifier)
    .solidBase(shape, HousingDeep, baseDepth)
    .background(Brush.verticalGradient(listOf(PlateTop, PlateBottom)), shape)
    .topHighlight(shape, 0.07f)

/** recessed.visor: near-black well with a deep inner shadow and a faint lower lip. */
fun Modifier.visor(shape: Shape = WwuShape.visorSmall, depth: Dp = 2.dp, blurRadius: Dp = 5.dp, lip: Boolean = true): Modifier = this
    .background(Visor, shape)
    .innerShadow(shape, blur(blurRadius, depth, 0.9f))
    .then(if (lip) Modifier.innerShadow(shape, blur(1.dp, (-1).dp, 0.05f, Ink)) else Modifier)

/** Speaker grille: dot grid on matte plastic. Used only on the alarm-list "PRÓXIMO" strip. */
fun Modifier.speakerGrille(shape: Shape = WwuShape.faceplateSmall): Modifier = this
    .solidBase(shape, HousingDeep, 2.dp)
    .background(GrilleFace, shape)
    .drawWithCache {
        val pitch = 7.dp.toPx()
        val r = 1.5.dp.toPx()
        onDrawBehind {
            var y = pitch / 2
            while (y < size.height) {
                var x = pitch / 2
                while (x < size.width) {
                    drawCircle(GrilleHole, r, Offset(x, y))
                    x += pitch
                }
                y += pitch
            }
        }
    }
    .topHighlight(shape, 0.06f)

// ---------------------------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------------------------

@Composable
fun glowShadow(color: Color, radius: Dp = 6.dp, alpha: Float = 0.55f): androidx.compose.ui.graphics.Shadow {
    val px = with(LocalDensity.current) { radius.toPx() }
    return androidx.compose.ui.graphics.Shadow(color = color.copy(alpha = alpha), offset = Offset.Zero, blurRadius = px)
}

/** Amber VFD readout in Barlow Condensed with its glow. */
@Composable
fun VfdText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Vfd,
    glowColor: Color = Vfd,
    glowRadius: Dp = 6.dp,
    glowAlpha: Float = 0.55f,
    tracking: Float = 0f,
    style: TextStyle = WwuType.vfd(size, tracking),
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text,
        modifier = modifier,
        style = style.copy(shadow = glowShadow(glowColor, glowRadius, glowAlpha)),
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

/** Screen-print label: 12/600, caps, wide tracking. */
@Composable
fun PrintLabel(text: String, modifier: Modifier = Modifier, color: Color = InkMuted, style: TextStyle = WwuType.label, textAlign: TextAlign? = null) {
    Text(text.uppercase(), modifier = modifier, style = style, color = color, textAlign = textAlign)
}

/** Back row: 48×48 "‹" + title 20/600. */
@Composable
fun BackHeader(title: String, backDescription: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClickLabel = backDescription, role = Role.Button) { onBack() }
                .semantics { contentDescription = backDescription },
            contentAlignment = Alignment.Center,
        ) {
            Chevron(pointsLeft = true, color = InkMuted, size = 14.dp)
        }
        Text(title, style = WwuType.titleS, color = Ink)
    }
}

/** The "›" / "‹" chevron, drawn so it's crisp at any font scale. */
@Composable
fun Chevron(pointsLeft: Boolean = false, color: Color = InkMuted, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (pointsLeft) {
                moveTo(w * 0.68f, h * 0.12f); lineTo(w * 0.3f, h * 0.5f); lineTo(w * 0.68f, h * 0.88f)
            } else {
                moveTo(w * 0.32f, h * 0.12f); lineTo(w * 0.7f, h * 0.5f); lineTo(w * 0.32f, h * 0.88f)
            }
        }
        drawPath(path, color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ---------------------------------------------------------------------------------------------
// Round buttons
// ---------------------------------------------------------------------------------------------

enum class RoundStyle { Action, Danger, Graphite }

/**
 * Physical round button. Orange is concave (darker at centre-bottom), danger and graphite are
 * convex (highlight 38% from the top). Pressing sinks the face onto its solid base.
 */
@Composable
fun RoundButton(
    onClick: () -> Unit,
    size: Dp,
    style: RoundStyle,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    baseDepth: Dp = 4.dp,
    softShadow: Shadow? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val travel = pressTravel(interaction, (baseDepth - 1.dp).coerceAtLeast(1.dp))
    val haptic = LocalHapticFeedback.current
    val baseColor = when (style) {
        RoundStyle.Action -> ActionBase
        RoundStyle.Danger -> DangerBase
        RoundStyle.Graphite -> HousingDeep
    }
    Box(
        modifier = modifier
            .size(size)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // Fixed base (and soft shadow): the face travels down onto it.
        Box(
            Modifier
                .matchParentSize()
                .then(if (softShadow != null) Modifier.dropShadow(CircleShape, softShadow) else Modifier)
                .solidBase(CircleShape, baseColor, baseDepth),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationY = travel.toPx() }
                .drawWithCache {
                    val d = this.size.width
                    val brush = when (style) {
                        RoundStyle.Action -> Brush.radialGradient(
                            0f to ActionDark, 0.72f to ActionLight,
                            center = Offset(d * 0.5f, d * 0.62f), radius = d * 0.7965f,
                        )
                        RoundStyle.Danger -> Brush.radialGradient(
                            0f to DangerLight, 0.6f to Danger, 1f to DangerDark,
                            center = Offset(d * 0.5f, d * 0.38f), radius = d * 0.7965f,
                        )
                        RoundStyle.Graphite -> Brush.radialGradient(
                            0f to GraphiteLight, 0.75f to GraphiteDark,
                            center = Offset(d * 0.5f, d * 0.38f), radius = d * 0.7965f,
                        )
                    }
                    onDrawBehind { drawCircle(brush) }
                }
                .then(
                    when (style) {
                        RoundStyle.Action -> Modifier.innerShadow(CircleShape, blur(4.dp, 2.dp, 0.25f))
                        RoundStyle.Danger -> Modifier.topHighlight(CircleShape, 0.15f, Color.White)
                        RoundStyle.Graphite -> Modifier.topHighlight(CircleShape, 0.10f, Color.White)
                    },
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** Round button with its screen-print caption underneath. */
@Composable
fun LabeledRoundButton(
    label: String,
    onClick: () -> Unit,
    size: Dp,
    style: RoundStyle,
    modifier: Modifier = Modifier,
    labelColor: Color = Ink,
    labelStyle: TextStyle = WwuType.label,
    gap: Dp = 8.dp,
    baseDepth: Dp = 4.dp,
    softShadow: Shadow? = null,
    glyph: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        RoundButton(onClick = onClick, size = size, style = style, contentDescription = label, baseDepth = baseDepth, softShadow = softShadow, content = glyph)
        PrintLabel(label, color = labelColor, style = labelStyle, textAlign = TextAlign.Center)
    }
}

fun softShadow(radius: Dp, y: Dp, alpha: Float) = blur(radius, y, alpha)

// Glyphs drawn on buttons (on.action / ink), proportional to the button size.

@Composable
fun PlusGlyph(length: Dp, thickness: Dp, color: Color = OnAction) {
    Canvas(Modifier.size(length)) {
        val t = thickness.toPx()
        val l = this.size.width
        val r = CornerRadius(t / 2, t / 2)
        drawRoundRect(color, Offset(0f, (l - t) / 2), Size(l, t), r)
        drawRoundRect(color, Offset((l - t) / 2, 0f), Size(t, l), r)
    }
}

@Composable
fun MinusGlyph(length: Dp, thickness: Dp, color: Color = Ink) {
    Canvas(Modifier.size(width = length, height = thickness)) {
        drawRoundRect(color, cornerRadius = CornerRadius(this.size.height / 2, this.size.height / 2))
    }
}

@Composable
fun CheckGlyph(buttonSize: Dp, color: Color = OnAction) {
    Canvas(Modifier.size(buttonSize)) {
        val d = this.size.width
        val path = Path().apply {
            moveTo(d * 0.34f, d * 0.47f)
            lineTo(d * 0.455f, d * 0.585f)
            lineTo(d * 0.667f, d * 0.353f)
        }
        drawPath(path, color, style = Stroke(width = d * 0.053f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CrossGlyph(buttonSize: Dp, color: Color = Ink) {
    Canvas(Modifier.size(buttonSize)) {
        val d = this.size.width
        val half = d * 0.118f
        val c = Offset(d / 2, d / 2)
        val stroke = d * 0.066f
        drawLine(color, c + Offset(-half, -half), c + Offset(half, half), stroke, StrokeCap.Round)
        drawLine(color, c + Offset(-half, half), c + Offset(half, -half), stroke, StrokeCap.Round)
    }
}

@Composable
fun PlayGlyph(width: Dp, height: Dp, color: Color = OnAction, nudge: Dp = 0.dp) {
    Canvas(Modifier.offset(x = nudge).size(width = width, height = height)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 2)
            lineTo(0f, this@Canvas.size.height)
            close()
        }
        drawPath(path, color)
    }
}

// ---------------------------------------------------------------------------------------------
// Keys
// ---------------------------------------------------------------------------------------------

/**
 * Latching key ("piano key"): raised with a 3 dp base when up; sunk 2 dp with an inner shadow
 * and a lit orange LED when down.
 */
@Composable
fun LatchKey(
    down: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = WwuShape.keySmall,
    description: String? = null,
    visualInset: Dp = 0.dp,
    content: @Composable BoxScope.(down: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sink by animateDpAsState(if (down) 2.dp else 0.dp, tween(PRESS_MS), label = "latch")
    Box(
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .semantics {
                if (description != null) contentDescription = description
                selected = down
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = visualInset)
                .then(if (!down) Modifier.solidBase(shape, HousingDeep, 3.dp) else Modifier)
                .graphicsLayer { translationY = sink.toPx() }
                .then(
                    if (down) {
                        Modifier.background(KeyPressed, shape).innerShadow(shape, blur(4.dp, 2.dp, 0.8f))
                    } else {
                        Modifier.background(Brush.verticalGradient(listOf(KeyFaceTop, KeyFaceBottom)), shape).topHighlight(shape, 0.08f, Color.White)
                    },
                ),
        )
        Box(Modifier.matchParentSize().padding(horizontal = visualInset).graphicsLayer { translationY = sink.toPx() }) { content(down) }
    }
}

/** The 3 dp LED strip on top of a latching key. */
@Composable
fun KeyLed(on: Boolean, width: Dp = 14.dp, modifier: Modifier = Modifier) {
    val shape = WwuShape.segment
    Box(
        modifier
            .size(width = width, height = 3.dp)
            .then(if (on) Modifier.glow(shape, Action, 6.dp, 1f) else Modifier)
            .background(if (on) Action else KeyLedOff, shape),
    )
}

/** Small round LED (status lamp). */
@Composable
fun Lamp(color: Color, lit: Boolean = true, size: Dp = 8.dp, glowAlpha: Float = 0.6f, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .then(if (lit) Modifier.glow(CircleShape, color, 6.dp, glowAlpha) else Modifier)
            .background(if (lit) color else LedOff, CircleShape),
    )
}

/** Rectangular orange action key (ATIVAR, PERMITIR): 48 dp tall, sinks 2 dp. */
@Composable
fun OrangeKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val travel = pressTravel(interaction, 2.dp)
    val haptic = LocalHapticFeedback.current
    val shape = WwuShape.key
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .solidBase(shape, ActionBase, 2.dp - travel)
            .graphicsLayer { translationY = travel.toPx() }
            .background(Brush.verticalGradient(listOf(ActionLight, ActionKeyBottom)), shape)
            .topHighlight(shape, 0.2f, Color.White)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = WwuType.label, color = OnAction, maxLines = 1)
    }
}

/** A key that's already latched down and can't be pressed (LIBERADO). */
@Composable
fun SunkKey(text: String, modifier: Modifier = Modifier, color: Color = Vfd) {
    val shape = WwuShape.key
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(KeyPressed, shape)
            .innerShadow(shape, blur(4.dp, 2.dp, 0.8f))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = WwuType.label, color = color, maxLines = 1)
    }
}

/** Full-width pill (CONTINUAR): orange when enabled, sunk otherwise. Always tappable. */
@Composable
fun PillButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val travel = pressTravel(interaction, 2.dp)
    val haptic = LocalHapticFeedback.current
    val shape = WwuShape.pill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(if (enabled) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
                onClick()
            }
            .then(
                if (enabled) {
                    Modifier
                        .dropShadow(shape, blur(14.dp, 8.dp, 0.5f))
                        .solidBase(shape, ActionBase, 3.dp - travel)
                        .graphicsLayer { translationY = travel.toPx() }
                        .drawWithCache {
                            val brush = Brush.radialGradient(
                                0f to ActionDark, 0.8f to ActionLight,
                                center = Offset(size.width / 2, size.height * 0.6f),
                                radius = size.width * 0.55f,
                            )
                            val outline = shape.createOutline(size, layoutDirection, this)
                            onDrawBehind { drawOutline(outline, brush) }
                        }
                } else {
                    Modifier
                        .graphicsLayer { translationY = travel.toPx() }
                        .background(KeyPressed, shape)
                        .innerShadow(shape, blur(4.dp, 2.dp, 0.8f))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = WwuType.pill, color = if (enabled) OnAction else InkOff)
    }
}

// ---------------------------------------------------------------------------------------------
// Switch (liga/desliga)
// ---------------------------------------------------------------------------------------------

/**
 * Physical slide switch: recessed 56×30 track, grooved 24 dp knob. On reveals a glowing
 * orange window on the left. 48 dp tall touch area, 240 ms overshoot snap + haptic.
 */
@Composable
fun SlideSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    onLabel: String = "",
    offLabel: String = "",
) {
    val haptic = LocalHapticFeedback.current
    val reduce = rememberReduceMotion()
    val knobX by animateDpAsState(
        if (checked) 29.dp else 3.dp,
        if (reduce) tween(0) else tween(240, easing = SnapEasing),
        label = "knob",
    )
    val trackShape = WwuShape.field
    Box(
        modifier = modifier
            .height(48.dp)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
            ) {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(it)
            }
            .semantics {
                if (description != null) contentDescription = description
                stateDescription = if (checked) onLabel else offLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 30.dp)
                .solidBase(trackShape, Ink.copy(alpha = 0.06f), 1.dp)
                .background(SwitchTrack, trackShape)
                .innerShadow(trackShape, blur(4.dp, 2.dp, 0.9f)),
        ) {
            val window = RoundedCornerShape(3.dp)
            Box(
                Modifier
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    .width(22.dp)
                    .fillMaxHeight()
                    .glow(window, Action, 8.dp, 0.5f)
                    .background(Action, window),
            )
            val knobShape = WwuShape.knob
            Row(
                Modifier
                    .offset(x = knobX, y = 3.dp)
                    .size(24.dp)
                    .dropShadow(knobShape, blur(3.dp, 2.dp, 0.7f))
                    .background(Brush.verticalGradient(listOf(Color(0xFF4A4148), Color(0xFF2E272D))), knobShape)
                    .topHighlight(knobShape, 0.14f, Color.White),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { Box(Modifier.size(width = 1.dp, height = 10.dp).background(KeyPressed)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// LED meter
// ---------------------------------------------------------------------------------------------

/**
 * Row of LED segments. Lit segments are amber with a glow, or coral in their [hot] zone.
 * Steps one segment at a time — never interpolates.
 */
@Composable
fun LedMeter(
    count: Int,
    isLit: (Int) -> Boolean,
    isHot: (Int) -> Boolean,
    segmentHeight: Dp,
    modifier: Modifier = Modifier,
    gap: Dp = 3.dp,
    shape: Shape = WwuShape.segment,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(count) { i -> LedSegment(isLit(i), isHot(i), Modifier.weight(1f).height(segmentHeight), shape) }
    }
}

@Composable
fun LedSegment(lit: Boolean, hot: Boolean, modifier: Modifier, shape: Shape = WwuShape.segment) {
    val color = if (hot) Action else Vfd
    Box(
        modifier
            .then(if (lit) Modifier.glow(shape, color, 6.dp, if (hot) 0.6f else 0.55f) else Modifier)
            .background(if (lit) color else LedOff, shape),
    )
}

// ---------------------------------------------------------------------------------------------
// Espere bar (snooze bar)
// ---------------------------------------------------------------------------------------------

/**
 * The classic snooze bar on top of the radio: full width, ribbed grip surface, centred label
 * plate. 4 dp of travel. While Espere is active it stays sunk with an orange LED.
 */
@Composable
fun EspereBar(
    active: Boolean,
    title: String,
    subtitle: String,
    activeTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val travel = pressTravel(interaction, 4.dp)
    val haptic = LocalHapticFeedback.current
    val shape = WwuShape.faceplate
    val ribLight = if (active) RibSunkLight else RibLight
    val ribDark = if (active) RibSunkDark else RibDark
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (active) 80.dp else 84.dp)
            .then(
                if (active) {
                    Modifier.semantics { contentDescription = activeTitle }
                } else {
                    Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = title) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onClick()
                    }
                },
            )
            .then(
                if (active) {
                    Modifier.solidBase(shape, HousingDeep, 1.dp)
                } else {
                    Modifier
                        .dropShadow(shape, blur(20.dp, 12.dp, 0.55f))
                        .solidBase(shape, HousingDeep, 5.dp - travel)
                },
            )
            .graphicsLayer { translationY = travel.toPx() }
            .drawWithCache {
                val stripe = 7.dp.toPx()
                val light = 5.dp.toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = Path().apply { addOutline(outline) }
                onDrawBehind {
                    drawOutline(outline, ribDark)
                    clipPath(path) {
                        var x = 0f
                        while (x < size.width) {
                            drawRect(ribLight, Offset(x, 0f), Size(light, size.height))
                            x += stripe
                        }
                    }
                }
            }
            .then(
                if (active) Modifier.innerShadow(shape, blur(6.dp, 2.dp, 0.8f))
                else Modifier.topHighlight(shape, 0.1f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Row(
                Modifier
                    .background(EspereLabelPlateSunk, WwuShape.field)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Lamp(Action, size = 8.dp, glowAlpha = 1f)
                Text(activeTitle.uppercase(), style = WwuType.espereLabel, color = Ink)
            }
        } else {
            Column(
                Modifier
                    .background(EspereLabelPlate, WwuShape.field)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title.uppercase(), style = WwuType.espereLabel, color = Ink)
                Text(subtitle.uppercase(), style = WwuType.espereSub, color = InkMuted)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Faceplate rows
// ---------------------------------------------------------------------------------------------

/** 1 dp separator between rows inside a faceplate. */
@Composable
fun PlateDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(com.wakewakeup.ui.theme.Divider))
}

/** A labelled readout inside a small visor (ALARME 06:30). */
@Composable
fun RowScope.VisorStat(label: String, value: String, bright: Boolean = false) {
    Column(
        Modifier
            .weight(1f)
            .visor(WwuShape.visorSmall, lip = false)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PrintLabel(label)
        VfdText(
            value,
            28.sp.nonScaling(),
            color = if (bright) com.wakewakeup.ui.theme.VfdBright else Vfd,
            glowColor = if (bright) com.wakewakeup.ui.theme.VfdBright else Vfd,
            glowRadius = if (bright) 8.dp else 6.dp,
            glowAlpha = if (bright) 0.6f else 0.55f,
            maxLines = 1,
        )
    }
}

/**
 * Display readouts are sized to their windows, so they don't grow with the system font scale
 * (labels and body copy still do). Converts the design's dp value to an equivalent sp.
 */
@Composable
fun TextUnit.nonScaling(): TextUnit {
    val density = LocalDensity.current
    return with(density) { this@nonScaling.value.dp.toSp() }
}
