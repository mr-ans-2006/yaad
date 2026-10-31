package io.github.mrans2006.yaad.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

/*
 * The neo-brutalist building blocks. The whole look comes from three things:
 *   1. a thick outline        (border)
 *   2. a hard shadow          (a solid copy of the shape, pushed down-right, no blur)
 *   3. flat, bright fill      (no gradients)
 */

val BrutalBorder = 2.5.dp
val BrutalRadius = 12.dp

/**
 * Draws the hard shadow + fill + outline behind any content.
 * The shadow is drawn OUTSIDE the box, so leave [shadow] of space to the right and bottom.
 */
fun Modifier.brutal(
    fill: Color,
    outline: Color,
    shadowColor: Color,
    shadow: Dp = 4.dp,
    radius: Dp = BrutalRadius,
    border: Dp = BrutalBorder,
): Modifier = this
    .drawBehind {
        val r = radius.toPx()
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(shadow.toPx(), shadow.toPx()),
            size = size,
            cornerRadius = CornerRadius(r, r),
        )
    }
    .clip(RoundedCornerShape(radius))
    .background(fill)
    .border(border, outline, RoundedCornerShape(radius))

/** A box with the brutal look. The most used piece in the app. */
@Composable
fun BrutalBox(
    modifier: Modifier = Modifier,
    fill: Color = Yaad.colors.card,
    shadow: Dp = 4.dp,
    radius: Dp = BrutalRadius,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.brutal(fill, Yaad.colors.outline, Yaad.colors.shadow, shadow, radius),
        content = content,
    )
}

/** A chunky button that "presses down" into its shadow when you tap it. */
@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Yaad.colors.card,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val push by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "press")
    Box(
        modifier
            .offset(push, push)
            .brutal(if (enabled) fill else fill.copy(alpha = 0.5f), Yaad.colors.outline, Yaad.colors.shadow, 4.dp - push, 10.dp)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (fill == Yaad.colors.card) Yaad.colors.text else Yaad.colors.onAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = YaadType.button, color = if (fill == Yaad.colors.card) Yaad.colors.text else Yaad.colors.onAccent, maxLines = 1)
        }
    }
}

/** A square icon button, same chunky look. */
@Composable
fun BrutalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Yaad.colors.card,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val push by animateDpAsState(if (pressed) 3.dp else 0.dp, label = "press")
    Box(
        modifier
            .offset(push, push)
            .size(46.dp)
            .brutal(fill, Yaad.colors.outline, Yaad.colors.shadow, 3.dp - push, 10.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (fill == Yaad.colors.card) Yaad.colors.text else Yaad.colors.onAccent)
    }
}

/** A tag chip. Filled with the tag colour when selected. */
@Composable
fun TagChip(label: String, color: Color, selected: Boolean = true, onClick: (() -> Unit)? = null, small: Boolean = false) {
    val fill = if (selected) color else Yaad.colors.card
    Box(
        Modifier
            .brutal(fill, Yaad.colors.outline, Yaad.colors.shadow, if (selected && !small) 3.dp else 0.dp, 8.dp, 2.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (small) 8.dp else 12.dp, vertical = if (small) 3.dp else 7.dp),
    ) {
        Text(label, style = if (small) YaadType.small else YaadType.label, color = if (selected) Yaad.colors.onAccent else Yaad.colors.text)
    }
}

/**
 * A folder-style card: a coloured tab sticks up from the top-left corner (like a file folder),
 * with the main tag written on it.
 */
@Composable
fun FolderCard(
    tabLabel: String,
    tabColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Box(
            Modifier
                .padding(start = 10.dp)
                .offset(y = BrutalBorder)
                .brutalTab(tabColor)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(tabLabel, style = YaadType.label, color = Yaad.colors.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BrutalBox(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            fill = Yaad.colors.card,
        ) { content() }
    }
}

@Composable
private fun Modifier.brutalTab(color: Color): Modifier {
    val outline = Yaad.colors.outline
    val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    return this
        .clip(shape)
        .background(color)
        .border(BrutalBorder, outline, shape)
}

/** A thick progress bar made of a bordered track and a flat fill. */
@Composable
fun BrutalProgress(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(18.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Yaad.colors.card)
            .border(2.dp, Yaad.colors.outline, RoundedCornerShape(6.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(18.dp)
                .background(color)
                .border(2.dp, Yaad.colors.outline, RoundedCornerShape(6.dp)),
        )
    }
}
