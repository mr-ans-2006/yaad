package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.components.BrutalIconButton
import io.github.mrans2006.yaad.ui.components.TagChip
import io.github.mrans2006.yaad.ui.theme.ThemeMode
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

data class SettingsState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val total: Int = 0,
    val failed: Int = 0,
    val autoPickOn: Boolean = true,
    val batteryOk: Boolean = false,
    val version: String = "1.0",
)

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onTheme: (ThemeMode) -> Unit = {},
    val onBatteryGuide: () -> Unit = {},
    val onScanNow: () -> Unit = {},
    val onRetryFailed: () -> Unit = {},
)

@Composable
fun SettingsScreen(state: SettingsState, actions: SettingsActions) {
    Column(
        Modifier.fillMaxSize().background(Yaad.colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions.onBack)
            Spacer(Modifier.width(14.dp))
            Text("Settings", style = YaadType.heading, color = Yaad.colors.text)
        }
        Spacer(Modifier.height(22.dp))

        Section("Look") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(ThemeMode.SYSTEM to "Auto", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                    TagChip(label, Yaad.colors.card, selected = state.theme == mode, onClick = { actions.onTheme(mode) })
                }
            }
        }

        Section("Auto-pick") {
            Text(
                when {
                    !state.autoPickOn -> "Off: Yaad has no photo access. Shared images still work."
                    state.batteryOk -> "On, and battery saver won't stop it. \uD83D\uDC4D"
                    else -> "On, but the battery saver may pause it. Yaad still catches up every time you open it."
                },
                style = YaadType.body, color = Yaad.colors.text,
            )
            Spacer(Modifier.height(12.dp))
            BrutalButton("Battery guide", actions.onBatteryGuide, icon = Icons.Rounded.BatteryChargingFull,
                modifier = Modifier)
            Spacer(Modifier.height(10.dp))
            BrutalButton("Check for new screenshots", actions.onScanNow, icon = Icons.Rounded.Refresh,
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
        }

        Section("Your library") {
            Text("${state.total} screenshots remembered" + if (state.failed > 0) ", ${state.failed} couldn't be read." else ".",
                style = YaadType.body, color = Yaad.colors.text)
            if (state.failed > 0) {
                Spacer(Modifier.height(12.dp))
                BrutalButton("Try the failed ones again", actions.onRetryFailed, primary = true, modifier = Modifier.padding(end = 3.dp, bottom = 3.dp))
            }
        }

        Section("Privacy") {
            Text("Everything happens on this phone: reading text, tagging and search. Yaad has no internet permission, " +
                "so it physically cannot upload anything. No account, no server, no tracking. The code is open source.",
                style = YaadType.body, color = Yaad.colors.text)
        }

        Section("About") {
            Text("Yaad ${state.version} by Ali Nawaz", style = YaadType.bodyBold, color = Yaad.colors.text)
            Spacer(Modifier.height(4.dp))
            Text("Open source (MIT). Text reading by Google ML Kit (on-device). Search model: all-MiniLM-L6-v2 (Apache 2.0). " +
                "Fonts: Inter and Noto Sans Arabic (SIL Open Font License).", style = YaadType.small, color = Yaad.colors.subtext)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), style = YaadType.label, color = Yaad.colors.subtext)
    Spacer(Modifier.height(8.dp))
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
        Column(Modifier.padding(14.dp)) { content() }
    }
    Spacer(Modifier.height(22.dp))
}
