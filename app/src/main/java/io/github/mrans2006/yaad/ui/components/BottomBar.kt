package io.github.mrans2006.yaad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.ui.theme.LocalReduceMotion
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

/** How much room the bottom bar takes, so lists can scroll their last row above it. 0 when there is no bar. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/**
 * Puts the bottom bar over [content] (aligned to the bottom) and tells the screens underneath how tall it is.
 * The bar slides away on screens that don't need it (detail, settings, battery guide).
 */
@Composable
fun BottomBarHost(visible: Boolean, selected: YaadTab, onSelect: (YaadTab) -> Unit, content: @Composable () -> Unit) {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val reduce = LocalReduceMotion.current
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarPadding provides (64.dp + Hairline + nav)) { content() }
        AnimatedVisibility(
            visible,
            Modifier.align(Alignment.BottomCenter),
            enter = if (reduce) fadeIn(tween(0)) else slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
            exit = if (reduce) fadeOut(tween(0)) else slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            BottomBar(selected, onSelect)
        }
    }
}

enum class YaadTab(val label: String) { SEARCH("Search"), FOLDERS("Folders") }

/** Cream bar, thin top outline, the selected tab as a black pill. */
@Composable
fun BottomBar(selected: YaadTab, onSelect: (YaadTab) -> Unit, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    Column(modifier.fillMaxWidth().background(c.background)) {
        Box(Modifier.fillMaxWidth().height(Hairline).background(c.outline))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(YaadTab.SEARCH, Icons.Rounded.Search, Icons.Rounded.Search, selected == YaadTab.SEARCH, onSelect, Modifier.weight(1f))
            TabItem(YaadTab.FOLDERS, Icons.Rounded.Folder, Icons.Outlined.Folder, selected == YaadTab.FOLDERS, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(tab: YaadTab, on: ImageVector, off: ImageVector, isSelected: Boolean, onSelect: (YaadTab) -> Unit, modifier: Modifier) {
    val c = Yaad.colors
    val ms = if (LocalReduceMotion.current) 0 else 220
    val fill by animateColorAsState(if (isSelected) c.primary else Color.Transparent, tween(ms), label = "tabFill")
    val content by animateColorAsState(if (isSelected) c.onPrimary else c.subtext, tween(ms), label = "tabText")
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.height(44.dp).clip(shape).background(fill)
            .semantics { role = Role.Tab; this.selected = isSelected }
            .pressable { onSelect(tab) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (isSelected) on else off, null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(tab.label, style = YaadType.nav, color = content)
    }
}
