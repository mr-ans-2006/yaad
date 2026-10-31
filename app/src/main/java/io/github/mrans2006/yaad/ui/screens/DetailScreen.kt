package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.core.LinkFinder
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.components.TagChip
import io.github.mrans2006.yaad.ui.components.Thumbnail
import io.github.mrans2006.yaad.ui.friendlyDate
import io.github.mrans2006.yaad.ui.theme.Palette
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType
import io.github.mrans2006.yaad.ui.theme.tagColor

data class DetailActions(
    val onBack: () -> Unit = {},
    val onOpenLink: (String) -> Unit = {},
    val onOpenInGallery: (Shot) -> Unit = {},
    val onDelete: (Shot) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** One screenshot up close: zoomable image, the key line, tags, tappable links and all the text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(shot: Shot, actions: DetailActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Yaad.colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions.onBack)
            Spacer(Modifier.width(14.dp))
            Text("MEMORY", style = YaadType.heading, color = Yaad.colors.text, modifier = Modifier.weight(1f))
            BrutalIconButton(Icons.Rounded.DeleteOutline, "Delete from Yaad", { confirmDelete = true }, fill = Palette.Pink)
        }
        Spacer(Modifier.height(18.dp))

        ZoomableScreenshot(shot.uri)
        Spacer(Modifier.height(6.dp))
        Text("Pinch or double-tap to zoom", style = YaadType.small, color = Yaad.colors.subtext)
        Spacer(Modifier.height(16.dp))

        // The key line, big and yellow: this is the v1 "summary".
        BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = Palette.Yellow) {
            Column(Modifier.padding(14.dp)) {
                Text("KEY LINE", style = YaadType.label, color = Palette.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    when (shot.status) {
                        Shot.STATUS_PENDING -> "Still reading this one…"
                        else -> shot.keyLine
                    },
                    style = YaadType.title, color = Palette.Ink,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shot.tagList.forEach { TagChip(it, tagColor(it)) }
            TagChip(if (shot.source == Shot.SOURCE_AUTO) "Auto-picked" else "Shared", Yaad.colors.card, selected = false)
            TagChip(friendlyDate(shot.takenAt), Yaad.colors.card, selected = false)
        }

        if (shot.status == Shot.STATUS_FAILED) {
            Spacer(Modifier.height(14.dp))
            BrutalButton("Try reading again", actions.onRetry, fill = Palette.Purple)
        }

        if (shot.linkList.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("LINKS", style = YaadType.label, color = Yaad.colors.subtext)
            Spacer(Modifier.height(8.dp))
            shot.linkList.forEach { url ->
                BrutalButton(
                    text = url.removePrefix("https://").removePrefix("http://"),
                    onClick = { actions.onOpenLink(url) },
                    icon = Icons.Rounded.Link,
                    fill = Palette.Blue,
                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 10.dp),
                )
            }
        }

        if (shot.text.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text("ALL TEXT", style = YaadType.label, color = Yaad.colors.subtext)
            Spacer(Modifier.height(8.dp))
            BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
                SelectionContainer {
                    Text(linkified(shot.text), style = YaadType.body, color = Yaad.colors.text, modifier = Modifier.padding(14.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        BrutalButton(
            "Open in gallery", { actions.onOpenInGallery(shot) }, icon = Icons.Rounded.PhotoLibrary,
            fill = Palette.Green, modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove from Yaad?", style = YaadType.title) },
            text = { Text("It stays in your gallery. Yaad just forgets it.", style = YaadType.body) },
            confirmButton = { TextButton({ confirmDelete = false; actions.onDelete(shot) }) { Text("Remove", style = YaadType.bodyBold) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep", style = YaadType.bodyBold) } },
        )
    }
}

/** Turns every link inside the text into a tappable, underlined link. */
@Composable
private fun linkified(text: String): AnnotatedString {
    val style = TextLinkStyles(SpanStyle(color = if (Yaad.colors.isDark) Palette.Blue else Palette.Ink,
        textDecoration = TextDecoration.Underline, background = Palette.Blue.copy(alpha = if (Yaad.colors.isDark) 0f else 0.35f)))
    return buildAnnotatedString {
        var last = 0
        for (link in LinkFinder.find(text)) {
            append(text.substring(last, link.start))
            withLink(LinkAnnotation.Url(link.url, style)) { append(link.text) }
            last = link.end
        }
        append(text.substring(last))
    }
}

/** The screenshot, inside a brutal frame, with pinch-to-zoom and double-tap-to-zoom. */
@Composable
private fun ZoomableScreenshot(uri: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    BrutalBox(Modifier.fillMaxWidth().padding(end = 5.dp, bottom = 5.dp), shadow = 5.dp) {
        Box(
            Modifier.fillMaxWidth().heightIn(max = 520.dp).clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Thumbnail(
                uri,
                Modifier.fillMaxWidth().heightIn(max = 520.dp).graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                },
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
            )
        }
    }
}
