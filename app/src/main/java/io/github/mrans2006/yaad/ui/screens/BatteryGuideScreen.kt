package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.core.BatteryGuide
import io.github.mrans2006.yaad.core.BatteryGuides
import io.github.mrans2006.yaad.core.GuideButton
import io.github.mrans2006.yaad.core.GuideStep
import io.github.mrans2006.yaad.core.PhoneBrand
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.theme.Palette
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

data class BatteryActions(
    val onBack: () -> Unit = {},
    /** A guide button was tapped. MainActivity tries each Settings screen it lists. */
    val onButton: (GuideButton) -> Unit = {},
    /** Opens dontkillmyapp.com in the phone's browser (Yaad itself never goes online). */
    val onMoreHelp: () -> Unit = {},
)

/** Each phone family gets its own accent colour, so the guides are easy to tell apart. */
fun PhoneBrand.accent(): Color = when (this) {
    PhoneBrand.SAMSUNG -> Palette.Blue
    PhoneBrand.XIAOMI -> Palette.Orange
    PhoneBrand.VIVO -> Palette.Teal
    PhoneBrand.TRANSSION -> Palette.Pink
    PhoneBrand.OPPO -> Palette.Green
    PhoneBrand.GENERIC -> Palette.Purple
}

/**
 * Phone makers add their own battery savers that close apps in the background.
 * This screen shows the steps for YOUR phone first (worked out from the phone's
 * maker name), then lets you open the steps for any other phone below.
 *
 * @param myBrand the phone family this phone belongs to (see PhoneBrand.detect).
 * @param openBrand which guide under "Other phones" starts opened (used for screenshots).
 */
@Composable
fun BatteryGuideScreen(
    batteryOk: Boolean,
    myBrand: PhoneBrand,
    actions: BatteryActions,
    openBrand: PhoneBrand? = null,
) {
    // Which "Other phones" card is open right now (only one at a time). Survives rotation.
    var opened by rememberSaveable { mutableStateOf(openBrand) }
    val mine = BatteryGuides.forBrand(myBrand)

    Column(
        Modifier.fillMaxSize().background(Yaad.colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions.onBack)
            Spacer(Modifier.width(14.dp))
            Text("STAY AWAKE", style = YaadType.heading, color = Yaad.colors.text)
        }
        Spacer(Modifier.height(18.dp))

        StatusBox(batteryOk, myBrand)
        Spacer(Modifier.height(10.dp))
        Text("Even if you skip this, Yaad catches up every time you open it. Nothing is lost.",
            style = YaadType.body, color = Yaad.colors.subtext)
        Spacer(Modifier.height(20.dp))

        // ---- Your phone ----
        BrandHeader(mine, label = "YOUR PHONE")
        Spacer(Modifier.height(8.dp))
        Text(mine.intro, style = YaadType.body, color = Yaad.colors.subtext)
        Spacer(Modifier.height(16.dp))
        mine.steps.forEachIndexed { i, step -> Step(i + 1, step, myBrand.accent(), actions.onButton) }

        // ---- Other phones ----
        Spacer(Modifier.height(6.dp))
        Text("OTHER PHONES", style = YaadType.heading, color = Yaad.colors.text)
        Spacer(Modifier.height(4.dp))
        Text("Not your phone, or helping a friend? Tap a brand to see its steps.",
            style = YaadType.body, color = Yaad.colors.subtext)
        Spacer(Modifier.height(14.dp))
        BatteryGuides.ordered(myBrand).drop(1).forEach { guide ->
            OtherPhone(
                guide = guide,
                isOpen = opened == guide.brand,
                onToggle = { opened = if (opened == guide.brand) null else guide.brand },
                onButton = actions.onButton,
            )
        }

        // ---- More help ----
        BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = Yaad.colors.card) {
            Column(Modifier.padding(14.dp)) {
                Text("Still stopping?", style = YaadType.title, color = Yaad.colors.text)
                Spacer(Modifier.height(4.dp))
                Text("dontkillmyapp.com has detailed, up-to-date steps for almost every phone. It opens in your browser; Yaad itself never goes online.",
                    style = YaadType.body, color = Yaad.colors.subtext)
                Spacer(Modifier.height(12.dp))
                BrutalButton("Open dontkillmyapp.com", actions.onMoreHelp, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatusBox(batteryOk: Boolean, brand: PhoneBrand) {
    // Android can only tell us about ITS battery optimisation. Brands like Xiaomi or Vivo
    // have extra switches of their own that apps can't read, so we say so honestly.
    val okText = if (brand == PhoneBrand.GENERIC) "Battery saver is off for Yaad. Auto-pick can run in the background."
    else "Android's battery saver is off for Yaad. ${brand.label.substringBefore(" ·")} has its own extra switches too, so check the steps below."
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = if (batteryOk) Palette.Green else Palette.Pink) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (batteryOk) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = Palette.Ink, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (batteryOk) okText
                else "Battery saver is still on for Yaad. New screenshots may wait until you open the app.",
                style = YaadType.bodyBold, color = Palette.Ink,
            )
        }
    }
}

/** The coloured "Samsung · One UI" label at the top of a guide. */
@Composable
private fun BrandHeader(guide: BatteryGuide, label: String) {
    BrutalBox(Modifier.padding(end = 4.dp, bottom = 4.dp), fill = guide.brand.accent(), radius = 10.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PhoneAndroid, null, tint = Palette.Ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = YaadType.small, color = Palette.Ink)
                Text("${guide.brand.label} · ${guide.brand.system}", style = YaadType.bodyBold, color = Palette.Ink)
            }
        }
    }
}

/** A brand under "Other phones": a bar you tap to open or close its steps. */
@Composable
private fun OtherPhone(guide: BatteryGuide, isOpen: Boolean, onToggle: () -> Unit, onButton: (GuideButton) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        BrutalBox(
            Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp).clickable(onClick = onToggle),
            fill = if (isOpen) guide.brand.accent() else Yaad.colors.card,
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(guide.brand.label, style = YaadType.title, color = if (isOpen) Palette.Ink else Yaad.colors.text)
                    Text(guide.brand.system, style = YaadType.small, color = if (isOpen) Palette.Ink else Yaad.colors.subtext)
                }
                Icon(
                    if (isOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    if (isOpen) "Hide steps" else "Show steps",
                    tint = if (isOpen) Palette.Ink else Yaad.colors.text,
                )
            }
        }
        if (isOpen) {
            Spacer(Modifier.height(12.dp))
            Text(guide.intro, style = YaadType.body, color = Yaad.colors.subtext, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(12.dp))
            guide.steps.forEachIndexed { i, step -> Step(i + 1, step, guide.brand.accent(), onButton) }
        }
    }
}

@Composable
private fun Step(n: Int, step: GuideStep, accent: Color, onButton: (GuideButton) -> Unit) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 18.dp)) {
        Row(Modifier.padding(14.dp)) {
            BrutalBox(fill = accent, shadow = 0.dp, radius = 8.dp) {
                Text("$n", style = YaadType.heading, color = Palette.Ink, textAlign = TextAlign.Center,
                    modifier = Modifier.width(38.dp).padding(vertical = 2.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(step.title, style = YaadType.title, color = Yaad.colors.text)
                Spacer(Modifier.height(4.dp))
                Text(step.body, style = YaadType.body, color = Yaad.colors.subtext)
                step.buttons.forEachIndexed { i, button ->
                    Spacer(Modifier.height(if (i == 0) 12.dp else 10.dp))
                    BrutalButton(
                        button.label, { onButton(button) },
                        fill = if (i == 0) Palette.Yellow else Yaad.colors.card,
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                    )
                }
            }
        }
    }
}
