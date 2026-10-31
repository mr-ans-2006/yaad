package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.theme.Palette
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

data class BatteryActions(
    val onBack: () -> Unit = {},
    val onOpenBatterySettings: () -> Unit = {},
    val onOpenAppInfo: () -> Unit = {},
    val onOpenAutoLaunch: () -> Unit = {},
)

/**
 * Oppo's ColorOS kills background apps hard. This screen walks you through the
 * settings that let Yaad notice screenshots while it's closed.
 */
@Composable
fun BatteryGuideScreen(batteryOk: Boolean, actions: BatteryActions) {
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

        BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), fill = if (batteryOk) Palette.Green else Palette.Pink) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (batteryOk) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = Palette.Ink, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    if (batteryOk) "Battery saver is off for Yaad. Auto-pick can run in the background."
                    else "Battery saver is still on for Yaad. New screenshots may wait until you open the app.",
                    style = YaadType.bodyBold, color = Palette.Ink,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Even if you skip this, Yaad catches up every time you open it. Nothing is lost.",
            style = YaadType.body, color = Yaad.colors.subtext)
        Spacer(Modifier.height(20.dp))

        Step(1, "Let Yaad run in the background",
            "Settings → Battery → More settings → Optimise battery use → All apps → Yaad → Don't optimise.\n" +
                "On some ColorOS versions: App info → Battery usage → turn on \u201CAllow background activity\u201D.") {
            BrutalButton("Open battery settings", actions.onOpenBatterySettings, fill = Palette.Yellow, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
            Spacer(Modifier.height(10.dp))
            BrutalButton("Open Yaad's app info", actions.onOpenAppInfo, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
        }
        Step(2, "Allow auto launch",
            "Settings → Apps → Auto launch → turn on Yaad. This lets Android wake Yaad when a new screenshot appears.") {
            BrutalButton("Try to open auto launch", actions.onOpenAutoLaunch, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
        }
        Step(3, "Lock Yaad in Recents",
            "Open the recent apps view, tap the ⋮ (or pull down) on Yaad's card and choose Lock. Cleaning up recents then won't close it.") {}
    }
}

@Composable
private fun Step(n: Int, title: String, body: String, buttons: @Composable () -> Unit) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 18.dp)) {
        Row(Modifier.padding(14.dp)) {
            BrutalBox(fill = Palette.Purple, shadow = 0.dp, radius = 8.dp) {
                Text("$n", style = YaadType.heading, color = Palette.Ink, textAlign = TextAlign.Center,
                    modifier = Modifier.width(38.dp).padding(vertical = 2.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = YaadType.title, color = Yaad.colors.text)
                Spacer(Modifier.height(4.dp))
                Text(body, style = YaadType.body, color = Yaad.colors.subtext)
                Spacer(Modifier.height(12.dp))
                buttons()
            }
        }
    }
}
