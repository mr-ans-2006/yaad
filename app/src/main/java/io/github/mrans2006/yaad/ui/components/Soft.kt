package io.github.mrans2006.yaad.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.ui.theme.LocalReduceMotion
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

/*
 * The "soft brutalist" building blocks (v1.1). The look comes from four rules:
 *   1. a thin 1.5 dp outline on everything        (border)
 *   2. ONE hard shadow, only on primary things    (search bar, primary buttons, the key-line card)
 *   3. colour only on tags                        (muted pastels)
 *   4. black primary buttons                      (bone in dark mode)
 */

val Hairline = 1.5.dp
val CardRadius = 14.dp
/** How far the single hard shadow sits down-right. Leave this much space to the right/bottom of a shadowed box. */
val HardShadow = 3.dp

/**
 * Fill + thin outline (+ an optional hard shadow drawn OUTSIDE the box, down-right, no blur).
 */
fun Modifier.outlined(
    fill: Color,
    outline: Color,
    radius: Dp = CardRadius,
    border: Dp = Hairline,
    shadow: Dp = 0.dp,
    shadowColor: Color = outline,
): Modifier = this
    .then(
        if (shadow > 0.dp) Modifier.drawBehind {
            val r = radius.toPx()
            drawRoundRect(color = shadowColor, topLeft = Offset(shadow.toPx(), shadow.toPx()), size = size, cornerRadius = CornerRadius(r, r))
        } else Modifier,
    )
    .clip(RoundedCornerShape(radius))
    .background(fill)
    .border(border, outline, RoundedCornerShape(radius))

/** Cards and buttons shrink a touch while your finger is on them. Off when "Remove animations" is on. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    if (LocalReduceMotion.current) return this
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A clickable that also does the press-scale. */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return pressScale(source).clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** An outlined box: the most used piece in the app. Give it [shadow] = [HardShadow] only for primary elements. */
@Composable
fun BrutalBox(
    modifier: Modifier = Modifier,
    fill: Color = Yaad.colors.card,
    shadow: Dp = 0.dp,
    radius: Dp = CardRadius,
    border: Dp = Hairline,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.outlined(fill, Yaad.colors.outline, radius, border, shadow, Yaad.colors.shadow), content = content)
}

/**
 * A button. [primary] = black with the hard shadow (one per screen, for the main action).
 * Otherwise a quiet outlined button.
 */
@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val c = Yaad.colors
    val fill = if (primary) c.primary else c.card
    val content = if (primary) c.onPrimary else c.text
    Box(
        modifier
            .pressable(enabled, onClick)
            .outlined(if (enabled) fill else fill.copy(alpha = 0.5f), c.outline, if (compact) 11.dp else 12.dp, Hairline,
                if (primary && !compact) HardShadow else 0.dp, c.shadow)
            .heightIn(min = if (compact) 38.dp else 48.dp)
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 8.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(if (compact) 17.dp else 19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = YaadType.button, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A square outlined icon button. */
@Composable
fun BrutalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    Box(
        modifier
            .pressable(onClick = onClick)
            .size(size)
            .outlined(Yaad.colors.card, Yaad.colors.outline, 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Yaad.colors.text, modifier = Modifier.size(21.dp))
    }
}

/**
 * A tag chip in the tag's muted colour.
 * Filter chips ([onClick] set) turn black when [selected]; the colour change is animated.
 * [small] = the little tag on cards.
 */
@Composable
fun TagChip(
    label: String,
    color: Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    small: Boolean = false,
    dashed: Boolean = false,
    trailing: String? = null,
) {
    val c = Yaad.colors
    val reduce = LocalReduceMotion.current
    val targetFill = when {
        selected -> c.primary
        dashed -> Color.Transparent
        else -> color
    }
    val targetText = when {
        selected -> c.onPrimary
        dashed -> c.subtext
        color == c.card -> c.text
        else -> c.onAccent
    }
    val fill by animateColorAsState(targetFill, tween(if (reduce) 0 else 220), label = "chipFill")
    val text by animateColorAsState(targetText, tween(if (reduce) 0 else 220), label = "chipText")
    val radius = if (small) 7.dp else 10.dp
    val dashColor = c.subtext
    Box(
        Modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .then(
                if (dashed) Modifier.drawBehind {
                    val w = 1.dp.toPx()
                    val r = radius.toPx()
                    drawRoundRect(
                        dashColor, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                } else Modifier.outlined(fill, c.outline, radius, if (small) 1.dp else Hairline),
            )
            .then(if (small) Modifier else Modifier.height(if (onClick != null) 34.dp else 30.dp))
            .padding(horizontal = if (small) 7.dp else if (onClick != null) 13.dp else 11.dp, vertical = if (small) 2.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = if (small) YaadType.tagSmall else YaadType.tag, color = text, maxLines = 1)
            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                Text(trailing, style = YaadType.tag, color = text.copy(alpha = 0.55f), maxLines = 1)
            }
        }
    }
}

/** A thin outlined progress bar with an ink fill. */
@Composable
fun BrutalProgress(fraction: Float, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    Box(
        modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(c.muted)
            .border(1.dp, c.outline, RoundedCornerShape(5.dp)),
    ) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(c.text))
    }
}
