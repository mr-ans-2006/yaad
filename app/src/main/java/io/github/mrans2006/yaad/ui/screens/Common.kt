package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.core.PhoneBrand
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.HomeState
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.components.BrutalProgress
import io.github.mrans2006.yaad.ui.components.HardShadow
import io.github.mrans2006.yaad.ui.components.Hairline
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

/** The logo row on both tabs: "Yaad یاد", then Add and Settings. */
@Composable
fun TopBar(onAdd: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Yaad", style = YaadType.logo, color = Yaad.colors.text)
        Spacer(Modifier.width(10.dp))
        Text("یاد", style = YaadType.urdu, color = Yaad.colors.subtext)
        Spacer(Modifier.weight(1f))
        BrutalIconButton(Icons.Rounded.Add, "Add screenshots from gallery", onAdd)
        Spacer(Modifier.width(10.dp))
        BrutalIconButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

/** The big search box. The one element on the Search tab with the hard shadow. */
@Composable
fun SearchBox(query: String, onChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    Row(
        modifier.fillMaxWidth().padding(end = HardShadow, bottom = HardShadow).height(54.dp)
            .outlined(c.card, c.outline, 14.dp, Hairline, HardShadow, c.shadow).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = c.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search what you remember…", style = YaadType.search, color = c.subtext, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = YaadType.search.copy(color = c.text, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                cursorBrush = SolidColor(c.text),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDone() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Close, "Clear search", tint = c.subtext, modifier = Modifier.size(22.dp).pressable { onChange("") })
        }
    }
}

/**
 * One remembered screenshot in a grid: thumbnail, the key line, tags and date.
 * The thumbnail carries the shared-element mark, so it grows into the detail screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShotCard(shot: Shot, onClick: () -> Unit, modifier: Modifier = Modifier, uniform: Boolean = false) {
    val c = Yaad.colors
    Column(modifier.fillMaxWidth().pressable(onClick = onClick).outlined(c.card, c.outline, 14.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (uniform) 0.9f else 0.8f).background(c.muted)) {
            Thumbnail(shot.uri, Modifier.fillMaxSize().sharedShot(shot.id), cacheKey = gridKey(shot.uri))
        }
        Box(Modifier.fillMaxWidth().height(Hairline).background(c.outline))
        Column(Modifier.padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 12.dp)) {
            val line = when (shot.status) {
                Shot.STATUS_PENDING -> "Reading…"
                Shot.STATUS_FAILED -> "Couldn't read this one"
                else -> shot.keyLine.ifBlank { "No text found" }
            }
            Text(line, style = YaadType.key, color = if (shot.status == Shot.STATUS_DONE && shot.keyLine.isNotBlank()) c.text else c.subtext,
                maxLines = if (uniform) 2 else 3, minLines = if (uniform) 2 else 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            if (uniform) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    shot.tagList.firstOrNull()?.let { TagChip(it, tagColor(it), small = true); Spacer(Modifier.width(6.dp)) }
                    Text(friendlyDate(shot.takenAt), style = YaadType.small, color = c.subtext, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                shot.tagList.take(2).forEach { TagChip(it, tagColor(it), small = true) }
                Text(friendlyDate(shot.takenAt), style = YaadType.small, color = c.subtext, maxLines = 1)
            }
        }
    }
}

/** Uppercase section line, e.g. "9 SCREENSHOTS REMEMBERED". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = YaadType.label, color = Yaad.colors.subtext, modifier = modifier)
}

/** Asks for the photos permission, so Yaad can pick up screenshots by itself. */
@Composable
fun PermissionCard(onAsk: () -> Unit) {
    BrutalBox(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text("Let Yaad see your screenshots", style = YaadType.cardTitle, color = Yaad.colors.text)
            Spacer(Modifier.height(6.dp))
            Text("Yaad needs photo access to pick up new screenshots by itself. Everything stays on your phone.",
                style = YaadType.body, color = Yaad.colors.subtext)
            Spacer(Modifier.height(12.dp))
            BrutalButton("Allow access", onAsk, primary = true, compact = true)
        }
    }
}

/** A slim "Reading 121 of 412…" line with a thin bar. Quiet on purpose. */
@Composable
fun ImportStatus(state: HomeState, modifier: Modifier = Modifier) {
    val total = state.progressTotal.takeIf { it > 0 }
    val text = if (total != null) "Reading ${state.progressDone} of $total screenshots…" else "Reading ${state.pending} new screenshot${if (state.pending == 1) "" else "s"}…"
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = YaadType.small, color = Yaad.colors.subtext, modifier = Modifier.weight(1f))
            if (total != null) Text("${(state.progressDone * 100 / total)}%", style = YaadType.small, color = Yaad.colors.text)
        }
        Spacer(Modifier.height(6.dp))
        BrutalProgress(if (total != null) state.progressDone / total.toFloat() else 0.08f, Modifier.fillMaxWidth())
    }
}

/** A one-line battery tip: tap to open the guide, X to hide it. */
@Composable
fun BatteryTip(brand: PhoneBrand, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Yaad.colors
    val phone = if (brand == PhoneBrand.GENERIC) "your phone" else brand.label.substringBefore(" ·")
    Row(
        modifier.fillMaxWidth().outlined(c.card, c.outline, 12.dp, 1.dp).pressable(onClick = onOpen)
            .padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.BatteryChargingFull, null, tint = c.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Keep Yaad working on $phone", style = YaadType.key, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Battery settings can stop auto-pick. 1 min fix →", style = YaadType.small, color = c.subtext, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(36.dp).pressable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, "Hide battery tip", tint = c.subtext, modifier = Modifier.size(18.dp))
        }
    }
}
