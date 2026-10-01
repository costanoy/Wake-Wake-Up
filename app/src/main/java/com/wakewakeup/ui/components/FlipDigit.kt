package com.wakewakeup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.wakewakeup.ui.theme.FlipBottomEnd
import com.wakewakeup.ui.theme.FlipBottomStart
import com.wakewakeup.ui.theme.FlipTopEnd
import com.wakewakeup.ui.theme.FlipTopStart
import com.wakewakeup.ui.theme.Hinge
import com.wakewakeup.ui.theme.Ink
import com.wakewakeup.ui.theme.WwuType
import kotlin.math.roundToInt

/** Card sizes from the spec (width × height, Barlow Condensed 600 size, hinge thickness). */
enum class FlipSize(val width: Dp, val height: Dp, val font: Dp, val hinge: Dp, val gap: Dp) {
    XL(68.dp, 104.dp, 92.dp, 2.dp, 6.dp),
    L(62.dp, 94.dp, 84.dp, 2.dp, 6.dp),
    M(50.dp, 74.dp, 64.dp, 2.dp, 4.dp),
    Empty(48.dp, 72.dp, 56.dp, 1.dp, 5.dp),
    S(26.dp, 38.dp, 32.dp, 1.dp, 3.dp),
}

/** Drop shadow under a card: "0 y blur black@alpha". */
data class CardShadow(val blur: Dp, val y: Dp, val alpha: Float)

private const val FLIP_MS = 420
private val FlipOutEasing = CubicBezierEasing(0.55f, 0f, 0.9f, 0.5f)
private val FlipInEasing = CubicBezierEasing(0.2f, 0.9f, 0.35f, 1.25f)

/**
 * One split-flap digit. On change the old top half falls 0 → −90° around its bottom edge, then
 * the new bottom half drops 90° → 0° around its top edge with a small bounce; the static bottom
 * half darkens as the flap passes over it. With reduce motion the digit simply swaps.
 */
@Composable
fun FlipDigit(
    char: Char,
    size: FlipSize,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    shadow: CardShadow? = CardShadow(10.dp, 4.dp, 0.6f),
    reduceMotion: Boolean = rememberReduceMotion(),
) {
    var shown by remember { mutableStateOf(char) }
    var previous by remember { mutableStateOf(char) }
    var animating by remember { mutableStateOf(false) }
    val progress = remember { Animatable(2f) }

    LaunchedEffect(char) {
        if (char == shown && !animating) return@LaunchedEffect
        if (reduceMotion) {
            previous = char
            shown = char
            animating = false
            progress.snapTo(2f)
            return@LaunchedEffect
        }
        previous = shown
        shown = char
        animating = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(FLIP_MS, easing = FlipOutEasing))
        progress.animateTo(2f, tween(FLIP_MS, easing = FlipInEasing))
        animating = false
    }

    val w = size.width
    val h = size.height
    val radius = (h.value * 0.08f).roundToInt().dp.coerceAtLeast(3.dp)
    val fontSp = with(LocalDensity.current) { size.font.toSp() }
    val style = WwuType.flip(fontSp)
    val cardShape = RoundedCornerShape(radius)

    Box(
        modifier
            .size(w, h)
            .then(
                if (shadow != null) {
                    Modifier.dropShadow(cardShape, Shadow(radius = shadow.blur, color = Color.Black, offset = DpOffset(0.dp, shadow.y), alpha = shadow.alpha))
                } else Modifier,
            )
    ) {
        // Static layers: new top, and the old bottom until the new flap lands on it.
        FlipHalf(shown, top = true, w, h, radius, style, color)
        FlipHalf(if (animating) previous else shown, top = false, w, h, radius, style, color, Modifier.offset(y = h / 2))

        if (animating) {
            // Shade cast on the static bottom half by the passing flap (peaks ~45%).
            Box(
                Modifier
                    .offset(y = h / 2)
                    .size(w, h / 2)
                    .graphicsLayer {
                        val t = progress.value / 2f
                        alpha = if (t < 0.45f) t / 0.45f * 0.8f else ((1f - t) / 0.55f * 0.8f).coerceAtLeast(0f)
                    }
                    .clip(RoundedCornerShape(bottomStart = radius, bottomEnd = radius))
                    .background(Brush.verticalGradient(listOf(Color.Black, Color.Black.copy(alpha = 0.3f)))),
            )
            // Falling top flap (old digit).
            FlipHalf(
                previous, top = true, w, h, radius, style, color,
                Modifier.graphicsLayer {
                    val p = progress.value.coerceIn(0f, 1f)
                    alpha = if (progress.value >= 1f) 0f else 1f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    cameraDistance = 6f * h.toPx()
                    rotationX = -90f * p
                },
                darken = { (progress.value.coerceIn(0f, 1f) * 0.65f) },
            )
            // Dropping bottom flap (new digit).
            FlipHalf(
                shown, top = false, w, h, radius, style, color,
                Modifier
                    .offset(y = h / 2)
                    .graphicsLayer {
                        val p = (progress.value - 1f).coerceIn(-0.1f, 1.2f)
                        alpha = if (progress.value < 1f) 0f else 1f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        cameraDistance = 6f * h.toPx()
                        rotationX = 90f * (1f - p)
                    },
                darken = { ((2f - progress.value).coerceIn(0f, 1f) * 0.65f) },
            )
        }

        // Hinge: the split line across the middle.
        Box(
            Modifier
                .offset(y = h / 2 - size.hinge / 2)
                .size(w, size.hinge)
                .background(Hinge),
        )
    }
}

@Composable
private fun FlipHalf(
    char: Char,
    top: Boolean,
    w: Dp,
    h: Dp,
    radius: Dp,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    darken: (() -> Float)? = null,
) {
    val shape = if (top) RoundedCornerShape(topStart = radius, topEnd = radius) else RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    val brush = if (top) Brush.verticalGradient(listOf(FlipTopStart, FlipTopEnd)) else Brush.verticalGradient(listOf(FlipBottomStart, FlipBottomEnd))
    Box(
        modifier
            .size(w, h / 2)
            .clip(shape)
            .background(brush),
    ) {
        Box(
            Modifier
                .offset(y = if (top) h / 4 else -h / 4)
                .requiredSize(w, h),
            contentAlignment = Alignment.Center,
        ) {
            Text(char.toString(), style = style, color = color, maxLines = 1, softWrap = false)
        }
        if (darken != null) {
            Box(
                Modifier
                    .size(w, h / 2)
                    .graphicsLayer { alpha = darken() }
                    .background(Color.Black),
            )
        }
    }
}

/** Two stacked colon dots between hour and minute cards. */
@Composable
fun ColonDots(dot: Dp, gap: Dp, color: Color, glow: Boolean = false, horizontalPadding: Dp = 4.dp, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = horizontalPadding), verticalArrangement = Arrangement.spacedBy(gap)) {
        repeat(2) {
            Box(
                Modifier
                    .size(dot)
                    .then(if (glow) Modifier.glow(CircleShape, color, 8.dp, 1f) else Modifier)
                    .background(color, CircleShape),
            )
        }
    }
}

/**
 * A row of flip digits, exposed to TalkBack as one node ([spokenText]) rather than as
 * separate digits.
 */
@Composable
fun FlipNumber(
    text: String,
    size: FlipSize,
    spokenText: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    shadow: CardShadow? = CardShadow(10.dp, 4.dp, 0.6f),
    colon: (@Composable () -> Unit)? = null,
    colonAfter: Int = 2,
) {
    val reduce = rememberReduceMotion()
    Row(
        modifier.clearAndSetSemantics { contentDescription = spokenText },
        horizontalArrangement = Arrangement.spacedBy(size.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { index, c ->
            if (colon != null && index == colonAfter) colon()
            FlipDigit(c, size, color = color, shadow = shadow, reduceMotion = reduce)
        }
    }
}
