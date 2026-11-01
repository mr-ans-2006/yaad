package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.Folder
import io.github.mrans2006.yaad.ui.Folders
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.components.Hairline
import io.github.mrans2006.yaad.ui.components.Thumbnail
import io.github.mrans2006.yaad.ui.components.LocalBottomBarPadding
import io.github.mrans2006.yaad.ui.components.outlined
import io.github.mrans2006.yaad.ui.components.pressable
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType
import io.github.mrans2006.yaad.ui.theme.tagColor

data class FoldersActions(
    val onOpenFolder: (Folder) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

/** The Folders tab: one card per auto-tag, plus All / Untagged / No text. */
@Composable
fun FoldersScreen(
    shots: List<Shot>,
    actions: FoldersActions,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val folders = androidx.compose.runtime.remember(shots) { Folders.build(shots) }
    val full = GridItemSpan(2)
    Box(Modifier.fillMaxSize().background(Yaad.colors.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp + LocalBottomBarPadding.current),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { full }, key = "top") { TopBar(actions.onAdd, actions.onSettings) }
            item(span = { full }, key = "title") {
                Column(Modifier.padding(top = 10.dp, bottom = 2.dp)) {
                    Text("Folders", style = YaadType.hero, color = Yaad.colors.text)
                    Spacer(Modifier.height(2.dp))
                    Text("Sorted for you by auto-tags. A screenshot with two tags is in both folders.",
                        style = YaadType.body, color = Yaad.colors.subtext)
                }
            }
            if (shots.isEmpty()) {
                item(span = { full }, key = "empty") {
                    Text("No screenshots yet. Take one, or tap + to add some from your gallery.",
                        style = YaadType.body, color = Yaad.colors.subtext, modifier = Modifier.padding(vertical = 24.dp))
                }
            } else {
                items(folders, key = { it.key }) { folder ->
                    FolderCard(folder, { actions.onOpenFolder(folder) }, Modifier.animateItem())
                }
            }
        }
    }
}

/** A folder card: a small fanned stack of up to 3 thumbnails on the tag's colour, then the name and count. */
@Composable
fun FolderCard(folder: Folder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    val tint = when {
        folder.tag != null -> tagColor(folder.tag)
        folder.key.startsWith("tag:") -> tagColor(folder.name)
        else -> c.muted
    }
    // Light mode: the preview sits on the tag's pastel. Dark mode: a quiet dark area, and the colour moves to a dot by the name.
    val area = if (c.isDark) c.muted else tint
    Column(modifier.fillMaxWidth().pressable(onClick = onClick).outlined(c.card, c.outline, 14.dp)) {
        Box(Modifier.fillMaxWidth().height(132.dp).background(area), contentAlignment = Alignment.Center) {
            val previews = folder.shots.take(3)
            val angles = listOf(0f, -7f, 7f)
            val offsets = listOf(0, -30, 30)
            // Draw back-to-front: the newest screenshot sits on top, in the middle.
            previews.indices.reversed().forEach { i ->
                val shape = RoundedCornerShape(7.dp)
                Box(
                    Modifier.offset(x = offsets[i].dp, y = if (i == 0) 2.dp else 8.dp).rotate(angles[i]).size(width = 62.dp, height = 96.dp)
                        .clip(shape).background(c.card).border(1.dp, c.outline, shape),
                ) {
                    Thumbnail(previews[i].uri, Modifier.fillMaxSize())
                }
            }
            if (previews.isEmpty()) Icon(Icons.Rounded.Folder, null, tint = c.subtext)
        }
        Box(Modifier.fillMaxWidth().height(Hairline).background(c.outline))
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (c.isDark && tint != c.muted) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(tint))
                Spacer(Modifier.width(8.dp))
            }
            Text(folder.name, style = YaadType.cardTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${folder.count}", style = YaadType.key, color = c.subtext)
        }
    }
}

data class FolderActions(
    val onBack: () -> Unit = {},
    val onOpen: (Shot) -> Unit = {},
)

/** Inside a folder: its screenshots in a neat 2-column grid, newest first, under date headers. */
@Composable
fun FolderScreen(
    folder: Folder?,
    actions: FolderActions,
    gridState: LazyGridState = rememberLazyGridState(),
    now: Long = System.currentTimeMillis(),
) {
    val c = Yaad.colors
    val groups = androidx.compose.runtime.remember(folder?.shots, now) { Folders.byDate(folder?.shots.orEmpty(), now) }
    val full = GridItemSpan(2)
    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + LocalBottomBarPadding.current),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { full }, key = "head") {
                Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrutalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions.onBack)
                    Spacer(Modifier.width(14.dp))
                    Text(folder?.name ?: "Folder", style = YaadType.heading, color = c.text, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val n = folder?.count ?: 0
                    Text("$n screenshot${if (n == 1) "" else "s"}", style = YaadType.small, color = c.subtext)
                }
            }
            if (folder == null || folder.shots.isEmpty()) {
                item(span = { full }, key = "empty") {
                    Text("This folder is empty now.", style = YaadType.body, color = c.subtext, modifier = Modifier.padding(vertical = 24.dp))
                }
            }
            groups.forEach { (header, shots) ->
                item(span = { full }, key = "h:$header", contentType = "header") {
                    SectionLabel(header, Modifier.animateItem().padding(top = 6.dp))
                }
                items(shots, key = { it.id }, contentType = { "shot" }) { shot ->
                    ShotCard(shot, { actions.onOpen(shot) }, Modifier.animateItem(), uniform = true)
                }
            }
        }
    }
}
