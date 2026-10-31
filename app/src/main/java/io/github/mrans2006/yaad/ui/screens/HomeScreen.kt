package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.core.Tag
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.HomeState
import io.github.mrans2006.yaad.ui.components.BrutalBorder
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.components.BrutalProgress
import io.github.mrans2006.yaad.ui.components.FolderCard
import io.github.mrans2006.yaad.ui.components.TagChip
import io.github.mrans2006.yaad.ui.components.Thumbnail
import io.github.mrans2006.yaad.ui.friendlyDate
import io.github.mrans2006.yaad.ui.theme.Palette
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType
import io.github.mrans2006.yaad.ui.theme.tagColor

/** Everything the home screen can ask the app to do. */
data class HomeActions(
    val onQueryChange: (String) -> Unit = {},
    val onTagSelected: (String?) -> Unit = {},
    val onOpen: (Shot) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onAskPermission: () -> Unit = {},
    val onBatteryGuide: () -> Unit = {},
    val onDismissBatteryTip: () -> Unit = {},
)

/**
 * The main screen: logo, the big "Save now. Find anytime." heading, the search box,
 * tag filters, and a two-column wall of folder cards.
 */
@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    val focus = LocalFocusManager.current
    val full = StaggeredGridItemSpan.FullLine
    Box(Modifier.fillMaxSize().background(Yaad.colors.background)) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalItemSpacing = 16.dp,
        ) {
            item(span = full, key = "top") { TopBar(actions.onSettings) }
            if (state.query.isEmpty()) item(span = full, key = "hero") { Hero() }
            item(span = full, key = "search") { SearchBox(state.query, actions.onQueryChange) { focus.clearFocus() } }
            item(span = full, key = "tags") { TagRow(state, actions.onTagSelected) }

            if (!state.hasPermission) item(span = full, key = "perm") { PermissionCard(actions.onAskPermission) }
            if (state.pending > 0) item(span = full, key = "progress") { ProgressCard(state) }
            if (state.showBatteryTip && state.hasPermission && state.query.isEmpty()) {
                item(span = full, key = "battery") { BatteryTip(actions.onBatteryGuide, actions.onDismissBatteryTip) }
            }

            item(span = full, key = "count") { CountLine(state) }

            if (state.loaded && state.visible.isEmpty()) {
                item(span = full, key = "empty") { EmptyState(state) }
            }
            items(state.visible, key = { it.id }) { shot -> ShotCard(shot) { actions.onOpen(shot) } }
        }

        BrutalButton(
            text = "Add",
            icon = Icons.Rounded.Add,
            onClick = actions.onAdd,
            fill = Palette.Yellow,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 22.dp),
        )
    }
}

@Composable
private fun TopBar(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BrutalBox(fill = Palette.Yellow, shadow = 3.dp, radius = 10.dp) {
            Text("YAAD", style = YaadType.logo, color = Palette.Ink, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("یاد", style = YaadType.title, color = Yaad.colors.subtext)
        Spacer(Modifier.weight(1f))
        BrutalIconButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

@Composable
private fun Hero() {
    Column(Modifier.padding(top = 4.dp)) {
        Text("Save now.", style = YaadType.hero, color = Yaad.colors.text)
        Spacer(Modifier.height(6.dp))
        BrutalBox(fill = Palette.Pink, shadow = 4.dp, radius = 8.dp) {
            Text("Find anytime.", style = YaadType.hero, color = Palette.Ink, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Never lose a screenshot again.", style = YaadType.body, color = Yaad.colors.subtext)
    }
}

@Composable
private fun SearchBox(query: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), shadow = 4.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = Yaad.colors.text)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search by meaning…", style = YaadType.body,
                        color = Yaad.colors.subtext, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = YaadType.bodyBold.copy(color = Yaad.colors.text),
                    cursorBrush = SolidColor(Yaad.colors.text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onDone() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(Icons.Rounded.Close, "Clear", tint = Yaad.colors.text, modifier = Modifier.size(22.dp).clickable { onChange("") })
            }
        }
    }
}

@Composable
private fun TagRow(state: HomeState, onSelect: (String?) -> Unit) {
    // Only show tags that at least one screenshot has, with a count.
    val counts = Tag.entries.map { it.label to state.shots.count { s -> it.label in s.tagList } }.filter { it.second > 0 }
    if (counts.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 8.dp, bottom = 4.dp)) {
        item { TagChip("All", Palette.Yellow, selected = state.selectedTag == null, onClick = { onSelect(null) }) }
        items(counts) { (label, n) ->
            TagChip("$label  $n", tagColor(label), selected = state.selectedTag == label, onClick = { onSelect(label) })
        }
    }
}

@Composable
private fun ProgressCard(state: HomeState) {
    val total = if (state.progressTotal > 0) state.progressTotal else state.pending
    val done = state.progressDone.coerceAtMost(total)
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = Palette.Purple) {
        Column(Modifier.padding(14.dp)) {
            Text("Reading your screenshots…", style = YaadType.title, color = Palette.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.progressTotal > 0) "$done of $total done. You can keep using Yaad." else "${state.pending} waiting. Starting…",
                style = YaadType.body, color = Palette.Ink,
            )
            Spacer(Modifier.height(10.dp))
            BrutalProgress(if (total > 0) done.toFloat() / total else 0f, Palette.Yellow, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionCard(onAsk: () -> Unit) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = Palette.Pink) {
        Column(Modifier.padding(14.dp)) {
            Text("Yaad can't see your screenshots yet", style = YaadType.title, color = Palette.Ink)
            Spacer(Modifier.height(4.dp))
            Text("Allow photo access so new screenshots are saved automatically. Shared images still work without it.",
                style = YaadType.body, color = Palette.Ink)
            Spacer(Modifier.height(12.dp))
            BrutalButton("Allow access", onAsk)
        }
    }
}

@Composable
private fun BatteryTip(onOpen: () -> Unit, onDismiss: () -> Unit) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = Palette.Green) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BatteryChargingFull, null, tint = Palette.Ink)
                Spacer(Modifier.width(8.dp))
                Text("Keep auto-pick awake", style = YaadType.title, color = Palette.Ink, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, "Dismiss", tint = Palette.Ink, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(4.dp))
            Text("Oppo phones put apps to sleep. One setting lets Yaad catch screenshots the moment you take them.",
                style = YaadType.body, color = Palette.Ink)
            Spacer(Modifier.height(12.dp))
            BrutalButton("Show me how", onOpen)
        }
    }
}

@Composable
private fun CountLine(state: HomeState) {
    val text = when {
        state.results != null && state.searching -> "Searching…"
        state.results != null -> "${state.visible.size} ${if (state.visible.size == 1) "match" else "matches"} for \u201C${state.query}\u201D"
        state.selectedTag != null -> "${state.visible.size} in ${state.selectedTag}"
        else -> "${state.shots.size} ${if (state.shots.size == 1) "screenshot" else "screenshots"} remembered"
    }
    Text(text.uppercase(), style = YaadType.label, color = Yaad.colors.subtext)
}

@Composable
private fun EmptyState(state: HomeState) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (state.results != null) "NO MATCH" else "EMPTY", style = YaadType.heading, color = Yaad.colors.text)
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.results != null) "Try other words. Yaad searches by meaning, so describe what was in the screenshot."
                else "Take a screenshot, or share any image to Yaad. It will show up here, already read and tagged.",
                style = YaadType.body, color = Yaad.colors.subtext,
            )
        }
    }
}

/** One screenshot in the grid: a folder tab with its main tag, the thumbnail, and the key line. */
@Composable
fun ShotCard(shot: Shot, onClick: () -> Unit) {
    val tags = shot.tagList
    val (tabLabel, tabColor) = when (shot.status) {
        Shot.STATUS_PENDING -> "Reading…" to Palette.Bone
        Shot.STATUS_FAILED -> "Oops" to Palette.Red
        else -> (tags.firstOrNull() ?: "Misc") to tagColor(tags.firstOrNull() ?: "")
    }
    FolderCard(tabLabel, tabColor, Modifier.padding(end = 4.dp, bottom = 4.dp), onClick = onClick) {
        Column {
            Thumbnail(shot.uri, Modifier.fillMaxWidth().aspectRatio(0.8f))
            Box(Modifier.fillMaxWidth().height(BrutalBorder).background(Yaad.colors.outline))
            Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Text(
                    when (shot.status) {
                        Shot.STATUS_PENDING -> "Reading the text…"
                        else -> shot.keyLine
                    },
                    style = YaadType.bodyBold, color = Yaad.colors.text, maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    tags.drop(1).take(1).forEach {
                        TagChip(it, tagColor(it), small = true)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(friendlyDate(shot.takenAt), style = YaadType.small, color = Yaad.colors.subtext, maxLines = 1)
                }
            }
        }
    }
}
