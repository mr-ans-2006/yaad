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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.core.LinkFinder
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.components.HardShadow
import io.github.mrans2006.yaad.ui.components.TagChip
import io.github.mrans2006.yaad.ui.components.Thumbnail
import io.github.mrans2006.yaad.ui.components.gridKey
import io.github.mrans2006.yaad.ui.components.outlined
import io.github.mrans2006.yaad.ui.components.pressable
import io.github.mrans2006.yaad.ui.friendlyDate
import io.github.mrans2006.yaad.ui.sharedShot
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
fun DetailScreen(shot: Shot, actions: DetailActions, origin: String = "list") {
    var confirmDelete by remember { mutableStateOf(false) }
    val c = Yaad.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            BrutalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions.onBack)
            Spacer(Modifier.width(14.dp))
            Text("Memory", style = YaadType.heading, color = c.text, modifier = Modifier.weight(1f))
            BrutalIconButton(Icons.Rounded.DeleteOutline, "Delete from Yaad", { confirmDelete = true })
        }
        Spacer(Modifier.height(8.dp))

        ZoomableScreenshot(shot, origin)
        Spacer(Modifier.height(9.dp))
        Text("Pinch or double-tap to zoom", style = YaadType.small, color = c.subtext)
        Spacer(Modifier.height(14.dp))

        // The key line: the one primary card on this screen, so it gets the hard shadow.
        BrutalBox(Modifier.fillMaxWidth().padding(end = HardShadow, bottom = HardShadow), shadow = HardShadow) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
                SectionLabel("Key line")
                Spacer(Modifier.height(4.dp))
                Text(
                    when (shot.status) {
                        Shot.STATUS_PENDING -> "Still reading this one…"
                        Shot.STATUS_FAILED -> "Couldn't read this one"
                        else -> shot.keyLine.ifBlank { "No text found" }
                    },
                    style = YaadType.title, color = c.text,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shot.tagList.forEach { TagChip(it, tagColor(it)) }
            TagChip(if (shot.source == Shot.SOURCE_AUTO) "Auto-picked" else "Shared", c.card, dashed = true)
            TagChip(friendlyDate(shot.takenAt), c.card, dashed = true)
        }

        if (shot.status == Shot.STATUS_FAILED) {
            Spacer(Modifier.height(16.dp))
            BrutalButton("Try reading again", actions.onRetry, primary = true, modifier = Modifier.padding(end = HardShadow, bottom = HardShadow))
        }

        if (shot.linkList.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Links")
            Spacer(Modifier.height(8.dp))
            shot.linkList.forEach { url ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).pressable { actions.onOpenLink(url) }
                        .outlined(c.card, c.outline, 12.dp).height(50.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Link, null, tint = c.text, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        url.removePrefix("https://").removePrefix("http://"),
                        style = YaadType.bodyBold.copy(textDecoration = TextDecoration.Underline),
                        color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open link", tint = c.subtext, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (shot.text.isNotBlank()) {
            Spacer(Modifier.height(if (shot.linkList.isEmpty()) 20.dp else 12.dp))
            SectionLabel("All text")
            Spacer(Modifier.height(8.dp))
            BrutalBox(Modifier.fillMaxWidth(), radius = 12.dp) {
                SelectionContainer {
                    Text(linkified(shot.text), style = YaadType.body, color = c.text, modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        BrutalButton("Open in gallery", { actions.onOpenInGallery(shot) }, icon = Icons.Rounded.PhotoLibrary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.card,
            title = { Text("Remove from Yaad?", style = YaadType.title, color = c.text) },
            text = { Text("It stays in your gallery. Yaad just forgets it.", style = YaadType.body, color = c.subtext) },
            confirmButton = { TextButton({ confirmDelete = false; actions.onDelete(shot) }) { Text("Remove", style = YaadType.button, color = c.text) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep", style = YaadType.button, color = c.subtext) } },
        )
    }
}

/** Turns every link inside the text into a tappable, underlined link. */
@Composable
private fun linkified(text: String): AnnotatedString {
    val style = TextLinkStyles(SpanStyle(color = Yaad.colors.text, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline))
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

/**
 * The screenshot in a quiet frame, with pinch-to-zoom and double-tap-to-zoom.
 * The image carries the shared-element mark: it is where the grid thumbnail grows to.
 */
@Composable
private fun ZoomableScreenshot(shot: Shot, origin: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val c = Yaad.colors
    Box(
        Modifier.fillMaxWidth().height(440.dp).outlined(c.muted, c.outline, 14.dp).clipToBounds()
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
        // Size the image to the screenshot's own shape (when known), so the grow animation lands exactly on it.
        // Crop + top alignment, like the grid thumbnail: at the start of the animation the image looks exactly like
        // the thumbnail, and as it grows to the screenshot's own shape the rest of it is revealed.
        val known = shot.width > 0 && shot.height > 0
        val ratio = if (known) shot.width / shot.height.toFloat() else 0.45f
        Thumbnail(
            shot.uri,
            Modifier.heightIn(max = 440.dp).aspectRatio(ratio, matchHeightConstraintsFirst = true)
                .sharedShot(shot.id, origin)
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
            contentScale = if (known) ContentScale.Crop else ContentScale.Fit,
            alignment = if (known) Alignment.TopCenter else Alignment.Center,
            placeholderKey = gridKey(shot.uri),
        )
    }
}
