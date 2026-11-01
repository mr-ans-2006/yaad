package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.HomeState
import io.github.mrans2006.yaad.ui.components.TagChip
import io.github.mrans2006.yaad.ui.components.LocalBottomBarPadding
import io.github.mrans2006.yaad.ui.components.outlined
import io.github.mrans2006.yaad.ui.components.pressable
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType
import io.github.mrans2006.yaad.ui.theme.tagColor

/** Everything the Search tab can ask the app to do. */
data class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onTagSelected: (String?) -> Unit = {},
    val onOpen: (Shot) -> Unit = {},
    val onSearchDone: () -> Unit = {},
    val onAdd: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onAskPermission: () -> Unit = {},
    val onBatteryGuide: () -> Unit = {},
    val onDismissBatteryTip: () -> Unit = {},
    val onClearRecent: () -> Unit = {},
)

/**
 * The Search tab. Before you type: just the logo, a big search box, a hint and your recent searches
 * (no screenshot wall). As you type: results appear (hybrid search), filterable by tag.
 */
@Composable
fun SearchScreen(
    state: HomeState,
    actions: SearchActions,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
    val focus = LocalFocusManager.current
    val full = StaggeredGridItemSpan.FullLine
    val searching = state.query.isNotBlank()
    Box(Modifier.fillMaxSize().background(Yaad.colors.background)) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp + LocalBottomBarPadding.current),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalItemSpacing = 14.dp,
        ) {
            item(span = full, key = "top") { TopBar(actions.onAdd, actions.onSettings) }
            if (!searching) item(span = full, key = "hero") { Hero(Modifier.animateItem()) }
            item(span = full, key = "search") {
                SearchBox(state.query, actions.onQueryChange, { focus.clearFocus(); actions.onSearchDone() },
                    Modifier.padding(top = if (searching) 6.dp else 0.dp))
            }

            if (!searching) {
                item(span = full, key = "hint") { Hint(state, actions, Modifier.animateItem()) }
                // The quiet status corner: permission, import progress, battery tip, count.
                if (!state.hasPermission) item(span = full, key = "perm") { Box(Modifier.animateItem().padding(top = 10.dp)) { PermissionCard(actions.onAskPermission) } }
                if (state.pending > 0) item(span = full, key = "progress") { ImportStatus(state, Modifier.animateItem().padding(top = 10.dp)) }
                if (state.showBatteryTip && state.hasPermission) {
                    item(span = full, key = "battery") {
                        BatteryTip(state.phoneBrand, actions.onBatteryGuide, actions.onDismissBatteryTip, Modifier.animateItem().padding(top = if (state.pending > 0) 0.dp else 10.dp))
                    }
                }
                if (state.loaded) item(span = full, key = "count") {
                    val n = state.shots.size
                    SectionLabel(if (n == 0) "No screenshots yet" else "$n screenshot${if (n == 1) "" else "s"} remembered",
                        Modifier.animateItem().padding(top = 6.dp))
                }
            } else {
                val tags = (state.results ?: emptyList()).flatMap { it.tagList }.distinct()
                if (tags.isNotEmpty()) item(span = full, key = "tags") { TagRow(tags, state, actions.onTagSelected, Modifier.animateItem()) }
                item(span = full, key = "count") { ResultCount(state, Modifier.animateItem()) }
                if (state.results != null && state.visible.isEmpty()) {
                    item(span = full, key = "empty") { NoResults(Modifier.animateItem()) }
                }
                items(state.visible, key = { it.id }) { shot ->
                    ShotCard(shot, { focus.clearFocus(); actions.onOpen(shot) }, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    val c = Yaad.colors
    Column(modifier.padding(top = 14.dp, bottom = 6.dp)) {
        Text("Save now.", style = YaadType.hero, color = c.text)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Find ", style = YaadType.hero, color = c.text)
            Box(Modifier.outlined(c.primary, c.primary, 8.dp, 0.dp).padding(start = 8.dp, end = 8.dp, bottom = 2.dp)) {
                Text("anytime.", style = YaadType.hero, color = c.onPrimary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Search by what you remember, not by date.", style = YaadType.bodyBold.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = c.subtext)
    }
}

/** Under the search box: recent searches if you have any, otherwise a short hint. */
@Composable
private fun Hint(state: HomeState, actions: SearchActions, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    Column(modifier.fillMaxWidth().padding(top = 4.dp)) {
        if (state.recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Recent", Modifier.weight(1f))
                Text("Clear", style = YaadType.small, color = c.subtext, modifier = Modifier.pressable(onClick = actions.onClearRecent).padding(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            state.recent.forEach { q ->
                Row(
                    Modifier.fillMaxWidth().pressable { actions.onQueryChange(q) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, null, tint = c.subtext, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(q, style = YaadType.bodyBold, color = c.text, maxLines = 1)
                }
            }
        } else {
            Text(
                buildAnnotatedString {
                    append("Search by meaning, like ")
                    withStyle(SpanStyle(color = c.text)) { append("\u201Ca tool for data recovery\u201D") }
                    append(", or by an exact word like ")
                    withStyle(SpanStyle(color = c.text)) { append("\u201Cllama.cpp\u201D") }
                    append(".")
                },
                style = YaadType.body, color = c.subtext,
            )
        }
    }
}

@Composable
private fun TagRow(tags: List<String>, state: HomeState, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    val all = state.results ?: emptyList()
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
        item(key = "all") { TagChip("All", Yaad.colors.card, selected = state.selectedTag == null, onClick = { onSelect(null) }, trailing = "${all.size}") }
        items(tags, key = { it }) { tag ->
            TagChip(tag, tagColor(tag), selected = state.selectedTag == tag, onClick = { onSelect(tag) },
                trailing = "${all.count { tag in it.tagList }}")
        }
    }
}

@Composable
private fun ResultCount(state: HomeState, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    val text = when {
        state.results == null -> "Searching…"
        else -> "${state.visible.size} result${if (state.visible.size == 1) "" else "s"}"
    }
    SectionLabel(text, modifier.padding(top = 2.dp))
}

@Composable
private fun NoResults(modifier: Modifier = Modifier) {
    val c = Yaad.colors
    Column(modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing found", style = YaadType.title, color = c.text)
        Spacer(Modifier.height(6.dp))
        Text("Try other words, or what the screenshot was about.", style = YaadType.body, color = c.subtext)
    }
}
